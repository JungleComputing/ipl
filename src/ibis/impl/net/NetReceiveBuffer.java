package ibis.impl.net;

/**
 * Provides a simple wrapper class for a byte array for receiving data.
 */
public class NetReceiveBuffer extends NetBuffer {

    public NetReceiveBuffer(byte[] data, int length) {
        super(data, length);
    }

    public NetReceiveBuffer(byte[] data, int base, int length) {
        super(data, base, length);
    }

    public NetReceiveBuffer(byte[] data, int length, NetAllocator allocator) {
        super(data, length, allocator);
    }

    public NetReceiveBuffer(byte[] data, int base, int length,
            NetAllocator allocator) {
        super(data, base, length, allocator);
    }

}