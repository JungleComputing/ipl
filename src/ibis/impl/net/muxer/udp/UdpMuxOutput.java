package ibis.ipl.impl.net.muxer.udp;

import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetServiceListener;
import ibis.ipl.impl.net.NetSendBuffer;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetBufferFactory;

import ibis.ipl.impl.net.muxer.MuxerOutput;
import ibis.ipl.impl.net.muxer.MuxerKey;

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
 * The UDP output implementation.
 *
 * <BR><B>Note</B>: this first implementation does not use UDP broadcast capabilities.
*/
public final class UdpMuxOutput
	extends MuxerOutput {

    /**
     * The UDP socket.
     */
    private DatagramSocket socket = null;

    /**
     * The UDP message wrapper.
     */
    private DatagramPacket packet = null;
    /**
     * The local socket IP address.
     */
    private InetAddress		laddr  = null;

    /**
     * The local socket IP port.
     */
    private int			lport  =    0;

    /**
     * The local MTU.
     */
    private int			lmtu   =    0;


    private int			liveConnections;



    /**
     * Constructor.
     *
     * @param sp the properties of the output's 
     * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
     * @param driver the TCP driver instance.
     * @param output the controlling output.
     */
    public UdpMuxOutput(NetPortType portType,
		        NetDriver   driver,
		        NetIO       up,
		        String      context) throws IbisIOException {

	super(portType, driver, up, context);

	try {
	    socket = new DatagramSocket(0, InetAddress.getLocalHost());
	    lmtu   = Math.min(socket.getSendBufferSize(), 32768);//TOCHANGE
	    laddr  = socket.getLocalAddress();
	    lport  = socket.getLocalPort();
	} catch (SocketException e) {
	    throw new IbisIOException(e);
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}

	packet = new DatagramPacket(new byte[0], 0, null, 0);

	System.err.println("*********************** Now should call postConstructor");
	// postConstructor();
    }


    /*
     * Sets up an outgoing multiplexed connection.
     *
     * <BR><B>Note</B>: this function also negociate the mtu.
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

	liveConnections++;
	try {
	    InetAddress raddr = (InetAddress)is.readObject();
	    int rport         = is.readInt();
	    int rmtu          = is.readInt();
	    int rKey          = is.readInt();

	    MuxerKey key = new UdpMuxerKey(rpn, raddr, rport, rKey);
	    registerKey(key);

	    os.writeObject(laddr);
	    os.writeInt(lport);
	    os.writeInt(lmtu);
	    os.writeInt(rpn.intValue());
	    os.flush();

	    mtu    = Math.min(lmtu, rmtu);

	} catch (ClassNotFoundException e) {
	    throw new IbisIOException(e);
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}
    }


    public void disconnect(MuxerKey key) throws IbisIOException {
	releaseKey(key);
	if (--liveConnections == 0) {
	    free();
	}
    }


    /**
     * {@inheritDoc}
     */
    synchronized
    public void sendByteBuffer(NetSendBuffer b)
	    throws IbisIOException {

	int lKey = b.connectionId.intValue();
	UdpMuxerKey uk = (UdpMuxerKey)locateKey(lKey);

	if (Driver.DEBUG) {
	    System.err.println("Send packet, key " + lKey + " is " + uk);
	    System.err.println("Send packet size " + b.length + " key " + uk.remoteKey + "@" + b.base);
	}
	NetConvert.writeInt(uk.remoteKey, b.data, b.base);

	packet.setAddress(uk.remoteAddress);
	packet.setPort(uk.remotePort);
	packet.setData(b.data, 0, b.length);
// System.err.print("|");
	try {
	    socket.send(packet);
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}
	if (! b.ownershipClaimed) {
	    b.free();
	}
    }


    public void free() throws IbisIOException {
	socket.close();
	socket = null;
    }


}
