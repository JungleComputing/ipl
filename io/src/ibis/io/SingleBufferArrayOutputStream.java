package ibis.io;

import java.io.IOException;

import org.apache.log4j.Logger;

public class SingleBufferArrayOutputStream extends DataOutputStream {

	private static final Logger logger = Logger.getLogger(BufferedArrayOutputStream.class);

	private static final boolean DEBUG = IOProperties.DEBUG;

	/** Size of the buffer in which output data is collected. */
	private final int BUF_SIZE;

	/** The buffer in which output data is collected. */
	private byte [] buffer;

	/** Size of the buffer in which output data is collected. */
	private int index = 0;
	
	private int offset = 0;

	/** Object used for conversion of primitive types to bytes. */
	private Conversion conversion;

	/**
	 * Constructor.
	 * @param out	the underlying <code>OutputStream</code>
	 */
	public SingleBufferArrayOutputStream(byte [] buffer) {
		this.buffer = buffer;
		BUF_SIZE = buffer.length;
		conversion = Conversion.loadConversion(false);
	}

	public void reset() { 
		index = 0;
	}
	
	public long bytesWritten() {
		return index - offset;
	}

	public void resetBytesWritten() {
		offset = index;
	}

	/**
	 * Checks if there is space for <code>incr</code> more bytes and if not,
	 * the buffer is written to the underlying <code>OutputStream</code>.
	 *
	 * @param incr		the space requested
	 * @exception IOException	in case of trouble.
	 */
	private void checkFreeSpace(int bytes) throws IOException {

		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("checkFreeSpace(" + bytes + ") : " + " "
					+ (index + bytes >= BUF_SIZE) + " " + (index) + ")");
		}

		if (index + bytes > BUF_SIZE) {
			throw new IOException("End of buffer reached");
		}
	}

	public void write(int b) throws IOException {
		writeByte((byte) b);
	}

	public void writeBoolean(boolean value) throws IOException {
		byte b = conversion.boolean2byte(value);
		checkFreeSpace(1);
		buffer[index++] = b;
	}

	public void writeByte(byte value) throws IOException {
		checkFreeSpace(1);
		buffer[index++] = value;
	}

	public void writeChar(char value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_CHAR);
		conversion.char2byte(value, buffer, index);
		index += Constants.SIZEOF_CHAR;
	}

	public void writeShort(short value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_SHORT);
		conversion.short2byte(value, buffer, index);
		index += Constants.SIZEOF_SHORT;
	}

	public void writeInt(int value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_INT);
		conversion.int2byte(value, buffer, index);
		index += Constants.SIZEOF_INT;
	}

	public void writeLong(long value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_LONG);
		conversion.long2byte(value, buffer, index);
		index += Constants.SIZEOF_LONG;
	}

	public void writeFloat(float value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_FLOAT);
		conversion.float2byte(value, buffer, index);
		index += Constants.SIZEOF_FLOAT;
	}

	public void writeDouble(double value) throws IOException {
		checkFreeSpace(Constants.SIZEOF_DOUBLE);
		conversion.double2byte(value, buffer, index);
		index += Constants.SIZEOF_DOUBLE;
	}

	public void write(byte[] b) throws IOException {
		writeArray(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		writeArray(b, off, len);
	}

	public void writeArray(boolean[] ref, int off, int len) throws IOException {
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(boolean[" + off + " ... "
			                                 + (off + len) + "])");
		}
		
		final int toWrite = len * Constants.SIZEOF_BOOLEAN; 
			
		checkFreeSpace(toWrite);
		conversion.boolean2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(byte[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(byte[" + off + " ... " + (off + len)
					+ "])");
		}

		checkFreeSpace(len);
		System.arraycopy(ref, off, buffer, index, len);
		index += len;
	}

	public void writeArray(char[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(char[" + off + " ... " + (off + len)
					+ "])");
		}

		final int toWrite = len * Constants.SIZEOF_CHAR; 		
		checkFreeSpace(toWrite);
		conversion.char2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(short[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(short[" + off + " ... "
					+ (off + len) + "])");
		}

		final int toWrite = len * Constants.SIZEOF_SHORT; 		
		checkFreeSpace(toWrite);
		conversion.short2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(int[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(int[" + off + " ... " + (off + len)
					+ "])");
		}

		final int toWrite = len * Conversion.INT_SIZE; 		
		checkFreeSpace(toWrite);
		conversion.int2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(long[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(long[" + off + " ... " + (off + len)
					+ "])");
		}

		final int toWrite = len * Conversion.INT_SIZE; 		
		checkFreeSpace(toWrite);
		conversion.long2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(float[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(float[" + off + " ... "
					+ (off + len) + "])");
		}
		
		final int toWrite = len * Conversion.FLOAT_SIZE; 		
		checkFreeSpace(toWrite);
		conversion.float2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void writeArray(double[] ref, int off, int len) throws IOException {
		
		if (DEBUG && logger.isDebugEnabled()) {
			logger.debug("writeArray(double[" + off + " ... "
					+ (off + len) + "])");
		}

		final int toWrite = len * Conversion.FLOAT_SIZE; 		
		checkFreeSpace(toWrite);
		conversion.double2byte(ref, off, len, buffer, index);
		index += toWrite;
	}

	public void flush() throws IOException {
		// empty
	}

	public void finish() {
		// empty
	}

	public boolean finished() {
		return true;
	}

	public void close() throws IOException {
		// empty
	}

	public int bufferSize() {
		return BUF_SIZE;
	}
}
