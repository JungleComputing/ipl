package ibis.ipl.impl.net.muxer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.generic.Monitor;
import ibis.ipl.impl.generic.ConditionVariable;

import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetServiceListener;

public abstract class MuxerInput extends NetBufferedInput implements Runnable {

    private Thread		poller;

    protected int		max_mtu       =    0;
    protected int		min_mtu       =    Integer.MAX_VALUE;

    private int			max_ever_mtu = -1;

    private int			liveConnections;

    private int			upcallReceivers;

    private final static int	defaultPollTimeout = 30;	// ms.
    private int			pollTimeout = defaultPollTimeout;



    /**
     * @constructor.
     *
     * Call this from all subclass constructors.
     */
    protected MuxerInput(NetPortType portType,
			 NetDriver   driver,
			 NetIO       up,
			 String      context) {
	super(portType, driver, up, context);
	mtu   	     =    0;
	headerLength = NetConvert.INT_SIZE;

	/* ... please be patient, we'll find out how setup works *
	String s = null;
	if ((s = getProperty("PollingTimeout")) != null) {
		pollTimeout = Integer.valueOf(s).intValue();
	}
	* but only ma~nana */

	poller = new Thread(this);
	poller.setDaemon(true);
	poller.setName("UDP multiplexer poller");
	poller.start();
    }


    /**
     * @method
     *
     * @param timeout poll timeout in msec. 0 signifies indefinite timeout.
     */
    abstract protected Integer poll(int timeout) throws IbisIOException;


    /**
     * @{inheritDoc}
     */
    public Integer poll() throws IbisIOException {
	return poll(pollTimeout);
    }


    /**
     * @method
     *
     * This should be called before setupConnection in the subclass, before
     * any communication takes place.
     */
    protected MuxerQueue createQueue(Integer spn) {
	MuxerQueue q = new MuxerQueue(spn);
	registerQueue(q);

	return q;
    }


    public void startQueue(MuxerQueue queue, NetBufferFactory factory)
	    throws IbisIOException {
	if (max_ever_mtu == -1) {
	    max_ever_mtu = max_mtu;
	} else if (max_mtu > max_ever_mtu) {
	    throw new IbisIOException("Cannot increase mtu beyond " + max_ever_mtu);
	}
	factory.setMaximumTransferUnit(max_ever_mtu);
	queue.setBufferFactory(factory);
	this.factory = factory;
	if (upcallReceivers++ == 0) {
	    synchronized (this) {
		notify();
	    }
	}
    }


    public void disconnect(MuxerQueue q) throws IbisIOException {
	if (Driver.DEBUG) {
	    Thread.dumpStack();
	    System.err.println("Now disconnect localQueue " + q.localKey() + " liveConnections was " + liveConnections());
	}
	releaseQueue(q);
	poller.interrupt();
    }


    private MuxerKeyHash keyHash = new MuxerKeyHash();

    synchronized
    protected void registerQueue(MuxerQueue q) {
	keyHash.registerKey(q);
	liveConnections++;
    }

    synchronized
    protected MuxerQueue locateQueue(int n) {
	return (MuxerQueue)keyHash.locateKey(n);
    }

    synchronized
    protected void releaseQueue(MuxerQueue key) throws IbisIOException {
	keyHash.releaseKey(key);
	liveConnections--;
	if (liveConnections == 0) {
	    free();
	}
    }


    protected int liveConnections() {
	return liveConnections;
    }


    /**
     * The poller thread.
     */
    public void run() {
	synchronized (this) {
	    while (upcallReceivers == 0) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    // Just go on waiting
		}
	    }
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": poller thread runs");
	}

	Thread me = Thread.currentThread();

	while (liveConnections > 0) {
	    try {
		if (Driver.DEBUG) {
		    System.err.println(this + ": poller thread does a blocking receive");
		}
		while (poll(0) == null) {
		    /* try again */
		}

		NetReceiveBuffer buffer = receiveByteBuffer(max_mtu);
		int rKey = NetConvert.readInt(buffer.data, buffer.base);
		MuxerQueue q = locateQueue(rKey);
		if (Driver.DEBUG) {
		    System.err.println("Receive downcall UDP packet len " + buffer.length + " data " + buffer.data + "; key " + rKey + " /bound to " + q);
		}
		q.enqueue(buffer);
	    } catch (Exception e) {
		System.err.println("************************ Poller thread handles exception " + e);
	    }
	}
    }

}
