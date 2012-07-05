package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

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
        return "[SPI, RPI] = " + 
                "[" + this.spi.toString() + "], " +
                "[" + this.rpi.toString() + "]";
    }
    
}
