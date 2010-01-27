package ibis.ipl.impl.stacking.sns;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.sns.util.SNSEncryption;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class SNSSendPort implements SendPort {
    
    private SendPort base;
    private SNSEncryption encryption;
        
    private static final class DisconnectUpcaller
            implements SendPortDisconnectUpcall {
        SNSSendPort port;
        SendPortDisconnectUpcall upcaller;

        public DisconnectUpcaller(SNSSendPort port,
                SendPortDisconnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        public void lostConnection(SendPort me,
                ReceivePortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }
    
    public SNSSendPort(PortType type, SNSIbis ibis, String name,
            SendPortDisconnectUpcall connectUpcall, Properties props) throws IOException {
   	
        if (connectUpcall != null) {
            connectUpcall = new DisconnectUpcaller(this, connectUpcall);
            base = ibis.mIbis.createSendPort(type, name, connectUpcall, props);
        } else {
            base = ibis.mIbis.createSendPort(type, name, null, props);
        }
        
//        this.encryption = ibis.encryption;
    }
    
    public void close() throws IOException {
        base.close();    
    }

    public void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {    	
        connect(receiver, 0L, true);
    }

    public void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
    	base.connect(receiver, timeoutMillis, fillTimeout);        
    }

    public ReceivePortIdentifier connect(IbisIdentifier id, String name) throws ConnectionFailedException {
        return connect(id, name, 0L, true);
    }

    public ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        return base.connect(id, name, timeoutMillis, fillTimeout);
    }

    public void connect(ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        connect(ports, 0L, true);       
    }

    public void connect(ReceivePortIdentifier[] ports, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
        base.connect(ports, timeoutMillis, fillTimeout);        
    }

    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0L, true);
    }

    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
        return base.connect(ports, timeoutMillis, fillTimeout);
    }

    public ReceivePortIdentifier[] connectedTo() {
        return base.connectedTo();
    }

    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
       base.disconnect(receiver);
    }

    public void disconnect(IbisIdentifier id, String name) throws IOException {
        base.disconnect(id, name);
    }

    public PortType getPortType() {
        return base.getPortType();
    }

    public SendPortIdentifier identifier() {
            return base.identifier();
    }

    public ReceivePortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    public String name() {
            return base.name();
    }

    public WriteMessage newMessage() throws IOException {    	
        return new SNSWriteMessage(base.newMessage(), this);
    }

    public Map<String, String> managementProperties() {
        return base.managementProperties();
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return base.getManagementProperty(key);
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        base.setManagementProperties(properties);      
    }

    public void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        base.setManagementProperty(key, val);
    }
    
    public void printManagementProperties(PrintStream stream) {
        base.printManagementProperties(stream);
    }
}
