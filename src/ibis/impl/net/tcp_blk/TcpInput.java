package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

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
public final class TcpInput extends NetBufferedInput {

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
	private Integer               spn  	      = null;

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

        private InetAddress           addr            = null;
        private int                   port            =    0;
        private byte []               hdr             = new byte[4];
        private NetReceiveBuffer      buf             = null;
        private UpcallThread          upcallThread    = null;
        private NetMutex              upcallEnd       = new NetMutex(true);

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	TcpInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
		headerLength = 4;
	}

        private final class UpcallThread extends Thread {
                
                public UpcallThread(String name) {
                        super("Tcp(blk)Input.UpcallThread: "+name);
                }

                public void run() {
                        while (true) {
                                try {
                                        buf = receiveByteBuffer(0);
                                        if (buf == null)
                                                break;

                                        activeNum = spn;
                                        initReceive();
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        activeNum = null;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }

                        upcallEnd.unlock();
                }
        }

	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
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
                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();

			tcpIs 	   = tcpSocket.getInputStream();
			tcpOs 	   = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu       = Math.min(lmtu, rmtu);
		allocator = new NetAllocator(mtu);
                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread(addr+"["+port+"]")).start();
                }
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

		if (spn == null) {
			return null;
		}

                //System.err.println("tcp_blk: poll -->");
		try {
			if (tcpIs.available() > 0) {
				activeNum = spn;
                                initReceive();
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
                //System.err.println("tcp_blk: poll <-- : " + activeNum);

		return activeNum;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveByteBuffer(int expectedLength)
		throws IbisIOException {
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        return temp;
                }

		byte [] b = allocator.allocate();
		int     l = 0;

		try {
			int offset = 0;

                        do {
                                int result = tcpIs.read(b, offset, 4);
                                if (result == -1) {
                                        if (offset != 0) {
                                                throw new Error("broken pipe");
                                        }
                                        
                                        return null;
                                }
                                
                                offset += result;
                        } while (offset < 4);

                        l = NetConvert.readInt(b);
                        //System.err.println("received "+l+" bytes");    
                        
			do {
				int result = tcpIs.read(b, offset, l - offset);
                                if (result == -1) {
                                        throw new Error("broken pipe");
                                }                                
                                offset += result;
			} while (offset < l);
                } catch (SocketException e) {
                        return null;
		} catch (IOException e) {
			throw new IbisIOException(e.getMessage());
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

                        if (upcallThread != null) {
                                upcallThread.interrupt();
                                upcallEnd.lock();
                                upcallThread = null;
                        }

			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			if (tcpServerSocket != null) {
                                tcpServerSocket.close();
			}
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
