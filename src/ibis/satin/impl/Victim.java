/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

final class Victim {

    IbisIdentifier ident;

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
            synchronized(this) {
                if (connected) {
                    connected = false;
                }
                s.disconnect(r);
            }
        }
    }

    private SendPort getSendPort() {
        if (! connected) {
            synchronized(this) {
                if (! connected) {
                    if (closed) {
                        return null;
                    }
                    if (Config.FAULT_TOLERANCE) {
                        if (! Communication.connect(s, r, SatinBase.connectTimeout)) {
                            if (Config.commLogger.isDebugEnabled()) {
                                Config.commLogger.debug("SATIN '" + s.identifier().ibis()

                                        + "': unable to connect to " + r.ibis()
                                        + ", might have crashed");
                            }
                            return null;
                        }
						connected = true;
                    } else {
                        Communication.connect(s, r);
                        connected = true;
                    }
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

    public synchronized void close() throws IOException {
        connected = false;
        closed = true;
        s.close();
    }
}
