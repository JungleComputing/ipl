package ibis.ipl.impl.net.muxer;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetBufferFactory;


public class MuxerQueue extends MuxerKey {

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


    /**
     * @Constructor
     *
     * Create the queue to receive messages from the global demultiplexer.
     *
     * @param spn the (globally unique?) Integer that characterizes our
     *        connection.
     */
    public MuxerQueue(Integer spn) {
	super(spn);
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
	    throws IbisIOException {

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
	    System.err.println("z");
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
	    throws IbisIOException {
	if (userBuffer != null) {
	    throw new IbisIOException("Racey downcall receive");
	}
	userBuffer = buffer;

	return dequeue();
    }


    synchronized
    public Integer poll() {
	if (false && Driver.DEBUG) {
	    System.err.println(this + ": poll; front " + front + " spn " + spn);
	    if (false && spn.intValue() == 0) {
		Thread.dumpStack();
	    }
	}
	if (front == null) {
	    return null;
	}

	return spn;
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
    synchronized
    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
	    throws IbisIOException {

	if (Driver.DEBUG) {
	    System.err.println("Downcall receive q " + this + ": start dequeue");
	}
	NetReceiveBuffer buffer = dequeue();
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
    synchronized
    public void receiveByteBuffer(NetReceiveBuffer userBuffer)
	    throws IbisIOException {

	if (Driver.DEBUG) {
	    System.err.println("Post downcall receive q " + this + " userBuffer " + userBuffer + ": start dequeue");
	}
	NetReceiveBuffer buffer = dequeue(userBuffer);
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

}
