package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetIO;
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


/**
 * The UDP input implementation.
 *
 * <BR><B>Note</B>: this first implementation allocate one receive socket per
 * input. It could be interesting to experiment with an implementation
 * using one socket per poller instead.
 */
public class UdpInput extends NetBufferedInput {

	/**
	 * The default polling timeout in milliseconds.
	 *
	 * <BR><B>Note</B>: this will be replaced by a property setting in the future.
	 */
	private final int             defaultPollTimeout    = 1; // milliseconds

	/**
	 * The default reception timeout in milliseconds.
	 *
	 * <BR><B>Note</B>: this will be replaced by a property setting in the future.
	 */
	private final int             defaultReceiveTimeout = 1000; // milliseconds

	/**
	 * The polling timeout in milliseconds.
	 *
	 * <BR><B>Note</B>: this will be replaced by a property setting in the future.
	 */
	private int                   pollTimeout    = defaultPollTimeout; // milliseconds

	/**
	 * The reception timeout in milliseconds.
	 *
	 * <BR><B>Note</B>: this will be replaced by a property setting in the future.
	 */
	private int                   receiveTimeout = defaultReceiveTimeout; // milliseconds

	/**
	 * The UDP socket.
	 */
	private DatagramSocket 	      socket 	     = null;

	/**
	 * The UDP message wrapper.
	 */
	private DatagramPacket 	      packet 	     = null;

	/**
	 * The UDP driver instance.
	 */
	private Driver         	      driver 	     = null;

	/**
	 * The local socket IP address.
	 */
	private InetAddress    	      laddr  	     = null;

	/**
	 * The local socket IP port.
	 */
	private int            	      lport  	     =    0;

	/**
	 * The local MTU.
	 */
	private int            	      lmtu   	     =    0;

	/**
	 * The remote socket IP address.
	 */
	private InetAddress    	      raddr  	     = null;

	/**
	 * The remote socket IP port.
	 */
	private int            	      rport  	     =    0;

	/**
	 * The remote MTU.
	 */
	private int            	      rmtu   	     =    0;

	/**
	 * The current reception byte array.
	 */
	private byte []               data           = null;

	/**
	 * The current reception buffer.
	 */
	private NetReceiveBuffer      buffer 	     = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer               rpn    	     = null;

	/**
	 * The buffer block allocator.
	 */
	private NetAllocator          allocator      = null;

	/**
	 * The current socket timeout.
	 */
	private int                   socketTimeout  =    0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	UdpInput(StaticProperties sp,
		 NetDriver        driver,
		 NetIO             up)
		throws IbisIOException {
		super(sp, driver, up);
	}

	/*
	 * Sets up an incoming UDP connection.
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
			lmtu = Math.min(socket.getReceiveBufferSize(), 16384);
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

		String s = null;
		if ((s = getProperty("ReceiveTimeout")) != null) {
			receiveTimeout = Integer.valueOf(s).intValue();
		}
		if ((s = getProperty("PollingTimeout")) != null) {
			pollTimeout = Integer.valueOf(s).intValue();
		}

		setReceiveTimeout(receiveTimeout);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This UDP polling implementation uses a timed out
	 * {@link DatagramSocket#receive(DatagramPacket)}. As the minimum timeout value is one
	 * millisecond, an unsuccessful polling operation is rather expensive.
	 *
	 * @return {@inheritDoc}
	 */
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
                        super.initReceive();
		} catch (InterruptedIOException e) {
			// Nothing
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		return activeNum;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 * <BR><B>Note</B>: The expectedLength argument is simply ignored because the
	 * packet actually received might not be the one that is expected.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer readByteBuffer(int expectedLength)
		throws IbisIOException {
		if (buffer == null) {
			byte[] b = allocator.allocate();
			packet.setData(b, 0, b.length);

			try {
				setReceiveTimeout(receiveTimeout);
				socket.receive(packet);
				buffer = new NetReceiveBuffer(b, packet.getLength(), allocator);
			} catch (InterruptedIOException e) {
				__.abort__("UDP packet lost");
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

	public void readByteBuffer(NetReceiveBuffer userBuffer)
		throws IbisIOException {
		if (buffer == null) {
                        //
			packet.setData(userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);

			try {
				setReceiveTimeout(receiveTimeout);
				socket.receive(packet);
			} catch (InterruptedIOException e) {
				__.abort__("UDP packet lost");
			} catch (IOException e) {
				throw new IbisIOException(e);
			}
		} else {
                        System.arraycopy(buffer.data, 0, userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);
                        buffer.free();
                        buffer = null;
                }
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		buffer = null;
		super.finish();
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

	/**
	 * {@inheritDoc}
	 */
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
