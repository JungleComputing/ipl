package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4 
import java.net.SocketTimeoutException;
*/
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;


/**
 * The TCP input implementation (block version).
 */
public class TcpInput extends NetInput {

	/**
	 * The connection socket.
	 */
	private ServerSocket 	      tcpServerSocket = null;

	/**
	 * The communication socket.
	 */
	private Socket                tcpSocket       = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer               rpn  	      = null;

	/**
	 * The communication input stream.
	 */
	private InputStream  	      tcpIs	      = null;

	/**
	 * The communication output stream.
	 *
	 * <BR><B>Note</B>: this stream is not really needed but may be used 
	 * for debugging purpose.
	 */
	private OutputStream 	      tcpOs	      = null;

	/**
	 * The buffer block allocator.
	 */
	private NetAllocator          allocator      = null;

	/**
	 * The local MTU.
	 */
	private int                   lmtu            = 32768;

	/**
	 * The remote MTU.
	 */
	private int                   rmtu            =   0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	TcpInput(StaticProperties sp,
		 NetDriver        driver,
		 NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
		headerLength = 4;
	}


	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                rpn,
				    ObjectInputStream 	   is,
				    ObjectOutputStream	   os)
		throws IbisIOException {
		this.rpn = rpn;
		 
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable lInfo    = new Hashtable();
			lInfo.put("tcp_address", tcpServerSocket.getInetAddress());
			lInfo.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
			lInfo.put("tcp_mtu",     new Integer(lmtu));
			sendInfoTable(os, lInfo);

			Hashtable rInfo = receiveInfoTable(is);
			rmtu  		= ((Integer) rInfo.get("tcp_mtu")).intValue();

			tcpSocket  = tcpServerSocket.accept();

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);

			tcpIs 	   = tcpSocket.getInputStream();
			tcpOs 	   = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu       = Math.min(lmtu, rmtu);
		allocator = new NetAllocator(mtu);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This TCP polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

		try {
			if (tcpIs.available() > 0) {
				activeNum = rpn;
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 

		return activeNum;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException {
		byte [] b = allocator.allocate();
		int     l = 0;
		
		try {
			int offset = 0;
			do {
				offset += tcpIs.read(b, offset, 4);
			} while (offset < 4);

			l = readInt(b, 0);

			do {
				offset += tcpIs.read(b, offset, l - offset);
			} while (offset < l);
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
		
		
		return new NetReceiveBuffer(b, l, allocator);
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (tcpOs != null) {
				tcpOs.close();
			}
		
			if (tcpIs != null) {
				tcpIs.close();
			}

			if (tcpSocket != null) {
				synchronized(tcpSocket) {
                                        if (!tcpSocket.isClosed()) {
                                                tcpSocket.close();
                                        }
                                }
                                
			}

			if (tcpServerSocket != null) {
				synchronized(tcpServerSocket) {
                                        if (!tcpServerSocket.isClosed()) {
                                                tcpServerSocket.close();
                                        }
                                }
			}

			tcpSocket       = null;
			tcpServerSocket = null;
			rpn             = null;
			tcpIs           = null;
			tcpOs           = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
