package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.Registry;
import ibis.ipl.impl.tcp.TcpIbis;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class StackingIbis extends Ibis {

    private static int count = 0;

    Ibis base;

    public StackingIbis(Ibis base, RegistryEventHandler registryHandler,
            IbisCapabilities caps, PortType[] types, Properties tp) {
        super(registryHandler, caps, types, tp, base);
    }

    public void printStatistics() {
        super.printStatistics();
        base.printStatistics();
    }

    @Override
    protected Registry initializeRegistry(RegistryEventHandler handler, IbisCapabilities capabilities, Ibis base) {
        this.base = base;
        // return new ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
        return base.registry();
    }

    @Override
    protected byte[] getData() throws IOException {
        return null;
    }

    @Override
    public void end() throws IOException {
        base.end();
    }
/*
    @Override
    public int getPoolSize() {
        return base.getPoolSize();
    }

    @Override
    public void waitForAll() {
        base.waitForAll();
    }

    @Override
    public void enableRegistryEvents() {
        super.enableRegistryEvents();
        base.enableRegistryEvents();
    }

    @Override
    public void disableRegistryEvents() {
        super.disableRegistryEvents();
        base.disableRegistryEvents();
    }
*/
    @Override
    protected void quit() {
    }

    public Map<String, String> dynamicProperties() {
        return base.dynamicProperties();
    }

    public String getDynamicProperty(String key)
            throws NoSuchPropertyException {
        return base.getDynamicProperty(key);
    }

    public void setDynamicProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        base.setDynamicProperties(properties);      
    }

    public void setDynamicProperty(String key, String val)
            throws NoSuchPropertyException {
        base.setDynamicProperty(key, val);
    }

    public void poll() {
        base.poll();
    }

    @Override
    protected ibis.ipl.ReceivePort doCreateReceivePort(PortType tp,
            String name, MessageUpcall u, ReceivePortConnectUpcall cU,
            Properties props) throws IOException {
        return new StackingReceivePort(tp, this, name, u, cU, props);
    }

    @Override
    protected ibis.ipl.SendPort doCreateSendPort(PortType tp, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new StackingSendPort(tp, this, name, cU, props);
    }
}
