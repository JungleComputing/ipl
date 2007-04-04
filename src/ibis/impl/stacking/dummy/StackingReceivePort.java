package ibis.impl.stacking.dummy;

import ibis.ipl.CapabilitySet;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.util.Map;

public class StackingReceivePort implements ibis.ipl.ReceivePort {

    final ibis.ipl.ReceivePort base;
    final CapabilitySet type;
    final ibis.impl.ReceivePortIdentifier identifier;

    private static final class ConnectUpcaller
            implements ReceivePortConnectUpcall {
        StackingReceivePort port;
        ReceivePortConnectUpcall upcaller;

        public ConnectUpcaller(StackingReceivePort port,
                ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        public boolean gotConnection(ibis.ipl.ReceivePort me,
                SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        public void lostConnection(ibis.ipl.ReceivePort me,
                SendPortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }
    
    private static final class Upcaller implements Upcall {
        Upcall upcaller;
        StackingReceivePort port;

        public Upcaller(Upcall upcaller, StackingReceivePort port) {
            this.upcaller = upcaller;
            this.port = port;
        }

        public void upcall(ReadMessage m) throws IOException {
            upcaller.upcall(new StackingReadMessage(m, port));
        }
    }
    
    public StackingReceivePort(CapabilitySet type, StackingIbis ibis,
            String name, Upcall upcall, ReceivePortConnectUpcall connectUpcall)
            throws IOException {
        this.type = type;
        this.identifier = ibis.createReceivePortIdentifier(name, ibis.ident);
        if (connectUpcall != null) {
            connectUpcall = new ConnectUpcaller(this, connectUpcall);
        }
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }
        base = ibis.base.createReceivePort(type, name, upcall, connectUpcall);
    }

    public void close() throws IOException {
        base.close();
    }

    public void close(long timeoutMillis) throws IOException {
        base.close(timeoutMillis);
    }

    public SendPortIdentifier[] connectedTo() {
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

    public CapabilitySet getType() {
        return type;
    }

    public ReceivePortIdentifier identifier() {
        return identifier;
    }

    public SendPortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    public String name() {
        return base.name();
    }

    public SendPortIdentifier[] newConnections() {
        return base.newConnections();
    }

    public ReadMessage poll() throws IOException {
        ReadMessage m = base.poll();
        if (m != null) {
            m = new StackingReadMessage(m, this);
        }
        return m;
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
