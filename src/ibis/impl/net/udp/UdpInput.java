package ibis.ipl.impl.net.udp;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

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


public final class UdpInput extends NetBufferedInput {

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
	private Integer               spn    	     = null;
	private NetAllocator          allocator      = null;
	private int                   socketTimeout  =    0;

	UdpInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
	}

        private final class UpcallThread extends Thread {
                
                public void run() {
                        while (true) {
                                if (data != null) {
                                        throw new Error("invalid state");
                                }
                                        
                                data = allocator.allocate();
                                packet.setData(data, 0, data.length);
                                
                                try {
                                        setReceiveTimeout(0);
                                        socket.receive(packet);
                                        buffer    = new NetReceiveBuffer(data, packet.getLength(), allocator);
                                        data      = null;
                                        activeNum = spn;
                                        UdpInput.super.initReceive();
                                        upcallFunc.inputUpcall(UdpInput.this, activeNum);
                                        activeNum = null;
                                } catch (InterruptedIOException e) {
                                        // Nothing
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }
        }

	public void setupConnection(Integer            spn,
				    ObjectInputStream  is,
				    ObjectOutputStream os,
                                    NetServiceListener nls)
		throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = spn;
		 
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
                if (upcallFunc != null) {
                        (new UpcallThread()).start();
                }
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

		if (spn == null) {
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
			activeNum = spn;
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
	public NetReceiveBuffer receiveByteBuffer(int expectedLength)
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

	public void receiveByteBuffer(NetReceiveBuffer userBuffer)
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
		spn    = null;

		super.free();
	}
	
}
