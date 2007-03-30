package ibis.impl.stacking.dummy;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.Registry;
import ibis.impl.tcp.TcpIbis;
import ibis.ipl.CapabilitySet;
import ibis.ipl.PortType;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public class StackingIbis extends Ibis {
    private static int count = 0;

    Ibis base;

    public StackingIbis(RegistryEventHandler registryHandler,
            CapabilitySet caps, Properties tp) {
        super(registryHandler, caps, tp, null);
    }

    public void printStatistics(PrintStream out) {
        super.printStatistics(out);
        base.printStatistics(out);
    }

    @Override
    protected Registry initializeRegistry(RegistryEventHandler handler) {
        // For a stacking Ibis that actually implements a feature,
        // remove this feature from the capabilities here.
        if (count < 5) {
            count++;
            if (handler != null) {
                base = new StackingIbis(handler, capabilities, properties);
            } else {
                base = new StackingIbis(null, capabilities, properties);
            }
        } else {
            if (handler != null) {
                base = new TcpIbis(handler, capabilities, properties);
            } else {
                base = new TcpIbis(null, capabilities, properties);
            }
        }
        // return new ibis.impl.registry.ForwardingRegistry(base.registry());
        return base.registry();
    }

    @Override
    protected byte[] getData() throws IOException {
        return null;
    }

    @Override
    protected PortType newPortType(CapabilitySet p, Properties attrib) {
         return new StackingPortType(this, p, attrib);
    }

    @Override
    public void end() {
        base.end();
    }

    @Override
    public int totalNrOfIbisesInPool() {
        return base.totalNrOfIbisesInPool();
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
}
