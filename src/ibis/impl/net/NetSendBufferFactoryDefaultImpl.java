package ibis.ipl.impl.net;

public class NetSendBufferFactoryDefaultImpl
    implements ibis.ipl.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator) {
	return new NetSendBuffer(data, length, allocator);
    }

}
