package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetConnection;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetReceiveBufferFactoryDefaultImpl;
import ibis.ipl.impl.net.NetIbisException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4
* import java.net.SocketTimeoutException;
*/
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * The Multiplexer input implementation.
 */
public final class Demuxer extends NetBufferedInput {

    private Integer		spn	     = null;

    private MuxerQueue		myQueue;


    private long		rcve_seqno;	/* For out-of-order debugging */
    private long		deliver_seqno;	/* For out-of-order debugging */


    private static ibis.ipl.impl.net.NetDriver	subDriver;
    private static MuxerInput	demux;


    /**
     * Constructor.
     *
     * {@inheritDoc}
     */
    Demuxer(NetPortType pt, NetDriver driver, String context)
	    throws NetIbisException {
	super(pt, driver, context);

	synchronized (driver) {
	    if (subDriver == null) {
		// String subDriverName = getMandatoryProperty("Driver");
		System.err.println("It should depend on Driver properties which muxer subinput is created");
		String subDriverName = "muxer.udp";
		subDriver = driver.getIbis().getDriver(subDriverName);
		System.err.println("The subDriver is " + subDriver);
		demux = (MuxerInput)newSubInput(subDriver, "muxer");
	    }
	}
    }


    /**
     * {@inheritDoc}
     *
     * Call this when the connection has been established, and the demuxer
     * queue is set. After we have set the buffer factory in our queue, it may
     * start delivering.
     */
    public void setBufferFactory(NetBufferFactory factory) {
	if (Driver.DEBUG) {
	    System.err.println(this + ": ++++++++++++++++++++++++++ set a new BufferFactory " + factory);
	    Thread.dumpStack();
	}
	// super.setBufferFactory(factory);
	// this.factory = factory;
	/* Pass on our factory to the global demuxer. Hopefully our factory
	 * generates a buffer class that is widely used. */
	demux.setBufferFactory(factory);
dumpBufferFactoryInfo();
	myQueue.setBufferFactory(factory);
    }


    /**
     * Sets up a connection over the underlying DemuxerInput.
     */
    /*
     * Because a blocking poll can be pending while we want
     * to connect, the ReceivePort's inputLock cannot be taken
     * during a connect.
     * This implies that the blocking poll _and_ setupConnection
     * must protect the data structures.
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws NetIbisException {
	if (Driver.DEBUG) {
	    System.err.println(this + ": setup connection, serviceLink " + cnx.getServiceLink());
	}

	if (this.spn != null) {
	    throw new Error(Thread.currentThread() + ": " + this + ": serviceLink " + cnx.getServiceLink() + " -- connection already established");
	}

	/* Set up the connection; it creates a MuxerQueue that is remembered
	 * by our cnx */
	demux.setupConnection(cnx, (NetIO)this);
	/* Drag up the MuxerQueue that demux has created for us. It is
	 * remembered by our cnx. */
	myQueue = demux.locateQueue(cnx);

	if (Driver.DEBUG) {
	    System.err.println(this + ": Input connect spn " + spn + " creates queue " + myQueue);
	    Thread.dumpStack();
	}

	headerOffset = demux.getHeaderLength();
	headerLength = headerOffset;
	if (Driver.DEBUG) {
	    headerLength += NetConvert.LONG_SIZE;
	}
	dataOffset = headerLength;

	mtu = demux.getMaximumTransfertUnit();

	if (factory == null) {
	    factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl());
	} else {
	    factory.setMaximumTransferUnit(mtu);
	}
	demux.startQueue(myQueue, factory);

	try {
	    /* The demuxer has reset its buffers to a sufficient size. Let the
	     * other side start sending if it feels like it. */
	    ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "muxer"));
	    os.writeInt(1);
	    os.close();
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": my queue is " + myQueue);
	}

	this.spn = cnx.getNum();
        startUpcallThread();
    }


    private void checkReceiveSeqno(NetReceiveBuffer buffer) {
	if (Driver.DEBUG) {
	    long seqno = NetConvert.readLong(buffer.data,
					headerOffset + NetConvert.INT_SIZE);
	    if (seqno < rcve_seqno) {
		System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Receive: packet overtakes: " + seqno + " expect " + rcve_seqno);
	    } else {
		rcve_seqno = seqno;
	    }
	}
    }


    private void checkDeliverSeqno(NetReceiveBuffer buffer) {
	if (Driver.DEBUG) {
	    long seqno = NetConvert.readLong(buffer.data,
					headerOffset + NetConvert.INT_SIZE);
	    if (seqno < deliver_seqno) {
		System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Deliver: packet overtakes: " + seqno + " expect " + deliver_seqno);
	    } else {
		deliver_seqno = seqno;
	    }
	}
    }

    protected void initReceive() {
        //
    }
    

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This UDP polling implementation uses a timed out
     * {@link DatagramSocket#receive(DatagramPacket)}. As the minimum timeout value is one
     * millisecond, an unsuccessful polling operation is rather expensive.
     *
     * @return {@inheritDoc}
     */
    public Integer doPoll(boolean block) throws NetIbisException {
	if (myQueue == null) {
	    System.err.println(this + ": This CANNOT be true ... setupConnection does a handshake, right?");
	    // Still connecting, presumably
	    return null;
	}

	return myQueue.poll(block);
    }


    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: this function may block if the expected data is not there.
     * <BR><B>Note</B>: The expectedLength argument is simply ignored because the
     * packet actually received might not be the one that is expected.
     *
     * @return {@inheritDoc}
     */
    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
	    throws NetIbisException {

	NetReceiveBuffer b = myQueue.receiveByteBuffer(expectedLength);
	return b;
    }


    public void receiveByteBuffer(NetReceiveBuffer userBuffer)
	    throws NetIbisException {

	if (Driver.DEBUG_HUGE) {
	    System.err.println(this + ": receiveByteBuffer, my headerLength " + headerLength);
	    Thread.dumpStack();
	}
	myQueue.receiveByteBuffer(userBuffer);
    }


    /**
     * {@inheritDoc}
     */
    public void doFinish() throws NetIbisException {
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void doClose(Integer num) throws NetIbisException {
	if (Driver.DEBUG) {
	    System.err.println(this + ": close.");
	    Thread.dumpStack();
	}

	if (spn == num) {
	    spn = null;
	    demux.disconnect(myQueue);
	    myQueue.free();
	    if (upcallFunc != null) {
		// Yes, what? How do we stop this thread?
	    }
	}
    }


    /**
     * {@inheritDoc}
     */
    public void doFree() throws NetIbisException {
	if (spn == null) {
	    return;
	}

	close(spn);

	super.free();
    }

}
