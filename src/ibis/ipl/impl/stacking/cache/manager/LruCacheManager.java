package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LruCacheManager extends CacheManager {

    private final List<Connection> fromSPLiveConns;
    private final List<Connection> fromRPLiveConns;
    private final List<Connection> fromSPCacheConns;
    private final List<Connection> fromRPCacheConns;

    public LruCacheManager(CacheIbis ibis) {
        super(ibis);
        fromSPLiveConns = new LinkedList<Connection>();
        fromSPCacheConns = new LinkedList<Connection>();
        fromRPLiveConns = new LinkedList<Connection>();
        fromRPCacheConns = new LinkedList<Connection>();
    }

    /*
     * Pick out the N least recently used live connections and cache them.
     */
    private void lruCache(int n) {
        assert n <= fromSPLiveConns.size()
                + fromRPLiveConns.size();

            /*
             * Try to get first from the connections from the send port side.
             * Faster to cache.
             */
            for (Iterator it = fromSPLiveConns.iterator(); n > 0 && it.hasNext();) {
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
                fromSPCacheConns.add(con);
                n--;
            }

            /*
             * Go for those on the receive port side.
             */
            for (Iterator it = fromRPLiveConns.iterator(); n > 0 && it.hasNext();) {
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
                fromRPCacheConns.add(con);
                n--;
            }
    }

    @Override
    public synchronized void cacheConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        fromSPCacheConns.add(con);
        fromSPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void removeConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        fromSPCacheConns.remove(con);
        fromSPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void removeAllConnections(SendPortIdentifier spi) {
        for (Iterator it = fromSPCacheConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }
        for (Iterator it = fromSPLiveConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }

        logReport();
    }

    @Override
    public synchronized void addConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (fromRPLiveConns.size() + fromSPLiveConns.size() >= MAX_CONNS) {
            this.lruCache(1);
        }
        fromRPLiveConns.add(con);

        logReport();
    }

    @Override
    public synchronized void cacheConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        fromRPCacheConns.add(con);
        fromRPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void removeConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        fromRPCacheConns.remove(con);
        fromRPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void restoreConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (fromRPLiveConns.size() + fromSPLiveConns.size() >= MAX_CONNS) {
            this.lruCache(1);
        }
        fromRPCacheConns.remove(con);
        fromRPLiveConns.add(con);

        logReport();
    }

    @Override
    public synchronized boolean isConnAlive(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return fromSPLiveConns.contains(con);
    }

    @Override
    public synchronized boolean isConnCached(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        return fromRPCacheConns.contains(con);
    }

    @Override
    public synchronized boolean hasConnections(ReceivePortIdentifier rpi) {
        for (Connection con : fromRPLiveConns) {
            if (con.contains(rpi)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<ReceivePortIdentifier> cachedRpisFrom(
            SendPortIdentifier spi) {
        List<ReceivePortIdentifier> result =
                new LinkedList<ReceivePortIdentifier>();
        for (Connection con : fromSPCacheConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        return result;
    }

    @Override
    public synchronized ReceivePortIdentifier[] allRpisFrom(SendPortIdentifier spi) {
        List<ReceivePortIdentifier> result = new LinkedList<ReceivePortIdentifier>();

        for (Connection con : fromSPLiveConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        for (Connection con : fromSPCacheConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        return result.toArray(new ReceivePortIdentifier[result.size()]);
    }

    @Override
    public synchronized SendPortIdentifier[] allSpisFrom(ReceivePortIdentifier rpi) {
        List<SendPortIdentifier> result = new LinkedList<SendPortIdentifier>();

        for (Connection con : fromRPLiveConns) {
            if (con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        for (Connection con : fromRPCacheConns) {
            if (con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        return result.toArray(new SendPortIdentifier[result.size()]);
    }

    @Override
    public synchronized Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port, Set<ReceivePortIdentifier> rpis,
            long timeoutMillis, boolean fillTimeout) throws
            ConnectionsFailedException, ConnectionTimedOutException {

        if (rpis == null || rpis.isEmpty()) {
            throw new ConnectionsFailedException("Array of send ports is null or empty.");
        }

        /*
         * Get the alive connections from this send port.
         */
        Set<ReceivePortIdentifier> aliveConn =
                new HashSet<ReceivePortIdentifier>(
                Arrays.asList(port.sendPort.connectedTo()));
        /*
         * Filter all the connections with the ones received as params.
         */
        aliveConn.retainAll(rpis);

        if (aliveConn.size() > 0) {
            rpis.removeAll(aliveConn);
            markAsUsed(port.identifier(), aliveConn);
        }

        if (rpis.isEmpty()) {
            /*
             * I'm already connected to every receive port passed as param.
             */
            return aliveConn;
        }

        Set<ReceivePortIdentifier> result = getSomeConnections(port,
                rpis, aliveConn.size(), timeoutMillis, fillTimeout);

        result.addAll(aliveConn);

        if (result.size() <= 0) {
            throw new ConnectionTimedOutException("Cache manager couldn't"
                    + " create at least one connection.", null);
        }

        logReport();

        return result;
    }

    /*
     * for each rpi in rpis[]: (port, rpi) is not alive.
     */
    private Set<ReceivePortIdentifier> getSomeConnections(CacheSendPort port,
            Set<ReceivePortIdentifier> rpis,
            int aliveConNo, long timeoutMillis, boolean fillTimeout) {

        Set<ReceivePortIdentifier> result = new HashSet<ReceivePortIdentifier>();
        int toConnectNo, freeConns;

        long deadline = 0, timeout;
        if (timeoutMillis > 0) {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }

        toConnectNo = Math.min(MAX_CONNS - aliveConNo, rpis.size());
        freeConns = Math.min(toConnectNo, 
                MAX_CONNS - fromRPLiveConns.size() - fromSPLiveConns.size());
        List<Connection> tempList = new LinkedList<Connection>();

        /*
         * Create connections whilst we have free space.
         */
        for (Iterator<ReceivePortIdentifier> it = rpis.iterator();
                freeConns > 0 && it.hasNext();) {
            try {
                ReceivePortIdentifier rpi = it.next();

                if (deadline > 0) {
                    timeout = deadline - System.currentTimeMillis();
                    if (timeout <= 0) {
                        break;
                    }

                    CacheManager.log.log(Level.INFO, "Base send port connecting:\t"
                            + "({0}, {1}, {2})",
                            new Object[]{rpi, timeout, fillTimeout});
                    port.sendPort.connect(rpi, timeout, fillTimeout);
                } else {
                    CacheManager.log.log(Level.INFO, "Base send port connecting:\t"
                            + "({0})", rpi);
                    port.sendPort.connect(rpi);
                }

                result.add(rpi);
                freeConns--;
                toConnectNo--;

                Connection con = new Connection(port.identifier(), rpi);
                tempList.add(con);
            } catch (IOException ex) {
                Logger.getLogger(RandomCacheManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (Iterator<ReceivePortIdentifier> it = rpis.iterator();
                toConnectNo > 0 && it.hasNext();) {
            try {
                ReceivePortIdentifier rpi = it.next();

                /*
                 * Check if I'm full of connections.
                 */
                if (fromSPLiveConns.size() + fromRPLiveConns.size()
                        + tempList.size() >= MAX_CONNS) {
                    if (deadline > 0 && deadline - System.currentTimeMillis() <= 0) {
                        break;
                    }
                    lruCache(1);
                }

                if (deadline > 0) {
                    timeout = deadline - System.currentTimeMillis();
                    if (timeout <= 0) {
                        break;
                    }
                    CacheManager.log.log(Level.INFO, "Base send port connecting:\t"
                            + "({0}, {1}, {2})",
                            new Object[]{rpi, timeout, fillTimeout});
                    port.sendPort.connect(rpi, timeout, fillTimeout);
                } else {
                    CacheManager.log.log(Level.INFO, "Base send port connecting:\t"
                            + "({0})",
                            new Object[]{rpi});
                    port.sendPort.connect(rpi);
                }

                result.add(rpi);
                toConnectNo--;

                Connection con = new Connection(port.identifier(), rpi);
                tempList.add(con);
            } catch (IOException ex) {
                Logger.getLogger(RandomCacheManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /*
         * I'm adding the connections here at the end, because I didn't want to
         * cache some of the connection I was actually fighting to make room
         * for.
         */
        for (Connection con : tempList) {
            fromSPLiveConns.add(con);
            fromSPCacheConns.remove(con);
        }

        return result;
    }

    private void markAsUsed(SendPortIdentifier spi,
            Set<ReceivePortIdentifier> allAliveConn) {
        
        Set<Connection> aliveConn = new HashSet<Connection>();
        for (ReceivePortIdentifier rpi : allAliveConn) {
            aliveConn.add(new Connection(spi, rpi));
        }

        fromSPLiveConns.removeAll(aliveConn);
        fromSPLiveConns.addAll(aliveConn);
    }

    private void logReport() {
        CacheManager.log.log(Level.INFO, "\n\t{0} alive connections:\t{1}, {2}"
                + "\n\t{3} cached connections:\t{4}, {5}",
                new Object[]{fromSPLiveConns.size() + fromRPLiveConns.size(),
                    fromSPLiveConns, fromRPLiveConns,
                    fromSPCacheConns.size() + fromRPCacheConns.size(),
                    fromSPCacheConns, fromRPCacheConns});
    }
}
