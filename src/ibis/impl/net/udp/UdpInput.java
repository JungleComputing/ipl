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
	private NetReceiveBuffer      buffer 	     = null;
	private Integer               spn    	     = null;
	private int                   socketTimeout  =    0;

	private int			receiveFromPoll;
	private long			t_receiveFromPoll;

	private long		rcve_seqno;	/* For out-of-order debugging */
	private long		deliver_seqno;	/* For out-of-order debugging */


	UdpInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);

		if (factory == null) {
		    factory = new NetBufferFactory(new NetReceiveBufferFactoryDefaultImpl());
		}

		if (Driver.DEBUG) {
		    headerLength = 8;
		}
	}

        private final class UpcallThread extends Thread {

                public UpcallThread(String name) {
                        super("UdpInput.UpcallThread: "+name);
                }
                
                public void run() {
                        while (true) {
                                if (buffer != null) {
                                        throw new Error("invalid state");
                                }
                                        
                                try {
					buffer = createReceiveBuffer();
					packet.setData(buffer.data, 0, buffer.data.length);

                                        setReceiveTimeout(0);
                                        socket.receive(packet);
// System.err.println("UdpInput thread receives buffer " + buffer);
                                        buffer.length = packet.getLength();
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
		factory.setMaximumTransferUnit(mtu);

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
                        (new UpcallThread(raddr+"["+rport+"]")).start();
                }
	}


	private void checkReceiveSeqno(NetReceiveBuffer buffer) {
	    if (Driver.DEBUG) {
		long seqno = NetConvert.readLong(buffer.data, 0);
		if (seqno < rcve_seqno) {
		    System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Receive: packet overtakes: " + seqno + " expect " + rcve_seqno);
		} else {
		    rcve_seqno = seqno;
		}
	    }
	}


	private void checkDeliverSeqno(NetReceiveBuffer buffer) {
	    if (Driver.DEBUG) {
		long seqno = NetConvert.readLong(buffer.data, 0);
		if (seqno < deliver_seqno) {
		    System.err.println("WHHHHHHHHHOOOOOOOOOOOAAAAAA UDP Deliver: packet overtakes: " + seqno + " expect " + deliver_seqno);
		} else {
		    deliver_seqno = seqno;
		}
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
		if (spn == null) {
			return null;
		}
// System.err.print("z");

		if (buffer != null) {
			// Pending packet -- shouldn't we finish that first?
// System.err.println("UdpInput.poll: See pending UDP packet len " + buffer.length);
			activeNum = spn;
		} else {
			activeNum = null;

			buffer = createReceiveBuffer();
// System.err.println("UdpInput.poll creates buffer " + buffer);
// Thread.dumpStack();
			packet.setData(buffer.data, 0, buffer.data.length);
			setReceiveTimeout(pollTimeout);
			long start = System.currentTimeMillis();
			try {
				socket.receive(packet);
				buffer.length = packet.getLength();
// System.err.println("UdpInput.poll: Receive UDP packet len " + packet.getLength());
				activeNum = spn;
				super.initReceive();
				checkReceiveSeqno(buffer);
			} catch (InterruptedIOException e) {
				receiveFromPoll++;
				t_receiveFromPoll += System.currentTimeMillis() - start;
// System.err.print("%");
				buffer.free();
				buffer = null;
				// Nothing
			} catch (IOException e) {
				buffer.free();
				buffer = null;
				throw new IbisIOException(e);
			}
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
// System.err.print("Z");
			buffer = createReceiveBuffer();
// System.err.println("UdpInput.downcall receive(expectedlength) creates buffer " + buffer);
			packet.setData(buffer.data, 0, buffer.data.length);

			try {
				/* This is a blocking receive call. Don't
				 * set a timeout. */
				setReceiveTimeout(0);
				socket.receive(packet);
				buffer.length = packet.getLength();
				super.initReceive();
				checkReceiveSeqno(buffer);
// System.err.println("Receive UDP packet len " + packet.getLength());
			} catch (IOException e) {
				buffer.free();
				buffer = null;
				throw new IbisIOException(e);
			}
		} else {
// System.err.print("_");
		}
		
		NetReceiveBuffer temp_buffer = buffer;
		buffer = null;

		checkDeliverSeqno(temp_buffer);
		
		return temp_buffer;
	}

	public void receiveByteBuffer(NetReceiveBuffer userBuffer)
		throws IbisIOException {
		if (buffer == null) {
                        //
			packet.setData(userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);

			try {
				/* This is a blocking receive call. Don't
				 * set a timeout. */
				setReceiveTimeout(0);
				socket.receive(packet);
				super.initReceive();
				checkReceiveSeqno(buffer);
// System.err.println("Receive downcall UDP packet len " + packet.getLength());
			} catch (IOException e) {
				throw new IbisIOException(e);
			}
		} else {
// System.err.print("-");
// System.err.println("Fill pre-received UDP packet len " + buffer.length);
                        System.arraycopy(buffer.data, 0, userBuffer.data, userBuffer.base, userBuffer.length - userBuffer.base);
                        buffer.free();
                        buffer = null;
                }

		checkDeliverSeqno(userBuffer);
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

		System.err.println("UdpInput: receiveFromPoll(timeout) " + receiveFromPoll + " (estimated loss " + (t_receiveFromPoll / 1000.0) + " s)");
	}
	
}
