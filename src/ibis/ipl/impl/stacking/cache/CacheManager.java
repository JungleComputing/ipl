package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.*;

/**
 * This abstract class keeps the counter. 
 * What is cached depends on its implementations.
 * True connection closing needs to be done separatly,
 * here we merely count the alive connections,
 * and decide which one to cache from the alive ones.
 */
public abstract class CacheManager {
    
    /* 
     * TODO: move it from here, it's ugly
     * This is the maximum size of the buffer used to stream data.
     */
    static final int BUFFER_CAPACITY = 1 << 16;

    public static final int MAX_CONNECTIONS;
    public static final int MAX_CONN_DEFAULT = 50;
    public static int noAliveConnections = 0;
    /**
     * Fields for logging.
     */
    public static final Logger log;
    public static final String cacheLogString;
    /**
     * Port type used for the creation of hub-based ports for the side-channel
     * communication.
     */
    public static final PortType ultraLightPT = new PortType(
            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_DATA,
            PortType.CONNECTION_MANY_TO_ONE);
    /**
     * These side channel sendport and receiveport names need to be unique. So
     * that the user won't create some other ports with these names.
     */
    public static final String sideChnSPName = "sideChannelSendport_uniqueStrainOfThePox";
    public static final String sideChnRPName = "sideChannelRecvport_uniqueStrainOfThePox";
    
    /**
     * The send and receive ports for the side channel should be static, i.e.
     * 1 SP and 1 RP per CacheManager = Ibis instance.
     * but in order to instantiate them I need the ibis reference from the
     * constructor.
     */
    public final ReceivePort sideChannelReceivePort;
    public final SendPort sideChannelSendPort;
    /**
     * This field handles the upcalls and provides a sendProtocol method.
     */
    public final SideChannelMessageHandler sideChannelHandler;

    static {
        cacheLogString = "cacheIbis.log";
        log = Logger.getAnonymousLogger();
        log.removeHandler(new ConsoleHandler());

        try {
            FileHandler fh = new FileHandler(cacheLogString);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
        } catch (Exception ex) {
            Logger.getLogger(CacheIbis.class.getName()).log(Level.SEVERE, null, ex);
        }

        MAX_CONNECTIONS = Integer.parseInt(
                System.getProperty("MAX_CONN", Integer.toString(MAX_CONN_DEFAULT)));
        
    }

    CacheManager(CacheIbis ibis) {
        try {
            sideChannelHandler = new SideChannelMessageHandler(this);
            sideChannelReceivePort = ibis.baseIbis.createReceivePort(
                    ultraLightPT, sideChnRPName,
                    sideChannelHandler);
            sideChannelReceivePort.enableConnections();
            sideChannelReceivePort.enableMessageUpcalls();

            sideChannelSendPort = ibis.baseIbis.createSendPort(
                    ultraLightPT, sideChnSPName);

            log.log(Level.INFO, "Cache manager instantiated.");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Failed to properly instantiate the Cache Manager.");
            throw new RuntimeException(ex);
        }
    }

    public void end() {
        try {
            sideChannelSendPort.close();
            // will this block?
            sideChannelReceivePort.close();
            log.log(Level.INFO, "Closed the cache manager.");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Failed to close the cache manager.");
            Logger.getLogger(CacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Fully revive this send port's connections.
     *
     * @param spi
     */
//    void reviveSendPort(SendPortIdentifier spi) {
//        int connToBeFreed = 0;
//        CacheSendPort sp = CacheSendPort.map.get(spi);
//        int cachedConn = sp.falselyConnected.size();
//
//        log.log(Level.INFO, "Reviving this sendport's connections: {0}", spi);
//
//        if (noAliveConnections + cachedConn > MAX_CONNECTIONS) {
//            connToBeFreed = noAliveConnections + cachedConn - MAX_CONNECTIONS;
//            log.log(Level.INFO, "Caching first {0} other connections.", connToBeFreed);
//            int n = cacheAtLeastNConnExcept(connToBeFreed, spi);
//            noAliveConnections -= n;
//            log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
//        }
//
//        try {
//            sp.revive();
//            noAliveConnections += cachedConn;
//
//            log.log(Level.INFO, "Revival was ok; no. conn. revived: {0}", connToBeFreed);
//            log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
//        } catch (IOException ex) {
//            // should not be here.
//            log.log(Level.SEVERE, "Revival failed. Cause: {0}", ex);
//            throw new RuntimeException("CacheManager level: "
//                    + "Couldn't reconnect a sendport to its receiveports.\n", ex);
//        }
//    }


    /**
     * Called from synchronized(CacheManager) context as well.
     */
    void addConnections(SendPortIdentifier spi,
            ReceivePortIdentifier[] rpis) {
        // let the implementation decide what to do with the alive connections.
        int addedConn = addConnectionsImpl(spi, rpis);

        // count: add the connections for which we made room
        noAliveConnections += addedConn;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    void restoreConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        // let the implementation decide what to do with the alive connections.
        int addedConn = restoreConnectionImpl(rpi, spi);

        // count
        noAliveConnections += addedConn;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    void removeAllConnections(SendPortIdentifier spi) {
        // let the implementation decide what to do with these removed connections        
        int removed = removeAllConnectionsImpl(spi);

        // keep on counting
        noAliveConnections -= removed;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    void removeConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        int removed = removeConnectionImpl(spi, rpi);
        // keep on counting
        noAliveConnections -= removed;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    /**
     * We are on the receive port side of the connection. This method is called
     * from the connection upcall.
     */
    void addConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        // let the implementation decide what to do with the alive connections.
        int addedConn = addConnectionImpl(rpi, spi);

        // count
        noAliveConnections += addedConn;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }
    
    void restoreConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        // let the implementation decide what to do with the alive connections.
        int addedConn = restoreConnectionImpl(rpi, spi);

        // count
        noAliveConnections += addedConn;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    void removeConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        // let the implementation decide what to do with the alive connections.
        int removed = removeConnectionImpl(rpi, spi);

        // count
        noAliveConnections -= removed;
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
    }

    /**
     * Cache any connection, except the ones containing this SPI. Returns the
     * number of cached connections.
     */
    protected abstract int cacheAtLeastNConnExcept(int n, Object spiOrRpi);
    
    /**
     * Connect the send port identifier to some receive ports from the list
     * passed as a param.
     * This method guarantees at least one successfull connection,
     * and it will try to connect to as many receive ports it can
     * in the given timeout.
     * 
     * If the sendport is already connected to some of the receive ports,
     * those connections are guaranteed not to be closed, but sent
     * as part of the result.
     * 
     * If timeout is zero, it will block until at least one connection
     * has been established and no exception will be thrown.
     * 
     * If timeout > 0, it will throw a ConnectionFailedException
     * or ConnectionsFailedException if rpis.length > 1.
     * 
     * for each rp, the connection (sp, rp) can be:
     * 1. already alive
     * 2. cached
     * 3. not even initiated
     * 
     * @param spi
     * @param rpis
     * @param timeoutMillis
     * @return 
     */
    // big TODO over here: timeout and stuff
    ReceivePortIdentifier[] getSomeConnections(CacheSendPort port, 
            Set<ReceivePortIdentifier> rpis, long timeoutMillis) throws
            IbisIOException {
        if (rpis == null || rpis.isEmpty()) {
            throw new ConnectionsFailedException("Array of send ports is: " + rpis);
        }
        
        /*
         * Get the alive connections from this send port.
         */
        Set<ReceivePortIdentifier> alreadyConnected =
                new HashSet<ReceivePortIdentifier>(
                Arrays.asList(port.sendPort.connectedTo()));

        rpis.removeAll(alreadyConnected);
        
        if(rpis.isEmpty()) {
            return alreadyConnected.toArray(
                new ReceivePortIdentifier[alreadyConnected.size()]);
        }
        
        /*
         * For now, I will assume I can connect to all the rpis in the list.
         */
        ReceivePortIdentifier[] toConnect = rpis.toArray(new ReceivePortIdentifier[rpis.size()]);
        port.sendPort.connect(toConnect);
        noAliveConnections += this.addConnectionsImpl(port.identifier(), toConnect);
        
        log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
        
        rpis.addAll(alreadyConnected);
        return rpis.toArray(
                new ReceivePortIdentifier[alreadyConnected.size()]);
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

    protected abstract int addConnectionsImpl(SendPortIdentifier spi,
            ReceivePortIdentifier[] rpis);
    
    protected abstract int restoreConnectionImpl(SendPortIdentifier spi,
            ReceivePortIdentifier rpi);

    protected abstract int removeConnectionImpl(SendPortIdentifier spi,
            ReceivePortIdentifier rpi);

    protected abstract int removeAllConnectionsImpl(SendPortIdentifier spi);

    protected abstract int addConnectionImpl(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);
    
    protected abstract int restoreConnectionImpl(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);

    protected abstract int removeConnectionImpl(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);
}
