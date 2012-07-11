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

public class RandomCacheManager extends CacheManager {

    /*
     * This set will contain all alive connections.
     */
    private final List<Connection> liveList;
    /*
     * This set will contain all cached connections.
     */
    private final List<Connection> cacheList;
    /*
     * My random generator.
     */
    private final Random r;

    public RandomCacheManager(CacheIbis ibis) {
        super(ibis);
        liveList = new LinkedList<Connection>();
        cacheList = new LinkedList<Connection>();
        r = new Random();
    }

    /*
     * Pick out one random connection and cache it.
     */
    private void randomCache() {
        assert !liveList.isEmpty();
        int idx = r.nextInt(liveList.size());
        /*
         * Connection removed from live list.
         */
        Connection con = liveList.remove(idx);
        try {
            con.cache();
            /*
             * Connection added to cache list only if successful.
             */
            cacheList.add(con);
        } catch (IOException ex) {
            CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                    + " to be cached."
                    + "\nException occured:\t{1}",
                    new Object[]{con.toString(), ex.toString()});
        }
    }

    @Override
    public synchronized void cacheConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        // the connection may be cached or alive.
        cacheList.add(con);
        liveList.remove(con);
        logReport();
    }

    @Override
    public synchronized void removeConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        // the connection may be cached or alive.
        cacheList.remove(con);
        liveList.remove(con);
        logReport();
    }

    @Override
    public synchronized void removeAllConnections(SendPortIdentifier spi) {
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
        logReport();
    }

    @Override
    public synchronized void addConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (liveList.contains(con)) {
            return;
        }
        if (liveList.size() >= MAX_CONNS) {
            this.randomCache();
        }
        liveList.add(con);
        logReport();
    }

    @Override
    public synchronized void cacheConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        cacheList.add(con);
        liveList.remove(con);
        logReport();
    }

    @Override
    public synchronized void removeConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        cacheList.remove(con);
        liveList.remove(con);
        logReport();
    }

    @Override
    public synchronized void restoreConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (liveList.size() >= MAX_CONNS) {
            this.randomCache();
        }
        cacheList.remove(con);
        liveList.add(con);
        logReport();
    }

    @Override
    public synchronized boolean isConnAlive(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return liveList.contains(con);
    }

    @Override
    public synchronized boolean isConnCached(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        return cacheList.contains(con);
    }

    @Override
    public synchronized boolean hasConnections(ReceivePortIdentifier rpi) {
        for(Connection con : liveList) {
            if(con.contains(rpi)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<ReceivePortIdentifier> cachedRpisFrom(SendPortIdentifier spi) {
        List<ReceivePortIdentifier> result =
                new LinkedList<ReceivePortIdentifier>();
        for (Connection con : cacheList) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        return result;
    }

    @Override
    public synchronized ReceivePortIdentifier[] allRpisFrom(SendPortIdentifier spi) {
        List<ReceivePortIdentifier> result = new LinkedList<ReceivePortIdentifier>();

        for (Connection con : liveList) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        for (Connection con : cacheList) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        return result.toArray(new ReceivePortIdentifier[result.size()]);
    }

    @Override
    public synchronized SendPortIdentifier[] allSpisFrom(ReceivePortIdentifier rpi) {
        List<SendPortIdentifier> result = new LinkedList<SendPortIdentifier>();

        for (Connection con : liveList) {
            if (con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        for (Connection con : cacheList) {
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
        }

        /*
         * I'm already connected to every receive port passed as param.
         */
        if (rpis.isEmpty()) {
            return aliveConn;
        }

        Set<ReceivePortIdentifier> result = getSomeConnections(port,
                rpis, aliveConn.size(), timeoutMillis, fillTimeout);

        result.addAll(aliveConn);

        if (result.size() <= 0) {
            throw new ConnectionTimedOutException("Cache manager couldn't"
                    + " create at least one connection.", null);
        }

        aliveConn.addAll(result);

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

        /*
         * decide randomly how many connections to actually create.
         */
        if (aliveConNo > 0) {
            toConnectNo = r.nextInt(1 + Math.min(MAX_CONNS - aliveConNo, rpis.size()));
        } else {
            toConnectNo = 1 + r.nextInt(Math.min(MAX_CONNS, rpis.size()));
        }

        freeConns = Math.min(toConnectNo, MAX_CONNS - liveList.size());

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

                if (liveList.size() + tempList.size() >= MAX_CONNS) {
                    if (deadline > 0 && deadline - System.currentTimeMillis() <= 0) {
                        break;
                    }
                    randomCache();
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
            liveList.add(con);
            cacheList.remove(con);
        }

        return result;
    }

    private void logReport() {
        CacheManager.log.log(Level.INFO, "\n\t{0} alive connections:\t{1}"
                + "\n\t{2} cached connections:\t{3}",
                new Object[]{liveList.size(), liveList,
                    cacheList.size(), cacheList});
    }

    
}
