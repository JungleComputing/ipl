/* $Id$ */

package ibis.impl.net.nio;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.ipl.IbisIOException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;

/**
 * The NIO TCP output implementation.
 */
public final class NioOutput extends NetOutput {

    public static int BUFFER_SIZE = 1024 * 1024; // bytes

    public static boolean DEBUG = false;

    /**
     * The communication channel.
     */
    private SocketChannel socketChannel = null;

    /**
     * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer rpn = null;

    /**
     * The number of bytes send since the last reset of this counter.
     */
    private long bytesSend = 0;

    /**
     * The buffer used for sending.
     */

    private ByteBuffer byteBuffer;

    /**
     * Views of the byteBuffer used for filling the buffer
     */
    private CharBuffer charBuffer;

    private ShortBuffer shortBuffer;

    private IntBuffer intBuffer;

    private LongBuffer longBuffer;

    private FloatBuffer floatBuffer;

    private DoubleBuffer doubleBuffer;

    /**
     * Constructor.
     *
     * @param pt the properties of the output's 
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the Tcp driver instance.
     */
    NioOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
        headerLength = 0;

    }

    private void setupBuffers(ByteOrder order) throws IOException {

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(order);

        charBuffer = byteBuffer.asCharBuffer();
        shortBuffer = byteBuffer.asShortBuffer();
        intBuffer = byteBuffer.asIntBuffer();
        longBuffer = byteBuffer.asLongBuffer();
        floatBuffer = byteBuffer.asFloatBuffer();
        doubleBuffer = byteBuffer.asDoubleBuffer();
    }

    /**
     * Sets up an outgoing TCP connection (using nio).
     */
    public void setupConnection(NetConnection cnx) throws IOException {
        ByteOrder peerOrder;

        if (this.rpn != null) {
            throw new Error("connection already established");
        }

        this.rpn = cnx.getNum();

        try {
            ObjectInputStream is = new ObjectInputStream(
                    cnx.getServiceLink().getInputSubStream(this, "nio"));

            Hashtable rInfo = (Hashtable) is.readObject();
            is.close();
            InetAddress raddr = (InetAddress) rInfo.get("tcp_address");
            int rport = ((Integer) rInfo.get("tcp_port")).intValue();
            InetSocketAddress sa = new InetSocketAddress(raddr, rport);

            socketChannel = SocketChannel.open(sa);

            //		socketChannel.socket().setTcpNoDelay(true);

            /* figure out what byteOrder we need
             for the output buffers */

            String order = (String) rInfo.get("byte_order");
            if (order.compareTo(ByteOrder.LITTLE_ENDIAN.toString()) == 0) {
                peerOrder = ByteOrder.LITTLE_ENDIAN;
            } else {
                peerOrder = ByteOrder.BIG_ENDIAN;
            }
            setupBuffers(peerOrder);

        } catch (ClassNotFoundException e) {
            throw new IbisIOException("Incompatible partner: " + e);
        }
    }

    public long finish() throws IOException {
        super.finish();
        long retval = bytesSend;
        bytesSend = 0;
        return retval;
    }

    public void reset() throws IOException {
        super.reset();
        bytesSend = 0;
    }

    /**
     * send the byteBuffer out to the network
     */
    private void sendByteBuffer() throws IOException {
        long oldBytesSend = bytesSend;

        do {
            bytesSend += socketChannel.write(byteBuffer);
        } while (byteBuffer.hasRemaining());

        if (DEBUG) {
            System.out.println("        sbb send " + (bytesSend - oldBytesSend)
                    + " bytes");
        }
    }

    public void writeBoolean(boolean value) throws IOException {
        /* least efficient way possible of doing this, 
         * i think (ideas welcome) --N
         */
        byteBuffer.clear();
        byteBuffer.put((byte) (value ? 1 : 0));
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send boolean " + value);
        }
    }

    public void writeByte(byte value) throws IOException {
        byteBuffer.clear();
        byteBuffer.put(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send byte " + value);
        }
    }

    public void writeChar(char value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putChar(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send char " + value);
        }
    }

    public void writeShort(short value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putShort(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send short " + value);
        }
    }

    public void writeInt(int value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putInt(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send int " + value);
        }
    }

    public void writeLong(long value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putLong(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send long " + value);
        }
    }

    public void writeFloat(float value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putFloat(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send float " + value);
        }
    }

    public void writeDouble(double value) throws IOException {
        byteBuffer.clear();
        byteBuffer.putDouble(value);
        byteBuffer.flip();
        sendByteBuffer();

        if (DEBUG) {
            System.out.println("send double " + value);
        }
    }

    public void writeString(String value) throws IOException {
        throw new IOException("writing Strings not implemented by net/nio");
    }

    public void writeObject(Object value) throws IOException {
        throw new IOException("writing Objects not implemented bt net/nio");
    }

    public void writeArray(boolean[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending boolean[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(byteBuffer.capacity(), size);

            byteBuffer.clear();
            for (int i = offset; i < (offset + length); i++) {
                byteBuffer.put(array[i] ? (byte) 1 : (byte) 0);
            }
            byteBuffer.flip();

            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    /*
     * Copies (!) data into a ByteBuffer, and writes it out to the network
     */
    public void writeArray(byte[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending byte[" + size + "]");
        }

        while (size > 0) {

            length = Math.min(byteBuffer.capacity(), size);
            byteBuffer.clear();
            byteBuffer.put(array, offset, length);
            byteBuffer.flip();
            sendByteBuffer();

            size -= length;
            offset += length;
        }

    }

    public void writeArray(char[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending char[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(charBuffer.capacity(), size);
            charBuffer.clear();
            charBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(charBuffer.position() * 2);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public void writeArray(short[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending short[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(shortBuffer.capacity(), size);
            shortBuffer.clear();
            shortBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(shortBuffer.position() * 2);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public void writeArray(int[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending int[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(intBuffer.capacity(), size);
            intBuffer.clear();
            intBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(intBuffer.position() * 4);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public void writeArray(long[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending long[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(longBuffer.capacity(), size);
            longBuffer.clear();
            longBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(longBuffer.position() * 8);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public void writeArray(float[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending float[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(floatBuffer.capacity(), size);
            floatBuffer.clear();
            floatBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(floatBuffer.position() * 4);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public void writeArray(double[] array, int offset, int size)
            throws IOException {
        int length;

        if (DEBUG) {
            System.out.println("sending double[" + size + "]");
        }

        while (size > 0) {
            length = Math.min(doubleBuffer.capacity(), size);
            doubleBuffer.clear();
            doubleBuffer.put(array, offset, length);

            byteBuffer.position(0).limit(doubleBuffer.position() * 8);
            sendByteBuffer();

            size -= length;
            offset += length;
        }
    }

    public synchronized void close(Integer num) throws IOException {
        if (DEBUG) {
            System.out.println("close called");
        }

        if (rpn == num) {

            if (socketChannel != null) {
                socketChannel.close();
            }

            rpn = null;
        }
    }

    public void free() throws IOException {
        if (DEBUG) {
            System.out.println("free called");
        }

        if (socketChannel != null) {
            socketChannel = null;
        }

        rpn = null;

        super.free();
    }
}
