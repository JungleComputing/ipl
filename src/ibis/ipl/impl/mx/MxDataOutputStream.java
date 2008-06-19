package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import ibis.io.DataOutputStream;

public class MxDataOutputStream extends DataOutputStream implements Config {
	// FIXME not threadsafe, do we want to?
	
    // primitives are send in order of size, largest first
    private static final int HEADER = 0;
    private static final int LONGS = 1;
    private static final int DOUBLES = 2;
    private static final int INTS = 3;
    private static final int FLOATS = 4;
    private static final int SHORTS = 5;
    private static final int CHARS = 6;
    private static final int BYTES = 7;
    private static final int NR_OF_BUFFERS = 8;

    /**
     * The header contains 1 byte for the byte order, one byte indicating the
     * length of the padding at the end of the packet (in bytes), and 7 shorts
     * (14 bytes) for the number of each primitive send (in bytes!)
     */
    private static final int SIZEOF_HEADER = 16;
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;
	
	
	private ByteBuffer[] buffers;
    ShortBuffer header;
    LongBuffer longs;
    DoubleBuffer doubles;
    IntBuffer ints;
    FloatBuffer floats;
    ShortBuffer shorts;
    CharBuffer chars;
    ByteBuffer bytes;
	
	
	private MxWriteChannel channel;
	private boolean finished = true;
	
	private long count = 0;
	
	public MxDataOutputStream(MxWriteChannel channel, ByteOrder order) {
		this.channel = channel;
		buffers = new ByteBuffer[NR_OF_BUFFERS];
		buffers[HEADER] = ByteBuffer.allocateDirect(SIZEOF_HEADER).order(order);
		for(int i = 1; i < NR_OF_BUFFERS; i++) {
			buffers[i] = ByteBuffer.allocateDirect(PRIMITIVE_BUFFER_SIZE).order(order);
		}
		
		header = buffers[HEADER].asShortBuffer();
        longs = buffers[LONGS].asLongBuffer();
        doubles = buffers[DOUBLES].asDoubleBuffer();
        ints = buffers[INTS].asIntBuffer();
        floats = buffers[FLOATS].asFloatBuffer();
        shorts = buffers[SHORTS].asShortBuffer();
        chars = buffers[CHARS].asCharBuffer();
        bytes = buffers[BYTES].duplicate();
	}

	@Override
	public int bufferSize() {
		// TODO think of something
		//total buffer size is safe, but maybe the size of the type buffers is also enough, and can lower memory usage
		return (NR_OF_BUFFERS - 1) * PRIMITIVE_BUFFER_SIZE;
	}

	@Override
	public long bytesWritten() {
		// TODO Auto-generated method stub
		return count;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		channel.close();
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub
		// post send for this buffer
		if(!finished) {
			return;
		}

		finished = false;
		flipBuffers();
		channel.write(buffers);
		//TODO: catch exceptions?	
	}
	
	/**
	 * 
	 */
	private void flipBuffers() {
		for(ByteBuffer b: buffers) {
			b.flip();
		}		
	}

	@Override
	public void finish() throws IOException {
		// TODO Auto-generated method stub
		if(!finished) {
			try {
				channel.finish();
			} catch (IOException e) {
				// TODO what to do with the status?
				throw(e);
			}
			finished = true;
		}
	}

	@Override
	public boolean finished() throws IOException {
		return finished;
	}

	@Override
	public void resetBytesWritten() {
		count = 0;
	}

	@Override
	public void write(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void writeArray(boolean[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(byte[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(char[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(short[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(int[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(long[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(float[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub
	}

	public void writeArray(double[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeBoolean(boolean value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeByte(byte value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeChar(char value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeDouble(double value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeFloat(float value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeInt(int value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeLong(long value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeShort(short value) throws IOException {
		// TODO Auto-generated method stub

	}

}
