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
	TcpOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
	}

	/*
	 * Sets up an outgoing TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
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

			tcpSocket = new Socket(raddr, rport);
			tcpOs 	  = new DataOutputStream(tcpSocket.getOutputStream());
			tcpIs 	  = new DataInputStream(tcpSocket.getInputStream());
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}

		mtu = 0;
	}

        public void finish() throws NetIbisException{
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
        }

        public void reset(boolean doSend) throws NetIbisException {
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
        }

        public void writeByteBuffer(NetSendBuffer b) throws NetIbisException {
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
        }
        
        
        public void writeBoolean(boolean b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }
			tcpOs.writeBoolean(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }
        
        public void writeByte(byte b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeByte((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }
        
        public void writeChar(char b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeChar((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeShort(short b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeShort((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeInt(int b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

                        tcpOs.writeInt((int)b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeLong(long b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeLong(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeFloat(float b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeFloat(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeDouble(double b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeDouble(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeString(String b) throws NetIbisException {
                try {
                        if (first) {
                                tcpOs.write(1);
                                first = false;
                        }

			tcpOs.writeUTF(b);
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
        }

        public void writeObject(Object o) throws NetIbisException {
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
        }
        


        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeBoolean(b[o+i]);
                }
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeByte(b[o+i]);
                }
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeChar(b[o+i]);
                }
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeShort(b[o+i]);
                }
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeInt(b[o+i]);
                }
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeLong(b[o+i]);
                }
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeFloat(b[o+i]);
                }
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeDouble(b[o+i]);
                }
        }

        public void writeArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeObject(b[o+i]);
                }
        }


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
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
	}

	public void free() throws NetIbisException {
                doFree();
		super.free();
	}


        private final class DummyOutputStream extends OutputStream {
                private long seq = 0;
                public void write(int b) throws IOException {
                        try {
                                writeByte((byte)b);
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }


        public synchronized void close(Integer num) throws NetIbisException {
                if (rpn == num) {
                        doFree();
                }
        }
}
