package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public abstract class CacheManagerImpl extends CacheManager {

    protected final List<Connection> fromSPLiveConns;
    protected final List<Connection> fromRPLiveConns;
    protected final List<Connection> fromSPCacheConns;
    protected final List<Connection> fromRPCacheConns;
    protected final List<Connection> reservedConns;
    protected final List<Connection> canceledReservations;

    protected CacheManagerImpl(CacheIbis ibis) {
        super(ibis);
        fromSPLiveConns = new LinkedList<Connection>();
        fromSPCacheConns = new LinkedList<Connection>();
        fromRPLiveConns = new LinkedList<Connection>();
        fromRPCacheConns = new LinkedList<Connection>();
        reservedConns = new LinkedList<Connection>();
        canceledReservations = new LinkedList<Connection>();
    }

    abstract protected Connection cacheOneConnection();

    abstract protected Connection cacheOneConnectionFor(Connection con);

    @Override
    public synchronized void cacheConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        while (reservedConns.contains(con)) {
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }

        /*
         * Situation: the two sides (SP and RP) want to cache the same
         * connection. Then this method will be called x2.
         */
        if (!fromSPCacheConns.contains(con)) {
            fromSPCacheConns.add(con);
        }
        fromSPLiveConns.remove(con);

        statistics.cache(con);

        logReport();
    }

    @Override
    public synchronized void removeConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        while (reservedConns.contains(con)) {
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }

        statistics.remove(con);

        fromSPCacheConns.remove(con);
        fromSPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void removeAllConnections(SendPortIdentifier spi) {
        boolean notAvailable;
        while (true) {
            notAvailable = false;
            for (Connection con : reservedConns) {
                if (con.contains(spi)) {
                    notAvailable = true;
                    break;
                }
            }
            if (notAvailable) {
                try {
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            } else {
                break;
            }
        }

        for (Iterator it = fromSPCacheConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
                statistics.remove(conn);
            }
        }
        for (Iterator it = fromSPLiveConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
                statistics.remove(conn);
            }
        }

        logReport();
    }

    @Override
    public synchronized void addConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        /*
         * The connection was reserved and now it's actually created.
         */
        if (reservedConns.contains(con)) {
            reservedConns.remove(con);
            fromRPLiveConns.add(con);

            notifyAll();

            statistics.add(con);

            logReport();
            return;
        }

        if (fullConns()) {
            Connection cachedCon = this.cacheOneConnection();

            statistics.cache(cachedCon);
        }

        fromRPLiveConns.add(con);

        statistics.add(con);

        logReport();
    }

    @Override
    public synchronized void cacheConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        while (reservedConns.contains(con)) {
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }

        /*
         * Situation: the two sides (SP and RP) want to cache the same
         * connection. Then this method will be called x2.
         */
        if (!fromRPCacheConns.contains(con)) {
            fromRPCacheConns.add(con);
            statistics.cache(con);
        }
        fromRPLiveConns.remove(con);

        logReport();
    }

    @Override
    public synchronized void removeConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        while (reservedConns.contains(con)) {
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }

        fromRPCacheConns.remove(con);
        fromRPLiveConns.remove(con);

        statistics.remove(con);

        logReport();
    }

    @Override
    public synchronized void restoreConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        /*
         * The connection was reserved and cached and now it's actually
         * restored.
         */
        if (reservedConns.contains(con)) {
            reservedConns.remove(con);
            fromRPCacheConns.remove(con);

            fromRPLiveConns.add(con);

            notifyAll();

            statistics.restore(con);

            logReport();
            return;
        }

        if (fullConns()) {
            Connection cached = cacheOneConnection();

            statistics.cache(cached);
        }

        fromRPCacheConns.remove(con);
        fromRPLiveConns.add(con);

        statistics.restore(con);

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

        for (Connection con : fromRPCacheConns) {
            if (con.contains(rpi)) {
                return true;
            }
        }

        for (Connection con : reservedConns) {
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
            ConnectionsFailedException, ibis.ipl.IbisIOException {

        if (rpis == null || rpis.isEmpty()) {
            throw new ibis.ipl.IbisIOException("Array of send ports is null or empty.");
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

        CacheManager.log.log(Level.SEVERE, "Getting some connections"
                + " from the following set:\t{0}", rpis);
        System.out.flush();
        Set<ReceivePortIdentifier> result = getSomeConnections(port,
                rpis, aliveConn.size(), timeoutMillis, fillTimeout);

        result.addAll(aliveConn);

        if (result.size() <= 0) {
            throw new ConnectionsFailedException("Coulnd't connect to even one RP.");
        }

        logReport();

        return result;
    }

    /*
     * for each rpi in rpis[]: (port, rpi) is not alive.
     */
    private synchronized Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port,
            Set<ReceivePortIdentifier> rpis,
            int aliveConnsNo, long timeoutMillis, boolean fillTimeout) {

        Set<ReceivePortIdentifier> result = new HashSet<ReceivePortIdentifier>();
        try {
            int maxPossibleConns;

            long deadline = 0, timeout;
            if (timeoutMillis > 0) {
                deadline = System.currentTimeMillis() + timeoutMillis;
            }

            maxPossibleConns = Math.min(MAX_CONNS - aliveConnsNo, rpis.size());

            /*
             * Create some connections.
             */
            loop:
            for (Iterator<ReceivePortIdentifier> it = rpis.iterator();
                    maxPossibleConns > 0 && it.hasNext();) {

                ReceivePortIdentifier rpi = it.next();

                if (deadline > 0 && deadline - System.currentTimeMillis() <= 0) {
                    break loop;
                }

                /*
                 * Check if I'm full of connections.
                 */
                if (fullConns()) {
                    /*
                     * If I can't cache anything, but I do have at least some
                     * connections to offer, exit now.
                     */
                    if (!canCache() && result.size() + aliveConnsNo > 0) {
                        break loop;
                    }
                }
                
                /*
                 * Reserve the connection from the send port side.
                 */
                reserveConnection(port.identifier(), rpi);

                /*
                 * We do not have the rpi's ack.
                 */
                port.reserveAckReceived.remove(rpi);

                /*
                 * Reserve the connection on the receive port side.
                 */
                super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                        rpi, SideChannelProtocol.RESERVE);

                while (!port.reserveAckReceived.contains(rpi)) {
                    try {
                        if (deadline > 0) {
                            timeout = deadline - System.currentTimeMillis();
                            if (timeout <= 0) {
                                cancelReservation(port.identifier(), rpi);
                                super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                        rpi, SideChannelProtocol.CANCEL_RESERVATION);
                                break loop;
                            }
                            wait(timeout);
                        } else {
                            wait();
                        }
                    } catch (InterruptedException ignoreMe) {
                    }
                }

                if (deadline > 0) {
                    timeout = deadline - System.currentTimeMillis();
                    if (timeout <= 0) {
                        cancelReservation(port.identifier(), rpi);
                        super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                rpi, SideChannelProtocol.CANCEL_RESERVATION);
                        break;
                    }

                    try {
                        port.sendPort.connect(rpi, timeout, fillTimeout);
                        CacheManager.log.log(Level.INFO, "Base send port connected:\t"
                                + "({0}, {1}, {2})",
                                new Object[]{rpi, timeout, fillTimeout});
                    } catch (IOException ex) {
                        CacheManager.log.log(Level.WARNING, "Base send port "
                                + "failed to connect to receive port. Got"
                                + "exception:\t{0}", ex.toString());
                        cancelReservation(port.identifier(), rpi);
                        super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                rpi, SideChannelProtocol.CANCEL_RESERVATION);
                        continue;
                    }
                } else {
                    try {
                        port.sendPort.connect(rpi);
                        CacheManager.log.log(Level.INFO, "Base send port connected:\t"
                                + "({0})", rpi);
                    } catch (IOException ex) {
                        CacheManager.log.log(Level.WARNING, "Base send port "
                                + "failed to connect to receive port. Got"
                                + "exception:\t{0}", ex.toString());
                        cancelReservation(port.identifier(), rpi);
                        super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                rpi, SideChannelProtocol.CANCEL_RESERVATION);
                        continue;
                    }
                }

                result.add(rpi);
                maxPossibleConns--;
            }
            return result;
        } finally {
            /*
             * Move the successful connections from the reserved list to the
             * live list.
             */
            unReserve(port.identifier(), result);
            /*
             * Maybe someone was waiting to cache one connection and needs live
             * connections for that to happen.
             */
            notifyAll();
        }
    }

    @Override
    public synchronized void reserveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        if (fullConns()) {
            Connection temp = cacheOneConnectionFor(con);
            statistics.cache(temp);
        }
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            reservedConns.add(con);
        }
        logReport();
    }

    @Override
    public synchronized void reserveConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        if (fullConns()) {
            Connection temp = cacheOneConnectionFor(con);
            statistics.cache(temp);
        }
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            reservedConns.add(con);
        }
        logReport();
    }

    @Override
    public synchronized void cancelReservation(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        canceledReservations.add(new Connection(spi, rpi));

        notifyAll();

        logReport();
    }

    @Override
    public synchronized void cancelReservation(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        canceledReservations.add(new Connection(rpi, spi));

        notifyAll();

        logReport();
    }

    private void unReserve(SendPortIdentifier spi,
            Set<ReceivePortIdentifier> rpiSet) {
        Set<Connection> toBeMoved = new HashSet<Connection>();
        for (ReceivePortIdentifier rpi : rpiSet) {
            Connection temp = new Connection(spi, rpi);
            toBeMoved.add(temp);

            if (fromSPCacheConns.contains(temp)) {
                statistics.restore(temp);
            } else {
                statistics.add(temp);
            }
        }

        reservedConns.removeAll(toBeMoved);
        fromSPLiveConns.addAll(toBeMoved);
        fromSPCacheConns.removeAll(toBeMoved);

        logReport();
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
                + "\n\t{3} cached connections:\t{4}, {5}"
                + "\n\t{6} reserved connections:\t {7}",
                new Object[]{fromSPLiveConns.size() + fromRPLiveConns.size(),
                    fromSPLiveConns, fromRPLiveConns,
                    fromSPCacheConns.size() + fromRPCacheConns.size(),
                    fromSPCacheConns, fromRPCacheConns,
                    reservedConns.size(), reservedConns});
    }

    private boolean fullConns() {
        Set<Connection> all = new HashSet<Connection>();
        all.addAll(fromSPLiveConns);
        all.addAll(fromRPLiveConns);
        all.addAll(reservedConns);

        return all.size() >= MAX_CONNS;
    }

    private boolean canCache() {
        return fromSPLiveConns.size() + fromRPLiveConns.size() > 0;
    }
}
