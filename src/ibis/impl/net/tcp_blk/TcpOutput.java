package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.*;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

/**
 * The TCP output implementation (block version).
 */
public final class TcpOutput extends NetBufferedOutput {

	/**
	 * The communication socket.
	 */
	private Socket                   tcpSocket = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer                  rpn 	   = null;

	/**
	 * The communication input stream.
	 *
	 * Note: this stream is not really needed but may be used for debugging
	 *       purpose.
	 */
	private InputStream  	         tcpIs	   = null;

	/**
	 * The communication output stream.
	 */
	private OutputStream 	         tcpOs	   = null;

	/**
	 * The local MTU.
	 */
	private int                      lmtu      = 32768;

	/**
	 * The remote MTU.
	 */
	private int                      rmtu      =   0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 */
	TcpOutput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
		headerLength = 4;
	}

	/*
	 * Sets up an outgoing TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx)
		throws NetIbisException {
                // System.err.println("tcp_blk.TcpOutput: setupConnection -->");
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();
	
		Hashtable lInfo = new Hashtable();
		lInfo.put("tcp_mtu",     new Integer(lmtu));
		Hashtable   rInfo = null;
                        try {
                                //System.err.println("TcpOutput: reading info table -->");
                                ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
                                rInfo = (Hashtable)is.readObject();
                                is.close();
                                //System.err.println("TcpOutput: reading info table <--");

                                //System.err.println("TcpOutput: writing info table -->");
                                ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
                                os.writeObject(lInfo);
                                os.close();
                                //System.err.println("TcpOutput: writing info table <--");
                        } catch (IOException e) {
                                throw new NetIbisException(e);
                        } catch (ClassNotFoundException e) {
                                throw new Error(e);
                        }

		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
		rmtu              = ((Integer)    rInfo.get("tcp_mtu")    ).intValue();

		try {
			tcpSocket = new Socket(raddr, rport);

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);
			
			tcpOs 	  = tcpSocket.getOutputStream();
			tcpIs 	  = tcpSocket.getInputStream();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		mtu = Math.min(lmtu, rmtu);
		// Don't always create a new factory here, just specify the mtu.
		// Possibly a subclass overrode the factory, and we must leave
		// that factory in place.
		if (factory == null) {
		    factory = new NetBufferFactory(new NetSendBufferFactoryDefaultImpl());
		} else {
		    factory.setMaximumTransferUnit(mtu);
		}
	}

        public void finish() throws NetIbisException {
                super.finish();
 		try {
                        //System.err.println("flushing: -->");
 			tcpOs.flush();
                        //System.err.println("flushing: <--");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}
                

	/*
	 * {@inheritDoc}
         */
	public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
 		try {
                        //System.err.println("sending "+b.length+" bytes");    
 			NetConvert.writeInt(b.length, b.data, 0);
 			tcpOs.write(b.data, 0, b.length);
                        //System.err.println("sending "+b.length+" bytes ok");    
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		if (! b.ownershipClaimed) {
		    b.free();
		}
	}

	public synchronized void close(Integer num) throws NetIbisException {
                if (rpn == num) {
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

                                rpn = null;
                        }
                        catch (Exception e) {
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

			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			rpn = null;
		}
		catch (Exception e) {
			throw new NetIbisException(e);
		}

		super.free();
	}
	
}
