package ibis.ipl.impl.net.tcp_plain;

import ibis.ipl.impl.net.*;

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
	private volatile Integer      spn  	      = null;

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

        private boolean               first          = false;
        private byte                  firstbyte      = 0;
        private UpcallThread          upcallThread   = null;
        private InetAddress           addr           = null;
        private int                   port           =    0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	TcpInput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
		headerLength = 0;
	}

        private final class UpcallThread extends Thread {

                private volatile boolean end = false;

                public UpcallThread(String name) {
                        super("Tcp(plain)Input.UpcallThread: "+name);
                }                
                
                public void end() {
                        end = true;
                        this.interrupt();
                }

                public void run() {
                        while (!end) {
                                try {
                                        int i = tcpIs.read();
                                        //System.err.println("TcpInput: "+addr+"["+port+"]read success: "+i);
                                        
                                        if (i < 0)
                                                break;

                                        first = true;
                                        firstbyte = (byte)(i & 0xFF);

                                        activeNum = spn;
                                        //System.err.println("TcpInput: "+addr+"["+port+"] upcall: -->");
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        //System.err.println("TcpInput: "+addr+"["+port+"] upcall: <--");
                                        activeNum = null;
                                } catch (java.io.InterruptedIOException e) {
                                        break;
                                } catch (SocketException e) {
                                        break;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }
        }

	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx)
		throws NetIbisException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable lInfo    = new Hashtable();
			lInfo.put("tcp_address", tcpServerSocket.getInetAddress());
			lInfo.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_plain"));
			os.writeObject(lInfo);
                        os.close();

			tcpSocket  = tcpServerSocket.accept();

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);
                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();

			tcpIs 	   = tcpSocket.getInputStream();
			tcpOs 	   = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		mtu = 0;

		this.spn = cnx.getNum();
		 
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
	 * @param block if true this method blocks until there is some data to read
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll(boolean block) throws NetIbisException {
		activeNum = null;

		if (spn == null) {
			return null;
		}

                //System.err.println("TcpInput: "+addr+"["+port+"] poll -->");
		try {
			if (block) {
				int i = tcpIs.read();
				//System.err.println("TcpInput: "+addr+"["+port+"]read success: "+i);
				
				if (i < 0)
					throw new NetIbisException("Broken pipe");

				first = true;
				firstbyte = (byte)(i & 0xFF);

				activeNum = spn;
			} else if (tcpIs.available() > 0) {
				activeNum = spn;
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		} 
                //System.err.println("TcpInput: "+addr+"["+port+"] poll <--: " + activeNum);

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
	public byte readByte() throws NetIbisException {
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
                                        throw new NetIbisException("unexpected EOF");
                                }
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        } 

                }

                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]read byte "+b);

                return b;
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (spn == num) {
                        try {
                                if (tcpOs != null) {
                                        tcpOs.close();
                                }
		
                                if (tcpIs != null) {
                                        tcpIs.close();
                                }

                                if (upcallThread != null) {
                                        upcallThread.end();
                                }

                                if (tcpSocket != null) {
                                        tcpSocket.close();
                                }

                                if (tcpServerSocket != null) {
                                        tcpServerSocket.close();
                                }
                        } catch (Exception e) {
                                throw new NetIbisException(e);
                        }

                        spn = null;
                }
        }

                /**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		try {
			if (tcpOs != null) {
				tcpOs.close();
			}
		
			if (tcpIs != null) {
				tcpIs.close();
			}

                        if (upcallThread != null) {
                                upcallThread.end();
                                //System.err.println("waiting for TCP upcall thread to join");
                                while (true) {
                                        try {
                                                upcallThread.join();
                                                break;
                                        } catch (InterruptedException e) {
                                                //
                                        }
                                }
                                //System.err.println("TCP upcall thread joined");
                        }

			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			if (tcpServerSocket != null) {
                                tcpServerSocket.close();
			}
		}
		catch (Exception e) {
			throw new NetIbisException(e);
		}

                spn = null;

		super.free();
	}
	
}
