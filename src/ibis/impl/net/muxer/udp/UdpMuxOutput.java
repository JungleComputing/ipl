package ibis.ipl.impl.net.muxer.udp;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetConnection;
import ibis.ipl.impl.net.NetSendBuffer;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetIbisException;

import ibis.ipl.impl.net.muxer.MuxerOutput;
import ibis.ipl.impl.net.muxer.MuxerKey;

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


    private Integer		rpn    = null;


    private int			liveConnections;



    /**
     * Constructor.
     *
     * @param sp the properties of the output's 
     * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
     * @param driver the TCP driver instance.
     */
    public UdpMuxOutput(NetPortType portType,
		        NetDriver   driver,
		        String      context) throws NetIbisException {

	super(portType, driver, context);

	try {
	    socket = new DatagramSocket(0, InetAddress.getLocalHost());
	    lmtu   = Math.min(socket.getSendBufferSize(), 32768);//TOCHANGE
	    laddr  = socket.getLocalAddress();
	    lport  = socket.getLocalPort();
	} catch (SocketException e) {
	    throw new NetIbisException(e);
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}

	packet = new DatagramPacket(new byte[0], 0, null, 0);
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
    public void setupConnection(NetConnection cnx)
	    throws NetIbisException {

	liveConnections++;
	try {
	    rpn = cnx.getNum();

	    ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "muxer.udp-" + rpn));
	    InetAddress raddr = (InetAddress)is.readObject();
	    int rport         = is.readInt();
	    int rmtu          = is.readInt();
	    int rKey          = is.readInt();
	    is.close();

	    MuxerKey key = new UdpMuxerKey(rpn, raddr, rport, rKey);
	    registerKey(key);

	    ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "muxer.udp-" + rpn));
	    os.writeObject(laddr);
	    os.writeInt(lport);
	    os.writeInt(lmtu);
	    os.writeInt(rpn.intValue());
	    os.close();

	    mtu    = Math.min(lmtu, rmtu);

	} catch (ClassNotFoundException e) {
	    throw new NetIbisException(e);
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}
    }


    public void disconnect(MuxerKey key) throws NetIbisException {
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
	    throws NetIbisException {

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
// System.err.print("w");
	    socket.send(packet);
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}
	if (! b.ownershipClaimed) {
	    b.free();
	}
    }


    synchronized public void close(Integer num) throws NetIbisException {
	if (rpn == num) {
	    if (socket != null) {
		socket.close();
		socket = null;
	    }
	}
    }


    public void free() throws NetIbisException {
	if (rpn == null) {
	    return;
	}
	super.free();
    }


}
