package ibis.ipl.impl.net.muxer.udp;

import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetServiceListener;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetBufferFactory;

import ibis.ipl.impl.net.muxer.MuxerInput;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import ibis.ipl.impl.generic.Monitor;
import ibis.ipl.impl.generic.ConditionVariable;

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


public final class UdpMuxInput extends MuxerInput {

    private DatagramSocket	socket 	      = null;
    private DatagramPacket	packet 	      = null;
    private InetAddress		laddr  	      = null;
    private int			lport  	      =    0;
    private int			lmtu   	      =    0;
    private int			socketTimeout =    0;

    private int			receiveFromPoll;
    private long		t_receiveFromPoll;

    private boolean		receiverBlocked;

    private NetReceiveBuffer	buffer;

    private Integer		spn;

    private final static int	UDP_MAX_MTU = 16384;



    protected UdpMuxInput(NetPortType portType,
			  NetDriver   driver,
			  NetIO       up,
			  String      context)
	    throws IbisIOException {

	super(portType, driver, up, context);

	try {
	    socket = new DatagramSocket(0, InetAddress.getLocalHost());
	    lmtu = Math.min(socket.getReceiveBufferSize(), UDP_MAX_MTU);
	    laddr = socket.getLocalAddress();
	    lport = socket.getLocalPort();
	} catch (SocketException e) {
	    throw new IbisIOException(e);
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}

	min_mtu = lmtu;
	max_mtu = UDP_MAX_MTU;
	mtu     = lmtu;

	packet = new DatagramPacket(new byte[max_mtu], max_mtu);

	spn    = new Integer(0);
    }


    // synchronized
    public void setupConnection(Integer            spn,
				ObjectInputStream  is,
				ObjectOutputStream os,
				NetServiceListener nls)
	    throws IbisIOException {

	if (Driver.DEBUG) {
	    System.err.println("Now enter UdpMuxInput.setupConnection");
	}

	try {
	    os.writeObject(laddr);
	    os.writeInt(lport);
	    os.writeInt(lmtu);
	    os.writeInt(spn.intValue());
	    os.flush();

	    InetAddress raddr = (InetAddress)is.readObject();
	    int         rport = is.readInt();
	    int         rmtu  = is.readInt();
	    int         rKey  = is.readInt();

	    int mtu = Math.min(lmtu, rmtu);
	    if (Driver.DEBUG) {
		System.err.println("Still consider what an MTU (now becomes " + mtu + ") means for a muxer");
	    }

	    if (mtu < min_mtu) {
		min_mtu = mtu;
		this.mtu = mtu;
	    }
	    if (mtu > max_mtu) {
		max_mtu = mtu;
		factory.setMaximumTransferUnit(max_mtu);
	    }

	} catch (ClassNotFoundException e) {
	    throw new IbisIOException(e);
	} catch (IOException e) {
	    throw new IbisIOException(e);
	}
    }


    private void setReceiveTimeout(int timeout)
	    throws java.net.SocketException {
	if (timeout == socketTimeout) {
	    return;
	}

	socketTimeout = timeout;
	socket.setSoTimeout(timeout);
    }


    protected Integer poll(int timeout) throws IbisIOException {
	if (spn == null) {
	    return null;
	}

	if (buffer != null) {
	    /* Pending packet. Finish that first. */
	    activeNum = spn;
	} else {
	    activeNum = null;
	    buffer = createReceiveBuffer();
	    /* Make a copy of the packet pointer. Maybe the mtu and the
	     * associated instance packet changes under our hands. */
	    DatagramPacket packet = this.packet;
	    packet.setData(buffer.data,
			   buffer.base,
			   buffer.data.length - buffer.base);
	    long start = 0;
	    if (timeout != 0) {
		start = System.currentTimeMillis();
	    }

	    try {
		setReceiveTimeout(timeout);
		socket.receive(packet);
		buffer.length = packet.getLength();
		activeNum = spn;
		// super.initReceive();
	    } catch (InterruptedIOException e) {
System.err.println(this + ": ***************** catch InterruptedIOException " + e);
		buffer.free();
		buffer = null;
		if (timeout == 0) {
		    throw new IbisIOException(e);
		} else {
		    receiveFromPoll++;
		    t_receiveFromPoll += System.currentTimeMillis() - start;
		}
	    } catch (IOException e) {
System.err.println(this + ": ***************** catch Exception " + e);
		buffer.free();
		buffer = null;
		throw new IbisIOException(e);
	    }
	}

	return activeNum;
    }


    protected NetReceiveBuffer receiveByteBuffer(int expectedLength)
	    throws IbisIOException {

	while (buffer == null) {
	    poll(0);
	}

	NetReceiveBuffer delivered = buffer;
	buffer = null;

	return delivered;
    }


    public void free() throws IbisIOException {
	if (socket != null) {
	    socket.close();
	}

	socket = null;

	System.err.println("UdpMuxInput: receiveFromPoll(timeout) " + receiveFromPoll + " (estimated loss " + (t_receiveFromPoll / 1000.0) + " s)");
    }

}
