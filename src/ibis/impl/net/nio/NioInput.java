package ibis.ipl.impl.net.nio;

import ibis.ipl.impl.net.*;

import ibis.ipl.ConnectionClosedException;

import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisDissipator;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.NetworkInterface;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;
import java.util.Enumeration;

import java.lang.Math;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;

/**
 * The NIO/TCP input implementation.
 */
public final class NioInput extends NetInput implements IbisDissipator {

	public static int	BUFFER_SIZE = 256;  // bytes

	public static int	BYTE_BUFFER = 0,
				CHAR_BUFFER = 1,
				SHORT_BUFFER = 2,
				INT_BUFFER = 3,
				LONG_BUFFER = 4,
				FLOAT_BUFFER = 5,
				DOUBLE_BUFFER = 6;
				
	public static int	NR_OF_PRIMITIVES = 7;

	/**
	 * The connection socket channel.
	 */
	private ServerSocketChannel		serverSocketChannel = null;

	/**
	 * The communication socket channel.
	 */
	private SocketChannel			socketChannel = null;

	/**
	 * The peer NetSendPort local number.
	 */
	private volatile Integer		spn = null;

	private InetAddress			addr = null;
	private int				port = 0;

	private long				bytesRead = 0;

	/**
	 * if isPendingByte a byte has been read from the channel,
	 * but has not been processed yet.
	 */
	private ByteBuffer			oneByteBuffer;
	private boolean				bytePending = false;

	/**
	 * an array of nio buffers used to hold all the primitive buffers.
	 */
	ByteBuffer[]	primitiveBuffers = new ByteBuffer[NR_OF_PRIMITIVES];

	/**
	 * nio buffer used to hold the header
	 */
	ByteBuffer	headerByteBuffer;

	/* views of the bytebuffers to fill/drain them */

	IntBuffer	header;
	ByteBuffer	byteBuffer;
	CharBuffer	charBuffer;
	ShortBuffer	shortBuffer;
	IntBuffer	intBuffer;
	LongBuffer	longBuffer;
	FloatBuffer	floatBuffer;
	DoubleBuffer	doubleBuffer;

	/**
	 * stream used to read objects and strings from the stream
	 */
	private IbisSerializationInputStream	serializationStream = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the Nio driver instance.
	 * @param input the controlling input.
	 */
	NioInput(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context);
		headerLength = 0;

		/* init buffers */

		headerByteBuffer = 
				ByteBuffer.allocateDirect(NR_OF_PRIMITIVES * 4).
				order(ByteOrder.nativeOrder());
		headerByteBuffer.clear();

		for(int i =  BYTE_BUFFER; i <= DOUBLE_BUFFER; i++) {
			primitiveBuffers[i] = 
				ByteBuffer.allocateDirect(BUFFER_SIZE).
				order(ByteOrder.nativeOrder());
			primitiveBuffers[i].clear();
		}

		header = headerByteBuffer.asIntBuffer();
		byteBuffer = primitiveBuffers[BYTE_BUFFER]; // just a shorthand
		charBuffer = primitiveBuffers[CHAR_BUFFER].asCharBuffer();
		shortBuffer = primitiveBuffers[SHORT_BUFFER].asShortBuffer();
		intBuffer = primitiveBuffers[INT_BUFFER].asIntBuffer();
		longBuffer = primitiveBuffers[LONG_BUFFER].asLongBuffer();
		floatBuffer = primitiveBuffers[FLOAT_BUFFER].asFloatBuffer();
		doubleBuffer = primitiveBuffers[DOUBLE_BUFFER].asDoubleBuffer();

		header.clear();
		/* set the buffers so that they appear empty
		   this way they will trigger a receive when touched */
		byteBuffer.limit(0);
		charBuffer.limit(0);
		shortBuffer.limit(0);
		intBuffer.limit(0);
		longBuffer.limit(0);
		floatBuffer.limit(0);
		doubleBuffer.limit(0);

		oneByteBuffer = ByteBuffer.allocateDirect(1).
					order(ByteOrder.nativeOrder());
		oneByteBuffer.clear();

	}

	public void initReceive(Integer num) {
		// NOTHING
	}


	/**
	  * return the "number"th local InetAddress. use this if there is more
	  * then one network interface in a machine
	  */
	private InetAddress getLocalAddress(int number) 
						throws IOException {
	    Enumeration interfaces, addresses;
	    NetworkInterface networkInterface;
	    InetAddress address;
	    int current = 0;

	    interfaces = NetworkInterface.getNetworkInterfaces();

	    if (interfaces == null) {
		throw new IOException("no network interfaces found");
	    }

	    while(interfaces.hasMoreElements()) {
		networkInterface = 
			    (NetworkInterface)interfaces.nextElement();

		addresses = networkInterface.getInetAddresses();

		while(addresses.hasMoreElements()) {
		    address = (InetAddress)addresses.nextElement();

		    if(number == current) {
			    return address;
		    }

		    current  += 1;
		}
	    }

	    throw new IOException("Interface " + number +
		    "not found. Only " + current + " interfaces found");
	}
		    
	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(NetConnection cnx) throws IOException {

		if (this.spn != null) {
			throw new Error("connection already established");
		}
		
		serverSocketChannel = ServerSocketChannel.open();
		ServerSocket tcpServerSocket = 
					serverSocketChannel.socket();

		InetSocketAddress socketAddress = 
			new InetSocketAddress(getLocalAddress(0), 0);

		tcpServerSocket.bind(socketAddress, 1);

		Hashtable lInfo    = new Hashtable();
		lInfo.put("tcp_address", 
				tcpServerSocket.getInetAddress());
		lInfo.put("tcp_port", new Integer(
				tcpServerSocket.getLocalPort()));
		lInfo.put("byte_order", 
				ByteOrder.nativeOrder().toString());

		ObjectOutputStream os = new ObjectOutputStream(
		 cnx.getServiceLink().getOutputSubStream(
		  this, "nio"));
		os.writeObject(lInfo);
		os.close();

		socketChannel = serverSocketChannel.accept();
		// use bocking mode
		socketChannel.configureBlocking(true);

		socketChannel.socket().setReceiveBufferSize(0x8000);

		addr = socketChannel.socket().getInetAddress();
		port = socketChannel.socket().getPort();
		this.spn = cnx.getNum();
									
		startUpcallThread();

		mtu = 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Poll by setting the channel to non blocking mode, try to receive one
	 * byte, and set it back to blocking mode again. This makes poll a bit
	 * expensive. If block is true keep the channel in blocking mode.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer doPoll(boolean block) throws IOException {
		if (spn == null) {
			// not connected yet
			return null;
		}

		try {
		   
		    if (!block) { 
			    socketChannel.configureBlocking(false);
		    }

		    if(bytePending) {
			    throw new IOException("NioInput: Eek! Trying to poll while there is still a byte pending!");
		    }

		    do {

			oneByteBuffer.clear();
			if (socketChannel.read(oneByteBuffer) == -1) {
				log.out("Received an end-of-stream");
				throw new ConnectionClosedException(
						"ConnectionClosedException");
			}

			if(oneByteBuffer.position() != 0) {	
				bytePending = true;
				oneByteBuffer.flip();
				bytesRead += 1;
				if(!block) {
					socketChannel.configureBlocking(true);
				}
				return spn;
			}

		    } while(!bytePending && block);

		} catch (AsynchronousCloseException e) {
			throw new ConnectionClosedException(e.getMessage());
		} catch (ConnectionClosedException e) {
			throw new ConnectionClosedException(e);
		} catch (ClosedChannelException e) {
			throw new ConnectionClosedException(e.getMessage());
		} catch (IOException e) {
			throw e;
		} finally {
			try {
			    if(!block) {
				socketChannel.configureBlocking(true);
			    }
			} catch(IOException e) {
				// IGNORE
			}
		}
				
		return null;
	}

	protected void doFinish() throws IOException {
		// NOTHING
	}

	/**
	 * Gets the primitive buffers from the channel
	 */
	private void receive() throws IOException {
		int lastBuffer;

		/* fill the header from the channel */
	
		headerByteBuffer.clear();

		// see if we "polled" a byte, and put it in the header if so
		if(bytePending) {
			headerByteBuffer.put(oneByteBuffer.get(0));
			bytePending = false;
		}

		do {
			bytesRead += socketChannel.read(headerByteBuffer);
		} while (headerByteBuffer.remaining() != 0);

		headerByteBuffer.flip();

		header.position(headerByteBuffer.position() / 4);
		header.limit(headerByteBuffer.limit() / 4);


		/* set up the primitive buffer so they can be filled */

		primitiveBuffers[BYTE_BUFFER].position(0);
		primitiveBuffers[BYTE_BUFFER].limit(header.get(BYTE_BUFFER));

		primitiveBuffers[CHAR_BUFFER].position(0);
		primitiveBuffers[CHAR_BUFFER].limit(header.get(CHAR_BUFFER));

		primitiveBuffers[SHORT_BUFFER].position(0);
		primitiveBuffers[SHORT_BUFFER].limit(header.get(SHORT_BUFFER));

		primitiveBuffers[INT_BUFFER].position(0);
		primitiveBuffers[INT_BUFFER].limit(header.get(INT_BUFFER));

		primitiveBuffers[LONG_BUFFER].position(0);
		primitiveBuffers[LONG_BUFFER].limit(header.get(LONG_BUFFER));

		primitiveBuffers[FLOAT_BUFFER].position(0);
		primitiveBuffers[FLOAT_BUFFER].limit(header.get(FLOAT_BUFFER));

		primitiveBuffers[DOUBLE_BUFFER].position(0);
		primitiveBuffers[DOUBLE_BUFFER].limit(
						header.get(DOUBLE_BUFFER));

		lastBuffer = 0;
		for(int i = 0; i <= DOUBLE_BUFFER; i++) {
			if(primitiveBuffers[i].hasRemaining()) {
				lastBuffer = i;
			}
		}
			
		/* receive the primitive buffer from the channel */
		do {
			bytesRead += socketChannel.read(primitiveBuffers);
		} while (primitiveBuffers[lastBuffer].hasRemaining());

		/* set the views of the buffers so they can be used in all the
		 * read functions 
		 */

		primitiveBuffers[BYTE_BUFFER].flip();

		charBuffer.position(0);
		charBuffer.limit(header.get(CHAR_BUFFER) / 2);

		shortBuffer.position(0);
		shortBuffer.limit(header.get(SHORT_BUFFER) / 2);

		intBuffer.position(0);
		intBuffer.limit(header.get(INT_BUFFER) / 4);

		longBuffer.position(0);
		longBuffer.limit(header.get(LONG_BUFFER) / 8);

		floatBuffer.position(0);
		floatBuffer.limit(header.get(FLOAT_BUFFER) / 4);

		doubleBuffer.position(0);
		doubleBuffer.limit(header.get(DOUBLE_BUFFER) / 8);
	}

	public NetReceiveBuffer readByteBuffer(int expectedLength) 
						throws IOException {
                int len = readInt();
                NetReceiveBuffer buffer = createReceiveBuffer(len);
                readArray(buffer.data, 0, len);
                return buffer;
        }
                                                                                
                                                                                
        public void readByteBuffer(NetReceiveBuffer buffer) 
						throws IOException {
                int len = readInt();
                readArray(buffer.data, 0, len);
                buffer.length = len;
        }

	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public boolean readBoolean() throws IOException {
		return (readByte() == (byte)1 );
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public byte readByte() throws IOException {
		try {
			return byteBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readByte();
		} 
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public char readChar() throws IOException {
		try {
			return charBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readChar();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public short readShort() throws IOException {
		try {
			return shortBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readShort();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public int readInt() throws IOException {
		try {
			return intBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readInt();
		} 
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public long readLong() throws IOException {
		try {
			return longBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readLong();
		} 
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public float readFloat() throws IOException {
		try {
			return floatBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readFloat();
		} 
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public double readDouble() throws IOException {
		try {
			return doubleBuffer.get();
		} catch (BufferUnderflowException e) {
			// we ran out of things to read, receive some...
			receive();
			// ...and try again
			return readDouble();
		} 
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public String readString() throws IOException {
	    if (serializationStream == null) {
		serializationStream = new IbisSerializationInputStream(this);
	    }

	    return serializationStream.readUTF();
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public Object readObject() throws IOException, ClassNotFoundException {
	    if (serializationStream == null) {
		serializationStream = new IbisSerializationInputStream(this);
	    }

	    return serializationStream.readObject();
	}



	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(boolean [] destination, 
					  int offset, 
					  int size) throws IOException {
	    ByteBuffer buffer;

	    buffer = ByteBuffer.allocateDirect(size);

	    /* check if a poll was done lately...
	       should not happen, but check anyway */
	    if(bytePending) {
		    buffer.put(oneByteBuffer.get(0));
		    bytePending = false;
	    }

	    /* receive the data from the channel */
	    do {
		    socketChannel.read(buffer);
	    } while (buffer.hasRemaining());

	    buffer.flip();

	    for(int i = offset; i <  (offset + size); i++) {
		destination[i] = (buffer.get() == (byte)1);
	    }

	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(byte [] destination, 
				       int offset, 
				       int size) throws IOException {
		ByteBuffer wrap;
		int length;

		if (size == 0) {
		    return; // Nothing to do...
		}

		if (size < 100) {
		    while(size > 0) {
			if(!byteBuffer.hasRemaining()) {
			    receive();
			}

			length = Math.min(byteBuffer.remaining(), size);
			byteBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		    }
		} else {
		    wrap = ByteBuffer.wrap(destination, offset, size);

		    /* check if a poll was done lately...
		       should not happen, but check anyway */
		    if(bytePending) {
			    wrap.put(oneByteBuffer.get(0));
			    bytePending = false;
		    }

		    /* receive the data from the channel */
		    do {
			    socketChannel.read(wrap);
		    } while (wrap.hasRemaining());
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(char [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;

		while(size > 0) {
		    if(!charBuffer.hasRemaining()) {
			receive();
		    }

		    length = Math.min(charBuffer.remaining(), size);
		    charBuffer.get(destination, offset, length);
		    size -= length;
		    offset += length;
		}
	}



	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(short [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;
		while(size > 0) {
		    if(!shortBuffer.hasRemaining()) {
			receive();
		    }
		    length = Math.min(shortBuffer.remaining(), size);
		    shortBuffer.get(destination, offset, length);
		    size -= length;
		    offset += length;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(int [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;
		while(size > 0) {
			if(!intBuffer.hasRemaining()) {
				receive();
			}
			length = Math.min(intBuffer.remaining(), size);
			intBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(long [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;

		while(size > 0) {
			if(!longBuffer.hasRemaining()) {
				receive();
			}
			length = Math.min(longBuffer.remaining(), size);
			longBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
	}


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(float [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;
		while(size > 0) {
		    if(!floatBuffer.hasRemaining()) {
			receive();
		    }
		    length = Math.min(floatBuffer.remaining(), size);
		    floatBuffer.get(destination, offset, length);
		    size -= length;
		    offset += length;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected 
	 * data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void readArray(double [] destination, 
				       int offset, 
				       int size) throws IOException {
		int length;

		while(size > 0) {
		    if(!doubleBuffer.hasRemaining()) {
			    receive();
		    }
		    length = Math.min(doubleBuffer.remaining(), size);
		    doubleBuffer.get(destination, offset, length);
		    size -= length;
		    offset += length;
		}
	}



	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {

		if(serverSocketChannel != null) {
			serverSocketChannel.close();
		}

		if(socketChannel != null) {
			socketChannel.close();
		}

		spn = null;
	}

        protected synchronized void doClose(Integer num)
						throws IOException {
                if (spn == num) {
                        doFree();
                }
        }
 
	// *** functions needed for the Dissipator interface ***

	public int available() {
		// not used anyway, don't bother;
		return 0;
	}

	public void close() {
		// NOTHING
	}

	public long bytesRead() {
		return bytesRead;
	}

	public void resetBytesRead() {
		bytesRead = 0;
	}
}
