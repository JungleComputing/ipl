package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisStarter;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.stacking.lrmc.util.DynamicObjectArray;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LrmcIbis implements Ibis {

    private static final Logger logger = LoggerFactory.getLogger(LrmcIbis.class);

    static final PortType baseType = new PortType(PortType.SERIALIZATION_DATA,
            PortType.COMMUNICATION_RELIABLE, PortType.CONNECTION_MANY_TO_ONE,
            PortType.RECEIVE_AUTO_UPCALLS);

    private static class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        LrmcIbis ibis;

        EventHandler(RegistryEventHandler h, LrmcIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }

        public void joined(IbisIdentifier id) {
            ibis.addIbis(id);
            if (h != null) {
                h.joined(id);
            }
        }

        public void left(IbisIdentifier id) {
            ibis.removeIbis(id);
            if (h != null) {
                h.left(id);
            }
        }

        public void died(IbisIdentifier id) {
            ibis.removeIbis(id);
            if (h != null) {
                h.died(id);
            }
        }

        public void gotSignal(String s, IbisIdentifier id) {
            if (h != null) {
                h.gotSignal(s, id);
            }
        }

        public void electionResult(String electionName, IbisIdentifier winner) {
            if (h != null) {
                h.electionResult(electionName, winner);
            }
        }

        public void poolClosed() {
            if (h != null) {
                h.poolClosed();
            }
        }

        public void poolTerminated(IbisIdentifier source) {
            if (h != null) {
                h.poolTerminated(source);
            }
        }
    }

    Ibis base;
    int myID;

    PortType[] portTypes;
    IbisCapabilities capabilities;

    private int nextIbisID = 0;

    HashMap<IbisIdentifier, Integer> knownIbis = new HashMap<IbisIdentifier, Integer>();
    DynamicObjectArray<IbisIdentifier> ibisList = new DynamicObjectArray<IbisIdentifier>();
    HashMap<String, Multicaster> multicasters = new HashMap<String, Multicaster>();

    LrmcIbis(List<IbisStarter> stack,
            RegistryEventHandler registryEventHandler,
            IbisCapabilities capabilities, PortType[] portTypes,
            Properties userProperties, String version) {
        logger.info("Constructor LRMC Ibis");
        IbisStarter s = stack.remove(0);
        EventHandler h = new EventHandler(registryEventHandler, this);
        base = s.startIbis(stack, h, userProperties, version);
        this.portTypes = portTypes;
        this.capabilities = capabilities;
    }

    public synchronized void addIbis(IbisIdentifier ibis) {

        if (!knownIbis.containsKey(ibis)) {
            knownIbis.put(ibis, new Integer(nextIbisID));
            ibisList.put(nextIbisID, ibis);

            logger.info("Adding Ibis " + nextIbisID + " " + ibis);

            if (ibis.equals(identifier())) {
                logger.info("I am " + nextIbisID + " " + ibis);
                myID = nextIbisID;
            }

            nextIbisID++;
            notifyAll();
        }
    }

    IbisIdentifier getId(int id) {
        IbisIdentifier ibisID = ibisList.get(id);

        if (ibisID == null) {
            synchronized (this) {
                try {
                    wait(10000);
                } catch (Exception e) {
                    // ignored
                }
            }
            return ibisList.get(id);
        }
        return ibisID;
    }

    synchronized int getIbisID(IbisIdentifier ibis) {

        Integer s = knownIbis.get(ibis);

        if (s != null) {
            return s.intValue();
        } else {
            logger.debug("Ibis " + ibis + " not known!");
            return -1;
        }
    }

    public synchronized void removeIbis(IbisIdentifier ibis) {

        Integer tmp = knownIbis.remove(ibis);

        if (tmp != null) {
            logger.info("Removing ibis " + tmp.intValue() + " " + ibis);
            ibisList.remove(tmp.intValue());
        }
    }

    synchronized Multicaster getMulticaster(String name, PortType portType)
            throws IOException {
        Multicaster om = multicasters.get(name);
        if (om == null) {
            om = new Multicaster(this, portType, name);
            multicasters.put(name, om);
        } else {
            if (!om.portType.equals(portType)) {
                throw new IOException("Mismatch in port types for name " + name);
            }
        }
        return om;
    }

    public String getVersion() {
        return "LrmcIbis on top of " + base.getVersion();
    }

    public SendPort createSendPort(PortType portType, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        matchPortType(portType);
        if (LrmcIbisStarter.ourPortType(portType)) {
            if (name == null) {
                throw new IOException("Anonymous  ports not supported");
            }
            if (cU != null
                    && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "connection upcalls not supported by this porttype");
            }
            synchronized (this) {
                Multicaster mc = getMulticaster(name, portType);
                if (mc.sendPort != null) {
                    throw new IOException(
                            "A sendport with the same name already exists");
                }
                mc.sendPort = new LrmcSendPort(mc, this, props);
                return mc.sendPort;
            }
        }
        return new StackingSendPort(portType, this, name, cU, props);
    }

    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        matchPortType(portType);
        if (LrmcIbisStarter.ourPortType(portType)) {
            if (name == null) {
                throw new IOException("Anonymous  ports not supported");
            }
            if (cU != null
                    && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "connection upcalls not supported by this porttype");
            }
            if (u != null
                    && !portType.hasCapability(PortType.RECEIVE_AUTO_UPCALLS)) {
                throw new IbisConfigurationException(
                        "upcalls not supported by this porttype");
            }
            if (u == null && !portType.hasCapability(PortType.RECEIVE_EXPLICIT)) {
                throw new IbisConfigurationException(
                        "explicit receive not supported by this porttype");
            }
            synchronized (this) {
                Multicaster mc = getMulticaster(name, portType);
                if (mc.receivePort != null) {
                    throw new IOException(
                            "A receiveport with the same name already exists");
                }
                mc.receivePort = new LrmcReceivePort(mc, this, u, props);
                return mc.receivePort;
            }
        }
        return new StackingReceivePort(portType, this, name, u, cU, props);
    }

    public ReceivePort createReceivePort(PortType portType,
            String receivePortName) throws IOException {
        return createReceivePort(portType, receivePortName, null, null, null);
    }

    public ReceivePort createReceivePort(PortType portType,
            String receivePortName, MessageUpcall messageUpcall)
            throws IOException {
        return createReceivePort(portType, receivePortName, messageUpcall,
                null, null);
    }

    public ReceivePort createReceivePort(PortType portType,
            String receivePortName,
            ReceivePortConnectUpcall receivePortConnectUpcall)
            throws IOException {
        return createReceivePort(portType, receivePortName, null,
                receivePortConnectUpcall, null);
    }

    public ibis.ipl.SendPort createSendPort(PortType tp) throws IOException {
        return createSendPort(tp, null, null, null);
    }

    public ibis.ipl.SendPort createSendPort(PortType tp, String name)
            throws IOException {
        return createSendPort(tp, name, null, null);
    }

    private void matchPortType(PortType tp) {
        boolean matched = false;
        for (PortType p : portTypes) {
            if (tp.equals(p)) {
                matched = true;
            }
        }
        if (!matched) {
            throw new IbisConfigurationException("PortType " + tp
                    + " not specified when creating this Ibis instance");
        }
    }

    public void end() throws IOException {
        for (Map.Entry<String, Multicaster> x : multicasters.entrySet()) {
            x.getValue().done();
        }
        base.end();
    }

    public Registry registry() {
        // return new
        // ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
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

    public Properties properties() {
        return base.properties();
    }

}
