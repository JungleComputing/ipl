package ibis.impl.net;

/**
 * Provides a simple wrapper class for a byte array for receiving data.
 */
public class NetReceiveBuffer extends NetBuffer {

	/**
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer(byte[] data,
				int    length) {
		super(data, length);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer(byte[] 	     data,
				int          base,
				int    	     length) {
		super(data, base, length);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer(byte[] 	     data,
				int    	     length,
				NetAllocator allocator) {
		super(data, length, allocator);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer(byte[] 	     data,
				int          base,
				int    	     length,
				NetAllocator allocator) {
		super(data, base, length, allocator);
	}

}
