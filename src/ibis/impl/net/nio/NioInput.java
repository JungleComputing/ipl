package ibis.ipl.impl.net.nio;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * The NIO/TCP input implementation.
 */
public final class NioInput extends NetInput {

	public static int BUFFER_SIZE = 0x8000;  // bytes

	/**
	 * The connection socket channel.
	 */
	private ServerSocketChannel 	      serverSocketChannel = null;

	/**
	 * The communication socket channel.
	 */
	private SocketChannel                socketChannel       = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer               spn  	      = null;


        private UpcallThread          upcallThread   = null;
        private NetMutex              upcallEnd      = new NetMutex(true);
        private InetAddress           addr           = null;
        private int                   port           =    0;

	/**
	 * The Nio buffer used for temp storage
	 */
	private ByteBuffer		inputBuffer	= 
					ByteBuffer.allocateDirect(BUFFER_SIZE);

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the Nio driver instance.
	 * @param input the controlling input.
	 */
	NioInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
		headerLength = 0;
	}

        private final class UpcallThread extends Thread {
                
                public UpcallThread(String name) {
                        super("NioInput.UpcallThread: "+name);
                }                
                
                public void run() {
                        while (true) {
                                try {
					inputBuffer.clear();
					socketChannel.read(inputBuffer);	

                                        if (inputBuffer.position() == 0) {
                                                break;
					}

					System.err.println("Received " + inputBuffer.position() + " bytes");

					inputBuffer.flip();

                                        activeNum = spn;
                                        upcallFunc.inputUpcall(NioInput.this, activeNum);
                                        activeNum = null;
                                } catch (java.nio.channels.AsynchronousCloseException e) {
					// meaning the send port was closed
                                        break;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }

                        upcallEnd.unlock();
                }
        }

	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer            spn,
				    ObjectInputStream  is,
				    ObjectOutputStream os,
                                    NetServiceListener nls)
		throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = spn;
		 
		try {
			serverSocketChannel = ServerSocketChannel.open();
			ServerSocket tcpServerSocket = 
						serverSocketChannel.socket();
			InetSocketAddress socketAddress = new InetSocketAddress(
						InetAddress.getLocalHost(), 0);
			tcpServerSocket.bind(socketAddress, 1);
			Hashtable lInfo    = new Hashtable();
			lInfo.put("tcp_address", 
					tcpServerSocket.getInetAddress());
			lInfo.put("tcp_port", new Integer(
					tcpServerSocket.getLocalPort()));
                        sendInfoTable(os, lInfo);

			socketChannel = serverSocketChannel.accept();
			// use bocking mode normally, non-blocking only used
			// when polling
			socketChannel.configureBlocking(true);

                        addr = socketChannel.socket().getInetAddress();
                        port = socketChannel.socket().getPort();

		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		mtu = 0;

                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread(addr+"["+port+"]")).start();
                }
	}

	/**
	 * {@inheritDoc}
	 *
	 * Poll by setting receive mode to non-blocking, try to receive, and
	 * return the mode to blocking again. 
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (spn == null) {
			// not connected yet
			return null;
		}

		try {
			socketChannel.configureBlocking(false);

			inputBuffer.clear();
			socketChannel.read(inputBuffer);
			
			if(inputBuffer.position() != 0) {
				// we received something!
				inputBuffer.flip();
				activeNum = spn;
			}
			socketChannel.configureBlocking(true);
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 

		return activeNum;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public byte readByte() throws IbisIOException {
                        
		try {
			if(!inputBuffer.hasRemaining()) {
				inputBuffer.clear();
				if(socketChannel.read(inputBuffer) == 0) {
				// although we are in blocking mode, we still
				// received nothing, so we probably encountered
				// an eof
                                        throw new IbisIOException("unexpected EOF");
                                }
				inputBuffer.flip();
			}
		} catch (IOException e) {
			throw new IbisIOException(e.getMessage());
		} 

                return inputBuffer.get();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {

			if(serverSocketChannel != null) {
				serverSocketChannel.close();
			}

			if(socketChannel != null) {
				socketChannel.close();
			}

			inputBuffer = null;
		
                        if (upcallThread != null) {
                                upcallThread.interrupt();
                                upcallEnd.lock();
                                upcallThread = null;
                        }

		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
	
}
