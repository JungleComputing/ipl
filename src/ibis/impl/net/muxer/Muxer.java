package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetSendBuffer;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetIbisException;
import ibis.ipl.impl.net.NetConnection;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * The UDP Multiplexer output implementation.
 *
 * <BR><B>Note</B>: this first implementation does not use UDP broadcast capabilities.
 */
public final class Muxer extends NetBufferedOutput {

    /**
     * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer		rpn    = null;

    private MuxerKey		myKey;


    private static ibis.ipl.impl.net.NetDriver	subDriver;
    private static MuxerOutput	muxer;


    private long		seqno;	/* For out-of-order debugging */


    /**
     * Constructor.
     *
     * @param sp the properties of the output's 
     * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
     * @param driver the TCP driver instance.
     */
    Muxer(NetPortType pt, NetDriver driver, String context)
	    throws NetIbisException {
	super(pt, driver, context);

	synchronized (driver) {
	    if (subDriver == null) {
		// String subDriverName = getMandatoryProperty("Driver");
		// System.err.println("subDriverName = " + subDriverName);
		System.err.println("It should depend on Driver properties which muxer suboutput is created");
		String subDriverName = "muxer.udp";
		subDriver = driver.getIbis().getDriver(subDriverName);
		System.err.println("The subDriver is " + subDriver);
		muxer = (MuxerOutput)newSubOutput(subDriver, "muxer");
	    }
	}
    }


    /**
     * {@inheritDoc}
     */
    public void setBufferFactory(NetBufferFactory factory) {
	if (Driver.DEBUG) {
	    System.err.println(this + ": +++++++++++ set a new BufferFactory " + factory);
	    Thread.dumpStack();
	}
	this.factory = factory;
	if (Driver.DEBUG) {
	    dumpBufferFactoryInfo();
	}
    }


    /*
     * Sets up an outgoing connection over the underlying MuxerOutput.
     *
     * <BR><B>Note</B>: this function also negociate the mtu.
     * <BR><B>Note</B>: the current UDP mtu is arbitrarily fixed at 32kB.
     *
     * @param rpn {@inheritDoc}
     * @param is {@inheritDoc}
     * @param os {@inheritDoc}
     */
    public void setupConnection(NetConnection cnx)
		throws NetIbisException {
	if (Driver.DEBUG) {
	    System.err.println(this + ": setup connection, serviceLink " + cnx.getServiceLink());
	}

	if (rpn != null) {
	    throw new Error(Thread.currentThread() + ": " + this + ": serviceLink " + cnx.getServiceLink() + " -- connection already established");
	}                

	rpn = cnx.getNum();

	muxer.setupConnection(cnx, (NetIO)this);

	headerOffset = muxer.getHeaderLength();
	headerLength = headerOffset;
	if (Driver.DEBUG) {
	    headerLength += NetConvert.LONG_SIZE;
	}

	mtu = muxer.getMaximumTransfertUnit();
	if (factory == null) {
	    factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
	} else {
	    factory.setMaximumTransferUnit(mtu);
	}
	myKey = muxer.getKey(cnx);

	int ok = 0;
	try {
	    ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "muxer"));
	    ok = is.readInt();
	    is.close();
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}
	if (ok != 1) {
	    throw new NetIbisException("Connection handshake failed");
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": new output connection established, mtu " + mtu);
	}
    }


    /**
     * {@inheritDoc}
     */
    public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {

	if (Driver.DEBUG) {
	    System.err.println(this + ": try to send buffer size " + b.length);
	}
	b.connectionId = myKey;
	NetConvert.writeInt(myKey.remoteKey, b.data, b.base + Driver.KEY_OFFSET);
	if (ibis.ipl.impl.net.muxer.Driver.PACKET_SEQNO) {
	    NetConvert.writeLong(myKey.seqno++, b.data,
				 b.base + Driver.SEQNO_OFFSET);
	}

	muxer.writeByteBuffer(b);
    }

    /**
     * {@inheritDoc}
     */
    public void release() {
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
    }


    /**
     * {@inheritDoc}
     */
    synchronized public void close(Integer num) throws NetIbisException {
	if (rpn == num) {
	    mtu   =    0;
	    rpn   = null;

	    muxer.disconnect(myKey);
	    myKey.free();
	}
    }


    /**
     * {@inheritDoc}
     */
    public void free() throws NetIbisException {
	if (rpn == null) {
	    return;
	}

	close(rpn);

	super.free();
    }
}
