package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelMessageHandler;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.*;

/**
 * Abstract class for different types of cache managers.
 */
public abstract class CacheManager {

    /*
     * TODO: move it from here, it's ugly This is the maximum size of the buffer
     * used to stream data.
     */
    public static final int BUFFER_CAPACITY = 1 << 16;
    public static final int MAX_CONNS;
    public static final int MAX_CONNS_DEFAULT = 1;
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
            //            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_MANY_TO_ONE);
    /**
     * These side channel sendport and receiveport names need to be unique. So
     * that the user won't create some other ports with these names.
     */
    public static final String sideChnSPName = "sideChannelSendport_uniqueStrainOfThePox";
    public static final String sideChnRPName = "sideChannelRecvport_uniqueStrainOfThePox";
    /**
     * The send and receive ports for the side channel should be static, i.e. 1
     * SP and 1 RP per CacheManager = Ibis instance. but in order to instantiate
     * them I need the ibis reference from the constructor.
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

        MAX_CONNS = Integer.parseInt(
                System.getProperty("MAX_CONNS", Integer.toString(MAX_CONNS_DEFAULT)));

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

            log.log(Level.INFO, "Cache manager instantiated on {0}", ibis.identifier().name());
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
    
    abstract public void removeAllConnections(SendPortIdentifier spi) ;
    
    abstract public void cacheConnection(SendPortIdentifier spi, 
            ReceivePortIdentifier rpi);

    abstract public void removeConnection(SendPortIdentifier spi, 
            ReceivePortIdentifier rpi);
    
    abstract public void cacheConnection(ReceivePortIdentifier rpi, 
            SendPortIdentifier spi);
    
    abstract public void removeConnection(ReceivePortIdentifier rpi, 
            SendPortIdentifier spi);
    
    abstract public void addConnection(ReceivePortIdentifier rpi,
            SendPortIdentifier spi);
    
    abstract public void restoreConnection(ReceivePortIdentifier rpi, 
            SendPortIdentifier spi);

    abstract public List<ReceivePortIdentifier> cachedRpisFrom(
            SendPortIdentifier identifier);
    
    abstract public boolean hasConnections();

    abstract public boolean isConnAlive(SendPortIdentifier identifier, 
            ReceivePortIdentifier rpi);
    
    abstract public boolean isConnCached(ReceivePortIdentifier identifier, 
            SendPortIdentifier spi);

    abstract public ReceivePortIdentifier[] allRpisFrom(
            SendPortIdentifier identifier);
    
    abstract public SendPortIdentifier[] allSpisFrom(
            ReceivePortIdentifier identifier);

    /**
     * Connect the send port identifier to some receive ports from the list
     * passed as a param. Returns the list of receive ports identifiers with
     * whom a connection has been established.
     *
     * This method guarantees at least one successful connection.
     *
     * If the send port is already connected to some of the receive ports, those
     * connections are guaranteed not to be closed, and sent as part of the
     * result.
     *
     * If timeout is zero, it will block until at least one connection has been
     * established.
     *
     * If timeout > 0, and no connection has been established until the
     * deadline, it will throw a ConnectionTimedOutException.
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
    abstract public Set<ReceivePortIdentifier> getSomeConnections(
            CacheSendPort port, Set<ReceivePortIdentifier> rpis,
            long timeoutMillis, boolean fillTimeout) throws
            ConnectionTimedOutException, ConnectionsFailedException;
    
    abstract protected void usingConnections(Set<ReceivePortIdentifier> allAliveConn);
}
