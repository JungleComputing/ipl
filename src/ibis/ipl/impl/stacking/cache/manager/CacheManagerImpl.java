package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.impl.stacking.cache.util.CacheStatistics;
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

    protected final List<Connection> aliveConns;
//    protected final List<Connection> fromRPLiveConns;
    protected final List<Connection> cachedConns;
//    protected final List<Connection> fromRPCacheConns;
    protected final List<Connection> aliveReservedConns;
    protected final List<Connection> notAliveReservedConns;
    protected final List<Connection> canceledReservations;
    private Random r;

    protected CacheManagerImpl(CacheIbis ibis) {
        super(ibis);
        aliveConns = new LinkedList<Connection>();
        cachedConns = new LinkedList<Connection>();
//        fromRPLiveConns = new LinkedList<Connection>();
//        fromRPCacheConns = new LinkedList<Connection>();
        aliveReservedConns = new LinkedList<Connection>();
        notAliveReservedConns = new LinkedList<Connection>();
        canceledReservations = new LinkedList<Connection>();
        r = new Random();
    }

    /*
     * Blocking method until one connection is cached to make room
     * for the passed connection
     * OR
     * the connection for which we are making room has been canceled,
     * i.e. we received a CANCEL_RESERVATION message.
     */
    abstract protected Connection cacheOneConnectionFor(Connection con);

    @Override
    public void cacheConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi, boolean heKnows) {
        Connection con = new Connection(spi, rpi);

        while (aliveReservedConns.contains(con)) {
            try {
                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                        + " waiting on some reservations to be"
                        + " removed.");
                super.reservationsCondition.await();
                Loggers.lockLog.log(Level.INFO, "Lock will reaquired.");
            } catch (InterruptedException ignoreMe) {
            }
        }
        
        /*
         * Situation: the two sides (SP and RP) want to cache the same
         * connection. Then this method will be called x2.
         */
        if (!cachedConns.contains(con)) {
            con.cache(heKnows);
            cachedConns.add(con);
        }
        aliveConns.remove(con);
        
        super.gotSpaceCondition.signalAll();

        logReport();
    }

    @Override
    public void removeConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        while (aliveReservedConns.contains(con) ||
                notAliveReservedConns.contains(con)) {
            try {
                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                        + " waiting on some reservations to be"
                        + " removed.");
                super.reservationsCondition.await();
                Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
            } catch (InterruptedException ignoreMe) {
            }
        }

        con.remove();

        cachedConns.remove(con);
        aliveConns.remove(con);
        
        super.gotSpaceCondition.signalAll();

        logReport();
    }

    @Override
    public void lostConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        cachedConns.remove(con);
        aliveConns.remove(con);
        aliveReservedConns.remove(con);
        notAliveReservedConns.remove(con);
        
        super.reservationsCondition.signalAll();
        
        super.gotSpaceCondition.signalAll();

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

        for (Iterator it = cachedConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }
        for (Iterator it = aliveConns.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
            }
        }
        
        super.gotSpaceCondition.signalAll();

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
        cachedConns.remove(con);
        super.reservationsCondition.signalAll();

        aliveConns.add(con);
        super.gotSpaceCondition.signalAll();

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
        if (!cachedConns.contains(con)) {
            cachedConns.add(con);
        }
        aliveConns.remove(con);
        
        super.gotSpaceCondition.signalAll();

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

        cachedConns.remove(con);
        aliveConns.remove(con);

        super.allClosedCondition.signal();
        super.gotSpaceCondition.signalAll();

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
        cachedConns.remove(con);
        super.reservationsCondition.signalAll();

        aliveConns.add(con);
        super.gotSpaceCondition.signalAll();

        logReport();
    }

    @Override
    public boolean isConnAlive(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return aliveConns.contains(con);
    }
    
    @Override
    public boolean isConnAlive(ReceivePortIdentifier rpi, 
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        return aliveConns.contains(con);
    }

    @Override
    public boolean isConnCached(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        return cachedConns.contains(con);
    }
    
    @Override
    public boolean isConnCached(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        return cachedConns.contains(con);
    }

    @Override
    public boolean hasConnections(ReceivePortIdentifier rpi) {
        for (Connection con : aliveConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                return true;
            }
        }

        for (Connection con : cachedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                return true;
            }
        }

        for (Connection con : aliveReservedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                return true;
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean containsReservedAlive(SendPortIdentifier spi) {
        for(Connection con : aliveReservedConns) {
            if(con.atSendPortSide && con.contains(spi)) {
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
        
        for (Connection con : cachedConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        return result;
    }

    @Override
    public ReceivePortIdentifier[] allRpisFrom(SendPortIdentifier spi) {
        List<ReceivePortIdentifier> result = new LinkedList<ReceivePortIdentifier>();

        for (Connection con : aliveConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        for (Connection con : cachedConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        
        for (Connection con : aliveReservedConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (con.atSendPortSide && con.contains(spi)) {
                result.add(con.rpi);
            }
        }

        return result.toArray(new ReceivePortIdentifier[result.size()]);
    }

    @Override
    public SendPortIdentifier[] allSpisFrom(ReceivePortIdentifier rpi) {
        List<SendPortIdentifier> result = new LinkedList<SendPortIdentifier>();

        for (Connection con : aliveConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        for (Connection con : cachedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                result.add(con.spi);
            }
        }
        
        for (Connection con : aliveReservedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                result.add(con.spi);
            }
        }
        
        for (Connection con : notAliveReservedConns) {
            if (!con.atSendPortSide && con.contains(rpi)) {
                result.add(con.spi);
            }
        }

        return result.toArray(new SendPortIdentifier[result.size()]);
    }
    
    private Set<ReceivePortIdentifier> aliveRpisFrom(
            SendPortIdentifier spi) {
        Set<ReceivePortIdentifier> result =
                new HashSet<ReceivePortIdentifier>();
        
        for (Connection con : aliveConns) {
            if (con.atSendPortSide && con.contains(spi)) {
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

        /*
         * re-add the intial alive connections to the result.
         */
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
                     * Reserve the connection on the receive port side as well.
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
                                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                                        + " waiting on a reservation ack.");
                                super.reserveAcksCond.await(timeout, TimeUnit.MILLISECONDS);
                                Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
                            } else {
                                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                                        + " waiting on a reservation ack.");
                                super.reserveAcksCond.await();
                                Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
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
                            CacheStatistics.connect(port.identifier(), rpi);
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
                            CacheStatistics.connect(port.identifier(), rpi);
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
        
        aliveConns.remove(con);
        aliveReservedConns.add(con);
        
        logReport();
    }
    
    @Override
    public void unReserveLiveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        
        aliveReservedConns.remove(con);
        aliveConns.add(con);
        
        super.reservationsCondition.signalAll();
        super.gotSpaceCondition.signalAll();
    }

    @Override
    public void reserveLiveConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        aliveConns.remove(con);
        aliveReservedConns.add(con);

        logReport();
    }
    
    @Override
    public void unReserveLiveConnection(ReceivePortIdentifier rpi, 
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        aliveReservedConns.remove(con);
        aliveConns.add(con);
        
        super.reservationsCondition.signalAll();
        super.gotSpaceCondition.signalAll();
    }

    @Override
    public void unReserveLiveToCacheConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        aliveReservedConns.remove(con);
        cachedConns.add(con);

        super.reservationsCondition.signalAll();
        super.gotSpaceCondition.signalAll();
        
        logReport();
    }

    @Override
    public void reserveConnection(SendPortIdentifier spi,
            ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);

        if (fullConns()) {
            cacheOneConnectionFor(con);
        }
        
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            cachedConns.remove(con);
            notAliveReservedConns.add(con);
        }
        
        super.gotSpaceCondition.signalAll();
        
        logReport();
    }

    @Override
    public void reserveConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);

        if (fullConns()) {
            cacheOneConnectionFor(con);
        }
        
        if (canceledReservations.contains(con)) {
            canceledReservations.remove(con);
        } else {
            /*
             * I need to let this conn in the cached list.
             * Otherwise, isConnCached() will not 
             * behave correctly.
             */
//            fromRPCacheConns.remove(con);
            notAliveReservedConns.add(con);
        }
        
        super.gotSpaceCondition.signalAll();
        
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
        } else {
            /*
             * If the cache manager is caching something to make way for the
             * reservation, then it's not in the reserved array, so mark it.
             */
            canceledReservations.add(con);
        }
        /*
         * Notify.
         */
        super.reservationsCondition.signalAll();
        /*
         * The reservation is waiting on this condition. ignore the name and use
         * it.
         */
        super.gotSpaceCondition.signalAll();

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
        } else {
            canceledReservations.add(con);
        }
        /*
         * Notify.
         */
        super.reservationsCondition.signalAll();
        /*
         * The reservation is waiting on this condition. ignore the name and use
         * it.
         */
        super.gotSpaceCondition.signalAll();

        logReport();
    }

    private void unReserve(SendPortIdentifier spi,
            Set<ReceivePortIdentifier> rpiSet) {
        Set<Connection> toBeMoved = new HashSet<Connection>();
        for (ReceivePortIdentifier rpi : rpiSet) {
            Connection temp = new Connection(spi, rpi);
            toBeMoved.add(temp);
        }

        notAliveReservedConns.removeAll(toBeMoved);
        cachedConns.removeAll(toBeMoved);
        /*
         * Notify anyone who was waiting on the reservations to clear.
         */
        super.reservationsCondition.signalAll();

        aliveConns.addAll(toBeMoved);
        /*
         * Maybe someone was waiting to cache one connection and needs live
         * connections for that to happen.
         */
        super.gotSpaceCondition.signalAll();

        cachedConns.removeAll(toBeMoved);

        logReport();
    }

    private void markAsUsed(SendPortIdentifier spi,
            Set<ReceivePortIdentifier> allAliveConn) {

        Set<Connection> aliveConn = new HashSet<Connection>();
        for (ReceivePortIdentifier rpi : allAliveConn) {
            aliveConn.add(new Connection(spi, rpi));
        }

        aliveConns.removeAll(aliveConn);
        aliveConns.addAll(aliveConn);
    }

    private void logReport() {
        Loggers.cacheLog.log(Level.INFO, "\n\t{0} alive connections:\t{1}"
                + "\n\t{2} cached connections:\t{3}"
                + "\n\t{4} alive reserved connections:\t{5}"
                + "\n\t{6} not alive reserved connections:\t{7}",
                new Object[]{aliveConns.size(), aliveConns, 
                    cachedConns.size(), cachedConns,
                    aliveReservedConns.size(), aliveReservedConns,
                    notAliveReservedConns.size(), notAliveReservedConns});
    }

    @Override
    public boolean fullConns() {
        Set<Connection> all = new HashSet<Connection>();
        all.addAll(aliveConns);
        all.addAll(aliveReservedConns);
        all.addAll(notAliveReservedConns);

        return all.size() >= MAX_CONNS;
    }
    
    @Override
    public boolean canCache() {
        return !aliveConns.isEmpty();
    }
}
