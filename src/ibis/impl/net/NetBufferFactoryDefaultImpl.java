package ibis.impl.net;

public class NetBufferFactoryDefaultImpl
    implements ibis.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new NetBuffer(data, length, allocator);
    }

    public boolean isSuitableClass(NetBuffer buffer) {
	return true;
    }

}
