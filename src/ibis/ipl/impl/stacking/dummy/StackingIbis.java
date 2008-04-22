package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Registry;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class StackingIbis implements Ibis {

    Ibis base;

    StackingIbis(Ibis base) {
        this.base = base;
    }

    public void end() throws IOException {
        base.end();
    }

    public Registry registry() {
        // return new ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
        return base.registry();
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

    public void poll() throws IOException {
        base.poll();
    }

    public IbisIdentifier identifier() {
        return base.identifier();
    }

    public String getVersion() {
        return "StackingIbis on top of " + base.getVersion();
    }

    public Properties properties() {
        return base.properties();
    }

    public SendPort createSendPort(PortType portType) throws IOException {
        return createSendPort(portType, null, null, null);
    }

    public SendPort createSendPort(PortType portType, String name) 
            throws IOException {
        return createSendPort(portType, name, null, null);
    }

    public SendPort createSendPort(PortType portType, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new StackingSendPort(portType, this, name, cU, props);
    }

    public ReceivePort createReceivePort(PortType portType, String name)
            throws IOException {
        return createReceivePort(portType, name, null, null, null);
    }

    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u) throws IOException {
        return createReceivePort(portType, name, u, null, null);
    }

    public ReceivePort createReceivePort(PortType portType, String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(portType, name, null, cU, null);
    }

    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        return new StackingReceivePort(portType, this, name, u, cU, props);
    }

 

}
