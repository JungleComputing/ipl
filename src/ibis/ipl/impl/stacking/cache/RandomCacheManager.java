package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

public class RandomCacheManager extends CacheManager{

    RandomCacheManager(CacheIbis ibis) {
        super(ibis);
    }

    @Override
    protected int cacheAtLeastNConnExcept(int n, SendPortIdentifier spi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected int cacheAtLeastNConnExcept(int n, ReceivePortIdentifier rpi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void cacheConnection(SendPortIdentifier sp, ReceivePortIdentifier rp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void addConnectionsImpl(SendPortIdentifier spi, ReceivePortIdentifier[] rpis) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeAllConnectionsImpl(SendPortIdentifier spi) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeConnectionImpl(SendPortIdentifier spi) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void addConnectionImpl(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeConnectionImpl(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
