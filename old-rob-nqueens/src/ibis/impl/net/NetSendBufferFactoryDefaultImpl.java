/* $Id$ */

package ibis.impl.net;

public class NetSendBufferFactoryDefaultImpl implements
        ibis.impl.net.NetBufferFactoryImpl {

    public NetBuffer createBuffer(byte[] data, int length,
            NetAllocator allocator) {
        return new NetSendBuffer(data, length, allocator);
    }

    public boolean isSuitableClass(NetBuffer buffer) {
        return buffer instanceof NetSendBuffer;
    }

}
