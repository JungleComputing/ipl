package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import ibis.ipl.impl.stacking.cache.util.Timers;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public abstract class CacheManagerImpl extends CacheManager {

    protected final List<Connection> fromSPLiveConns;
    protected final List<Connection> fromRPLiveConns;
    protected final List<Connection> fromSPCacheConns;
    protected final List<Connection> fromRPCacheConns;
    protected final List<Connection> aliveReservedConns;
    protected final List<Connection> notAliveReservedConns;
    protected final List<Connection> canceledReservations;
    private Random r;

    protected CacheManagerImpl(CacheIbis ibis) {
        super(ibis);
        fromSPLiveConns = new LinkedList<Connection>();
        fromSPCacheConns = new LinkedList<Connection>();
        fromRPLiveConns = new LinkedList<Connection>();
        fromRPCacheConns = new LinkedList<Connection>();
        aliveReservedConns = new LinkedList<Connection>();
        notAliveReservedConns = new LinkedList<Connection>();
        canceledReservations = new LinkedList<Connection>();
        r = new Random();
    }

    abstract protected Connection cacheOneConnection();

    abstract protected Connection cacheOneConnectionFor(Connection con);

    @Override
    public void cacheConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi, boolean heKnows) {
        Connection con = new Connection(spi, rpi);

        while (aliveReservedConns.contains(con)) {
            try {
                super.reservationsCondition.await();
            } catch (InterruptedException ignoreMe) {
            }
        }
        
        /*
         * Situation: the two sides (SP and RP) want to cache the same
         * connection. Then this method will be called x2.
         */
        if (!fromSPCacheConns.contains(con)) {
            con.cache(heKnows);
            fromSPCacheConns.add(con);
            statistics.cache(con);
        }
        fromSPLiveConns.remove(con);

        logReport();
    }

    @Override
    public void removeConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        while (aliveReservedConns.contains(con) ||
                notAliveReservedConns.contains(con)) {
            try {
                super.reservationsCondition.await();
            } catch (InterruptedException ignoreMe) {
            }
        }

        con.remove();

        statistics.remove(con);

        fromSPCacheConns.remove(con);
        fromSPLiveConns.remove(con);

        super.allClosedCondition.signal();

        logReport();
    }

    @Override
    public void lostConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        statistics.remove(con);

        fromSPCacheConns.remove(con);
        fromSPLiveConns.remove(con);
        aliveReservedConns.remove(con);
        notAliveReservedConns.remove(con);
        
        super.reservationsCondition.signalAll();
        super.allClosedCondition.signalAll();

        logReport();
    }

    @Override
    public void closeSendPort(SendPortIdentifier spi) {
        boolean notAvailable;
        while (true) {
            notAvailable = false;
            for (Connection con : aliveReservedConns) {
                if (con.contains(spi)) {
                    notAvailable = true;
                    break;
                }
            }
            for (Connection con : notAliveReservedConns) {
                if (con.contains(spi)) {
                    notAvailable = true;
                    break;
                }
            }
            if (notAvailable) {
                try {
                    super.reservationsCondition.await();
                } catch (InterruptedException ignoreMe) {
                }
            } else {
                break;
            }
        }

        Connection.closeSendPort(spi);

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

        super.allClosedCondition.signal();

        logReport();
    }

    @Override
    public void activateReservedConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        /*
         * The connection was reserved and now it's actually created.
         */
        assert notAliveReservedConns.contains(con);

        notAliveReservedConns.remove(con);
        super.reservationsCondition.signalAll();

        fromRPLiveConns.add(con);
        super.noLiveConnCondition.signalAll();

        statistics.add(con);

        logReport();
    }

    @Override
    public void cacheConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        while (aliveReservedConns.contains(con)) {
            try {
                super.reservationsCondition.await();
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
    public void removeConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        while (aliveReservedConns.contains(con) ||
                notAliveReservedConns.contains(con)) {
            try {
                super.reservationsCondition.await();
            } catch (InterruptedException ignoreMe) {
            }
        }

        fromRPCacheConns.remove(con);
        fromRPLiveConns.remove(con);

        statistics.remove(con);

        super.allClosedCondition.signal();

        logReport();
    }

    @Override
    public void restoreReservedConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        /*
         * The connection was reserved and cached and now it's actually
         * restored.
         */
        assert notAliveReservedConns.contains(con);

        notAliveReservedConns.remove(con);
        super.reservationsCondition.signalAll();

        fromRPCacheConns.remove(con);

        fromRPLiveConns.add(con);
        super.noLiveConnCondition.signalAll();

        statistics.restore(con);

        logReport();
    }

    @Override
    public boolean isConnAlive(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return fromSPLiveConns.contains(con) || aliveReservedConns.contains(con);
    }

    @Override
    public boolean isConnCached(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        return fromRPCacheConns.contains(con);
    }
    
    @Override
    public boolean isConnCached(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return fromSPCacheConns.contains(con);
    }

    @Override
    public boolean hasConnections(ReceivePortIdentifier rpi) {
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

        for (Connection con : aliveReservedConns) {
            if (con.contains(rpi)) {
                return true;
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (con.contains(rpi)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ReceivePortIdentifier> cachedRpisFrom(
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
    public ReceivePortIdentifier[] allRpisFrom(SendPortIdentifier spi) {
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
        
        for (Connection con : aliveReservedConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        return result.toArray(new ReceivePortIdentifier[result.size()]);
    }

    @Override
    public SendPortIdentifier[] allSpisFrom(ReceivePortIdentifier rpi) {
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
        
        for (Connection con : aliveReservedConns) {
            if (con.contains(rpi)) {
                result.add(con.spi);
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        return result.toArray(new SendPortIdentifier[result.size()]);
    }
    
    private Set<ReceivePortIdentifier> aliveRpisFrom(
            SendPortIdentifier spi) {
        Set<ReceivePortIdentifier> result =
                new HashSet<ReceivePortIdentifier>();
        
        for (Connection con : fromSPLiveConns) {
            if (con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        
        for (Connection con : aliveReservedConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        return result;
    }

    @Override
    public Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port, Set<ReceivePortIdentifier> rpis,
            long timeoutMillis, boolean fillTimeout) throws
            ConnectionsFailedException, ibis.ipl.IbisIOException {

        if (rpis == null || rpis.isEmpty()) {
            throw new ibis.ipl.IbisIOException("Array of send ports is null or empty.");
        }

        Loggers.cacheLog.log(Level.INFO, "\n\t\tGetting some connections from:\t{0}", rpis);

        /*
         * Get the alive connections from this send port.
         */
        Set<ReceivePortIdentifier> aliveConn = aliveRpisFrom(port.identifier());
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
        
        /*
         * Reserve the already alive connections,
         * so that we won't cache them when 
         */
        for(ReceivePortIdentifier rpi : aliveConn) {
            reserveLiveConnection(port.identifier(), rpi);
        }
        
        logReport();

        Set<ReceivePortIdentifier> result = getSomeConnections(port,
                rpis, aliveConn.size(), timeoutMillis, fillTimeout);
        
        /*
         * I'm done with getting connections. Unreserve the connections
         * which were already alive,
         */
        for(ReceivePortIdentifier rpi: aliveConn) {
            unReserveLiveConnection(port.identifier(), rpi);
        }

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
    private Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port,
            Set<ReceivePortIdentifier> rpis,
            int alreadyAliveConnsNo, long timeoutMillis, boolean fillTimeout) {

        Set<ReceivePortIdentifier> result = new HashSet<ReceivePortIdentifier>();
        try {

            long deadline = 0, timeout;
            if (timeoutMillis > 0) {
                deadline = System.currentTimeMillis() + timeoutMillis;
            }

            int maxPossibleConns = Math.min(MAX_CONNS - alreadyAliveConnsNo, rpis.size());
            long sleepMillis = 3;

            whileLoop:
            while (true) {
                /*
                 * Create some connections.
                 */
                forLoop:
                for (Iterator<ReceivePortIdentifier> it = rpis.iterator();
                        maxPossibleConns > 0 && it.hasNext();) {

                    ReceivePortIdentifier rpi = it.next();

                    if (deadline > 0 && deadline - System.currentTimeMillis() <= 0) {
                        break forLoop;
                    }

                    /*
                     * Check if I'm full of connections.
                     */
                    if (fullConns()) {
                        /*
                         * If I can't cache anything, but I do have at least
                         * some connections to offer, exit now.
                         */
                        if (!canCache() && result.size() + alreadyAliveConnsNo > 0) {
                            break forLoop;
                        }
                    }

                    /*
                     * Reserve the connection over here, because we will 
                     * release the lock before doing
                     * baseSendPort.connect() (see reason below).
                     */
                    reserveConnection(port.identifier(), rpi);
                    
                    Timers.ackTimer.start();
                    
                    /*
                     * We do not have the rpi's ack.
                     */
                    port.reserveAcks.remove(rpi);

                    /*
                     * Reserve the connection on the receive port side.
                     */
                    super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                            rpi, SideChannelProtocol.RESERVE);

                    while (!port.reserveAcks.containsKey(rpi)) {
                        try {
                            if (deadline > 0) {
                                timeout = deadline - System.currentTimeMillis();
                                if (timeout <= 0) {
                                    cancelReservation(port.identifier(), rpi);
                                    super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                            rpi, SideChannelProtocol.CANCEL_RESERVATION);
                                    break forLoop;
                                }
                                super.reserveAcksCond.await(timeout, TimeUnit.MILLISECONDS);
                            } else {
                                super.reserveAcksCond.await();
                            }
                        } catch (InterruptedException ignoreMe) {
                        }
                    }
                    
                    Timers.ackTimer.stop();
                    
                    /*
                     * The connection was refused. 
                     * It was unreserve it from this side;
                     * now move on.
                     */
                    if(port.reserveAcks.get(rpi).equals(SideChannelProtocol.RESERVE_NACK)) {
                        continue forLoop;
                    }
                    
                    /*
                     * We got the connection reserved at both sides.
                     * Feel free to connect now.
                     */

                    if (deadline > 0) {
                        timeout = deadline - System.currentTimeMillis();
                        if (timeout <= 0) {
                            cancelReservation(port.identifier(), rpi);
                            super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                    rpi, SideChannelProtocol.CANCEL_RESERVATION);
                            break;
                        }

                        /*
                         * For this I changed every synchronized to lock. I need
                         * to release the lock whilst connecting, because I can
                         * get deadlock: two machines simultaneously connect and
                         * they both enter in the gotConnection upcall where
                         * they need the lock again; but neither of them can get
                         * it, because both of them hold it here.
                         */
                        super.lock.unlock();
                        Loggers.lockLog.log(Level.INFO, "Lock unlocked before"
                                + " base sendport connection.");

                        try {
                            port.baseSendPort.connect(rpi, timeout, fillTimeout);
                            Loggers.cacheLog.log(Level.INFO, "Base send port connected:\t"
                                    + "({0}, {1}, {2})",
                                    new Object[]{rpi, timeout, fillTimeout});
                        } catch (IOException ex) {
                            Loggers.cacheLog.log(Level.WARNING, "Base send port "
                                    + "failed to connect to receive port. Got"
                                    + "exception:\t{0}", ex.toString());
                            super.lock.lock();
                            try {
                                cancelReservation(port.identifier(), rpi);
                            } finally {
                                super.lock.unlock();
                            }
                            super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                    rpi, SideChannelProtocol.CANCEL_RESERVATION);
                            continue forLoop;
                        } finally {
                            /*
                             * Ironic, huh? locking in the finally block.
                             */
                            super.lock.lock();
                            Loggers.lockLog.log(Level.INFO, "Lock locked after"
                                + " base sendport connection.");
                        }
                    } else {
                        super.lock.unlock();
                        Loggers.lockLog.log(Level.INFO, "Lock unlocked before"
                                + " base sendport connection.");
                        try {
                            port.baseSendPort.connect(rpi);
                            Loggers.cacheLog.log(Level.INFO, "Base send port connected:\t"
                                    + "({0})", rpi);
                        } catch (IOException ex) {
                            Loggers.cacheLog.log(Level.WARNING, "Base send port "
                                    + "failed to connect to receive port. Got"
                                    + "exception:\t{0}", ex.toString());
                            super.lock.lock();
                            try {
                                cancelReservation(port.identifier(), rpi);
                            } finally {
                                super.lock.unlock();
                            }
                            super.sideChannelHandler.newThreadSendProtocol(port.identifier(),
                                    rpi, SideChannelProtocol.CANCEL_RESERVATION);
                            continue forLoop;
                        } finally {
                            super.lock.lock();
                            Loggers.lockLog.log(Level.INFO, "Lock locked after"
                                + " base sendport connection.");
                        }
                    }

                    result.add(rpi);
                    maxPossibleConns--;
                }
                /*
                 * It is possible that we have no connections, i.e. result is empty.
                 * this has happened because we got all NACKs.
                 * 
                 * Either the machines which sent us NACKS had been busy 
                 * with other connections - good for them
                 * OR (the actual reason for the code below)
                 * this machine and the other one were simultaneously 
                 * requesting connections to one another, but
                 * the reservations filled up until the MAX no of conns was reached.
                 * And they are both replying to each other a NACK.
                 * 
                 * solution: inspired from the root contension situation
                 * in the FireWire election algorithm, we randomly choose 
                 * to wait or not to wait a small period of time before
                 * retrying the connections.
                 */
                if(result.size() + alreadyAliveConnsNo == 0) {
                    /*
                     * Would it be better to have a bigger chance to 
                     * immediatly send again than to sleep?
                     */
                    if (r.nextDouble() > 0.5) {
                        Loggers.cacheLog.log(Level.INFO, "\n\tCould not connect"
                                + " to anyone. Sleeping now for {0}"
                                + " millis.", sleepMillis);
                        try {
                            long sleepDeadline = System.nanoTime() + 
                                    TimeUnit.MILLISECONDS.toNanos(sleepMillis);
                            
                            while (System.nanoTime() < sleepDeadline) {
                                /*
                                 * In this timeout, the other machine should
                                 * have enough time to get some free or cacheble
                                 * connections.
                                 */
                                super.sleepCondition.awaitNanos(
                                        sleepDeadline - System.nanoTime());
                            }
                            /*
                             * Exp backoff, because we might be in the deadlock
                             * situation and the connection is just slow.
                             * So I need to wait more.
                             */
                            sleepMillis = Math.min(sleepMillis * 2,
                                    /*
                                     * x2 because we need a send and an ack.
                                     */
                                    2 * MSG_MAX_ARRIVAL_TIME_MILLIS);
                        } catch (InterruptedException ignoreMe) {}
                    }
                    
                    /*
                     * Done waiting, go again.
                     */
                    continue whileLoop;
                }
                      
                return result;
            }
        } finally {
            /*
             * Move the successful connections from the reserved list to the
             * live list.
             */
            unReserve(port.identifier(), result);
        }
    }
    
    @Override
    public void reserveLiveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        
        fromSPLiveConns.remove(con);
        aliveReservedConns.add(con);
        
        logReport();
    }
    
    @Override
    public void unReserveLiveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        
        aliveReservedConns.remove(con);
        fromSPLiveConns.add(con);
        
        super.reservationsCondition.signalAll();
        super.allClosedCondition.signalAll();
        
        logReport();
    }

    @Override
    public void reserveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        if (fullConns()) {
            Connection temp = cacheOneConnectionFor(con);
            if (temp != null) {
                statistics.cache(temp);
            }
        }
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            fromSPCacheConns.remove(con);
            notAliveReservedConns.add(con);
        }
        logReport();
    }

    @Override
    public void reserveConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        if (fullConns()) {
            Connection temp = cacheOneConnectionFor(con);
            if (temp != null) {
                statistics.cache(temp);
            }
        }
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            notAliveReservedConns.add(con);
        }
        logReport();
    }

    @Override
    public void cancelReservation(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        if (notAliveReservedConns.contains(con)) {
            /*
             * If it was reserved, remove it.
             */
            notAliveReservedConns.remove(con);
            /*
             * Notify.
             */
            super.reservationsCondition.signalAll();
        } else {
            /*
             * If the cache manager is caching something to make way for the
             * reservation, then it's not in the reserved array, so mark it.
             */
            canceledReservations.add(con);
            /*
             * The reservation is waiting on this condition. ignore the name and
             * use it.
             */
            super.noLiveConnCondition.signalAll();
        }

        logReport();
    }

    @Override
    public void cancelReservation(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        if (notAliveReservedConns.contains(con)) {
            /*
             * If it was reserved, remove it.
             */
            notAliveReservedConns.remove(con);
            /*
             * Notify.
             */
            super.reservationsCondition.signalAll();
        } else {
            canceledReservations.add(con);
            /*
             * The reservation is waiting on this condition. ignore the name and
             * use it.
             */
            super.noLiveConnCondition.signalAll();
        }

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

        notAliveReservedConns.removeAll(toBeMoved);
        /*
         * Notify anyone who was waiting on the reservations to clear.
         */
        super.reservationsCondition.signalAll();

        fromSPLiveConns.addAll(toBeMoved);
        /*
         * Maybe someone was waiting to cache one connection and needs live
         * connections for that to happen.
         */
        super.noLiveConnCondition.signalAll();

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
        Loggers.cacheLog.log(Level.INFO, "\n\t{0} alive connections:\t{1}, {2}"
                + "\n\t{3} cached connections:\t{4}, {5}"
                + "\n\t{6} alive reserved connections:\t{7}"
                + "\n\t{8} not alive reserved connections:\t{9}",
                new Object[]{fromSPLiveConns.size() + fromRPLiveConns.size(),
                    fromSPLiveConns, fromRPLiveConns,
                    fromSPCacheConns.size() + fromRPCacheConns.size(),
                    fromSPCacheConns, fromRPCacheConns,
                    aliveReservedConns.size(), aliveReservedConns,
                    notAliveReservedConns.size(), notAliveReservedConns});
    }

    @Override
    public boolean fullConns() {
        Set<Connection> all = new HashSet<Connection>();
        all.addAll(fromSPLiveConns);
        all.addAll(fromRPLiveConns);
        all.addAll(aliveReservedConns);
        all.addAll(notAliveReservedConns);

        return all.size() >= MAX_CONNS;
    }
    
    @Override
    public boolean canCache() {
        return fromSPLiveConns.size() + fromRPLiveConns.size() > 0;
    }
}
