package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.*;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

/**
 * The TCP output implementation.
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
	private DataInputStream  	         tcpIs	   = null;

	/**
	 * The communication output stream.
	 */
	private DataOutputStream 	         tcpOs	   = null;

        /*
         * Object stream for the internal fallback serialization.
         */
        private ObjectOutputStream _outputConvertStream = null;


        private InetAddress raddr = null;
        private int         rport =    0;
        private long        seq   =    0;
        private boolean     first = true;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param output the controlling output.
	 */
	TcpOutput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
	}

	/*
	 * Sets up an outgoing TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();
	
		try {
                        ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "tcp"));
                        Hashtable remoteInfo = (Hashtable)is.readObject();
                        is.close();
                        
                        raddr =  (InetAddress)remoteInfo.get("tcp_address");
                        rport = ((Integer)    remoteInfo.get("tcp_port")   ).intValue();
                        log.disp("raddr = "+raddr);
                        log.disp("rport = "+rport);

			tcpSocket = new Socket(raddr, rport);
			tcpOs 	  = new DataOutputStream(tcpSocket.getOutputStream());
			tcpIs 	  = new DataInputStream(tcpSocket.getInputStream());
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}

		mtu = 0;
                log.out();
	}

        public void finish() throws NetIbisException{
                log.in();
                super.finish();
                if (_outputConvertStream != null) {
                        try {
                                _outputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }

                        _outputConvertStream = null;
                }

                try {
                        tcpOs.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
                first = true;
                log.out();
        }

        public void reset(boolean doSend) throws NetIbisException {
                log.in();
                if (doSend) {
                        send();
                } else {
                        throw new Error("full reset unimplemented");
                }
                
                if (_outputConvertStream != null) {
                        try {
                                _outputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }

                        _outputConvertStream = null;
                }
                first = true;
                log.out();
        }

        public void writeByteBuffer(NetSendBuffer b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }
                        for (int i = 0; i < b.length; i++) {
                                tcpOs.writeByte((int)b.data[i]);
                        }
		} catch (IOException e) {
			throw new NetIbisException(e);
		}

		if (! b.ownershipClaimed) {
		    b.free();
		}
                log.out();
        }
        
        public void writeBoolean(boolean b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }
			tcpOs.writeBoolean(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }
        
        public void writeByte(byte b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeByte((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }
        
        public void writeChar(char b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeChar((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeShort(short b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeShort((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeInt(int b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

                        tcpOs.writeInt((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeLong(long b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeLong(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeFloat(float b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeFloat(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeDouble(double b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeDouble(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeString(String b) throws NetIbisException {
                log.in();
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeUTF(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }

        public void writeObject(Object o) throws NetIbisException {
                log.in();
                try {
                        if (_outputConvertStream == null) {
                                DummyOutputStream dos = new DummyOutputStream();
                                _outputConvertStream = new ObjectOutputStream(dos);
                                _outputConvertStream.flush();
                        }
                        _outputConvertStream.writeObject(o);
                        _outputConvertStream.flush();
                } catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }
        


        public void writeArray(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeBoolean(b[o+i]);
                }
                log.out();
        }

        public void writeArray(byte [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeByte(b[o+i]);
                }
                log.out();
        }

        public void writeArray(char [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeChar(b[o+i]);
                }
                log.out();
        }

        public void writeArray(short [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeShort(b[o+i]);
                }
                log.out();
        }

        public void writeArray(int [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeInt(b[o+i]);
                }
                log.out();
        }

        public void writeArray(long [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeLong(b[o+i]);
                }
                log.out();
        }

        public void writeArray(float [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeFloat(b[o+i]);
                }
                log.out();
        }

        public void writeArray(double [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeDouble(b[o+i]);
                }
                log.out();
        }

        public void writeArray(Object [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeObject(b[o+i]);
                }
                log.out();
        }


	/**
	 * Reset the TCP connection if it exists.
	 */
	public void doFree() throws NetIbisException {
                log.in();
		try {
			if (tcpIs != null) {
				tcpIs.close();
			}

			if (tcpOs != null) {
				tcpOs.close();
			}
		
			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			rpn = null;
		} catch (Exception e) {
			throw new NetIbisException(e);
		}
                log.out();
	}

	public void free() throws NetIbisException {
                log.in();
                doFree();
		super.free();
                log.out();
	}


        private final class DummyOutputStream extends OutputStream {
                private long seq = 0;

                public void write(int b) throws IOException {
                        log.in();
                        try {
                                writeByte((byte)b);
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }
                        log.out();
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }


        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
                if (rpn == num) {
                        doFree();
                }
                log.out(); 
        }
}
