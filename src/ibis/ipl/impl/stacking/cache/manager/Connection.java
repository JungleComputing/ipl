package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.util.logging.Level;

/**
 * A wrapper class for a pair of sendport and receive port identifiers.
 */
public class Connection {

    final SendPortIdentifier spi;
    final ReceivePortIdentifier rpi;
    final boolean atSendPortSide;

    Connection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        this(spi, rpi, true);
    }

    Connection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        this(spi, rpi, false);
    }

    private Connection(SendPortIdentifier spi, ReceivePortIdentifier rpi, boolean b) {
        this.spi = spi;
        this.rpi = rpi;
        this.atSendPortSide = b;
    }

    public void cache(boolean heKnows) {
        try {
            Loggers.cacheLog.log(Level.INFO, "Caching connection\t{0}.", this.toString());
            if (atSendPortSide) {
                CacheSendPort port = CacheSendPort.map.get(spi);
                port.cache(rpi, heKnows);
            } else {
                CacheReceivePort port = CacheReceivePort.map.get(rpi);
                /*
                 * This takes time.
                 */
                port.cache(spi);
                synchronized (port.cachingInitiatedByMeSet) {
                    while (port.cachingInitiatedByMeSet.contains(spi)) {
                        try {
                            port.cachingInitiatedByMeSet.wait();
                        } catch (InterruptedException ignoreMe) {
                        }
                    }
                }
                Loggers.lockLog.log(Level.INFO, "Base receive port connected to"
                        + " {0} send ports.", port.recvPort.connectedTo().length);                
            }
        } catch (IOException ex) {
            Loggers.cacheLog.log(Level.SEVERE, "Caching failed:\t{0}", ex);
        }
    }
    
    static void closeSendPort(SendPortIdentifier spi) {
        CacheSendPort sendPort = CacheSendPort.map.get(spi);
        /*
         * Send a DISCONNECT message to the receive ports with whom we have
         * cached connections. Otherwise, they won't get the lostConnection()
         * upcall.
         */
        for (ReceivePortIdentifier rpi :
                sendPort.cacheManager.cachedRpisFrom(spi)) {
            sendPort.cacheManager.sideChannelHandler.sendProtocol(spi,
                    rpi, SideChannelProtocol.DISCONNECT);
        }
        
        try {
            /*
             * Disconnect from whoever is connected to the base send port.
             */
            Loggers.conLog.log(Level.INFO, "Closing base send port\t{0}", 
                    sendPort.baseSendPort.identifier());
            sendPort.baseSendPort.close();
        } catch (IOException ex) {
            Loggers.cacheLog.log(Level.SEVERE, "Could not close send port.", ex);
        }
    }

    void remove() {
        CacheSendPort sendPort = CacheSendPort.map.get(spi);
        if (sendPort.cacheManager.isConnAlive(spi, rpi)) {
            try {
                sendPort.disconnect(rpi);
            } catch (IOException ex) {
                Loggers.cacheLog.log(Level.SEVERE, "Could not disconnect send port.", ex);
            }
        } else {
            /*
             * Send a DISCONNECT message to the receive ports with whom we have
             * cached connections. Otherwise, they won't get the
             * lostConnection() upcall.
             */
            sendPort.cacheManager.sideChannelHandler.sendProtocol(spi,
                    rpi, SideChannelProtocol.DISCONNECT);
        }        
    }

    public boolean contains(Object spiOrRpi) {
        return spi.equals(spiOrRpi) || rpi.equals(spiOrRpi);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Connection)) {
            return false;
        }
        Connection other = (Connection) o;
        return spi.equals(other.spi) && rpi.equals(other.rpi)
                && (atSendPortSide == other.atSendPortSide);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.spi != null ? this.spi.hashCode() : 0);
        hash = 67 * hash + (this.rpi != null ? this.rpi.hashCode() : 0);
        hash = 67 * hash + (this.atSendPortSide ? 1 : 0);
        return hash;
    }

    @Override
    public String toString() {
        if (atSendPortSide) {
            return "(" + spi.name() + "-" + spi.ibisIdentifier().name() + ", "
                    + rpi.name() + "-" + rpi.ibisIdentifier().name() + ")";
        } else {
            return "(" + rpi.name() + "-" + rpi.ibisIdentifier().name() + ", "
                    + spi.name() + "-" + spi.ibisIdentifier().name() + ")";
        }
    }
}
