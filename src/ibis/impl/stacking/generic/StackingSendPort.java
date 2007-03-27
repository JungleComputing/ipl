package ibis.impl.stacking.generic;

import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
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
        // TODO
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver, long timeoutMillis) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public ibis.ipl.ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        // TODO Auto-generated method stub
        
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports, long timeoutMillis) throws ConnectionsFailedException {
        // TODO Auto-generated method stub
        
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        // TODO Auto-generated method stub
        return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis) throws ConnectionsFailedException {
        // TODO Auto-generated method stub
        return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        // TODO Auto-generated method stub
        return null;
    }

    public void disconnect(ibis.ipl.ReceivePortIdentifier receiver) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public long getCount() {
        return base.getCount();
    }

    public Object getProperty(String key) {
        return base.getProperty(key);
    }

    public PortType getType() {
        return type;
    }

    public SendPortIdentifier identifier() {
            return identifier;
    }

    public ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        // TODO Auto-generated method stub
        return null;
    }

    public String name() {
            return base.name();
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        return new StackingWriteMessage(base.newMessage(), this);
    }

    public Map<String, Object> properties() {
        return base.properties();
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
