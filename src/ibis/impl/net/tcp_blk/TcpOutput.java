package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

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
public class TcpOutput extends NetBufferedOutput {

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

	/**
	 * The local MTU.
	 */
	private int                      lmtu      = 32768;

	/**
	 * The remote MTU.
	 */
	private int                      rmtu      =   0;
        
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
		  NetOutput   	   output)
		throws IbisIOException {
		super(sp, driver, output);
		headerLength = 4;
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
	
		Hashtable   rInfo = receiveInfoTable(is);
		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
		rmtu  		  = ((Integer)	  rInfo.get("tcp_mtu")    ).intValue();
		
		Hashtable lInfo = new Hashtable();
		lInfo.put("tcp_mtu",     new Integer(lmtu));
		sendInfoTable(os, lInfo);

		try {
			tcpSocket = new Socket(raddr, rport);

			tcpSocket.setSendBufferSize(0x8000);
			tcpSocket.setReceiveBufferSize(0x8000);
			tcpSocket.setTcpNoDelay(true);
			
			tcpOs 	  = tcpSocket.getOutputStream();
			tcpIs 	  = tcpSocket.getInputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = Math.min(lmtu, rmtu);
	}


	/*
	 * {@inheritDoc}
         */
	public void writeByteBuffer(NetSendBuffer b) throws IbisIOException {
		try {
                        //System.err.println("tcp_blk: writeByteBuffer --> : " + b.length);
			writeInt(b.data, 0, b.length);
			tcpOs.write(b.data, 0, b.length);
                        //System.err.println("tcp_blk: writeByteBuffer <-- : " + b.length);
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		// nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		// nothing
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

			if (tcpSocket != null) {
                                tcpSocket.close();
			}

			tcpSocket = null;
			rpn       = null;
			tcpIs     = null;
			tcpOs     = null;

		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
