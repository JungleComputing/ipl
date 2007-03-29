package ibis.impl.stacking.dummy;

import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StackingSendPort implements ibis.ipl.SendPort {
    
    final ibis.ipl.SendPort base;
    final StackingPortType type;
    final ibis.impl.SendPortIdentifier identifier;
    
    public StackingSendPort(StackingPortType type, String name,
            SendPortDisconnectUpcall connectUpcall, boolean connectionDowncalls)
            throws IOException {
        this.type = type;
        this.identifier = type.ibis.createSendPortIdentifier(name, type.ibis.ident);
        if (connectionDowncalls) {
            base = type.base.createSendPort(name, true);
        } else if (connectUpcall != null) {
            base = type.base.createSendPort(name, connectUpcall);
        } else {
            base = type.base.createSendPort(name);
        }
    }

    
    public void close() throws IOException {
        base.close();    
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver) throws IOException {
        connect(receiver, 0L);
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver, long timeoutMillis) throws IOException {
        base.connect(((StackingIbis) type.ibis).toBase(receiver), timeoutMillis);
        
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name) throws IOException {
        return connect(id, name, 0L);
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis) throws IOException {
        ibis.impl.IbisIdentifier idBase = ((StackingIbis)type.ibis).toBase(id);
        ibis.ipl.ReceivePortIdentifier rp = base.connect(idBase, name, timeoutMillis);
        return ((StackingIbis)type.ibis).fromBase(rp);
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        connect(ports, 0L);
        
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports, long timeoutMillis) throws ConnectionsFailedException {
        base.connect(((StackingIbis)type.ibis).toBase(ports), timeoutMillis);
        
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0L);
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis) throws ConnectionsFailedException {
        HashMap<IbisIdentifier, String> h = new HashMap<IbisIdentifier, String>();
        for (IbisIdentifier i : ports.keySet()) {
            IbisIdentifier id = ((StackingIbis)type.ibis).toBase(i);
            h.put(id, ports.get(i));
        }
        return ((StackingIbis)type.ibis).fromBase(base.connect(h, timeoutMillis));
    }

    public ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        return ((StackingIbis)type.ibis).fromBase(base.connectedTo());
    }

    public void disconnect(ibis.ipl.ReceivePortIdentifier receiver) throws IOException {
       base.disconnect(((StackingIbis)type.ibis).toBase(receiver)); 
    }

    public long getCount() {
        return base.getCount();
    }

    public void resetCount() {
        base.resetCount();   
    }

    public PortType getType() {
        return type;
    }

    public SendPortIdentifier identifier() {
            return identifier;
    }

    public ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        return ((StackingIbis) type.ibis).fromBase(base.lostConnections());
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
