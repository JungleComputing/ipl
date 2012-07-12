package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * A wrapper class for a pair of sendport and receive port identifiers.
 */
public class Connection {
    
    final SendPortIdentifier spi;
    final ReceivePortIdentifier rpi;
    final boolean atSendPortSide;
    
    private static Map<ReceivePortIdentifier, Connection> spiMap;
    private static Map<SendPortIdentifier, Connection> rpiMap;
    
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
    
    public void cache() throws IOException {
        CacheManager.log.log(Level.INFO, "Caching connection\t{0}.", this.toString());
        if(atSendPortSide) {
            CacheSendPort port = CacheSendPort.map.get(spi);
            boolean doesHeKnow = false;
            port.cache(rpi, doesHeKnow);
        } else {
            CacheReceivePort port = CacheReceivePort.map.get(rpi);
            /*
             * This takes time.
             */
            port.cache(spi);
            synchronized(port.initiatedCachingByMe) {
                while(port.initiatedCachingByMe.contains(spi)) {
                    try {
                        port.initiatedCachingByMe.wait();
                    } catch (InterruptedException ignoreMe) {}
                }
            }
        }
    }
    
    
    public boolean contains(Object spiOrRpi) {
        return spi.equals(spiOrRpi) || rpi.equals(spiOrRpi);
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(!(o instanceof Connection)) {
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
        if(atSendPortSide) {
            return "(" + spi.name() +"-" + spi.ibisIdentifier().name() + ", " +
               rpi.name() +"-" + rpi.ibisIdentifier().name() + ")";
        } else {
            return "(" + rpi.name() +"-" + rpi.ibisIdentifier().name() + ", " +
               spi.name() +"-" + spi.ibisIdentifier().name() + ")";
        }
    }
}
