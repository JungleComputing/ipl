package ibis.impl.net.tcp_blk;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.ipl.ConnectionClosedException;

import ibis.io.Conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
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

	/**
	 * The local MTU.
	 */
	private int                      lmtu      = 32768;
        //private int                      lmtu      = 5*1024;
        //private int                      lmtu      = 256;

	/**
	 * The remote MTU.
	 */
	private int                      rmtu      =   0;

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
		throws IOException {
                log.in();

                if (this.rpn != null) {
                        throw new Error("connection already established");
                }

		this.rpn = cnx.getNum();

		Hashtable lInfo = new Hashtable();
		lInfo.put("tcp_mtu",     new Integer(lmtu));
		Hashtable   rInfo = null;
		ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
		try {
			rInfo = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		is.close();

		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
		os.writeObject(lInfo);
		os.close();

		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
		rmtu              = ((Integer)    rInfo.get("tcp_mtu")    ).intValue();

		tcpSocket = new Socket(raddr, rport);

		tcpSocket.setSendBufferSize(0x8000);
		tcpSocket.setReceiveBufferSize(0x8000);
		tcpSocket.setTcpNoDelay(true);

		tcpOs 	  = tcpSocket.getOutputStream();
		tcpIs 	  = tcpSocket.getInputStream();

		mtu = Math.min(lmtu, rmtu);
		// Don't always create a new factory here, just specify the mtu.
		// Possibly a subclass overrode the factory, and we must leave
		// that factory in place.
		if (factory == null) {
		    factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
		} else {
		    factory.setMaximumTransferUnit(mtu);
		}
                log.out();
	}

        public void finish() throws IOException {
                log.in();
                super.finish();
		tcpOs.flush();
                log.out();
	}


	/*
	 * {@inheritDoc}
         */
	public void sendByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
// System.err.print(this + ": write[" + b.length + "] = '"); for (int i = 0; i < Math.min(32, b.length); i++) System.err.print(b.data[i] + ","); System.err.println("'");
		try {
			Conversion.defaultConversion.int2byte(b.length, b.data, 0);
			tcpOs.write(b.data, 0, b.length);
			tcpOs.flush();

			if (! b.ownershipClaimed) {
				b.free();
			} else {
				throw new IOException("buffer is owned, cannot free");
			}
		} catch (IOException e) {
			throw new ConnectionClosedException(e);
		}

                log.out();
	}

	public synchronized void close(Integer num) throws IOException {
                log.in();
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
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IOException {
                log.in();
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
                log.out();
	}

}
