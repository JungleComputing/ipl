package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetServiceListener;
import ibis.ipl.impl.net.NetSendBuffer;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.ipl.impl.net.NetConvert;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

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
    private int			myKeyVal;


    private static ibis.ipl.impl.net.NetDriver	subDriver;
    private static MuxerOutput	muxer;


    private long		seqno;	/* For out-of-order debugging */


    /**
     * Constructor.
     *
     * @param sp the properties of the output's 
     * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
     * @param driver the TCP driver instance.
     * @param output the controlling output.
     */
    Muxer(NetPortType pt, NetDriver driver, NetIO up, String context)
	    throws IbisIOException {
	super(pt, driver, up, context);

	if (subDriver == null) {
	    System.err.println("It should depend on Driver properties which muxer suboutput is created");
	    // String subDriverName = getMandatoryProperty("Driver");
	    String subDriverName = "muxer.udp";
	    subDriver = driver.getIbis().getDriver(subDriverName);
	    System.err.println("The subDriver is " + subDriver);
	    muxer = (MuxerOutput)subDriver.newOutput(null, null, null);
	}
    }


    /**
     * @{inheritDoc}
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
    public void setupConnection(Integer            rpn,
				ObjectInputStream  is,
				ObjectOutputStream os,
				NetServiceListener nls)
	    throws IbisIOException {
	if (this.rpn != null) {
	    throw new Error("connection already established");
	}                

	this.rpn = rpn;

	myKeyVal = rpn.intValue();

	muxer.setupConnection(rpn, is, os, nls);

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
	myKey = muxer.getKey(rpn);

	int ok = 0;
	try {
	    ok = is.readInt();
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}
	if (ok != 1) {
	    throw new IbisIOException("Connection handshake failed");
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": new output connection established, mtu " + mtu);
	}
    }


    /**
     * {@inheritDoc}
     */
    public void sendByteBuffer(NetSendBuffer b) throws IbisIOException {

	if (Driver.DEBUG) {
	    System.err.println(this + ": try to send buffer size " + b.length);
	}
	b.connectionId = rpn;

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
    public void free() throws IbisIOException {
	if (rpn == null) {
	    return;
	}

	mtu   =    0;
	rpn   = null;

	muxer.disconnect(myKey);

	super.free();
    }
}
