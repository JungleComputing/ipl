package ibis.impl.net.nio;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

import java.net.ServerSocket;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The NIO/TCP input implementation.
 */
public final class NioInput extends NetInput {

    public static boolean		DEBUG	    = false;

    public static int			BUFFER_SIZE = 1024*1024;  // bytes

    /**
     * The connection socket channel.
     */
    private ServerSocketChannel		serverSocketChannel = null;

    /**
     * The communication socket channel.
     */
    private SocketChannel		socketChannel = null;

    /**
     * The peer NetSendPort local number.
     */
    private volatile Integer		spn = null;

    private InetAddress			addr = null;
    private int				port = 0;

    /**
     * if isPendingByte a byte has been read from the channel,
     * but has not been processed yet.
     */
    private ByteBuffer			oneByteBuffer;
    private boolean			bytePending = false;

    /*
     * buffer to recieve data from the network
     */

    private ByteBuffer			byteBuffer;

    /*
     * views of the byteBuffer used to drain it
     */
    private CharBuffer			charBuffer;
    private ShortBuffer			shortBuffer;
    private IntBuffer			intBuffer;
    private LongBuffer			longBuffer;
    private FloatBuffer			floatBuffer;
    private DoubleBuffer		doubleBuffer;

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

	    byteBuffer = 
		ByteBuffer.allocateDirect(BUFFER_SIZE).
		order(ByteOrder.nativeOrder());

	    charBuffer = byteBuffer.asCharBuffer();
	    shortBuffer = byteBuffer.asShortBuffer();
	    intBuffer = byteBuffer.asIntBuffer();
	    longBuffer = byteBuffer.asLongBuffer();
	    floatBuffer = byteBuffer.asFloatBuffer();
	    doubleBuffer = byteBuffer.asDoubleBuffer();

	    oneByteBuffer = ByteBuffer.allocateDirect(1).
		order(ByteOrder.nativeOrder());
	    oneByteBuffer.clear();
	}

    public void initReceive(Integer num) {
	// NOTHING
    }


    /**
     * Returns the "number"th local InetAddress.
     * Use this if there is more than one network interface in a machine.
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

//	socketChannel.socket().setTcpNoDelay(true);

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
	if(DEBUG) {
	    if(block) {
		System.out.println("Doing blocking poll");
	    } else {
		System.out.println("Doing NON blocking poll");
	    }
	}
    
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
	if (DEBUG) {
	    System.out.println("Finish called");
	}
	// NOTHING
    }

    /**
     * receive data into the ByteBuffer
     */
    private void receiveByteBuffer() throws IOException {
	long bytesRead = 0;

	while(byteBuffer.hasRemaining()) {
	    // See if we "polled" a byte, and put it in the buffer if so.
	    // In this while loop to save a hasRemaining() call
	    if(bytePending) {
		byteBuffer.put(oneByteBuffer.get(0));
		bytePending = false;
		bytesRead += 1;
	    }

	    bytesRead += socketChannel.read(byteBuffer);

	    if(DEBUG) {
		System.out.println("        rbb received " + bytesRead + " bytes");
	    }
	}
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
	boolean result;

	byteBuffer.position(0).limit(1);

	receiveByteBuffer();
	byteBuffer.flip();

	result = (byteBuffer.get() == (byte)1);

	if(DEBUG) {
	    System.out.println("received boolean " + result);
	}

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
	byte result;

	byteBuffer.position(0).limit(1);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.get();

	if(DEBUG) {
	    System.out.println("received byte " + result);
	}

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
	char result;

	byteBuffer.position(0).limit(2);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.getChar();

	if(DEBUG) {
	    System.out.println("received char " + result);
	}

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
	short result;

	byteBuffer.position(0).limit(2);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.getShort();

	if(DEBUG) {
	    System.out.println("received short " + result);
	}

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
	int result;

	byteBuffer.position(0).limit(4);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.getInt();

	if(DEBUG) {
	    System.out.println("received int " + result);
	}

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
	long result;

	byteBuffer.position(0).limit(8);

	receiveByteBuffer();
	byteBuffer.flip();

	result =  byteBuffer.getLong();

	if(DEBUG) {
	    System.out.println("received long " + result);
	}

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
	float result;

	byteBuffer.position(0).limit(4);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.getFloat();

	if(DEBUG) {
	    System.out.println("received float " + result);
	}

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
	double result;

	byteBuffer.position(0).limit(8);

	receiveByteBuffer();
	byteBuffer.flip();

	result = byteBuffer.getDouble();

	if(DEBUG) {
	    System.out.println("received double " + result);
	}

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
	throw new IOException("reading Strings not implemented by net/nio");
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
	throw new IOException("reading Objects not implemented by net/nio");
    }



    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: this function may block if the expected 
     * data is not there.
     *
     * @return {@inheritDoc}
     */
    public void readArray(boolean [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving boolean[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(byteBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length);
	    receiveByteBuffer();

	    byteBuffer.flip();
	    for(int i = offset; i <  (offset + length); i++) {
		array[i] = (byteBuffer.get() == (byte)1);
	    }

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
    public void readArray(byte [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving byte[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(byteBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length);
	    receiveByteBuffer();

	    byteBuffer.flip();
	    byteBuffer.get(array, offset, length);

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
    public void readArray(char [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving char[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(charBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 2);
	    receiveByteBuffer();

	    charBuffer.position(0).limit(length);
	    charBuffer.get(array, offset, length);

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
    public void readArray(short [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving short[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(shortBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 2);
	    receiveByteBuffer();

	    shortBuffer.position(0).limit(length);
	    shortBuffer.get(array, offset, length);

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
    public void readArray(int [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving int[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(intBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 4);
	    receiveByteBuffer();

	    intBuffer.position(0).limit(length);
	    intBuffer.get(array, offset, length);

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
    public void readArray(long [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving long[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(longBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 8);
	    receiveByteBuffer();

	    longBuffer.position(0).limit(length);
	    longBuffer.get(array, offset, length);

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
    public void readArray(float [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving float[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(floatBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 4);
	    receiveByteBuffer();

	    floatBuffer.position(0).limit(length);
	    floatBuffer.get(array, offset, length);

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
    public void readArray(double [] array, 
	    int offset, 
	    int size) throws IOException {
	int length;

	if(DEBUG) {
	    System.out.println("receiving double[" + size + "]");
	}

	while(size > 0) {
	    length = Math.min(doubleBuffer.capacity(), size);

	    byteBuffer.position(0).limit(length * 8);
	    receiveByteBuffer();

	    doubleBuffer.position(0).limit(length);
	    doubleBuffer.get(array, offset, length);

	    size -= length;
	    offset += length;
	}

    }



    /**
     * {@inheritDoc}
     */
    public void doFree() throws IOException {
	if(DEBUG) {
	    System.out.println("doFree() called");
	}

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
} 
