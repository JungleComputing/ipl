package ibis.ipl.impl.net.nio;

import ibis.ipl.impl.net.*;

import ibis.ipl.ConnectionClosedException;

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
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;

/**
 * The NIO/TCP input implementation.
 */
public final class NioInput extends NetInput {

	public static int 	BUFFER_SIZE = 2048;  // bytes

	public static int 	BYTE_BUFFER = 0,
				CHAR_BUFFER = 1,
				SHORT_BUFFER = 2,
				INT_BUFFER = 3,
				LONG_BUFFER = 4,
				FLOAT_BUFFER = 5,
				DOUBLE_BUFFER = 6;
				
	public static int 	NR_OF_PRIMITIVES = 7;

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
	private volatile Integer      spn  	     = null;


        private InetAddress           addr           = null;
        private int                   port           =    0;

	private int			bytesRead = 0;

	/**
	 * if isPendingByte a byte has been read from the channel,
	 * but has not been processed yet.
	 */
	private ByteBuffer		oneByteBuffer;
	private boolean			bytePending = false;

	/**
	 * an array of nio buffers used to hold all the primitive buffers.
	 */
	ByteBuffer[] 	primitiveBuffers = new ByteBuffer[NR_OF_PRIMITIVES];

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


		headerByteBuffer = 
				ByteBuffer.allocateDirect(NR_OF_PRIMITIVES * 4).
				order(ByteOrder.LITTLE_ENDIAN);
		headerByteBuffer.clear();

		for(int i =  BYTE_BUFFER; i <= DOUBLE_BUFFER; i++) {
			primitiveBuffers[i] = 
				ByteBuffer.allocateDirect(BUFFER_SIZE).
				order(ByteOrder.LITTLE_ENDIAN);
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
		/* set the buffers so that they appear empty */
		byteBuffer.clear().limit(0);
		charBuffer.clear().limit(0);
		shortBuffer.clear().limit(0);
		intBuffer.clear().limit(0);
		longBuffer.clear().limit(0);
		floatBuffer.clear().limit(0);
		doubleBuffer.clear().limit(0);

		oneByteBuffer = ByteBuffer.allocateDirect(1).
					order(ByteOrder.LITTLE_ENDIAN);
		oneByteBuffer.clear();

	}

	public void initReceive(Integer num) {
		log.in();
		// NOTHING
		log.out();
	}

	/*
	 * Sets up an incoming TCP connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(NetConnection cnx)
		 throws IOException {
		log.in();
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		serverSocketChannel = ServerSocketChannel.open();
		ServerSocket tcpServerSocket = 
					serverSocketChannel.socket();
		InetSocketAddress socketAddress = 
		 new InetSocketAddress(InetAddress.getLocalHost(),
				       0);
		tcpServerSocket.bind(socketAddress, 1);
		Hashtable lInfo    = new Hashtable();
		lInfo.put("tcp_address", 
				tcpServerSocket.getInetAddress());
		lInfo.put("tcp_port", new Integer(
				tcpServerSocket.getLocalPort()));


		ObjectOutputStream os = new ObjectOutputStream(
		 cnx.getServiceLink().getOutputSubStream(
		  this, "nio"));
		os.writeObject(lInfo);
		os.close();

		socketChannel = serverSocketChannel.accept();
		// use bocking mode
		socketChannel.configureBlocking(true);

		addr = socketChannel.socket().getInetAddress();
		port = socketChannel.socket().getPort();
		this.spn = cnx.getNum();
									
		startUpcallThread();

		mtu = 0;
		log.out();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Poll by setting the channel to non blocking mode, try to receive one
	 * byte, and set it back to blocking mode again. This makes poll a bit
	 * expensive. If "block == true" it keeps the channel in blocking mode
	 *
	 * @return {@inheritDoc}
	 */
	public Integer doPoll(boolean block) throws IOException {
		log.in();
		if (spn == null) {
			// not connected yet
			return null;
		}

		try {
			if(!block) {
				socketChannel.configureBlocking(false);
				log.disp("doing a non-blocking poll");
			} else {
				log.disp("doing a blocking poll");
			}

			if(bytePending) {
				throw new IOException("NioInput: Eek! Trying to poll while there is still a byte pending!");
			}

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
				log.out("data found");
				return spn;
			}

		} catch (AsynchronousCloseException e) {
			log.out("channel closed by another thread");
			throw new ConnectionClosedException(e.getMessage());
		} catch (ConnectionClosedException e) {
			log.out("received an end-of-stream");
			throw new ConnectionClosedException(e);
		} catch (ClosedChannelException e) {
			log.out("channel already closed");
			throw new ConnectionClosedException(e.getMessage());
		} catch (IOException e) {
			log.out("exception thrown");
			throw e;
		} finally {
			try {
			    if(!block) {
			        log.disp("returning channel to blokking mode");
				socketChannel.configureBlocking(true);
			    }
			} catch(IOException e) {
				// IGNORE
			}
		}
				
		log.out("no data found");
		return null;
	}

	protected void doFinish() throws IOException {
		log.in();
		// NOTHING
		log.out();
	}

	/**
	 * Gets the primitive buffers from the channel
	 */
	private void receive() throws IOException {
		int lastBuffer;

		log.in();	
		/* fill the header from the channel */
	
		headerByteBuffer.clear();

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

 //System.err.println("# bytes in buffer: " + header.get(BYTE_BUFFER));	
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

		primitiveBuffers[CHAR_BUFFER].flip();
		charBuffer.position(0);
		charBuffer.limit(primitiveBuffers[CHAR_BUFFER].limit() / 2);

		primitiveBuffers[SHORT_BUFFER].flip();
		shortBuffer.position(0);
		shortBuffer.limit(primitiveBuffers[SHORT_BUFFER].limit() / 2);

		primitiveBuffers[INT_BUFFER].flip();
		intBuffer.position(0);
		intBuffer.limit(primitiveBuffers[INT_BUFFER].limit() / 4);

		primitiveBuffers[LONG_BUFFER].flip();
		longBuffer.position(0);
		longBuffer.limit(primitiveBuffers[LONG_BUFFER].limit() / 8);

		primitiveBuffers[FLOAT_BUFFER].flip();
		floatBuffer.position(0);
		floatBuffer.limit(primitiveBuffers[FLOAT_BUFFER].limit() / 4);

		primitiveBuffers[DOUBLE_BUFFER].flip();
		doubleBuffer.position(0);
		doubleBuffer.limit(primitiveBuffers[DOUBLE_BUFFER].limit() / 8);

		log.disp("Received " + 
				byteBuffer.limit() + " b, "+
                                charBuffer.limit() + " c, "+
                                shortBuffer.limit() + " s, "+
                                intBuffer.limit() + " i, "+
                                longBuffer.limit() + " l, "+
                                floatBuffer.limit() + " f, "+
                                doubleBuffer.limit() + " d");
		log.out();
	}

	public NetReceiveBuffer readByteBuffer(int expectedLength) 
						throws IOException {
		log.in();
                int len = readInt();
                NetReceiveBuffer buffer = createReceiveBuffer(len);
                readArray(buffer.data, 0, len);
		log.out();
                return buffer;
        }
                                                                                
                                                                                
        public void readByteBuffer(NetReceiveBuffer buffer) 
						throws IOException {
		log.in();
                int len = readInt();
                readArray(buffer.data, 0, len);
                buffer.length = len;
		log.out();
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
                log.in(); 
		if(!byteBuffer.hasRemaining()) {
			receive();
		}
		boolean result = (byteBuffer.get() == (byte)1 );
		log.disp("received a boolean: " + result);
		log.out();
		return result;
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
		log.in();
		if(!byteBuffer.hasRemaining()) {
			receive();
		}
		byte result = byteBuffer.get();
		log.disp("received a byte: " + result);
		log.out();
		return result;
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
		log.in();
		if(!charBuffer.hasRemaining()) {
			receive();
		}
		char result = charBuffer.get();
		log.disp("received a char: " + result);
		log.out();
		return result;
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
                        
		log.in();
                        
		if(!shortBuffer.hasRemaining()) {
			receive();
		}
		short result = shortBuffer.get();
		log.disp("received a short: " + result);
		log.out();
		return result;
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
                        
		log.in();
		if(!intBuffer.hasRemaining()) {
			receive();
		}
		int result = intBuffer.get();
		log.disp("received a int: " + result);
		log.out();
		return result;
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
                        
		log.in();
		if(!longBuffer.hasRemaining()) {
			receive();
		}
		long result = longBuffer.get();
		log.disp("received a long: " + result);
		log.out();
		return result;
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
                        
		log.in();
		if(!floatBuffer.hasRemaining()) {
			receive();
		}
		float result = floatBuffer.get();
		log.disp("received a float: " + result);
		log.out();
		return result;
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
                        
		log.in();
		if(!doubleBuffer.hasRemaining()) {
			receive();
		}
		double result = doubleBuffer.get();
		log.disp("received a double: " + result);
		log.out();
		return result;
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
                        
		log.in();
		throw new IOException("NioInput: readString not implemented (yet)");
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
        
		log.in();
		throw new IOException("NioInput: readObject not implemented (yet)");                
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
		log.in();
		log.disp("read a boolean array at offset " + offset +
			 " of length " + size);
		for(int i = offset; i <  (offset + size); i++) {
			destination[i] = readBoolean();
		}
		log.out();
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
		log.in();
		log.disp("reading a byte array at offset " + offset +
			 " of length " + size);
		for (int i = offset; i < (offset + size);i++) {
			destination[i] = readByte();
		}

		/* wrap the buffer in a nio ByteBuffer */
//		wrap = ByteBuffer.wrap(destination, offset, size);


		/* check if a poll was done lately... */
//		if(bytePending) {
//			wrap.put(oneByteBuffer.get(0));
//			bytePending = false;
//		}

		/* receive the data from the channel */
//		do {
//			socketChannel.read(wrap);
//		} while (wrap.hasRemaining());

		log.out();
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
		log.in();
		log.disp("reading a char array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!charBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					charBuffer.remaining(), size);
			charBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
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
		log.in();
		log.disp("reading a short array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!shortBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					shortBuffer.remaining(), size);
			shortBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
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
		log.in();
		log.disp("reading a int array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!intBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					intBuffer.remaining(), size);
			intBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
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
		log.in();
		log.disp("reading a long array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!longBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					longBuffer.remaining(), size);
			longBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
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
		log.in();
		log.disp("reading a float array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!floatBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					floatBuffer.remaining(), size);
			floatBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
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
		log.in();
		log.disp("reading a double array at offset " + offset +
			 " of length " + size);
		while(size > 0) {
			if(!doubleBuffer.hasRemaining()) {
				receive();
			}

			length = java.lang.Math.min(
					doubleBuffer.remaining(), size);
			doubleBuffer.get(destination, offset, length);
			size -= length;
			offset += length;
		}
		log.out();
	}



	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {

		log.in();

		if(serverSocketChannel != null) {
			serverSocketChannel.close();
		}

		if(socketChannel != null) {
			socketChannel.close();
		}

		spn = null;
		log.out();
	}

        protected synchronized void doClose(Integer num)
						throws IOException {
		log.in();
                if (spn == num) {
                        doFree();
                }
		log.out();
        }
 

	
}
