package ibis.ipl.impl.net.tcp_plain;

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
 * The TCP input implementation.
 */
public final class TcpInput extends NetInput {

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

        private boolean               first           = false;
        private byte                  firstbyte       = 0;
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
		headerLength = 0;
	}

        private final class UpcallThread extends Thread {
                
                public void run() {
                        while (true) {
                                try {
                                        int i = tcpIs.read();
                                        //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]read success");
                                        
                                        if (i < 0)
                                                break;

                                        first = true;
                                        firstbyte = (byte)(i & 0xFF);

                                        activeNum = spn;
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        activeNum = null;
                                } catch (java.io.InterruptedIOException e) {
                                        break;
                                } catch (SocketException e) {
                                        break;
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
                        sendInfoTable(os, lInfo);
			tcpSocket  = tcpServerSocket.accept();

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);

			tcpIs 	   = tcpSocket.getInputStream();
			tcpOs 	   = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = 0;

                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread()).start();
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
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
                //System.err.println("tcp_blk: poll <-- : " + activeNum);

                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]poll");
		return activeNum;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public byte readByte() throws IbisIOException {
                byte b = 0;
                        
                if (first) {
                        first = false;
                        b = firstbyte;
                } else {
                        try {
                                int i = tcpIs.read();
                                if (i >= 0) {
                                        b = (byte)(i & 0xFF);
                                } else {
                                        throw new IbisIOException("unexpected EOF");
                                }
                        } catch (IOException e) {
                                throw new IbisIOException(e.getMessage());
                        } 

                }

                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]read byte "+b);

                return b;
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
