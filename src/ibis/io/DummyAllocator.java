package ibis.io;


/**
 * Allocator for arrays of primitives that is tailored towards the
 * IbisSerializationOutputStream.
 */
public class DummyAllocator extends DataAllocator implements IbisStreamFlags {

    public DummyAllocator() {
	super(0);
    }

    /**
     * Obtain an array of shorts fit to store an IbisSerializationOutputStream
     * index descriptor. Get it from the cache if possible.
     *
     * @return an index descriptor buffer
     */
    public short[] getIndexArray() {
	return new short[PRIMITIVE_TYPES];
    }

    /**
     * Obtain an array of bytes for an IbisSerializationOutputStream byte
     * buffer. The size is determined in IbisStreamFlags.BYTE_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a byte buffer
     */
    public byte[] getByteArray() {
	return new byte[BYTE_BUFFER_SIZE];
    }

    /**
     * Obtain an array of chars for an IbisSerializationOutputStream char
     * buffer. The size is determined in IbisStreamFlags.CHAR_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a char buffer
     */
    public char[] getCharArray() {
	return new char[CHAR_BUFFER_SIZE];
    }

    /**
     * Obtain an array of shorts for an IbisSerializationOutputStream short
     * buffer. The size is determined in IbisStreamFlags.SHORT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a short buffer
     */
    public short[] getShortArray() {
	return new short[SHORT_BUFFER_SIZE];
    }

    /**
     * Obtain an array of ints for an IbisSerializationOutputStream int
     * buffer. The size is determined in IbisStreamFlags.INT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a int buffer
     */
    public int[] getIntArray() {
	return new int[INT_BUFFER_SIZE];
    }

    /**
     * Obtain an array of longs for an IbisSerializationOutputStream long
     * buffer. The size is determined in IbisStreamFlags.LONG_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a long buffer
     */
    public long[] getLongArray() {
	return new long[LONG_BUFFER_SIZE];
    }

    /**
     * Obtain an array of floats for an IbisSerializationOutputStream float
     * buffer. The size is determined in IbisStreamFlags.FLOAT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a float buffer
     */
    public float[] getFloatArray() {
	return new float[FLOAT_BUFFER_SIZE];
    }

    /**
     * Obtain an array of doubles for an IbisSerializationOutputStream double
     * buffer. The size is determined in IbisStreamFlags.DOUBLE_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a double buffer
     */
    public double[] getDoubleArray() {
	return new double[DOUBLE_BUFFER_SIZE];
    }

    /**
     * Return an array of shorts fit to store an IbisSerializationOutputStream
     * index descriptor. Put it in the cache if possible.
     *
     * @param buffer the index buffer
     */
    public void putIndexArray(short[] buffer) {
    }

    /**
     * Return an array of bytes for an IbisSerializationOutputStream byte
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the byte buffer
     */
    public void putByteArray(byte[] buffer) {
    }

    /**
     * Return an array of chars for an IbisSerializationOutputStream char
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the char buffer
     */
    public void putCharArray(char[] buffer) {
    }

    /**
     * Return an array of shorts for an IbisSerializationOutputStream short
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the short buffer
     */
    public void putShortArray(short[] buffer) {
    }

    /**
     * Return an array of ints for an IbisSerializationOutputStream int
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the int buffer
     */
    public void putIntArray(int[] buffer) {
    }

    /**
     * Return an array of longs for an IbisSerializationOutputStream long
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the long buffer
     */
    public void putLongArray(long[] buffer) {
    }

    /**
     * Return an array of floats for an IbisSerializationOutputStream float
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the float buffer
     */
    public void putFloatArray(float[] buffer) {
    }

    /**
     * Return an array of doubles for an IbisSerializationOutputStream double
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the double buffer
     */
    public void putDoubleArray(double[] buffer) {
    }

}
