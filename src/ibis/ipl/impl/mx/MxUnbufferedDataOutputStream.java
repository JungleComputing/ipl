package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public abstract class MxUnbufferedDataOutputStream extends MxDataOutputStream implements Config {
	//FIXME not threadsafe

	ByteBuffer buffer;
	
	protected MxUnbufferedDataOutputStream(ByteOrder order) {
		super();
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
	}

	@Override
	public int bufferSize() {
		return buffer.capacity();
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.MxDataOutputStream#isEmpty()
	 */
	@Override
	protected boolean isEmpty() {
		return buffer.position() == 0;
	}

	@Override
	public void write(int arg0) throws IOException {
		writeByte((byte) arg0);
	}

	public void writeBoolean(boolean value) throws IOException {
        if (value) {
            writeByte((byte) 1);
        } else {
            writeByte((byte) 0);
        }
	}

	public void writeByte(byte value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeByte(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.put(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.put(value);
        }
	}

	public void writeChar(char value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeChar(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putChar(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putChar(value);
        }
	}

	public void writeDouble(double value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeDouble(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putDouble(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putDouble(value);
        }
	}

	public void writeFloat(float value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeFloat(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putFloat(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putFloat(value);
        }
	}

	public void writeInt(int value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeInt(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putInt(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putInt(value);
        }
	}

	public void writeLong(long value) throws IOException {
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeLong(" + value + ")");
        }*/
        try {
            buffer.putLong(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putLong(value);
        }
	}

	public void writeShort(short value) throws IOException {
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeShort(" + value + ")");
        }*/
        try {
            buffer.putShort(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putShort(value);
        }
	}
	
	public void writeArray(boolean[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeBoolean(source[i]);
        }
	}

	public void writeArray(byte[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeByte(source[i]);
        }
	}

	public void writeArray(char[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeChar(source[i]);
        }
	}

	public void writeArray(short[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeShort(source[i]);
        }
	}

	public void writeArray(int[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeInt(source[i]);
        }
	}

	public void writeArray(long[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeLong(source[i]);
        }
	}

	public void writeArray(float[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeFloat(source[i]);
        }
	}

	public void writeArray(double[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeDouble(source[i]);
        }
	}	
}
