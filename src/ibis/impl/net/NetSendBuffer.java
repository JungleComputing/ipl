package ibis.impl.net;

/**
 * Provides a simple wrapper class for a byte array for sending data.
 */
public class NetSendBuffer extends NetBuffer {

	public NetSendBuffer(byte[] data,
			     int    length) {
		super(data, length);
	}

	public NetSendBuffer(byte[] 	  data,
			     int    	  base,
			     int          length) {
		super(data, base, length);
	}

	public NetSendBuffer(byte[] 	  data,
			     int    	  length,
			     NetAllocator allocator) {
		super(data, length, allocator);
	}

	public NetSendBuffer(byte[] 	  data,
			     int          base,
			     int    	  length,
			     NetAllocator allocator) {
		super(data, base, length, allocator);
	}

}
