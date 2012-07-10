package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LruCacheManager extends CacheManager {

    private final List<Connection> liveList;
    private final List<Connection> cacheList;

    LruCacheManager(CacheIbis ibis) {
        super(ibis);
        liveList = new LinkedList<Connection>();
        cacheList = new LinkedList<Connection>();
    }

    /*
     * Pick out the N least recently used live connections and cache them.
     */
    private synchronized void lruCache(int n) {
        assert n <= liveList.size();

        for (Iterator it = liveList.iterator(); n > 0 && it.hasNext();) {
            Connection con = (Connection) it.next();
            try {
                con.cache();
            } catch (IOException ex) {
                CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                        + " to be cached, but still counted."
                        + "\nException occured:\t{1}",
                        new Object[]{con.toString(), ex.toString()});
            }
            it.remove();
            cacheList.add(con);
            n--;
        }
    }

    @Override
    public void cacheConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        // the connection may be cached or alive.
        cacheList.add(con);
        liveList.remove(con);
    }

    @Override
    public void removeConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        // the connection may be cached or alive.
        cacheList.remove(con);
        liveList.remove(con);
    }

    @Override
    public void removeAllConnections(SendPortIdentifier spi) {
        for (Iterator it = cacheList.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }
        for (Iterator it = liveList.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }
    }

    @Override
    public void addConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (liveList.contains(con)) {
            return;
        }
        if (liveList.size() >= MAX_CONNS) {
            this.lruCache(1);
        }
        liveList.add(con);
    }

    @Override
    public void cacheConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        cacheList.add(con);
        liveList.remove(con);
    }

    @Override
    public void removeConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        cacheList.remove(con);
        liveList.remove(con);
    }

    @Override
    public void restoreConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (liveList.size() >= MAX_CONNS) {
            this.lruCache(1);
        }
        cacheList.remove(con);
        liveList.add(con);
    }

    /**
     * Big TODO: implement it, and handle timeout.
     *
     * @param port
     * @param rpis
     * @param timeoutMillis
     * @return
     * @throws ConnectionsFailedException
     */
    @Override
    public Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port, Set<ReceivePortIdentifier> rpis,
            long timeoutMillis, boolean fillTimeout) {
        Set<ReceivePortIdentifier> result;
        /*
         * For now, I will assume I can connect to all the rpis in the list.
         * store the connections .. fake anyway
         */
        ReceivePortIdentifier[] toConnect =
                rpis.toArray(new ReceivePortIdentifier[rpis.size()]);
        try {
            port.sendPort.connect(toConnect);
        } catch (ConnectionsFailedException ex) {
            Logger.getLogger(LruCacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
//        if (noAliveConnections + noConn > MAX_CONNECTIONS) {
//            int connToBeFreed = noAliveConnections + noConn - MAX_CONNECTIONS;
//            log.log(Level.INFO, "Caching {0} connections.", connToBeFreed);
//            connToBeFreed = cacheAtLeastNConnExcept(connToBeFreed, spi);
//            // subtract the cached connections
//            noAliveConnections -= connToBeFreed;
//            log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
//        }
    }

    @Override
    protected void usingConnections(Set<ReceivePortIdentifier> allAliveConn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<ReceivePortIdentifier> cachedRpisFrom(SendPortIdentifier identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isConnAlive(SendPortIdentifier identifier, ReceivePortIdentifier rpi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ReceivePortIdentifier[] allRpisFrom(SendPortIdentifier identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasConnections() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isConnCached(ReceivePortIdentifier identifier, SendPortIdentifier spi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SendPortIdentifier[] allSpisFrom(ReceivePortIdentifier identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
