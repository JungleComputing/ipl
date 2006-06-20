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

public final class Victim {

    private IbisIdentifier ident;

    private SendPort s;

    private ReceivePortIdentifier r;

    private boolean connected = false;

    private boolean closed = false;

    public Victim(IbisIdentifier ident, SendPort s, ReceivePortIdentifier r) {
        this.ident = ident;
        this.s = s;
        this.r = r;
        if (s != null && s.connectedTo().length != 0) {
            connected = true;
        }
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

    public void disconnect() throws IOException {
        if (connected) {
            synchronized (s) {
                if (connected) {
                    connected = false;
                }
                s.disconnect(r);
            }
        }
    }

    private SendPort getSendPort() {
        if (!connected) {
            synchronized (s) {
                if (!connected) {
                    if (closed) {
                        return null;
                    }
                    if (!Communication.connect(s, r, Satin.CONNECT_TIMEOUT)) {
                        Config.commLogger.debug("SATIN '"
                            + s.identifier().ibis()
                            + "': unable to connect to " + r.ibis()
                            + ", might have crashed");
                        return null;
                    }
                    connected = true;
                }
            }
        }
        return s;
    }

    public WriteMessage newMessage() throws IOException {
        SendPort s = getSendPort();
        if (s != null) {
            return getSendPort().newMessage();
        }
        throw new IOException("Could not connect");
    }

    public void close() {
        synchronized (s) {
            connected = false;
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
