package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.NetBuffer;
import ibis.ipl.impl.net.NetAllocator;

class RelReceiveBufferFactoryImpl
	implements ibis.ipl.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new RelReceiveBuffer(data, length, allocator);
    }

    public boolean isSuitableClass(NetBuffer buffer) {
	return buffer instanceof RelReceiveBuffer;
    }

}
