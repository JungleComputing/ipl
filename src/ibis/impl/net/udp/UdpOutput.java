package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.*;

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
public final class UdpOutput extends NetBufferedOutput {

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
	private InetAddress    laddr  = null;

	/**
	 * The local socket IP port.
	 */
	private int            lport  =    0;

	/**
	 * The local MTU.
	 */
	private int            lmtu   =   0;

	/**
	 * The remote socket IP address.
	 */
	private InetAddress    raddr  = null;

	/**
	 * The remote socket IP port.
	 */
	private int            rport  =   0;

	/**
	 * The remote MTU.
	 */
	private int            rmtu   =   0;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer        rpn    = null;


	private long		seqno;	/* For out-of-order debugging */


	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param driver the TCP driver instance.
	 * @param output the controlling output.
	 */
	UdpOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);

		if (Driver.DEBUG) {
		    headerLength = 8;
		}
	}

	/*
	 * Sets up an outgoing UDP connection.
	 *
	 * <BR><B>Note</B>: this function also negociate the mtu.
	 * <BR><B>Note</B>: the current UDP mtu is arbitrarily fixed at 32kB.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx)
		throws NetIbisException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }                

		this.rpn = rpn;
	
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

                Hashtable lInfo = new Hashtable();
                lInfo.put("udp_address", laddr);
                lInfo.put("udp_port", 	 new Integer(lport));
                lInfo.put("udp_mtu",  	 new Integer(lmtu));
                Hashtable rInfo = null;

                try {
                        ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream("udp"));
                        rInfo = (Hashtable)is.readObject();
                        is.close();

                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream("udp"));
                        os.writeObject(lInfo);
                        os.close();
                } catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}

		raddr =  (InetAddress)rInfo.get("udp_address");
		rport = ((Integer)    rInfo.get("udp_port")   ).intValue();
		rmtu  = ((Integer)    rInfo.get("udp_mtu")    ).intValue();

		mtu    = Math.min(lmtu, rmtu);
		packet = new DatagramPacket(new byte[0], 0, raddr, rport);
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
		if (Driver.DEBUG) {
		    NetConvert.writeLong(seqno++, b.data, 0);
		}
		packet.setData(b.data, 0, b.length);
// System.err.print("|");
		try {
			socket.send(packet);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
		if (! b.ownershipClaimed) {
		    b.free();
		}
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

        public synchronized void close(Integer num) throws NetIbisException {
                if (rpn == num) {
                        if (socket != null) {
                                socket.close();
                        }
                        rpn = null;
                }
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		if (socket != null) {
			socket.close();
		}

		super.free();
	}
}
