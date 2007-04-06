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
    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.WORLDMODEL_OPEN,
            IbisCapabilities.WORLDMODEL_CLOSED,
            IbisCapabilities.REGISTRY_DOWNCALLS,
            IbisCapabilities.REGISTRY_UPCALLS,
            "nickname.dummy"
        );

        static final PortType portCapabilities = new PortType(
            PortType.SERIALIZATION_OBJECT,
            PortType.SERIALIZATION_DATA,
            PortType.SERIALIZATION_BYTE,
            PortType.SERIALIZATION_REPLACER + "=*",
            PortType.COMMUNICATION_FIFO,
            PortType.COMMUNICATION_NUMBERED,
            PortType.COMMUNICATION_RELIABLE,
            PortType.CONNECTION_DOWNCALLS,
            PortType.CONNECTION_UPCALLS,
            PortType.CONNECTION_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY,
            PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_ONE_TO_MANY,
            PortType.CONNECTION_ONE_TO_ONE,
            PortType.RECEIVE_POLL,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_POLL_UPCALLS,
            PortType.RECEIVE_TIMEOUT
        );

    Ibis base;

    public StackingIbis(RegistryEventHandler registryHandler,
            IbisCapabilities caps, PortType[] types, Properties tp) {
        super(registryHandler, caps, types, tp, null);
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

    @Override
    protected ibis.ipl.ReceivePort doCreateReceivePort(PortType tp,
            String name, MessageUpcall u, ReceivePortConnectUpcall cU)
            throws IOException {
        return new StackingReceivePort(tp, this, name, u, cU);
    }

    @Override
    protected ibis.ipl.SendPort doCreateSendPort(PortType tp, String name,
            SendPortDisconnectUpcall cU) throws IOException {
        return new StackingSendPort(tp, this, name, cU);
    }

    @Override
    protected IbisCapabilities getCapabilities() {
        return ibisCapabilities;
    }

    @Override
    protected PortType getPortCapabilities() {
        return portCapabilities;
    }

}
