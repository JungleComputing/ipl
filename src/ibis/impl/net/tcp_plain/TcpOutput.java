package ibis.ipl.impl.net.tcp_plain;

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
public final class TcpOutput extends NetOutput {

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
        private InetAddress              raddr     = null;
        private int                      rport     = 0;
        
	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param output the controlling output.
	 */
	TcpOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
		headerLength = 0;
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
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();

                try {
                        ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_plain"));
                        Hashtable rInfo = (Hashtable)is.readObject();
                        is.close();

                        raddr =  (InetAddress)rInfo.get("tcp_address");
                        rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();

			tcpSocket = new Socket(raddr, rport);

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);
			
			tcpOs = tcpSocket.getOutputStream();
			tcpIs = tcpSocket.getInputStream();
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

		mtu = 0;
	}

        public void finish() throws NetIbisException {
                super.finish();
 		try {
 			tcpOs.flush();
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}
                


	/*
	 * {@inheritDoc}
         */
	public void writeByte(byte b) throws NetIbisException {
                //System.err.println("TcpOutput: "+raddr+"["+rport+"]writing byte "+b);
 		try {
 			tcpOs.write(b);
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
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
                        } catch (Exception e) {
                                throw new NetIbisException(e);
                        }

                        rpn = null;
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
		} catch (Exception e) {
			throw new NetIbisException(e);
		}

                rpn = null;

		super.free();
	}
	
}
