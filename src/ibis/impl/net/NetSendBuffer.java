package ibis.ipl.impl.net;

/**
 * Provides a simple wrapper class for a byte array for sending data.
 */
public class NetSendBuffer extends NetBuffer {

	/**
	 * {@inheritDoc}
	 */
	public NetSendBuffer(byte[] data,
			     int    length) {
		super(data, length);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetSendBuffer(byte[] 	  data,
			     int    	  base,
			     int          length) {
		super(data, base, length);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetSendBuffer(byte[] 	  data,
			     int    	  length,
			     NetAllocator allocator) {
		super(data, length, allocator);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetSendBuffer(byte[] 	  data,
			     int          base,
			     int    	  length,
			     NetAllocator allocator) {
		super(data, base, length, allocator);
	}

}
