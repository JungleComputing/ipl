package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible with keeping the number of open ports beneath
 * the limit; it also decides what to cache.
 *
 * We'll see later how this will be implemented. not so hard, I believe.
 */
class CacheManager {
    public static final int MAX_CONNECTIONS;
    
   
    public AtomicInteger currOpenConn;

    /**
     * Port type used for the creation of hub-based ports for the side-channel
     * communication.
     */
    public static final PortType ultraLightPT = new PortType(
            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_DATA,
            PortType.CONNECTION_MANY_TO_ONE);
    public static final String sideChnSPName = "sidechannelsendport";
    public static final String sideChnRPName = "sidechannelrecvport";
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
            
            currOpenConn = new AtomicInteger(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void end(){
        try {
            sideChannelSendPort.close();
            // will this block?
            sideChannelReceivePort.close();
        } catch (IOException ex) {
            Logger.getLogger(CacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Don't need to worry about this connection anymore.
     * Remove it
     * @param sendPortIdentifier
     * @param receiver 
     */
    void removeConnection(SendPortIdentifier sendPortIdentifier, ReceivePortIdentifier receiver) {
       // throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Fully revive this send port's connections.
     *
     * @param sendPortIdentifier
     */
    void revive(SendPortIdentifier sendPortIdentifier) {
        throw new UnsupportedOperationException("Not yet implemented");
        /*
         * if(no of alive ports < no max ports) { obj.revive(); no alive ports
         * ++; } else { other = choose one alive port; other.cache();
         * obj.revive(); }
         */
    }

    /**
     * Cache anything to make space.
     */
    void cache() {
        throw new UnsupportedOperationException("Not yet implemented");
        /*
         * obj.cache(); no alive ports --;
         */
    }

    /**
     * if(no_conn at max) { 
     *      make space for a future connection 
     * }
     * keep the free space reserved until the ii connects with a sp.
     * 
     * @param ibisIdentifier 
     */
    void reserve(IbisIdentifier ibisIdentifier) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * The send port is on this machine.
     * This method will cache the connection from this send port
     * to the receive port associated with the param rpi.
     * @param spi
     * @param rpi 
     */
    void cache(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * The send port is on the remote machine and it's cached.
     * Here, all we have to do is count this cached connection
     * on this machine's receive port.
     * @param spi
     * @param rpi 
     */
    void alreadyCached(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
