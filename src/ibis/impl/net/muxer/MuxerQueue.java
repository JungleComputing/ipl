package ibis.impl.net.muxer;

import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBufferFactory;

import java.io.IOException;


public class MuxerQueue {

    private final static boolean WAIT_YIELDING = false;
    private final static int	OPTIMISTIC_YIELDS = 0;

    protected MuxerInput	input;

    private NetReceiveBuffer	front;
    private NetReceiveBuffer	tail;

    private NetReceiveBuffer	userBuffer;

    private NetBufferFactory	factory;

    private int			waitingReceivers;

    private long		t_enqueue;
    private long		t_queued;
    private int			n_q;
    private int			n_q_wait;
    private int			n_q_present;

    private int			n_deq_wait;
    private int			n_poll_blocking;
    private int			n_poll_nonblocking;
    private int			n_poll_wait;
    private int			n_poll_yield;

    private Integer		spn;

    private Integer		activeInput;

    protected int		connectionKey;


    /**
     * @Constructor
     *
     * Create the queue to receive messages from the global demultiplexer.
     *
     * @param spn the (globally unique?) Integer that characterizes our
     *        connection.
     */
    public MuxerQueue(MuxerInput input, Integer spn) {
	this.input = input;
	this.spn   = spn;
	this.connectionKey = -1;
    }


    public int connectionKey() {
	return connectionKey;
    }

    public void setConnectionKey(int id) {
	this.connectionKey = id;
    }


    /**
     * @method
     *
     * Set the factory that generates the kind of buffers our Input driver
     * wants.
     */
    synchronized
    void setBufferFactory(NetBufferFactory factory) {
	if (Driver.DEBUG) {
	    System.err.println(this + ": +++++++++++++++++++++++++++ set buffer factory " + factory);
	    Thread.dumpStack();
	}
	this.factory = factory;
	// Connection setup has completed.
	// That is signalled by a nonnull factory.
	notifyAll();
    }


    /**
     * @method
     *
     * The global demultiplexer Driver delivers a message to this queue.
     *
     * @param buffer can be of arbitrary Class; if necessary, this method
     *        will convert it to the class that is required by our owning
     *        NetInput.
     */
    synchronized
    public void enqueue(NetReceiveBuffer buffer)
	    throws IOException {

	if (! factory.isSuitableClass(buffer)) {
	    if (true || Driver.DEBUG) {
		System.err.println(this + ": A pity upon enqueue: received buffer type does not match requested buffer, now transfer");
	    }
	    NetReceiveBuffer b =
		(NetReceiveBuffer)factory.createBuffer(buffer.data,
						       buffer.length,
						       buffer.allocator);
	    buffer.data = null;
	    buffer.allocator = null;
	    buffer.length = 0;
	    buffer.free();

	    if (Driver.DEBUG) {
		System.err.println(this + ": create new buffer " + b + " factory " + factory);
		System.err.println(this + ": enqueue; buffer.data " + b.data);
		System.err.println(this + ": enqueue; buffer.length " + b.length);
		System.err.println(this + ": enqueue; buffer.data.length " + b.data.length);
	    }

	    buffer = b;
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": enqueue buffer " + buffer + " length " + buffer.length);
	}

	if (front == null) {
	    front = buffer;
	} else {
	    tail.next = buffer;
	}
	tail = buffer;
	buffer.next = null;

	if (Driver.STATISTICS) {
	    t_enqueue = System.currentTimeMillis();
	    n_q++;
	}

	if (waitingReceivers > 0) {
	    notify();
	}
    }


    private NetReceiveBuffer dequeue() {
	if (Driver.DEBUG) {
	    System.err.println(this + ": enter dequeue, front = " + front);
	}
	if (Driver.STATISTICS) {
	    if (front == null) {
		n_q_wait++;
	    } else {
		n_q_present++;
	    }
	}
	while (front == null) {
	    // System.err.println("z");
	    if (Driver.STATISTICS) {
		n_deq_wait++;
	    }
	    waitingReceivers++;
	    try {
		wait();
	    } catch (InterruptedException e) {
		// Just continue
	    }
	    waitingReceivers--;
	}

	if (Driver.STATISTICS) {
	    long now = System.currentTimeMillis();
	    t_queued += now - t_enqueue;
	    if (Driver.DEBUG && (n_q % 1000) == 0) {
		System.err.println("<Queue delay> " + n_q + " " + (t_queued / (1000.0 * n_q)) + " s");
		System.err.println("Dequeue: present " + n_q_present + " wait " + n_q_wait);
	    }
	    if (now - t_enqueue > 5) {
		// System.err.println("Queue delay " + (now - t_enqueue) / 1000.0 + " s");
	    }
	}

	NetReceiveBuffer buffer = front;
	front = (NetReceiveBuffer)front.next;

	if (Driver.DEBUG) {
	    System.err.println(this + ": dequeue buffer " + buffer + " length " + buffer.length + " data " + buffer.data + " data.length " + buffer.data.length);
	}

	if (userBuffer != null) {
	    if (Driver.DEBUG) {
		System.err.println(this + ": have a registered userBuffer " + userBuffer);
	    }
	    userBuffer = null;
	}

	return buffer;
    }


    private NetReceiveBuffer dequeue(NetReceiveBuffer buffer)
	    throws IOException {
	if (userBuffer != null) {
	    throw new IOException("Racey downcall receive");
	}
	userBuffer = buffer;

	return dequeue();
    }


    public Integer poll(boolean block) throws IOException {
	if (false && Driver.DEBUG) {
	    System.err.println(this + ": poll; front " + front + " key " + connectionKey);
	    if (false && connectionKey == 0) {
		Thread.dumpStack();
	    }
	}
if (false && ! block) {
System.err.println(this + ": Nonblocking poll");
Thread.dumpStack();
}

	if (activeInput != null) {
	    throw new IOException(this + ": call finish before a new poll()");
	}

	if (! MuxerInput.USE_POLLER_THREAD) {
	    try {
		while (front == null && input.attemptPoll(block) != null) {
		    // perform this poll
		}
	    } catch (IOException e) {
		// Ignore; if this fails, just continue on the normal route.
	    }
	}

	if (Driver.STATISTICS) {
	    if (block) {
		n_poll_blocking++;
	    } else {
		n_poll_nonblocking++;
	    }
	}

	if (! WAIT_YIELDING) {
	    for (int i = 0; i < OPTIMISTIC_YIELDS && block && front == null; i++) {
		Thread.yield();
		if (Driver.STATISTICS) {
		    n_poll_yield++;
		}
	    }
	    if (block && front == null) {
		synchronized (this) {
		    while (block && front == null) {
			if (Driver.STATISTICS) {
			    n_poll_wait++;
			}
			// System.err.println("z");
			waitingReceivers++;
			try {
			    wait();
			} catch (InterruptedException e) {
			    // Just continue
			}
			waitingReceivers--;
		    }
		}
	    }
	} else {
	    if (Driver.STATISTICS) {
		if (block && front == null) {
		    n_poll_wait++;
		}
	    }
	    while (block && front == null) {
		/*
		synchronized (this) {
		    waitingReceivers++;
		    waitingReceivers--;
		}
		*/
		Thread.yield();
		if (Driver.STATISTICS) {
		    n_poll_yield++;
		}
	    }
	}

	if (front != null) {
	    activeInput = spn;
	}

	return activeInput;
    }


    /**
     * @method
     *
     * <BR><B>Note</B>: this function may block if the expected data is not there.
     * @param expectedLength is ignored because the packet actually received
     *        might not be the one that is expected.
     *
     * @return the first delivered buffer
     */
    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
	    throws IOException {

	if (Driver.DEBUG) {
	    System.err.println("Downcall receive q " + this + ": start dequeue");
	}

	if (! MuxerInput.USE_POLLER_THREAD) {
	    try {
		while (front == null && input.attemptPoll(true) != null) {
		    // perform this poll
		}
	    } catch (IOException e) {
		// Ignore; if this fails, just continue on the normal route.
	    }
	}

	NetReceiveBuffer buffer;
	synchronized (this) {
	    buffer = dequeue();
	}
	if (Driver.DEBUG) {
	    System.err.println("Downcall receive q " + this + ": after dequeue; buffer " + buffer + " buffer.data " + buffer.data + " buffer.length " + buffer.length + " buffer.data.length " + buffer.data.length);
	}
	return buffer;
    }


    /**
     * @method
     *
     * <BR><B>Note</B>: this function may block if the expected data is not there.
     * @param userBuffer Receive data into this buffer. In the general case
     *        a copy is incurred.
     */
    public void receiveByteBuffer(NetReceiveBuffer userBuffer)
	    throws IOException {

	if (Driver.DEBUG) {
	    System.err.println("Post downcall receive q " + this + " userBuffer " + userBuffer + ": start dequeue");
	}

	if (! MuxerInput.USE_POLLER_THREAD) {
	    try {
		while (front == null && input.attemptPoll(true) != null) {
		    // perform this poll
		}
	    } catch (IOException e) {
		// Ignore; if this fails, just continue on the normal route.
	    }
	}

	NetReceiveBuffer buffer;
	synchronized (this) {
	    buffer = dequeue(userBuffer);
	}
	if (buffer != userBuffer) {
	    // The message was delivered before we could post our buffer.
	    // Incur a copy.
	    if (Driver.DEBUG) {
		System.err.println(this + ": blocking receive q " + this + " in nonposted buffer, copy the thing; length " + buffer.length + " base " + buffer.base + " data.length " + buffer.data.length + " userBuffer.base " + userBuffer.base + " userBuffer.data.length " + (userBuffer.data.length - userBuffer.base));
	    }
	    System.arraycopy(buffer.data, 0,
			     userBuffer.data, userBuffer.base,
			     buffer.length);
	    userBuffer.length = buffer.length;
	    buffer.free();
	}
    }


    public void doFinish() throws IOException {
	if (activeInput == null) {
	    throw new IOException(this + ": call poll before you finish");
	}
	activeInput = null;
    }


    public void free() throws IOException {
	if (Driver.STATISTICS) {
	    System.err.println(this + ": #enqueue " + n_q +
		    "; #wait(poll) " + n_poll_wait +
		    "; #wait(dequeue) " + n_deq_wait +
		    "; t(wait) " + ((1000.0 * t_queued) / n_q)  + " us" +
		    "; poll/block " + n_poll_blocking +
			" /nonblock " + n_poll_nonblocking +
		    "; yield " + n_poll_yield);
	}
    }

}
