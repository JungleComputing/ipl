package ibis.ipl.impl.net;

/**
 * Provides a simple wrapper class for a byte array for sending data.
 */
public final class NetSendBuffer extends NetBuffer {

	/**
	 * Constructor.
	 *
	 * @param data the buffer.
	 * @param length the length of valid data. 
	 */
	public NetSendBuffer(byte[] data,
			     int    length) {
		super(data, length);
	}

	/**
	 * Constructor.
	 *
	 * @param data the buffer.
	 * @param length the length of valid data. 
	 * @param allocator the allocator that was used to allocate the buffer.
	 */
	public NetSendBuffer(byte[] 	  data,
			     int    	  length,
			     NetAllocator allocator) {
		super(data, length, allocator);
	}

	/**
	 * Constructor.
         *
	 * @param data the buffer.
         * @param base the base index of the data stored in the buffer.
	 * @param length the length of the data stored in the buffer.
	 */
	public NetSendBuffer(byte[] data,
                             int    base,
			     int    length) {
		super(data, base, length);
	}
}
