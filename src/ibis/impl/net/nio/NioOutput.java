package ibis.ipl.impl.net.nio;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/**
 * The NIO TCP output implementation.
 */
public final class NioOutput extends NetOutput {

	public static int BUFFER_SIZE =	0x8000;	// bytes

	/**
	 * The communication channel.
	 */
	private SocketChannel                   socketChannel = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer                  rpn 	   = null;

	/**
	 * A nio buffer to hold the data. 
	 */
	private ByteBuffer 	outputBuffer = 
				ByteBuffer.allocateDirect(BUFFER_SIZE);

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the Tcp driver instance.
	 * @param output the controlling output.
	 */
	NioOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
		headerLength = 0;
	}

	/*
	 * Sets up an outgoing TCP connection (using nio).
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os,
                                    NetServiceListener nls)
		throws IbisIOException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = rpn;
	
		Hashtable   rInfo = receiveInfoTable(is);
		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
		InetSocketAddress sa = new InetSocketAddress(raddr, rport);
		
		try {
			socketChannel = SocketChannel.open(sa);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = 0;
	}

        public void finish() throws IbisIOException {
                super.finish();
 		try {
			outputBuffer.flip();
			while(outputBuffer.hasRemaining()) {
				socketChannel.write(outputBuffer);
			}
			outputBuffer.clear();
 		} catch (IOException e) {
 			throw new IbisIOException(e.getMessage());
 		} 
	}
                

	/*
	 * {@inheritDoc}
         */
	public void writeByte(byte b) throws IbisIOException {
 		try {
			if(!outputBuffer.hasRemaining()) {
				outputBuffer.flip();
				while(!outputBuffer.hasRemaining()) {
					socketChannel.write(outputBuffer);
				}
				outputBuffer.compact();
			}

			outputBuffer.put(b);
		
 		} catch (IOException e) {
 			throw new IbisIOException(e.getMessage());
 		} 
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (socketChannel != null) {
                                socketChannel.close();
			}

			rpn       = null;
			socketChannel = null;
			outputBuffer = null;

		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
