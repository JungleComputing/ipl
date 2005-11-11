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

    public SendPort getSendPort() {
        if (! connected) {
            synchronized(this) {
                if (! connected) {
                    Communication.connect(s, r);
                    connected = true;
                }
            }
        }
        return s;
    }

    public WriteMessage newMessage() throws IOException {
        return getSendPort().newMessage();
    }

    public void close() throws IOException {
        connected = false;
        s.close();
    }
}
