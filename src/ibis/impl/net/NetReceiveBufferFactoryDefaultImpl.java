package ibis.ipl.impl.net;

public class NetReceiveBufferFactoryDefaultImpl
    implements ibis.ipl.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new NetReceiveBuffer(data, length, allocator);
    }

}
