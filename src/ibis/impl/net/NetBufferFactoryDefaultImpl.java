package ibis.ipl.impl.net;

public class NetBufferFactoryDefaultImpl
    implements ibis.ipl.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new NetBuffer(data, length, allocator);
    }

}
