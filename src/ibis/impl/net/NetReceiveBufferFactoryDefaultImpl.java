package ibis.impl.net;

public class NetReceiveBufferFactoryDefaultImpl implements
        ibis.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data, int length,
            NetAllocator allocator) {
        return new NetReceiveBuffer(data, length, allocator);
    }

    public boolean isSuitableClass(NetBuffer buffer) {
        return buffer instanceof NetReceiveBuffer;
    }

}