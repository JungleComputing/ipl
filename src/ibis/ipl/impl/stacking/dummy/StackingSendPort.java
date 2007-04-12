package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class StackingSendPort implements ibis.ipl.SendPort {
    
    final ibis.ipl.SendPort base;
    final PortType type;
    final ibis.ipl.impl.SendPortIdentifier identifier;
    
    private static final class DisconnectUpcaller
            implements SendPortDisconnectUpcall {
        StackingSendPort port;
        SendPortDisconnectUpcall upcaller;

        public DisconnectUpcaller(StackingSendPort port,
                SendPortDisconnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        public void lostConnection(ibis.ipl.SendPort me,
                ibis.ipl.ReceivePortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }
    
    public StackingSendPort(PortType type, StackingIbis ibis, String name,
            SendPortDisconnectUpcall connectUpcall, Properties props) throws IOException {
        this.type = type;
        this.identifier = ibis.createSendPortIdentifier(name, ibis.ident);

        if (connectUpcall != null) {
            connectUpcall = new DisconnectUpcaller(this, connectUpcall);
            base = ibis.base.createSendPort(type, name, connectUpcall, props);
        } else {
            base = ibis.base.createSendPort(type, name, null, props);
        }
    }

    
    public void close() throws IOException {
        base.close();    
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver) throws ConnectionFailedException {
        connect(receiver, 0L);
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver, long timeoutMillis) throws ConnectionFailedException {
        base.connect(receiver, timeoutMillis);
        
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name) throws ConnectionFailedException {
        return connect(id, name, 0L);
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis) throws ConnectionFailedException {
        return base.connect(id, name, timeoutMillis);
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        connect(ports, 0L);
        
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports, long timeoutMillis) throws ConnectionsFailedException {
        base.connect(ports, timeoutMillis);
        
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0L);
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis) throws ConnectionsFailedException {
        return connect(ports, timeoutMillis);
    }

    public ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        return base.connectedTo();
    }

    public void disconnect(ibis.ipl.ReceivePortIdentifier receiver) throws IOException {
       base.disconnect(receiver);
    }

    public long getCount() {
        return base.getCount();
    }

    public void resetCount() {
        base.resetCount();   
    }

    public PortType getPortType() {
        return type;
    }

    public SendPortIdentifier identifier() {
            return identifier;
    }

    public ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    public String name() {
            return base.name();
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        return new StackingWriteMessage(base.newMessage(), this);
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
