package ibis.ipl.impl.stacking.cc.manager;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import ibis.ipl.impl.stacking.cc.CCSendPort;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cc.util.CCStatistics;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class for a pair of sendport and receive port identifiers.
 */
public class Connection {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(Connection.class);

    final SendPortIdentifier spi;
    final ReceivePortIdentifier rpi;
    final boolean atSendPortSide;

    public Connection(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        this(spi, rpi, true);
    }

    public Connection(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        this(spi, rpi, false);
    }

    private Connection(SendPortIdentifier spi, ReceivePortIdentifier rpi, boolean b) {
        this.spi = spi;
        this.rpi = rpi;
        this.atSendPortSide = b;
    }

    public void cache(boolean heKnows) {
        try {
            logger.debug("Caching connection\t{}.", this.toString());
            if (atSendPortSide) {
                CCSendPort port = CCSendPort.map.get(spi);
                port.cache(rpi, heKnows);
            } else {
                CCReceivePort port = CCReceivePort.map.get(rpi);

                if (port.ccManager.isConnAlive(rpi, spi)) {
                    /*
                     * This takes time.
                     */
                    port.cache(spi);
                }
            }
        } catch (IOException ex) {
            logger.error("Caching failed:\t{}", ex);
        }
    }
    
    static void closeSendPort(SendPortIdentifier spi) {
        CCSendPort sendPort = CCSendPort.map.get(spi);
        /*
         * Send a DISCONNECT message to the receive ports with whom we have
         * cached connections. Otherwise, they won't get the lostConnection()
         * upcall.
         */
        for (ReceivePortIdentifier rpi :
                sendPort.ccManager.cachedRpisFrom(spi)) {
            sendPort.ccManager.sideChannelHandler.sendProtocol(spi,
                    rpi, SideChannelProtocol.DISCONNECT);
        }
        List<ReceivePortIdentifier> tempList =
                new LinkedList<ReceivePortIdentifier>();
        /*
         * Move live connections to safe place.
         */
        for (ReceivePortIdentifier rpi : sendPort.baseSendPort.connectedTo()) {
            sendPort.ccManager.reserveLiveConnection(spi, rpi);
            tempList.add(rpi);
        }
        /*
         * Release the lock.
         */        
        logger.debug("Releasing lock so that"
                + " sendport {} can close.", sendPort.identifier());
        sendPort.ccManager.lock.unlock();
        
        try {
            /*
             * Close now.
             */
            logger.debug("Base send port now closing...");
            for(ReceivePortIdentifier rpi : sendPort.baseSendPort.connectedTo()) {
                CCStatistics.remove(sendPort.identifier(), rpi);
            }
            sendPort.baseSendPort.close();
            logger.debug("Base send port now closed.");
        } catch (Exception ex) {
            logger.error("Failed to close send port "
                    + sendPort.identifier(), ex);
        } finally {
            /*
             * Reaquire the lock.
             */
            sendPort.ccManager.lock.lock();
            logger.debug("{} reaquired the lock.", sendPort.identifier());
            /*
             * Move back connections.
             */
            for(ReceivePortIdentifier rpi : tempList) {
                sendPort.ccManager.unReserveLiveConnection(spi, rpi);
            }
        }
    }

    void remove() {
        CCSendPort sendPort = CCSendPort.map.get(spi);
        if (sendPort.ccManager.isConnAlive(spi, rpi)) {
            
            sendPort.ccManager.reserveLiveConnection(spi, rpi);

            logger.debug("Releasing lock so {} can disconnect.", spi);
            sendPort.ccManager.lock.unlock();            

            try {
                logger.debug("Base send port now disconnecting...");
                CCStatistics.remove(sendPort.identifier(), rpi);
                sendPort.baseSendPort.disconnect(rpi.ibisIdentifier(), rpi.name());
                logger.debug("Base send port now connected"
                        + " to {} recv ports.", sendPort.baseSendPort.connectedTo().length);
            } catch (Exception ex) {
                logger.error("Base send port "
                        + spi + " failed to "
                        + "properly disconnect from "
                        + rpi + ".", ex);
            } finally {
                sendPort.ccManager.lock.lock();
                logger.debug("{} reaquired lock.", spi);
                sendPort.ccManager.unReserveLiveConnection(spi, rpi);
            }
        } else {
            /*
             * Send a DISCONNECT message to the receive ports with whom we have
             * cached connections. Otherwise, they won't get the
             * lostConnection() upcall.
             */
            sendPort.ccManager.sideChannelHandler.sendProtocol(spi,
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
