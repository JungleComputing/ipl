package ibis.ipl.impl.stacking.cc.manager;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import ibis.ipl.impl.stacking.cc.CCSendPort;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cc.util.CCStatistics;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * A wrapper class for a pair of sendport and receive port identifiers.
 */
public class Connection {

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
            Loggers.ccLog.log(Level.INFO, "Caching connection\t{0}.", this.toString());
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
                Loggers.lockLog.log(Level.INFO, "Base receive port connected to"
                        + " {0} send ports.", port.recvPort.connectedTo().length);
            }
        } catch (IOException ex) {
            Loggers.ccLog.log(Level.SEVERE, "Caching failed:\t{0}", ex);
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
        sendPort.ccManager.lock.unlock();
        Loggers.lockLog.log(Level.INFO, "Lock released so that"
                + " sendport {0} can close.", sendPort.identifier());
        
        try {
            /*
             * Close now.
             */
            Loggers.ccLog.log(Level.INFO, "Base send port now closing...");
            for(ReceivePortIdentifier rpi : sendPort.baseSendPort.connectedTo()) {
                CCStatistics.remove(sendPort.identifier(), rpi);
            }
            sendPort.baseSendPort.close();
            Loggers.ccLog.log(Level.INFO, "Base send port now closed.");
        } catch (Exception ex) {
            Loggers.conLog.log(Level.SEVERE, "Failed to close send port "
                    + sendPort.identifier(), ex);
        } finally {
            /*
             * Reaquire the lock.
             */
            sendPort.ccManager.lock.lock();
            Loggers.lockLog.log(Level.INFO, "{0} reaquired the lock.", sendPort.identifier());
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

            sendPort.ccManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock released so {0} can disconnect.", spi);

            try {
                Loggers.ccLog.log(Level.INFO, "Base send port now disconnecting...");
                CCStatistics.remove(sendPort.identifier(), rpi);
                sendPort.baseSendPort.disconnect(rpi.ibisIdentifier(), rpi.name());
                Loggers.ccLog.log(Level.INFO, "Base send port now connected"
                        + " to {0} recv ports.", sendPort.baseSendPort.connectedTo().length);
            } catch (Exception ex) {
                Loggers.ccLog.log(Level.SEVERE, "Base send port "
                        + spi + " failed to "
                        + "properly disconnect from "
                        + rpi + ".", ex);
            } finally {
                sendPort.ccManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "{0} reaquired lock.", spi);
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