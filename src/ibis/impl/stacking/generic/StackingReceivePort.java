package ibis.impl.stacking.generic;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.util.Map;

public class StackingReceivePort implements ibis.ipl.ReceivePort {

    final ibis.ipl.ReceivePort base;
    final StackingPortType type;
    final ibis.impl.ReceivePortIdentifier identifier;
    
    public StackingReceivePort(StackingPortType type, String name,
            Upcall upcall, ReceivePortConnectUpcall connectUpcall,
            boolean connectionDowncalls) throws IOException {
        this.type = type;
        this.identifier = type.ibis.createReceivePortIdentifier(name, type.ibis.ident);
        if (connectionDowncalls) {
            base = type.base.createReceivePort(name, upcall, true);
        } else {
            base = type.base.createReceivePort(name, upcall, connectUpcall);
        }
    }

    public void close() throws IOException {
        base.close();
    }

    public void close(long timeoutMillis) throws IOException {
        base.close(timeoutMillis);
    }

    public SendPortIdentifier[] connectedTo() {
        // TODO: fix
        return base.connectedTo();
    }

    public void disableConnections() {
        base.disableConnections();
    }

    public void disableUpcalls() {
        base.disableUpcalls();
    }

    public void enableConnections() {
        base.enableConnections();
    }

    public void enableUpcalls() {
        base.enableUpcalls();
    }

    public long getCount() {
        return base.getCount();
    }

    public Object getProperty(String key) {
        return base.getProperty(key);
    }

    public ibis.ipl.PortType getType() {
        return type;
    }

    public ReceivePortIdentifier identifier() {
        // TODO: fix
        return base.identifier();
    }

    public SendPortIdentifier[] lostConnections() {
        // TODO: fix
        return base.lostConnections();
    }

    public String name() {
        return base.name();
    }

    public SendPortIdentifier[] newConnections() {
        // TODO: fix
        return base.newConnections();
    }

    public ReadMessage poll() throws IOException {
        return base.poll();
    }

    public Map<String, Object> properties() {
        return base.properties();
    }

    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    public ReadMessage receive(long timeoutMillis) throws IOException {
        // TODO fix
        return base.receive(timeoutMillis);
    }

    public void resetCount() {
        base.resetCount();
    }

    public void setProperties(Map<String, Object> properties) {
        base.setProperties(properties);
    }

    public void setProperty(String key, Object val) {
        base.setProperty(key, val);
    }

}
