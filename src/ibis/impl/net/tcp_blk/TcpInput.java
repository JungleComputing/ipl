package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
	//private int                   lmtu            = 5*1024;
	//private int                   lmtu            = 256;

	/**
	 * The remote MTU.
	 */
	private int                   rmtu            =   0;

        private InetAddress           addr            = null;
        private int                   port            =    0;
        private byte []               hdr             = new byte[4];
        private volatile NetReceiveBuffer      buf    = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 */
	TcpInput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
		headerLength = 4;
	}

	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                if (this.spn != null) {
                        throw new Error("connection already established");
                }

		try {
                        tcpServerSocket   = new ServerSocket();
			tcpServerSocket.setReceiveBufferSize(0x8000);
                        tcpServerSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0), 1);
			Hashtable lInfo = new Hashtable();
			lInfo.put("tcp_address", tcpServerSocket.getInetAddress());
			lInfo.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
			lInfo.put("tcp_mtu",     new Integer(lmtu));
                        Hashtable rInfo = null;

                        try {
                                ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
                                os.writeObject(lInfo);
                                os.close();

                                ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
                                rInfo = (Hashtable)is.readObject();
                                is.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e);
                        } catch (ClassNotFoundException e) {
                                throw new Error(e);
                        }

			rmtu = ((Integer) rInfo.get("tcp_mtu")).intValue();

			tcpSocket  = tcpServerSocket.accept();

			tcpSocket.setSendBufferSize(0x8000);
                        tcpSocket.setTcpNoDelay(true);

                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();

			tcpIs = tcpSocket.getInputStream();
			tcpOs = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		mtu = Math.min(lmtu, rmtu);
		// Don't always create a new factory here, just specify the mtu.
		// Possibly a subclass overrode the factory, and we must leave
		// that factory in place.
		if (factory == null) {
                        factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl());
		} else {
                        factory.setMaximumTransferUnit(mtu);
		}

		this.spn = cnx.getNum();
                startUpcallThread();
                log.out();
	}


	/* Create a NetReceiveBuffer and do a blocking receive. */
	private NetReceiveBuffer receive() throws NetIbisException {
                log.in();
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

                                        log.out("connection lost");

                                        return null;
                                }

                                offset += result;
                        } while (offset < 4);

                        l = NetConvert.readInt(b);

			do {
				int result = tcpIs.read(b, offset, l - offset);
                                if (result == -1) {
                                        throw new Error("broken pipe");
                                }
                                offset += result;
			} while (offset < l);
                } catch (SocketException e) {
                        log.out("SocketException");
                        return null;
		} catch (IOException e) {
			throw new NetIbisException(e.getMessage());
		}

		buf.length = l;
                log.out();

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
	public Integer doPoll(boolean block) throws NetIbisException {
                log.in();
		if (spn == null) {
                        log.out("not connected");
			return null;
		}

		try {
			if (block) {
				if (buf != null) {
                                        log.out("early return");
					return null;
				}
				buf = receive();
                                return spn;
			} else if (tcpIs.available() > 0) {
                                return spn;
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

                log.out();

		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                if (buf != null) {
                        NetReceiveBuffer temp = buf;
                        buf = null;
                        log.out("early receive");

                        return temp;
                }

		NetReceiveBuffer buf = receive();
                log.out();

		return buf;
	}

        public void doFinish() throws NetIbisException {
                log.in();
                //synchronized(this)
                        {
                        buf = null;
                }
                log.out();
        }


        public void doClose(Integer num) throws NetIbisException {
                log.in();
                if (spn == num) {
                        try {
                                if (tcpOs != null) {
                                        tcpOs.close();
                                }

                                if (tcpIs != null) {
                                        tcpIs.close();
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
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
                log.in();
		try {
			if (tcpOs != null) {
				tcpOs.close();
			}

			if (tcpIs != null) {
				tcpIs.close();
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
                log.out();
	}

}
