/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Communication;

import java.io.IOException;

/**
 * 
 * @author rob
 *
 * A Victim represents an Ibis we can steal work from.
 * This class is immutable, only the sendport itself could be connected and 
 * disconnected.
 *  
 */
public final class Victim implements Config {

    // @@@ TODO should be synchronized on static object, not sendport! 
    private static int connectionCount = 0;

    private IbisIdentifier ident;

    private SendPort sendPort;

    private ReceivePortIdentifier r;

    private boolean connected = false;

    private boolean closed = false;

    private int referenceCount = 0;
    
    public Victim(IbisIdentifier ident, SendPort s) {
        this.ident = ident;
        this.sendPort = s;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Victim) {
            Victim other = (Victim) o;
            return other.ident.equals(ident);
        }
        return false;
    }

    public boolean equals(Victim other) {
        if (other == this) {
            return true;
        }
        return other.ident.equals(ident);
    }

    public int hashCode() {
        return ident.hashCode();
    }

    private void disconnect() throws IOException {
        if (connected) {
            connected = false;
            connectionCount--;
            sendPort.disconnect(r);
        }
    }

    public void connect() {
        synchronized (sendPort) {
            getSendPort();
        }
    }
    
    private SendPort getSendPort() {
        if (closed) {
            return null;
        }

        if (!connected) {
            r = Communication.connect(sendPort, ident, "satin port",
                Satin.CONNECT_TIMEOUT);
            if (r == null) {
                Config.commLogger.debug("SATIN '" + sendPort.identifier().ibisIdentifier()
                    + "': unable to connect to " + ident
                    + ", might have crashed");
                return null;
            }
            connected = true;
            connectionCount++;
        }
        
        return sendPort;
    }

    public WriteMessage newMessage() throws IOException {
        SendPort send;
        synchronized (sendPort) {
            send = getSendPort();
            if (send != null) {
                referenceCount++;
            } else {
                throw new IOException("Could not connect to " + ident);
            }
        }
        return send.newMessage();
    }

    public long finish(WriteMessage m) throws IOException {
        try {
            return m.finish();
        }  finally {
            synchronized (sendPort) {
                referenceCount--;

                if (CLOSE_CONNECTIONS) {
                    if (connectionCount >= MAX_CONNECTIONS && referenceCount == 0) {
                        disconnect();
                    }
                }
            }
        }
    }

    public long finishKeepConnection(WriteMessage m) throws IOException {
        try {
            return m.finish();
        } finally {
            synchronized (sendPort) {
                referenceCount--;
            }
        }
    }

    public void loseConnection() throws IOException {
        if (CLOSE_CONNECTIONS) {
            synchronized(sendPort) {
                if (connectionCount >= MAX_CONNECTIONS && referenceCount == 0) {
                    disconnect();
                }
            }
        }
    }

    public void close() {
        synchronized (sendPort) {
            if (connected) {
                connected = false;
                connectionCount--;
            }
            closed = true;
            try {
                sendPort.close();
            } catch (Exception e) {
                // ignore
                Config.commLogger.warn("SATIN '" + sendPort.identifier().ibisIdentifier()
                    + "': port.close() throws exception (ignored)", e);
            }
        }
    }

    public IbisIdentifier getIdent() {
        return ident;
    }

    public boolean inDifferentCluster(IbisIdentifier other) {
        return !clusterOf(ident).equals(clusterOf(other));
    }
    
    public static String clusterOf(IbisIdentifier id) {
        // Not correct: considers all nodes to be in different clusters
        // if there is only one level. --Ceriel
        // int count = id.location().numberOfLevels();
        // return id.location().getLevel(count-1);
        return id.location().getParent().toString();
    }
}
