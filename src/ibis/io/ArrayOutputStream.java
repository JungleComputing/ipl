package ibis.io;

import java.io.IOException;

/**
 * This is an implementation of the <code>IbisAccumulator</code> interface
 * which is actually the (old) <code>ArrayOutputStream</code>.
 * This way the whole thing becomes backwards
 * compatible with older implementations.
 */
public abstract class ArrayOutputStream
	implements IbisAccumulator, IbisStreamFlags
{
    /**
     * Constructor.
     */
    public ArrayOutputStream() {
    }

    /**
     * Tells the underlying implementation to flush all the data.
     *
     * @exception IOException on IO error.
     */
    public abstract void flush() throws IOException;

    /**
     * Checks whether all data has been written after a flush.
     * @return true if all data has been written after a flush.
     */
    public abstract boolean finished();

    /**
     * Blocks until the data is written.
     * @exception IOException on IO error.
     */
    public abstract void finish() throws IOException;

    /**
     * Tells the underlying implementation that this stream is closed.
     * @exception IOException on IO error.
     */
    public abstract void close() throws IOException;

    /**
     * Returns the number of bytes that was written to the message, 
     * in the stream dependant format.
     * This is the number of bytes that will be sent over the network 
     * @return the number of bytes written
     */
    public abstract long bytesWritten();

    /** 
     * Resets the counter for the number of bytes written
     */
    public abstract void resetBytesWritten();

    /**
     * Writes a boolean value to the accumulator.
     * @param     value             The boolean value to write.
     * @exception IOException on IO error.
     */
    public void writeBoolean(boolean value) throws IOException {
	throw new IOException("Not implemented");
    }


    /**
     * Writes a byte value to the accumulator.
     * @param     value             The byte value to write.
     * @exception IOException on IO error.
     */
    public void writeByte(byte value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a char value to the accumulator.
     * @param     value             The char value to write.
     * @exception IOException on IO error.
     */
    public void writeChar(char value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a short value to the accumulator.
     * @param     value             The short value to write.
     * @exception IOException on IO error.
     */
    public void writeShort(short value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a int value to the accumulator.
     * @param     value             The int value to write.
     * @exception IOException on IO error.
     */
    public void writeInt(int value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a long value to the accumulator.
     * @param     value             The long value to write.
     * @exception IOException on IO error.
     */
    public void writeLong(long value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a float value to the accumulator.
     * @param     value             The float value to write.
     * @exception IOException on IO error.
     */
    public void writeFloat(float value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes a double value to the accumulator.
     * @param     value             The double value to write.
     * @exception IOException on IO error.
     */
    public void writeDouble(double value) throws IOException {
	throw new IOException("Not implemented");
    }

    /**
     * Writes (a slice of) an array of Booleans into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(boolean [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Bytes into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(byte [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Characters into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(char [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Short Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(short [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(int [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Long Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(long [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Floats into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(float [] source, int offset, int size)
	    throws IOException;

    /**
     * Writes (a slice of) an array of Doubles into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public abstract void writeArray(double [] source, int offset, int size)
	    throws IOException;
}
