package ibis.ipl.impl.net.muxer.udp;

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

import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetConnection;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetIbisException;

import ibis.ipl.impl.net.muxer.MuxerInput;
import ibis.ipl.impl.net.muxer.MuxerQueue;


public final class UdpMuxInput extends MuxerInput {

    private DatagramSocket	socket 	      = null;
    private DatagramPacket	packet 	      = null;
    private InetAddress		laddr  	      = null;
    private int			lport  	      =    0;
    private int			lmtu   	      =    0;
    private int			socketTimeout =    0;

    private int			receiveFromPoll;
    private long		t_receiveFromPoll;
    private int			polls;
    private long		t_poll_start;
    private long		t_receive_delay;

    private boolean		receiverBlocked;

    private NetReceiveBuffer	buffer;

    private volatile Integer	spn;

    private final static int	UDP_MAX_MTU = 16384;

    private int			connections;



    protected UdpMuxInput(NetPortType portType,
			  NetDriver   driver,
			  String      context)
	    throws NetIbisException {

	super(portType, driver, context);

	try {
	    socket = new DatagramSocket(0, InetAddress.getLocalHost());
	    lmtu = Math.min(socket.getReceiveBufferSize(), UDP_MAX_MTU);
	    laddr = socket.getLocalAddress();
	    lport = socket.getLocalPort();
	} catch (SocketException e) {
	    throw new NetIbisException(e);
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}

	min_mtu = lmtu;
	max_mtu = UDP_MAX_MTU;
	mtu     = lmtu;

	packet = new DatagramPacket(new byte[max_mtu], max_mtu);

	spn    = new Integer(0);
    }


    synchronized
    public void setupConnection(NetConnection cnx, NetIO io)
	    throws NetIbisException {

	if (Driver.DEBUG) {
	    System.err.println(this + ": Now enter UdpMuxInput.setupConnection, cnx = " + cnx + " cnx.serviceLink " + cnx.getServiceLink());
Thread.dumpStack();
	}

	try {
	    Integer num = cnx.getNum();

	    // ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "muxer.udp-" + num));
	    ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(io, ":down"));

	    /* We don't use the IP address of the sender port. Maybe it comes
	     * in handy for debugging. */
	    InetAddress raddr = (InetAddress)is.readObject();
	    int         rport = is.readInt();
	    int         rmtu  = is.readInt();
	    is.close();

	    if (Driver.DEBUG) {
		System.err.println(this + ": in UdpMuxInput.setupConnection, Integer = " + cnx.getNum() + " start info send");
	    }

	    // ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "muxer.udp-" + num));
	    ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(io, ":up"));

	    MuxerQueue q = createQueue(cnx, num);
	    os.writeObject(laddr);
	    os.writeInt(lport);
	    os.writeInt(lmtu);
	    os.writeInt(q.connectionKey());
	    os.close();

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

	    spn = num;

	    connections++;

	} catch (ClassNotFoundException e) {
	    throw new NetIbisException(e);
	} catch (IOException e) {
	    throw new NetIbisException(e);
	}

	if (Driver.DEBUG) {
	    System.err.println(this + ": Now leave UdpMuxInput.setupConnection, Integer = " + cnx.getNum());
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


    protected Integer doPoll(int timeout)
	    throws NetIbisException {
	if (spn == null) {
	    return null;
	}

	if (Driver.DEBUG_HUGE) {
	    System.err.print("z");
	}

	polls++;

        boolean result = false;

	if (buffer != null) {
	    if (Driver.DEBUG_HUGE) {
		System.err.println(this + ": poll: See pending UDP packet len " + buffer.length);
	    }
	    /* Pending packet. Finish that first. */
	    result = true;
	} else {
	    buffer = createReceiveBuffer(mtu);
	    if (Driver.DEBUG_HUGE) {
		System.err.println(this + ": poll creates buffer " + buffer);
		Thread.dumpStack();
	    }
	    /* Make a copy of the packet pointer. Maybe the mtu and the
	     * associated instance packet changes under our hands. */
	    DatagramPacket packet = this.packet;
	    packet.setData(buffer.data,
			   buffer.base,
			   buffer.data.length - buffer.base);
	    long start = 0;
	    if (Driver.STATISTICS && timeout != 0) {
		start = System.currentTimeMillis();
	    }

	    try {
		setReceiveTimeout(timeout);
		socket.receive(packet);
		if (Driver.DEBUG_HUGE) {
		    System.err.println(this + ": poll Receive UDP packet len " + packet.getLength() + " buffer " + buffer);
		}
		buffer.length = packet.getLength();
		result = true;
		// super.initReceive();
		if (Driver.DEBUG_HUGE) {
		    System.err.print("^");
		}
	    } catch (InterruptedIOException e) {
		if (Driver.DEBUG_HUGE) {
		    System.err.println(this + ": ***************** catch InterruptedIOException " + e);
		    Thread.dumpStack();
		}
		buffer.free();
		buffer = null;
		if (timeout == 0) {
		    throw new NetIbisException(e);
		} else if (Driver.STATISTICS) {
		    receiveFromPoll++;
		    t_receiveFromPoll += System.currentTimeMillis() - start;
		}
		if (Driver.DEBUG_HUGE) {
		    System.err.print("%");
		}
	    } catch (IOException e) {
System.err.println(this + ": ***************** catch Exception " + e);
		buffer.free();
		buffer = null;
		throw new NetIbisException(e);
	    }
	}

	return result?spn:null;
    }


    protected NetReceiveBuffer receiveByteBuffer(int expectedLength)
	    throws NetIbisException {

	while (buffer == null) {
	    if (Driver.DEBUG_HUGE) {
		System.err.print("Z");
	    }
	    doPoll(0);
	}

	if (Driver.DEBUG_HUGE) {
	    // System.err.println(this + ": hi -- activeNum " + activeNum + " buffer " + buffer);
	    // Thread.dumpStack();
	    System.err.print("_");
	}

	if (Driver.STATISTICS) {
	    long now = System.currentTimeMillis();
	    t_receive_delay += now - t_poll_start;
	    if (Driver.DEBUG && (polls % 1000) == 0) {
		System.err.println("<Time between poll and receive>: " + polls + " = " + t_receive_delay / (1000.0 * polls) + " s");
	    }
	}

	NetReceiveBuffer delivered = buffer;
	buffer = null;

	return delivered;
    }


    synchronized public void doClose(Integer num) throws NetIbisException {
	System.err.println(this + ": close. connections " + connections);
	if (num == spn) {
	    if (socket != null) {
		socket.close();
		socket = null;
	    }

	    spn = null;

	    if (Driver.STATISTICS) {
		System.err.println("UdpMuxInput: receiveFromPoll(timeout) " + receiveFromPoll + " (estimated loss " + (t_receiveFromPoll / 1000.0) + " s)");
	    }
	}
    }


    public void doFree() throws NetIbisException {
	System.err.println(this + ": doFree. connections " + connections);

	if (spn == null) {
	    return;
	}

	close(spn);
    }

}
