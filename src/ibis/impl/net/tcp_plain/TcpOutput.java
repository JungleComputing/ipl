package ibis.impl.net.tcp_plain;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
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
	 * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
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
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 */
	TcpOutput(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context);
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
		throws IOException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();

                        ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_plain"));
		Hashtable rInfo;
                try {
                        rInfo = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }
		is.close();

		raddr =  (InetAddress)rInfo.get("tcp_address");
		rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();

		tcpSocket = new Socket(raddr, rport);

		tcpSocket.setSendBufferSize(0x8000);
		tcpSocket.setReceiveBufferSize(0x8000);
		tcpSocket.setTcpNoDelay(true);
		
		tcpOs = tcpSocket.getOutputStream();
		tcpIs = tcpSocket.getInputStream();

		mtu = 0;
	}

        public long finish() throws IOException {
                super.finish();
		tcpOs.flush();
		// TODO: return byte count of message
		return 0;
	}
                


	/*
	 * {@inheritDoc}
         */
	public void writeByte(byte b) throws IOException {
                //System.err.println("TcpOutput: "+raddr+"["+rport+"]writing byte "+b);
		tcpOs.write(b);
	}

        public synchronized void close(Integer num) throws IOException {
                if (rpn == num) {
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
	}


	/**
	 * {@inheritDoc}
	 */
	public void free() throws IOException {
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

		super.free();
	}
	
}
