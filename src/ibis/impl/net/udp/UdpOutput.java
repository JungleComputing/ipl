package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
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

public class UdpOutput extends NetOutput {
	private DatagramSocket 		 socket = null;
	private DatagramPacket 		 packet = null;
	private InetAddress    		 laddr  = null;
	private int            		 lport  =    0;
	private int            		 lmtu   =    0;
	private InetAddress    		 raddr  = null;
	private int            		 rport  =    0;
	private int            		 rmtu   =    0;
	private Integer                  rpn 	= null;
	private NetReceivePortIdentifier rpi 	= null;

	UdpOutput(StaticProperties sp,
		  NetDriver   	   driver,
		  NetOutput   	   output)
		throws IbisIOException {
		super(sp, driver, output);
	}

	public void setupConnection(Integer                  rpn,
				    ObjectInputStream 	     is,
				    ObjectOutputStream	     os)
		throws IbisIOException {
		this.rpn = rpn;
		this.rpi = rpi;
	
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

	public void sendBuffer(NetSendBuffer b) throws IbisIOException {
		packet.setData(b.data, 0, b.length);
		try {
			socket.send(packet);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
	}

	public void release() {
		// nothing
	}

	public void reset() {
		// nothing
	}

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
		rpi    = null;

		super.free();
	}
}
