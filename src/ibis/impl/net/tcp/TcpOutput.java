package ibis.ipl.impl.net.tcp;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
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

public class TcpOutput extends NetOutput {

	private Socket                   tcpSocket = null;   
	private Integer                  rpn 	   = null;
	private InputStream  	         tcpIs	   = null;
	private OutputStream 	         tcpOs	   = null;

	TcpOutput(StaticProperties sp,
		  NetDriver   	   driver,
		  NetOutput   	   output)
		throws IbisIOException {
		super(sp, driver, output);
	}

	public void setupConnection(Integer                  rpn,
				    ObjectInputStream 	     is,
				    ObjectOutputStream	     os)
		throws IbisIOException {
		this.rpn = rpn;
	
		Hashtable   remoteInfo = receiveInfoTable(is);
		InetAddress raddr =  (InetAddress)remoteInfo.get("tcp_address");
		int         rport = ((Integer)    remoteInfo.get("tcp_port")   ).intValue();

		try {
			tcpSocket = new Socket(raddr, rport);
			tcpOs 	  = tcpSocket.getOutputStream();
			tcpIs 	  = tcpSocket.getInputStream();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = 0;
	}

	public void sendBuffer(NetSendBuffer b) throws IbisIOException {
		try {
			tcpOs.write(b.data, 0, b.length);
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
	}

	public void release() {
		// nothing
	}

	public void reset() {
		// nothing
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
