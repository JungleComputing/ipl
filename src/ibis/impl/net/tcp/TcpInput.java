package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

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
	private Integer            spn  	      = null;
	private DataInputStream    tcpIs	      = null;
	private DataOutputStream   tcpOs	      = null;
        private InetAddress        addr               = null;
        private int                port               =    0;
        private ObjectInputStream _inputConvertStream = null;        
        private long               seq                =    0;
        private UpcallThread       upcallThread       = null;
        private NetMutex           upcallEnd          = new NetMutex(true);

	TcpInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
	}

        private final class UpcallThread extends Thread {

                public UpcallThread(String name) {
                        super("TcpInput.UpcallThread: "+name);
                }                
                
                public void run() {
                        while (true) {
                                try {
                                        int i = tcpIs.read();
                                        if (i == -1) {
                                                break;
                                        }
                                        
                                        if (i != 1) {
                                                throw new Error("invalid code "+i);
                                        }
                                        
                                        activeNum = spn;
                                        upcallFunc.inputUpcall(TcpInput.this, activeNum);
                                        activeNum = null;
                                } catch (java.io.InterruptedIOException e) {
                                        break;
                                } catch (SocketException e) {
                                        break;
                                } catch (IOException e) {
                                        throw new Error(e);
                                }
                        }

                        upcallEnd.unlock();
                }
        }        

	public void setupConnection(Integer                spn,
				    ObjectInputStream 	   is,
				    ObjectOutputStream	   os,
                                    NetServiceListener     nls)
		throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = spn;
		 
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable info    = new Hashtable();
			info.put("tcp_address", tcpServerSocket.getInetAddress());
			info.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
			sendInfoTable(os, info);

			tcpSocket  = tcpServerSocket.accept();
                        addr = tcpSocket.getInetAddress();
                        port = tcpSocket.getPort();
                        
			tcpIs 	   = new DataInputStream(tcpSocket.getInputStream());
			tcpOs 	   = new DataOutputStream(tcpSocket.getOutputStream());
                        if (upcallFunc != null) {
                                (upcallThread = new UpcallThread(addr+"["+port+"]")).start();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This TCP polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (spn == null) {
			return null;
		}

		try {
			if (tcpIs.available() > 0) {
                                tcpIs.read();
				activeNum = spn;
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 

		return activeNum;
	}

       	public void finish() throws IbisIOException {
                super.finish();
                if (_inputConvertStream != null) {
                        try {
                                _inputConvertStream.close();
                        } catch (IOException e) {
                                throw new IbisIOException(e.getMessage());
                        }
                        
                        _inputConvertStream = null;
                }

                activeNum = null;
        }
        
        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                NetReceiveBuffer b = createReceiveBuffer(expectedLength);

                try {
                        for (int i = 0; i < expectedLength; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

                return b;
        }
        

        public void readByteBuffer(NetReceiveBuffer b) throws IbisIOException {
                try {
                        for (int i = 0; i < b.data.length; i++) {
			        b.data[i] = readByte();
                                b.length++;
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }
        


	public boolean readBoolean() throws IbisIOException {
                boolean result = false;
                
		try {
                        result = tcpIs.readBoolean();
                } catch (IOException e) {
                        throw new IbisIOException(e);
		}
   
                return result;
        }
        

        
	public byte readByte() throws IbisIOException {
                byte result = 0;
                
		try {
                        result = tcpIs.readByte();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
        
	public char readChar() throws IbisIOException {
                char result = 0;
                
		try {
                        result = tcpIs.readChar();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
	public short readShort() throws IbisIOException {
                short result = 0;
                
		try {
                        result = tcpIs.readShort();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
	public int readInt() throws IbisIOException {
                int result = 0;
                
		try {
                        result = tcpIs.readInt();
                } catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
	public long readLong() throws IbisIOException {
                long result = 0;
                
		try {
                        result = tcpIs.readLong();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
	public float readFloat() throws IbisIOException {
                float result = 0;
                
		try {
                        result = tcpIs.readFloat();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }
        
	public double readDouble() throws IbisIOException {
                double result = 0;
                
		try {
                        result = tcpIs.readDouble();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }

	public String readString() throws IbisIOException {
                String result = "";
                
		try {
                        result = tcpIs.readUTF();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
   
                return result;
        }

        public Object readObject() throws IbisIOException {
                Object o = null;
                try {
                        if (_inputConvertStream == null) {
                                DummyInputStream dis = new DummyInputStream();
                                _inputConvertStream = new ObjectInputStream(dis);
                        }
                
                        o = _inputConvertStream.readObject();
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return o;
        }
        
        
	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readBoolean();
                }
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readByte();
                }
        }


	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readChar();
                }
        }


	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readShort();
                }
        }


	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readInt();
                }
        }


	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readLong();
                }
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readFloat();
                }
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readDouble();
                }
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        b[o+i] = readObject();
                }
        }


	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (tcpOs != null) {
                                tcpOs.close();
			}
		
			if (tcpIs != null) {
				tcpIs.close();
			}

                        if (upcallThread != null) {
                                upcallThread.interrupt();
                                upcallEnd.lock();
                                upcallThread = null;
                        }

			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			if (tcpServerSocket != null) {
                                tcpServerSocket.close();
			}
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}

        private final class DummyInputStream extends InputStream {
                private long seq = 0;


                public int read() throws IOException {
                        int result = 0;
                        
                        try {
                                result = readByte();
                        } catch (IbisIOException e) {
                                throw new IOException(e.getMessage());
                        }

                        return (result & 255);
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }
}
