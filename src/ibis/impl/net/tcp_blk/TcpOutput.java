package ibis.impl.net.tcp_blk;

import ibis.impl.net.NetIbis;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetReceivePort;
import ibis.impl.net.NetSendPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;

import ibis.io.Conversion;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.DynamicProperties;

import ibis.connect.socketFactory.ConnectProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

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
	// private int                      lmtu      = 16 * 1024;
	private int                      lmtu      = 32 * 1024;
        //private int                      lmtu      = 5*1024;
        //private int                      lmtu      = 256;
	{
	    if (lmtu != 32 * 1024) {
		System.err.println("net.tcp_blk.TcpOutput.lmtu " + lmtu);
	    }
	}

	/**
	 * The remote MTU.
	 */
	private int                      rmtu      =   0;

	static {
	    if (false) {
		System.err.println("WARNING: Class net.tcp_blk.TcpOutput (still) uses Conversion.defaultConversion");
	    }
	}

	/**
	 * Constructor.
	 *
	 * @param pt the properties of the output's
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

		DataInputStream is = new DataInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
		rmtu = is.readInt();
		is.close();

		DataOutputStream os = new DataOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
		os.writeInt(lmtu);
		os.flush();
		os.close();

		InputStream brokering_in =
			cnx.getServiceLink().getInputSubStream(this, "tcp_blk_brokering");
		OutputStream brokering_out =
			cnx.getServiceLink().getOutputSubStream(this, "tcp_blk_brokering");

		NetPort port = cnx.getPort();

		final DynamicProperties p;
		if (port != null) {
		    if (port instanceof NetReceivePort) {
			p = ((NetReceivePort) port).properties();
		    }
		    else if (port instanceof NetSendPort) {
			p = ((NetSendPort) port).properties();
		    }
		    else {
			p = null;
		    }
		} else {
		    p = null;
		}

		final NetIO nn = this;
		ConnectProperties props = 
		    new ConnectProperties() {
			    public String getProperty(String name) {
				if (p != null) {
				    String result = (String) p.find(name);
				    if (result != null) return result;
				}
				return nn.getProperty(name);
			    }
			};

		tcpSocket = NetIbis.socketFactory.createBrokeredSocket(
			brokering_in,
			brokering_out,
			false,
			props);

		brokering_in.close();
		brokering_out.close();

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

        public long finish() throws IOException {
                log.in();
                super.finish();
		tcpOs.flush();
                log.out();
		// TODO: return byte count of message
		return 0;
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
