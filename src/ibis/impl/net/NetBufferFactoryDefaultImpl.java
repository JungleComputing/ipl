package ibis.impl.net;

/**
 * Default implementation for {@link ibis.impl.net.NetBufferFactoryImpl}.
 */
public class NetBufferFactoryDefaultImpl
    implements ibis.impl.net.NetBufferFactoryImpl {

    /**
     * Create a buffer around a specified byte array and
     * {@link ibis.impl.net.NetAllocator}.
     *
     * @param data a byte array that becomes the data buffer of the created
     * 		{@link ibis.impl.net.NetBuffer}
     * @param length the length of the data to be stored in the buffer
     * @param allocator the {@link ibis.impl.net.NetAllocator} used to manage the
     * 		data array
     * @return the buffer
     */
    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new NetBuffer(data, length, allocator);
    }

    /**
     * Test type compatibility of a buffer with this factory
     *
     * @param buffer the buffer
     * @return true because all buffers are compatible with this buffer factory
     */
    public boolean isSuitableClass(NetBuffer buffer) {
	return true;
    }

}
