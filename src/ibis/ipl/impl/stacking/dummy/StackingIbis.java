package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
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

    public StackingIbis(RegistryEventHandler registryHandler,
            IbisCapabilities caps, PortType[] types, Properties tp) {
        super(registryHandler, caps, types, tp);
    }

    public void printStatistics(PrintStream out) {
        super.printStatistics(out);
        base.printStatistics(out);
    }

    @Override
    protected Registry initializeRegistry(RegistryEventHandler handler, IbisCapabilities capabilities) {
        // For a stacking Ibis that actually implements a feature,
        // remove this feature from the capabilities here.
        if (count < 5) {
            count++;
            if (handler != null) {
                base = new StackingIbis(handler, capabilities, portTypes,  properties);
            } else {
                base = new StackingIbis(null, capabilities, portTypes, properties);
            }
        } else {
            if (handler != null) {
                base = new TcpIbis(handler, capabilities, portTypes, properties);
            } else {
                base = new TcpIbis(null, capabilities, portTypes, properties);
            }
        }
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
