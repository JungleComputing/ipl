package ibis.impl.net.rel;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBuffer;

class RelReceiveBufferFactoryImpl implements ibis.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data, int length,
            NetAllocator allocator) {
        return new RelReceiveBuffer(data, length, allocator);
    }

    public boolean isSuitableClass(NetBuffer buffer) {
        return buffer instanceof RelReceiveBuffer;
    }

}