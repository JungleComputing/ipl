package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4 
import java.net.SocketTimeoutException;
*/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;


public final class TcpInput extends NetInput {
	private ServerSocket 	   tcpServerSocket    = null;
	private Socket             tcpSocket          = null;
	private volatile Integer   spn  	      = null;
	private DataInputStream    tcpIs	      = null;
	private DataOutputStream   tcpOs	      = null;
        private InetAddress        addr               = null;
        private int                port               =    0;
        private ObjectInputStream _inputConvertStream = null;        
        private long               seq                =    0;
        private UpcallThread       upcallThread       = null;
        private NetMutex           upcallEnd          = new NetMutex(true);

	TcpInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
	}

        private final class UpcallThread extends Thread {

                private volatile boolean end = false;

                public UpcallThread(String name) {
                        super("TcpInput.UpcallThread: "+name);
                }                

                public void end() {
                        log.in();
                        end = true;
                        this.interrupt();
                        log.out();
                }
                
                public void run() {
                        log.in();
                        while (!end) {
                                try {
                                        Integer num = spn;
                                        int i = tcpIs.read();
                                        if (i == -1) {
                                                break;
                                        }
                                        
                                        if (i != 1) {
                                                throw new Error("invalid code "+i);
                                        }
                                        
                                        synchronized(TcpInput.this) {
                                                activeNum = num;
                                        }
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        synchronized(TcpInput.this) {
                                                if (activeNum == num) {
                                                        finish();
                                                }
                                        }
                                        
                                } catch (java.io.InterruptedIOException e) {
                                        break;
                                } catch (SocketException e) {
                                        break;
                                } catch (IOException e) {
                                        throw new Error(e);
                                }
                        }

                        upcallEnd.unlock();
                        log.out();
                }
        }        

	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable info    = new Hashtable();
			info.put("tcp_address", tcpServerSocket.getInetAddress());
			info.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "tcp"));
			os.writeObject(info);
                        os.close();

			tcpSocket  = tcpServerSocket.accept();
                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();
                        
			tcpIs 	   = new DataInputStream(tcpSocket.getInputStream());
			tcpOs 	   = new DataOutputStream(tcpSocket.getOutputStream());
                        this.spn = cnx.getNum();
                        if (upcallFunc != null) {
                                (upcallThread = new UpcallThread(addr+"["+port+"]")).start();
                        }
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
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
                log.in();
                if (activeNum != null) {
                        throw new Error("invalid call");
                }                

		if (spn == null) {
                        log.out("not connected");
			return null;
		}

		try {
			if (block || tcpIs.available() > 0) {
                                tcpIs.read();
				activeNum = spn;
			}
		} catch (IOException e) {
			throw new NetIbisException(e);
		} 

                log.out();
		return activeNum;
	}

        public void finish() throws NetIbisException {
                log.in();
                super.finish();
                if (_inputConvertStream != null) {
                        try {
                                _inputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }
                        
                        _inputConvertStream = null;
                }

                synchronized(this) {
                        activeNum = null;
                }
                log.out();
        }        

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                NetReceiveBuffer b = createReceiveBuffer(expectedLength);

                try {
                        for (int i = 0; i < expectedLength; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
                return b;
        }
        

        public void readByteBuffer(NetReceiveBuffer b) throws NetIbisException {
                log.in();
                try {
                        for (int i = 0; i < b.data.length; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
        }
        


	public boolean readBoolean() throws NetIbisException {
                log.in();
                boolean result = false;
                
		try {
                        result = tcpIs.readBoolean();
                } catch (IOException e) {
                        throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        

        
	public byte readByte() throws NetIbisException {
                log.in();
                byte result = 0;
                
		try {
                        result = tcpIs.readByte();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
        
	public char readChar() throws NetIbisException {
                log.in();
                char result = 0;
                
		try {
                        result = tcpIs.readChar();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
	public short readShort() throws NetIbisException {
                log.in();
                short result = 0;
                
		try {
                        result = tcpIs.readShort();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
	public int readInt() throws NetIbisException {
                log.in();
                int result = 0;
                
		try {
                        result = tcpIs.readInt();
                } catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
	public long readLong() throws NetIbisException {
                log.in();
                long result = 0;
                
		try {
                        result = tcpIs.readLong();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
	public float readFloat() throws NetIbisException {
                log.in();
                float result = 0;
                
		try {
                        result = tcpIs.readFloat();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }
        
	public double readDouble() throws NetIbisException {
                log.in();
                double result = 0;
                
		try {
                        result = tcpIs.readDouble();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }

	public String readString() throws NetIbisException {
                log.in();
                String result = "";
                
		try {
                        result = tcpIs.readUTF();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
                log.out();
   
                return result;
        }

        public Object readObject() throws NetIbisException {
                log.in();
                Object o = null;
                try {
                        if (_inputConvertStream == null) {
                                DummyInputStream dis = new DummyInputStream();
                                _inputConvertStream = new ObjectInputStream(dis);
                        }
                
                        o = _inputConvertStream.readObject();
                } catch (Exception e) {
                        throw new NetIbisException(e.getMessage());
                }
                log.out();
                
                return o;
        }
        
        
	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readBoolean();
                }
                log.out();
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readByte();
                }
                log.out();
        }


	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readChar();
                }
                log.out();
        }


	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readShort();
                }
                log.out();
        }


	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readInt();
                }
                log.out();
        }


	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                 log.in();
               for (int i = 0; i < l; i++) {
                        b[o+i] = readLong();
                }
                log.out();
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readFloat();
                }
                log.out();
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readDouble();
                }
                log.out();
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        b[o+i] = readObject();
                }
                log.out();
        }


	/**
	 * Reset the TCP connection if it exists.
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

                        if (upcallThread != null) {
                                upcallThread.end();
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
                log.out();
	}

	public void free() throws NetIbisException {
                log.in();
                doFree();
		super.free();
                log.out();
        }
        
        private final class DummyInputStream extends InputStream {
                private long seq = 0;


                public int read() throws IOException {
                        log.in();
                        int result = 0;
                        
                        try {
                                result = readByte();
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }

                        log.out();
                        return (result & 255);
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }

        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
                if (spn == num) {
                        doFree();
                }
                log.out();
        }
        
}
