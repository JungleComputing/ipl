package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.util.logging.*;

/**
 * This abstract class keeps the counter.
 * How and what is cached depends on its implementations.
 */
abstract class CacheManager {
    public static final int MAX_CONNECTIONS;
    public static int noAliveConnections = 0;
    
    public static final Logger log;
    public static final String cacheLogString;
    
    static {
        cacheLogString = "cacheIbis.log";
        log = Logger.getLogger("cacheLog");
        log.removeHandler(new ConsoleHandler());
        
        try {
            FileHandler fh = new FileHandler(cacheLogString);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
        } catch (Exception ex) {
            Logger.getLogger(CacheIbis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Port type used for the creation of hub-based ports for the side-channel
     * communication.
     */
    public static final PortType ultraLightPT = new PortType(
            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_MANY_TO_ONE);
    /**
     * These side channel sendport and receiveport names need to be unique.
     * So that the user won't create some other ports with these names.
     */
    public static final String sideChnSPName = "sidechannelsendport" + System.currentTimeMillis();
    public static final String sideChnRPName = "sidechannelrecvport" + System.currentTimeMillis();
    ReceivePort sideChannelReceivePort;
    SendPort sideChannelSendPort;
    
     static {
        // initialize from somewhere or set default.
        MAX_CONNECTIONS = 10;
    }
    
    CacheManager(CacheIbis ibis) {
        try {
            sideChannelReceivePort = ibis.baseIbis.createReceivePort(
                    ultraLightPT, sideChnRPName, 
                    new SideChannelMessageUpcall(this));
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
    
    public void end(){
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
    void revive(SendPortIdentifier spi) {
        int connToBeFreed = 0;
        CacheSendPort sp = CacheSendPort.map.get(spi);
        int cachedConn = sp.getNoCachedConnections();
        
        log.log(Level.INFO, "Reviving this sendport's connections: {0}", spi);

        if (noAliveConnections + cachedConn > MAX_CONNECTIONS) {
            connToBeFreed = noAliveConnections + cachedConn - MAX_CONNECTIONS;
            log.log(Level.INFO, "Caching first {0} other connections.", connToBeFreed);
            int n = cacheAtLeastNConnExcept(connToBeFreed, spi);            
            noAliveConnections -= n;
            log.log(Level.INFO, "Cached. {0} alive connections.", noAliveConnections);
        }

        try {
            sp.revive();
            noAliveConnections += cachedConn;

            log.log(Level.INFO, "Revival was ok; no. conn. revived: {0}", connToBeFreed);
            log.log(Level.INFO, "Current alive connections: {0}", noAliveConnections);
        } catch (IOException ex) {
            // should not be here.
            log.log(Level.SEVERE, "Revival failed. Cause: {0}", ex);
            throw new RuntimeException("CacheManager level: "
                + "Couldn't reconnect a sendport to its receiveports.\n", ex);
        }        
    }

    /**
     * Cache any connection, except the ones containing this SPI.
     * Returns the number of cached connections.
     */
    protected abstract int cacheAtLeastNConnExcept(int n, SendPortIdentifier spi);
    
    /**
     * Cache any connection, except the ones containing this SPI.
     * Returns the number of cached connections.
     */
    protected abstract int cacheAtLeastNConnExcept(int n, ReceivePortIdentifier rpi);

    /**
     * The send port is on this machine.
     * This method will cache the connection from this send port
     * to the receive port associated with the param rpi.
     * @param spi
     * @param rpi 
     */
    void cache(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        cacheConnection(spi, rpi);
        noAliveConnections--;
    }
    
    protected abstract void cacheConnection(SendPortIdentifier sp, 
            ReceivePortIdentifier rp);
    
    /*
     * This method will make space if needed for the future connections.
     * It will try and cache (depending on implementation) connections,
     * except the ones containing the param sendport.
     * (because the future connections will be open from it, so it makes no 
     * sense to cache some of its connections whilst opening the rest)
     * 
     * this method is always called from: synchronized(cacheManager).
     */
    void makeWay(SendPortIdentifier spi, int noConn) {
        if(noAliveConnections + noConn > MAX_CONNECTIONS) {
            int connToBeFreed = noAliveConnections + noConn - MAX_CONNECTIONS;
            log.log(Level.INFO, "Caching {0} connections.", connToBeFreed);
            connToBeFreed = cacheAtLeastNConnExcept(connToBeFreed, spi);
            // subtract the cached connections
            noAliveConnections -= connToBeFreed;
            log.log(Level.INFO, "Cached. Now {0} alive connections.", noAliveConnections);
        }
    }

    /*
     * Called from synchronized(CacheManager) context as well.
     */
    void addConnections(SendPortIdentifier spi, 
            ReceivePortIdentifier[] rpis) {
        // let the implementation decide what to do with the alive connections.
        addConnectionsImpl(spi, rpis);
        
        // count: add the connections for which we made room
        noAliveConnections += rpis.length;
    }
    
    protected abstract void addConnectionsImpl(SendPortIdentifier spi, 
            ReceivePortIdentifier[] rpis);
    

    void removeAllConnections(SendPortIdentifier spi) {
        // let the implementation decide what to do with these removed connections        
        removeAllConnectionsImpl(spi);
        
        // keep on counting
        noAliveConnections -= CacheSendPort.map.get(spi).getNoTrueConnections();
    }
    
    protected abstract void removeAllConnectionsImpl(SendPortIdentifier spi);
    
    void removeConnection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        removeConnectionImpl(spi);
        // keep on counting
        noAliveConnections--;
    }
    
    protected abstract void removeConnectionImpl(SendPortIdentifier spi);

    /*
     * We are on the receive port side of the connection.
     * This method is called from the connection upcall.
     */
    void addConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        // let the implementation decide what to do with the alive connections.
        addConnectionImpl(rpi, spi);
        
        // count
        noAliveConnections++;
    }
    
    protected abstract void addConnectionImpl(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);

    void removeConnection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        // let the implementation decide what to do with the alive connections.
        removeConnectionImpl(rpi, spi);
        
        // count
        noAliveConnections--;
    }
    
    protected abstract void removeConnectionImpl(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);
}
