package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

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

/*
 * Note: this first implementation allocate one receive socket per
 * input. It could be interesting to experiment with an implementation
 * using one socket per poller instead. */
public class UdpInput extends NetInput {
	private final int             pollTimeout    = 1; // milliseconds
	private final int             receiveTimeout = 1; // milliseconds
	private DatagramSocket 	      socket 	     = null;
	private DatagramPacket 	      packet 	     = null;
	private Driver         	      driver 	     = null;
	private InetAddress    	      laddr  	     = null;
	private int            	      lport  	     =    0;
	private int            	      lmtu   	     =    0;
	private InetAddress    	      raddr  	     = null;
	private int            	      rport  	     =    0;
	private int            	      rmtu   	     =    0;
	private byte []               data           = null;
	private NetReceiveBuffer      buffer 	     = null;
	private Integer               rpn    	     = null;
	private NetAllocator          allocator      = null;
	private int                   socketTimeout  =    0;

	UdpInput(StaticProperties sp,
		 NetDriver        driver,
		 NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
	}

	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		this.rpn = rpn;
		 
		try {
			socket = new DatagramSocket(0, InetAddress.getLocalHost());
			lmtu  = Math.min(socket.getSendBufferSize(), 32768);//TOCHANGE
			laddr = socket.getLocalAddress();
			lport = socket.getLocalPort();
		} catch (SocketException e) {
			throw new IbisIOException(e);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		Hashtable lInfo = new Hashtable();
		lInfo.put("udp_address", laddr);
		lInfo.put("udp_port", 	 new Integer(lport));
		lInfo.put("udp_mtu",  	 new Integer(lmtu));
		sendInfoTable(os, lInfo);

		Hashtable rInfo = receiveInfoTable(is);
		raddr =  (InetAddress)rInfo.get("udp_address");
		rport = ((Integer)    rInfo.get("udp_port")  ).intValue();
		rmtu  = ((Integer)    rInfo.get("udp_mtu")   ).intValue();

		mtu       = Math.min(lmtu, rmtu);
		allocator = new NetAllocator(mtu);
		packet    = new DatagramPacket(new byte[mtu], mtu);

		setReceiveTimeout(receiveTimeout);
	}

	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

		if (data == null) {
			data = allocator.allocate();
			packet.setData(data, 0, data.length);
			setReceiveTimeout(pollTimeout);
		}
		try {
			socket.receive(packet);
			buffer    = new NetReceiveBuffer(data, packet.getLength(), allocator);
			data      = null;
			activeNum = rpn;
		} catch (InterruptedIOException e) {
			// Nothing
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
		
		return activeNum;
	}
	
	//
	// The expectedLength argument is simply ignored because the
	// packet actually received might not be the one that is expected.
	//
	public NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException {

		if (buffer == null) {
			byte[] b = allocator.allocate();
			packet.setData(b, 0, b.length);

			try {
				setReceiveTimeout(receiveTimeout);
				socket.receive(packet);
				buffer = new NetReceiveBuffer(b, packet.getLength(), allocator);
			} catch (InterruptedIOException e) {
				__.warning__("UDP packet lost");
				//return a dummy buffer
				buffer = new NetReceiveBuffer(b, b.length, allocator);
			} catch (IOException e) {
				throw new IbisIOException(e);
			}
		}
		
		NetReceiveBuffer temp_buffer = buffer;
		buffer = null;
		
		return temp_buffer;
	}

	public void release() {
		super.release();
		buffer = null;
	}

	/*
	 * We need a way to set timeout through properties 
	 */
	// timeout should be expressed in milliseconds
	void setReceiveTimeout(int timeout) throws IbisIOException {
		if (timeout != socketTimeout) {
			try {
				socket.setSoTimeout(timeout);
				socketTimeout = timeout;
			} catch (SocketException e) {
				throw new IbisIOException(e);
			}
		}		
	}
	
	// returns the current reception timeout in milliseconds
	// 0 means an infinite timeout
	int getReceiveTimeout() throws IbisIOException {
		int t = 0;

		try {
			t = socket.getSoTimeout();
		} catch (SocketException e) {
			throw new IbisIOException(e);
		}

		return t;
	}

	public void free() throws IbisIOException {
		if (socket != null) {
			socket.close();
		}
		
		socket = null;
		packet = null;
		driver = null;
		laddr  = null;
		lport  =    0;
		lmtu   =    0;
		raddr  = null;
		rport  =    0;
		rmtu   =    0;
		buffer = null;
		rpn    = null;

		super.free();
	}
	
}
