package ibis.ipl.impl.net.tcp_blk;

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
        private volatile NetReceiveBuffer      buf    = null;
        private UpcallThread          upcallThread    = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	TcpInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
		headerLength = 4;
	}

        private final class UpcallThread extends Thread {
                
                private volatile boolean end = false;

                public UpcallThread(String name) {
                        super("Tcp(blk)Input.UpcallThread: "+name);
                }

                public void end() {
                        end = true;
                        this.interrupt();
                }

                public void run() {
                        while (!end) {
                                try {
                                        Integer num = spn;
                                        buf = receiveByteBuffer(0);
                                        if (buf == null)
                                                break;

                                        synchronized(TcpInput.this) {
                                                activeNum = num;
                                        }
                                        initReceive();
                                        // System.err.println("TcpInput.UpcallThread: upcall-->");
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        // System.err.println("TcpInput.UpcallThread: upcall<--");
                                        synchronized(TcpInput.this) {
                                                if (activeNum == num) {
                                                        finish();
                                                }
                                        }
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
                // System.err.println("tcp_blk.TcpInput: setupConnection -->");
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = cnx.getNum();
		 
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable lInfo = new Hashtable();                        
			lInfo.put("tcp_address", tcpServerSocket.getInetAddress());
			lInfo.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
			lInfo.put("tcp_mtu",     new Integer(lmtu));
                        Hashtable rInfo = null;

                        try {
                                //System.err.println("TcpInput: writing info table -->");
                                ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
                                os.writeObject(lInfo);
                                os.close();
                                //System.err.println("TcpInput: writing info table <--");

                                //System.err.println("TcpInput: reading info table -->");
                                ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
                                rInfo = (Hashtable)is.readObject();
                                is.close();
                                //System.err.println("TcpInput: reading info table <--");
                        } catch (IOException e) {
                                throw new NetIbisException(e);
                        } catch (ClassNotFoundException e) {
                                throw new Error(e);
                        }
                
			rmtu = ((Integer) rInfo.get("tcp_mtu")).intValue();

			tcpSocket  = tcpServerSocket.accept();

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);
                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();

			tcpIs = tcpSocket.getInputStream();
			tcpOs = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		mtu       = Math.min(lmtu, rmtu);
		// Don't always create a new factory here, just specify the mtu.
		// Possibly a subclass overrode the factory, and we must leave
		// that factory in place.
		if (factory == null) {
		    factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl());
		} else {
		    factory.setMaximumTransferUnit(mtu);
		}

                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread(addr+"["+port+"]")).start();
                }
                // System.err.println("tcp_blk.TcpInput: setupConnection <--");
	}


	/* Create a NetReceiveBuffer and do a blocking receive. */
	private NetReceiveBuffer receive() throws NetIbisException {

		NetReceiveBuffer buf = createReceiveBuffer(0);
		byte [] b = buf.data;
		int     l = 0;

		try {
			int offset = 0;

                        do {
                                int result = tcpIs.read(b, offset, 4);
                                if (result == -1) {
                                        if (offset != 0) {
                                                throw new Error("broken pipe");
                                        }
                                        
                                        // System.err.println("tcp_blk: receiveByteBuffer <-- null");
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
			throw new NetIbisException(e.getMessage());
		} 

		buf.length = l;
		return buf;
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
	public synchronized Integer poll(boolean block) throws NetIbisException {
                if (activeNum != null) {
                        throw new Error("invalid call");
                }
                
		if (spn == null) {
			return null;
		}

                // System.err.println("tcp_blk: poll -->");
		try {
			if (block) {
				if (buf != null) {
					return activeNum;
				}
				buf = receive();
				if (buf != null) {
					activeNum = spn;
					initReceive();
				}
			} else if (tcpIs.available() > 0) {
				activeNum = spn;
                                initReceive();
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		} 
                // System.err.println("tcp_blk: poll <-- : " + activeNum);

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
		throws NetIbisException {
                // System.err.println("tcp_blk: receiveByteBuffer -->");
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        // System.err.println("tcp_blk: receiveByteBuffer <-- quick");
                        return temp;
                }

		NetReceiveBuffer buf = receive();
                // System.err.println("tcp_blk: receiveByteBuffer <--");
		return buf;
	}

        public void finish() throws NetIbisException {
                // System.err.println("TcpInput: finish-->");
                super.finish();
                synchronized(this) {
                        activeNum = null;
                        buf = null;
                }
                // System.err.println("TcpInput: finish<--");
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

		super.free();
	}
	
}
