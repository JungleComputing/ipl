package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetServiceListener;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetReceiveBufferFactoryDefaultImpl;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

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

import java.util.Hashtable;


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
     * @{inheritDoc}
     */
    Demuxer(NetPortType pt, NetDriver driver, NetIO up, String context)
	    throws IbisIOException {
	super(pt, driver, up, context);

	if (subDriver == null) {
	    System.err.println("It should depend on Driver properties which muxer subinput is created");
	    // String subDriverName = getMandatoryProperty("Driver");
	    String subDriverName = "muxer.udp";
	    subDriver = driver.getIbis().getDriver(subDriverName);
	    System.err.println("The subDriver is " + subDriver);
	    demux = (MuxerInput)subDriver.newInput(null, null, null);
	}
    }


    /**
     * @{inheritDoc}
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


    private final class UpcallThread extends Thread {

	    public void run() {
		while (true) {
		    try {
			NetReceiveBuffer buffer = createReceiveBuffer();
			myQueue.receiveByteBuffer(buffer);
			activeNum = spn;
			Demuxer.super.initReceive();
			upcallFunc.inputUpcall(Demuxer.this, activeNum);
			activeNum = null;
		    } catch (Exception e) {
			System.err.println("Upcall thread: " + e);
		    }
		}
	    }

    }


    /**
     * Sets up a connection over the underlying DemuxerInput.
     */
    public void setupConnection(Integer	       spn,
				ObjectInputStream  is,
				ObjectOutputStream os,
				NetServiceListener nls)
	    throws IbisIOException {
	if (Driver.DEBUG) {
	    System.err.println("Now enter Demuxer.setupConnection");
	}

	if (this.spn != null) {
	    throw new Error("connection already established");
	}

	this.spn = spn;

	/* Create our MuxerQueue */
	myQueue = demux.createQueue(spn);
	/* Set up the connection */
	demux.setupConnection(spn, is, os, nls);

System.err.println(this + ": Input connect spn " + spn + " creates queue " + myQueue);
Thread.dumpStack();

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
	    os.writeInt(1);
	    os.flush();
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": my queue is " + myQueue);
	}

	if (upcallFunc != null) {
	    (new UpcallThread()).start();
	}
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


    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This UDP polling implementation uses a timed out
     * {@link DatagramSocket#receive(DatagramPacket)}. As the minimum timeout value is one
     * millisecond, an unsuccessful polling operation is rather expensive.
     *
     * @return {@inheritDoc}
     */
    public Integer poll() throws IbisIOException {
	if (myQueue == null) {
	    // Still connecting, presumably
	    return null;
	}

	return myQueue.poll();
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
	    throws IbisIOException {

	NetReceiveBuffer b = myQueue.receiveByteBuffer(expectedLength);
	super.initReceive();
	return b;
    }


    public void receiveByteBuffer(NetReceiveBuffer userBuffer)
	    throws IbisIOException {

// System.err.println(this + ": receiveByteBuffer, my headerLength " + headerLength);
// Thread.dumpStack();
	myQueue.receiveByteBuffer(userBuffer);
	super.initReceive();
    }


    /**
     * {@inheritDoc}
     */
    public void finish() throws IbisIOException {
	super.finish();
    }


    /**
     * {@inheritDoc}
     */
    public void free() throws IbisIOException {
	if (spn == null) {
	    return;
	}

	spn = null;
	super.free();
	demux.disconnect(myQueue);
	if (upcallFunc != null) {
	    // Yes, what? How do we stop this thread?
	}
    }

}
