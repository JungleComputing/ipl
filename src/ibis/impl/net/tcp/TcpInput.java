package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

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


/**
 * The TCP input implementation.
 */
public class TcpInput extends NetInput {

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
	private Integer               rpn  	      = null;

	/**
	 * The communication input stream.
	 */
	private DataInputStream  	      tcpIs	      = null;

	/**
	 * The communication output stream.
	 *
	 * <BR><B>Note</B>: this stream is not really needed but may be used 
	 * for debugging purpose.
	 */
	private DataOutputStream 	      tcpOs	      = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param input the controlling input.
	 */
	TcpInput(StaticProperties sp,
		 NetDriver        driver,
		 NetIO            up)
		throws IbisIOException {
		super(sp, driver, up);
	}


	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                rpn,
				    ObjectInputStream 	   is,
				    ObjectOutputStream	   os)
		throws IbisIOException {
		this.rpn = rpn;
		 
		try {
			tcpServerSocket   = new ServerSocket(0, 1, InetAddress.getLocalHost());
			Hashtable info    = new Hashtable();
			info.put("tcp_address", tcpServerSocket.getInetAddress());
			info.put("tcp_port",    new Integer(tcpServerSocket.getLocalPort()));
			sendInfoTable(os, info);

			tcpSocket  = tcpServerSocket.accept();
			tcpIs 	   = new DataInputStream(tcpSocket.getInputStream());
			tcpOs 	   = new DataOutputStream(tcpSocket.getOutputStream());
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
                //System.err.println("TCP: doPoll -->");
		activeNum = null;

		if (rpn == null) {
			return null;
		}

		try {
			if (tcpIs.available() > 0) {
				activeNum = rpn;
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
                //System.err.println("TCP: doPoll <--");

		return activeNum;
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
        
	public void readSubArrayBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readBoolean();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readByte();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readChar();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readShort();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readInt();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readLong();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readFloat();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


	public void readSubArrayDouble(double [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        b[o+i] = tcpIs.readDouble();
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

	public void readArrayBoolean(boolean [] b) throws IbisIOException {
                readSubArrayBoolean(b, 0, b.length);
        }


	public void readArrayByte(byte [] b) throws IbisIOException {
                readSubArrayByte(b, 0, b.length);
        }


	public void readArrayChar(char [] b) throws IbisIOException {
                readSubArrayChar(b, 0, b.length);
        }


	public void readArrayShort(short [] b) throws IbisIOException {
                readSubArrayShort(b, 0, b.length);
        }


	public void readArrayInt(int [] b) throws IbisIOException {
                readSubArrayInt(b, 0, b.length);
        }


	public void readArrayLong(long [] b) throws IbisIOException {
                readSubArrayLong(b, 0, b.length);
        }


	public void readArrayFloat(float [] b) throws IbisIOException {
                readSubArrayFloat(b, 0, b.length);
        }


	public void readArrayDouble(double [] b) throws IbisIOException {
                readSubArrayDouble(b, 0, b.length);
        }
	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (tcpOs != null) {
                                tcpOs.close();
                                tcpOs = null;
			}
		
			if (tcpIs != null) {
				tcpIs.close();
                                tcpIs = null;
			}

			if (tcpSocket != null) {
                                tcpSocket.close();
                                tcpSocket = null;
			}

			if (tcpServerSocket != null) {
                                tcpServerSocket.close();
                                tcpServerSocket = null;

			}

			rpn = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
}
