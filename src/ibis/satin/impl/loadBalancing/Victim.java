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

    private static int connectionCount = 0;

    private IbisIdentifier ident;

    private SendPort s;

    private ReceivePortIdentifier r;

    private boolean connected = false;

    private boolean closed = false;

    private int referenceCount = 0;
    
    public Victim(IbisIdentifier ident, SendPort s) {
        this.ident = ident;
        this.s = s;
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
            s.disconnect(r);
        }
    }

    private SendPort getSendPort() {
        if (closed) {
            return null;
        }

        if (!connected) {
            r = Communication.connect(s, ident, "satin port",
                Satin.CONNECT_TIMEOUT);
            if (r == null) {
                Config.commLogger.debug("SATIN '" + s.identifier().ibis()
                    + "': unable to connect to " + ident
                    + ", might have crashed");
                return null;
            }
            connected = true;
            connectionCount++;
        }
        
        return s;
    }

    public WriteMessage newMessage() throws IOException {
        synchronized (s) {
            SendPort send = getSendPort();
            if (send != null) {
                referenceCount++;
                return send.newMessage();
            }
            throw new IOException("Could not connect");
        }
    }

    public long finish(WriteMessage m) throws IOException {
        synchronized (s) {
            long res = 0;
            IOException e = null;
            referenceCount--;
            
            try {
                res = m.finish();
            } catch (IOException x) {
                e = x;
            }

            if (CLOSE_CONNECTIONS) {
                if (connectionCount >= MAX_CONNECTIONS && referenceCount == 0) {
                    disconnect();
                }
            }

            if(e != null) {
                throw e;
            }
            
            return res;
        }
    }

    public long finishKeepConnection(WriteMessage m) throws IOException {
        synchronized (s) {
            referenceCount--;
            return m.finish();
        }
    }

    public void loseConnection() throws IOException {
        if (CLOSE_CONNECTIONS) {
            synchronized(s) {
                if (connectionCount >= MAX_CONNECTIONS && referenceCount == 0) {
                    disconnect();
                }
            }
        }
    }

    public void close() {
        synchronized (s) {
            if (connected) {
                connected = false;
                connectionCount--;
            }
            closed = true;
            try {
                s.close();
            } catch (Exception e) {
                // ignore
                Config.commLogger.warn("SATIN '" + s.identifier().ibis()
                    + "': port.close() throws exception", e);
            }
        }
    }

    public IbisIdentifier getIdent() {
        return ident;
    }
}
