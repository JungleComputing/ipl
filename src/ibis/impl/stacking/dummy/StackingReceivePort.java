package ibis.impl.stacking.dummy;

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
        return ((StackingIbis) type.ibis).fromBase(base.connectedTo());
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

    public ibis.ipl.PortType getType() {
        return type;
    }

    public ReceivePortIdentifier identifier() {
        return identifier;
    }

    public SendPortIdentifier[] lostConnections() {
        return ((StackingIbis) type.ibis).fromBase(base.lostConnections());
    }

    public String name() {
        return base.name();
    }

    public SendPortIdentifier[] newConnections() {
        return ((StackingIbis) type.ibis).fromBase(base.newConnections());
    }

    public ReadMessage poll() throws IOException {
        return base.poll();
    }

    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    public ReadMessage receive(long timeoutMillis) throws IOException {
        return new StackingReadMessage(base.receive(timeoutMillis), this);
    }

    public void resetCount() {
        base.resetCount();
    }

    public Map<String, Object> dynamicProperties() {
        return base.dynamicProperties();
    }

    public Object getDynamicProperty(String key) {
        return base.getDynamicProperty(key);
    }

    public void setDynamicProperties(Map<String, Object> properties) {
        base.setDynamicProperties(properties);      
    }

    public void setDynamicProperty(String key, Object val) {
        base.setDynamicProperty(key, val);
    }
}
