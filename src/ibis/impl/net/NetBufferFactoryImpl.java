package ibis.impl.net;

/**
 * Interface to specify a NetBufferFactoryImpl.
 * Instantiate this to create custom NetBuffers.
 */
public interface NetBufferFactoryImpl {

    /**
     * Create a NetBuffer.
     *
     * @param data the data buffer
     * @param length the length of the data stored in the buffer
     * @param allocator the allocator used to allocate the buffer, or
     *        <CODE>null</CODE> if no allocator has been used to allocate
     *        <CODE>data</CODE>
     */
    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator);

    /**
     * Test type compatibility of a buffer with this factory
     *
     * @param buffer the buffer
     * @return whether the buffer is compatible with this buffer factory
     */
    public boolean isSuitableClass(NetBuffer buffer);

}
