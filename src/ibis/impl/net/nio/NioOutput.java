package ibis.ipl.impl.net.nio;

import ibis.ipl.impl.net.*;

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
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.SocketChannel;
import java.nio.ByteOrder;

/**
 * The NIO TCP output implementation.
 */
public final class NioOutput extends NetOutput {

	public static int BUFFER_SIZE =	2048;	// bytes
	public static int NR_OF_BUFFERS = 50;

	public static int 	HEADER = 0,
				BYTE_BUFFER = 1,
				CHAR_BUFFER = 2,
				SHORT_BUFFER = 3,
				INT_BUFFER = 4,
				LONG_BUFFER = 5,
				FLOAT_BUFFER = 6,
				DOUBLE_BUFFER = 7;
	public static int NR_OF_PRIMITIVES = 7;

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
	 * The number of bytes send since the last reset of this counter.
	 */
	private int			bytesSend	= 0;

	/**
	 * The send buffers. The first buffer is reserved for the header
         * containing how many primitives are send, the next NR_OF_PRIMITIVES
	 * are reserved for the buffers containing primitives, the rest of
	 * the buffers can be used to store byte arrays that need to be
	 * send
	 */
	private ByteBuffer[]	buffers = new ByteBuffer[NR_OF_BUFFERS];

	/**
	 * The views of the bytebuffers used to fill them.
         */
	private IntBuffer    header;
	private ByteBuffer   byteBuffer;
	private CharBuffer   charBuffer;
	private ShortBuffer  shortBuffer;
	private IntBuffer    intBuffer;
	private LongBuffer   longBuffer;
	private FloatBuffer  floatBuffer;
	private DoubleBuffer doubleBuffer;

	/* the number of buffers already in use, the minimum is 8, 1 header
	 * and 7 primitive buffers
	 */
	private int buffersFilled = 1 + NR_OF_PRIMITIVES;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the Tcp driver instance.
	 * @param output the controlling output.
	 */
	NioOutput(NetPortType pt, NetDriver driver, String context) 
						throws NetIbisException {
		super(pt, driver, context);
		headerLength = 0;

		buffers[HEADER] = 
			ByteBuffer.allocateDirect(NR_OF_PRIMITIVES * 4).
			 order(ByteOrder.LITTLE_ENDIAN);

		for(int i = BYTE_BUFFER; i <= DOUBLE_BUFFER; i++) {
			buffers[i] = ByteBuffer.allocateDirect(BUFFER_SIZE).
					order(ByteOrder.LITTLE_ENDIAN);
			buffers[i].clear();
		}
		
		header = buffers[HEADER].asIntBuffer();
		byteBuffer = buffers[BYTE_BUFFER]; // just a shorthand 
		charBuffer = buffers[CHAR_BUFFER].asCharBuffer();
		shortBuffer = buffers[SHORT_BUFFER].asShortBuffer();
		intBuffer = buffers[INT_BUFFER].asIntBuffer();
		longBuffer = buffers[LONG_BUFFER].asLongBuffer();
		floatBuffer = buffers[FLOAT_BUFFER].asFloatBuffer();
		doubleBuffer = buffers[DOUBLE_BUFFER].asDoubleBuffer();

		header.clear();
		byteBuffer.clear();
		charBuffer.clear();
		shortBuffer.clear();
		intBuffer.clear();
		longBuffer.clear();
		floatBuffer.clear();
		doubleBuffer.clear();
	}

	/*
	 * Sets up an outgoing TCP connection (using nio).
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(NetConnection cnx)
						 throws NetIbisException {
		log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }
                
		this.rpn = cnx.getNum();

		try {
			ObjectInputStream is = new ObjectInputStream(
			 cnx.getServiceLink().
			 getInputSubStream(this, "nio"));
	
		Hashtable   rInfo = (Hashtable)is.readObject();
		is.close();
		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int rport = ((Integer)rInfo.get("tcp_port")).intValue();
		InetSocketAddress sa = new InetSocketAddress(raddr, rport);
		
			socketChannel = SocketChannel.open(sa);
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }
		log.out();
	}

        public void finish() throws NetIbisException {
		log.in();
                super.finish();
		try {
			flush();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
		log.out();
	}

	/**
	 * make sure all the data is written to the channel, so the data
	 * can be touched by the user.
	 */
	public void reset(boolean doSend) throws NetIbisException {
		log.in();
		super.reset(doSend);

		try {
			flush();
		} catch (IOException e) {
			throw new NetIbisException(e);
		}
		log.out();
	}
		
	/** writes out all buffers to the channel */
	private void flush() throws IOException {
		int firstSendBuffer = 0,
		    lastSendBuffer;

		log.in();

		log.disp("Sending " + 
			byteBuffer.position() + " b, "+
			charBuffer.position() + " c, "+
			shortBuffer.position() + " s, "+
			intBuffer.position() + " i, "+
			longBuffer.position() + " l, "+
			floatBuffer.position() + " f, "+
			doubleBuffer.position() + " d" +
			" and " + (buffersFilled - NR_OF_PRIMITIVES - 1)
			+ " byte buffers");

		if ((byteBuffer.position() +
		     charBuffer.position() +
		     shortBuffer.position() +
		     intBuffer.position() +
		     longBuffer.position() +
		     floatBuffer.position() +
		     doubleBuffer.position()) == 0) {

			/* no need to send the header and primitive
			 * buffers, the're empty
			 */
			firstSendBuffer = NR_OF_PRIMITIVES + 1;

		        if ((buffersFilled - NR_OF_PRIMITIVES - 1) == 0 ) {
				log.out("no data to send");
				return;
			}
		}

		if (firstSendBuffer == 0) {	
			/* fill the header buffer with the sizes of the 
			   primitive buffers */ 
			header.clear();
			header.put(byteBuffer.position());
			header.put(charBuffer.position() * 2);
			header.put(shortBuffer.position() * 2);
			header.put(intBuffer.position() * 4);
			header.put(longBuffer.position() * 8);
			header.put(floatBuffer.position() * 4);
			header.put(doubleBuffer.position() * 8);

			/* set up the bytearrays so that they can be drained */
			buffers[HEADER].position(0);
			buffers[HEADER].limit(header.position() * 4);

			byteBuffer.flip();

			buffers[CHAR_BUFFER].position(0);
			buffers[CHAR_BUFFER].limit(charBuffer.position() * 2);

			buffers[SHORT_BUFFER].position(0);
			buffers[SHORT_BUFFER].limit(shortBuffer.position() * 2);

			buffers[INT_BUFFER].position(0);
			buffers[INT_BUFFER].limit(intBuffer.position() * 4);

			buffers[LONG_BUFFER].position(0);
			buffers[LONG_BUFFER].limit(longBuffer.position() * 8);

			buffers[FLOAT_BUFFER].position(0);
			buffers[FLOAT_BUFFER].limit(floatBuffer.position() * 4);

			buffers[DOUBLE_BUFFER].position(0);
			buffers[DOUBLE_BUFFER].limit(doubleBuffer.position()*8);
		}

		lastSendBuffer = firstSendBuffer;
		int j = buffersFilled - 1;
		while(j > firstSendBuffer && 
		      lastSendBuffer == firstSendBuffer) {
			if(buffers[j].hasRemaining()) {
				lastSendBuffer = j;
			}
			j -= 1;
		}

		log.disp("sending buffers " + firstSendBuffer + " to " +
			lastSendBuffer);

		for(int i = 0; i < buffersFilled;i++) {
			if(buffers[i].limit() > 0) {
				log.disp(i + " " + buffers[i].get(0));
			}
		}

		/* write the array of buffers to the channel */
		do {
		   bytesSend += socketChannel.write(buffers, 
			firstSendBuffer, buffersFilled);
		   log.disp("tried to send something, total bytes send now"
				+ bytesSend);
		} while(buffers[lastSendBuffer].hasRemaining());

		/* release all the (non-header non-primitive) buffers written */
		for (int i = NR_OF_PRIMITIVES + 1; i < buffersFilled; i++) {
			buffers[i] = null;
		}
		buffersFilled = NR_OF_PRIMITIVES + 1;

		/* clear the primitive buffers so that they can be filled
		   again */
		byteBuffer.clear();
		charBuffer.clear();
		shortBuffer.clear();
		intBuffer.clear();
		longBuffer.clear();
		floatBuffer.clear();
		doubleBuffer.clear();
		
		log.out();
	}	

	/*
	 * {@inheritDoc}
         */
	public int getCount() {
		log.in();
		log.disp("send " + bytesSend + " bytes since last reset");
		log.out();
		return bytesSend;
	}

	/*
	 * {@inheritDoc}
         */
	public void resetCount() {
		log.in();
		bytesSend = 0;
		log.out();
	}
		
	/*
	 * {@inheritDoc}
         */
        public void writeBoolean(boolean value) throws NetIbisException {
		log.in();
		try {
			if(!byteBuffer.hasRemaining()) {
				flush();
			}
		
			/* least efficient way possible of doing this, 
			 * i think (ideas welcome) --N
			 */
			byteBuffer.put((byte) (value ? 1 : 0) );
			log.disp("put boolean " + value + 
				 " in the byte buffer");

		} catch (IOException e) {
			throw new NetIbisException(e);
		}
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeByte(byte value) throws NetIbisException {
		log.in();
 		try {
			if(!byteBuffer.hasRemaining()) {
				flush();
			}

			byteBuffer.put(value);
			log.disp("put byte " + value + " in buffer");
		
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}


	
	/*
	 * {@inheritDoc}
         */
	public void writeChar(char value) throws NetIbisException {
		log.in();
		try {
			if(!charBuffer.hasRemaining()) {
				flush();
			}
	
			charBuffer.put(value);
			log.disp("put char " + value + " in buffer");

 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		}
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeShort(short value) throws NetIbisException {
		log.in();
		try {
			if(!shortBuffer.hasRemaining()) {
				flush();
			}
			shortBuffer.put(value);
			log.disp("put short " + value + " in buffer");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeInt(int value) throws NetIbisException {
		log.in();
		try {
			if(!intBuffer.hasRemaining()) {
				flush();
			}
			intBuffer.put(value);
			log.disp("put int " + value + " hex: " +
				 Integer.toHexString(value) + " in buffer");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		}
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeLong(long value) throws NetIbisException {
		log.in();
		try {
			if(!longBuffer.hasRemaining()) {
				flush();
			}
			longBuffer.put(value);
			log.disp("put long " + value + " hex: " +
				Long.toHexString(value) + " in buffer");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeFloat(float value) throws NetIbisException {
		log.in();
		try {
			if(!floatBuffer.hasRemaining()) {
				flush();
			}
			floatBuffer.put(value);
			log.disp("put float " + value + " in buffer");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeDouble(double value) throws NetIbisException {
		log.in();
		try {
			if(!doubleBuffer.hasRemaining()) {
				flush();
			}
			doubleBuffer.put(value);
			log.disp("put double " + value + " in buffer");
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	/*
	 * {@inheritDoc}
         */
	public void writeString(String value) throws NetIbisException {
		throw new NetIbisException("NioOutput: writeString not implemented (yet?)");
	}

	/*
	 * {@inheritDoc}
         */
	public void writeObject(Object value) throws NetIbisException {
		throw new NetIbisException("NioOutput: writeObject not implemented (yet?)");
	}

	/*
	 * {@inheritDoc}
	 */
	public void writeArray(boolean [] destination,
					   int offset,
					   int size) throws NetIbisException {
		log.in();
		try {
			log.disp("writing boolean array at offset " + offset +
				 " of length " + size);
			for(int i = offset; i <  (offset + size); i++) {
				writeBoolean(destination[i]);
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}


	/*
	 * {@inheritDoc}
	 *
	 * This function wraps the given byte array into a Nio ByteBuffer,
	 * then adds it to the buffers that need to be send out.
	 */
	public void writeArray(byte [] destination,
					int offset,
					int size) throws NetIbisException {
		ByteBuffer buffer = null;
		log.in();
		try {
			for (int i = offset;i < (offset + size);i++) {
				writeByte(destination[i]);
			}

//			if(buffersFilled >= NR_OF_BUFFERS) {
//				flush();
//			}			

//			log.disp("wrapping byte array. offset: " + offset +
//				 " length: " + size);
//			buffers[buffersFilled] = 
//				byteBuffer.wrap(destination, offset, size);

//			buffersFilled += 1;
//			log.disp("number of buffers filled now " + 
//				 buffersFilled);

 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	public void writeArray(char [] destination,
					int offset,
					int size) throws NetIbisException {
		int length;
		log.in();
		try {
			while(size > 0) {
				if(!charBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						charBuffer.remaining(), size);
				log.disp("putting char array subarray [" +
					 offset + "," +
					 (offset + length) + "]");
				charBuffer.put(destination, offset, length);
				size -= length;
				offset += length;

			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
		log.out();
	}

	public void writeArray(short [] destination,
					 int offset,
					 int size) throws NetIbisException {
		int length;
		try {
			while(size > 0) {
				if(!shortBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						shortBuffer.remaining(), size);
				shortBuffer.put(destination, offset, length);
				size -= length;
				offset += length;
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

	public void writeArray(int [] destination,
				       int offset,
				       int size) throws NetIbisException {
		int length;
		try {
			while(size > 0) {
				if(!intBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						intBuffer.remaining(), size);
				intBuffer.put(destination, offset, length);
				size -= length;
				offset += length;
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

	public void writeArray(long [] destination,
				       int offset,
				       int size) throws NetIbisException {
		int length;
		try {
			while(size > 0) {
				if(!longBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						longBuffer.remaining(), size);
				longBuffer.put(destination, offset, length);
				size -= length;
				offset += length;
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

	public void writeArray(float [] destination,
				       int offset,
				       int size) throws NetIbisException {
		int length;
		try {
			while(size > 0) {
				if(!floatBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						floatBuffer.remaining(), size);
				floatBuffer.put(destination, offset, length);
				size -= length;
				offset += length;
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

	public void writeArray(double [] destination,
				       int offset,
				       int size) throws NetIbisException {
		int length;
		try {
			while(size > 0) {
				if(!doubleBuffer.hasRemaining()) {
					flush();
				}

				length = java.lang.Math.min(
						doubleBuffer.remaining(), size);
				doubleBuffer.put(destination, offset, length);
				size -= length;
				offset += length;
			}
 		} catch (IOException e) {
 			throw new NetIbisException(e.getMessage());
 		} 
	}

	/*
	 * {@inheritDoc}
         */
	public synchronized void close(Integer num) throws NetIbisException {
                if (rpn == num) {
                        try {
				if (socketChannel != null) {
					flush();
					socketChannel.close();
				}

                        } catch (Exception e) {
                                throw new NetIbisException(e);
                        }
                                                                            
                        rpn = null;
                }
        }



	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		try {
			if (socketChannel != null) {
				socketChannel = null;
			}


		}
		catch (Exception e) {
			throw new NetIbisException(e);
		}

		rpn       = null;

		super.free();
	}
	
}
