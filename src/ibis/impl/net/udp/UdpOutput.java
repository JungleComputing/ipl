package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetReceivePortIdentifier;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

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
public class UdpOutput extends NetBufferedOutput {

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

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param driver the TCP driver instance.
	 * @param output the controlling output.
	 */
	UdpOutput(StaticProperties sp,
		  NetDriver   	   driver,
		  NetOutput   	   output)
		throws IbisIOException {
		super(sp, driver, output);
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
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		this.rpn = rpn;
	
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

		Hashtable rInfo = receiveInfoTable(is);
		raddr 		=  (InetAddress)rInfo.get("udp_address");
		rport 		= ((Integer)	rInfo.get("udp_port")   ).intValue();
		rmtu  		= ((Integer)	rInfo.get("udp_mtu")    ).intValue();

		Hashtable lInfo = new Hashtable();
		lInfo.put("udp_address", laddr);
		lInfo.put("udp_port",    new Integer(lport));
		lInfo.put("udp_mtu",     new Integer(lmtu));
		sendInfoTable(os, lInfo);

		mtu    = Math.min(lmtu, rmtu);
		packet = new DatagramPacket(new byte[0], 0, raddr, rport);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeByteBuffer(NetSendBuffer b) throws IbisIOException {
		packet.setData(b.data, 0, b.length);
		try {
			socket.send(packet);
		} catch (IOException e) {
			throw new IbisIOException(e);
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

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (socket != null) {
			socket.close();
		}
		
		socket = null;
		packet = null;
		laddr  = null;
		lport  =    0;
		lmtu   =    0;
		raddr  = null;
		rport  =    0;
		rmtu   =    0;
		rpn    = null;

		super.free();
	}
}
