package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

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
public class TcpOutput extends NetOutput {

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

        private InetAddress raddr = null;
        private int         rport =    0;



	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the TCP driver instance.
	 * @param output the controlling output.
	 */
	TcpOutput(StaticProperties sp,
		  NetDriver   	   driver,
		  NetIO   	   up)
		throws IbisIOException {
		super(sp, driver, up);
	}

	/*
	 * Sets up an outgoing TCP connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                  rpn,
				    ObjectInputStream 	     is,
				    ObjectOutputStream	     os)
		throws IbisIOException {
		this.rpn = rpn;
	
		Hashtable   remoteInfo = receiveInfoTable(is);
		raddr =  (InetAddress)remoteInfo.get("tcp_address");
		rport = ((Integer)    remoteInfo.get("tcp_port")   ).intValue();

		try {
			tcpSocket = new Socket(raddr, rport);
			tcpOs 	  = new DataOutputStream(tcpSocket.getOutputStream());
			tcpIs 	  = new DataInputStream(tcpSocket.getInputStream());
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = 0;
	}

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean b) throws IbisIOException {
                try {
			tcpOs.writeBoolean(b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte b) throws IbisIOException {
                try {
                        //System.err.println("sending byte: ["+b+"] to "+raddr+"("+rport+")");
			tcpOs.writeByte((int)b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char b) throws IbisIOException {
                try {
			tcpOs.writeChar((int)b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short b) throws IbisIOException {
                try {
			tcpOs.writeShort((int)b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int b) throws IbisIOException {
                try {
			tcpOs.writeInt((int)b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long b) throws IbisIOException {
                try {
			tcpOs.writeLong(b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float b) throws IbisIOException {
                try {
			tcpOs.writeFloat(b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double b) throws IbisIOException {
                try {
			tcpOs.writeDouble(b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }


        public void writeSubArrayBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeBoolean(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeByte(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }
        public void writeSubArrayChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeChar(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeShort(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeInt(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeLong(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeFloat(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeSubArrayDouble(double [] b, int o, int l) throws IbisIOException {
                try {
                        for (int i = 0; i < l; i++) {
			        tcpOs.writeDouble(b[o+i]);
                        }
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
        }

        public void writeArrayBoolean(boolean [] b) throws IbisIOException {
                writeSubArrayBoolean(b, 0, b.length);
        }

        public void writeArrayByte(byte [] b) throws IbisIOException {
                writeSubArrayByte(b, 0, b.length);
        }

        public void writeArrayChar(char [] b) throws IbisIOException {
                writeSubArrayChar(b, 0, b.length);
        }

        public void writeArrayShort(short [] b) throws IbisIOException {
                writeSubArrayShort(b, 0, b.length);
        }

        public void writeArrayInt(int [] b) throws IbisIOException {
                writeSubArrayInt(b, 0, b.length);
        }


        public void writeArrayLong(long [] b) throws IbisIOException {
                writeSubArrayLong(b, 0, b.length);
        }

        public void writeArrayFloat(float [] b) throws IbisIOException {
                writeSubArrayFloat(b, 0, b.length);
        }

        public void writeArrayDouble(double [] b) throws IbisIOException {
                writeSubArrayDouble(b, 0, b.length);
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

			rpn = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
}
