package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
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
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

public class TcpInput extends NetInput {
	private ServerSocket 	      tcpServerSocket = null;
	private Socket                tcpSocket       = null;
	private Integer               rpn  	      = null;
	private InputStream  	      tcpIs	      = null;
	private OutputStream 	      tcpOs	      = null;

	TcpInput(StaticProperties sp,
		 NetDriver        driver,
		 NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
	}

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
			tcpIs 	   = tcpSocket.getInputStream();
			tcpOs 	   = tcpSocket.getOutputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
	}

	public Integer poll() throws IbisIOException {
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

		return activeNum;
	}
	
	public NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException {

		byte [] b = new byte[expectedLength];
		try {
			tcpIs.read(b);
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
		
		return new NetReceiveBuffer(b, expectedLength);
	}

	/*
	 * We need a way to set timeout through properties 
	 */
	// timeout should be expressed in milliseconds
	void setReceiveTimeout(int timeout) throws IbisIOException {
		try {
			tcpSocket.setSoTimeout(timeout);
		} catch (SocketException e) {
			throw new IbisIOException(e);
		}
	}

	// returns the current reception timeout in milliseconds
	// 0 means an infinite timeout
	int getReceiveTimeout() throws IbisIOException {
		int t = 0;

		try {
			t = tcpSocket.getSoTimeout();
		} catch (SocketException e) {
			throw new IbisIOException(e);
		}

		return t;
	}

	public void free() throws IbisIOException {
		try {
			if (tcpOs != null) {
				tcpOs.close();
			}
		
			if (tcpIs != null) {
				tcpIs.close();
			}

			if (tcpSocket != null) {
				tcpSocket.shutdownOutput();
				tcpSocket.shutdownInput();
				tcpSocket.close();
			}

			if (tcpServerSocket != null) {
				tcpServerSocket.close();
			}

			tcpSocket       = null;
			tcpServerSocket = null;
			rpn             = null;
			tcpIs           = null;
			tcpOs           = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
