/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.impl.stacking.lrmc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
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

public class LrmcIbis implements Ibis {

    private static final Logger logger = LoggerFactory.getLogger(LrmcIbis.class);

    static final PortType additionalPortType = new PortType(PortType.SERIALIZATION_DATA, PortType.COMMUNICATION_RELIABLE,
            PortType.CONNECTION_MANY_TO_ONE, PortType.RECEIVE_AUTO_UPCALLS);

    private static class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;

        LrmcIbis ibis;

        EventHandler(RegistryEventHandler h, LrmcIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }

        @Override
        public void joined(IbisIdentifier id) {
            ibis.addIbis(id);
            if (h != null) {
                h.joined(id);
            }
        }

        @Override
        public void left(IbisIdentifier id) {
            ibis.removeIbis(id);
            if (h != null) {
                h.left(id);
            }
        }

        @Override
        public void died(IbisIdentifier id) {
            ibis.removeIbis(id);
            if (h != null) {
                h.died(id);
            }
        }

        @Override
        public void gotSignal(String s, IbisIdentifier id) {
            if (h != null) {
                h.gotSignal(s, id);
            }
        }

        @Override
        public void electionResult(String electionName, IbisIdentifier winner) {
            if (h != null) {
                h.electionResult(electionName, winner);
            }
        }

        @Override
        public void poolClosed() {
            if (h != null) {
                h.poolClosed();
            }
        }

        @Override
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

    private BitSet diedIbises = new BitSet();

    HashMap<IbisIdentifier, Integer> knownIbis = new HashMap<>();

    DynamicObjectArray<IbisIdentifier> ibisList = new DynamicObjectArray<>();

    HashMap<String, Multicaster> multicasters = new HashMap<>();

    public LrmcIbis(IbisFactory factory, RegistryEventHandler registryEventHandler, Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation, LrmcIbisStarter lrmcIbisStarter)
            throws IbisCreationFailedException {
        List<PortType> requiredPortTypes = new ArrayList<>();
        logger.info("Constructor LRMC Ibis");

        if (specifiedSubImplementation == null) {
            throw new IbisCreationFailedException("LrmcIbis: child Ibis implementation not specified");
        }

        EventHandler h = null;

        if (registryEventHandler != null) {
            h = new EventHandler(registryEventHandler, this);
        }

        this.portTypes = portTypes;
        this.capabilities = capabilities;

        // add additional port-type as a requirement, and remove port-types
        // that we deal with ourselves.
        for (PortType portType : portTypes) {
            if (!ourPortType(portType)) {
                requiredPortTypes.add(portType);
            }
        }

        requiredPortTypes.add(additionalPortType);

        base = factory.createIbis(h, capabilities, userProperties, credentials, applicationTag,
                requiredPortTypes.toArray(new PortType[requiredPortTypes.size()]), specifiedSubImplementation);
    }

    public synchronized void addIbis(IbisIdentifier ibis) {

        if (!knownIbis.containsKey(ibis)) {
            knownIbis.put(ibis, Integer.valueOf(nextIbisID));
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

    synchronized IbisIdentifier getId(int id) {

        if (diedIbises.get(id)) {
            return null;
        }

        IbisIdentifier ibisID = ibisList.get(id);

        if (ibisID == null) {
            try {
                wait(10000);
            } catch (Exception e) {
                // ignored
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
        diedIbises.set(tmp.intValue());
    }

    synchronized Multicaster getMulticaster(String name, PortType portType) throws IOException {
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

    @Override
    public String getVersion() {
        return "LrmcIbis on top of " + base.getVersion();
    }

    @Override
    public SendPort createSendPort(PortType portType, String name, SendPortDisconnectUpcall cU, Properties props) throws IOException {
        matchPortType(portType);
        if (ourPortType(portType)) {
            if (name == null) {
                throw new IOException("Anonymous  ports not supported");
            }
            if (cU != null && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException("connection upcalls not supported by this porttype");
            }
            synchronized (this) {
                Multicaster mc = getMulticaster(name, portType);
                if (mc.sendPort != null) {
                    throw new IOException("A sendport with the same name already exists");
                }
                mc.sendPort = new LrmcSendPort(mc, this, props);
                return mc.sendPort;
            }
        }
        return new StackingSendPort(portType, this, name, cU, props);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name, MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        matchPortType(portType);
        if (ourPortType(portType)) {
            if (name == null) {
                throw new IOException("Anonymous  ports not supported");
            }
            if (cU != null && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException("connection upcalls not supported by this porttype");
            }
            if (u != null && !portType.hasCapability(PortType.RECEIVE_AUTO_UPCALLS)) {
                throw new IbisConfigurationException("upcalls not supported by this porttype");
            }
            if (u == null && !portType.hasCapability(PortType.RECEIVE_EXPLICIT)) {
                throw new IbisConfigurationException("explicit receive not supported by this porttype");
            }
            synchronized (this) {
                Multicaster mc = getMulticaster(name, portType);
                if (mc.receivePort != null) {
                    throw new IOException("A receiveport with the same name already exists");
                }
                mc.receivePort = new LrmcReceivePort(mc, this, u, props);
                return mc.receivePort;
            }
        }
        return new StackingReceivePort(portType, this, name, u, cU, props);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String receivePortName) throws IOException {
        return createReceivePort(portType, receivePortName, null, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String receivePortName, MessageUpcall messageUpcall) throws IOException {
        return createReceivePort(portType, receivePortName, messageUpcall, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String receivePortName, ReceivePortConnectUpcall receivePortConnectUpcall)
            throws IOException {
        return createReceivePort(portType, receivePortName, null, receivePortConnectUpcall, null);
    }

    @Override
    public ibis.ipl.SendPort createSendPort(PortType tp) throws IOException {
        return createSendPort(tp, null, null, null);
    }

    @Override
    public ibis.ipl.SendPort createSendPort(PortType tp, String name) throws IOException {
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
            throw new IbisConfigurationException("PortType " + tp + " not specified when creating this Ibis instance");
        }
    }

    @Override
    public void end() throws IOException {
        for (Map.Entry<String, Multicaster> x : multicasters.entrySet()) {
            x.getValue().done();
        }
        base.end();
    }

    @Override
    public Registry registry() {
        // return new
        // ibis.ipl.impl.registry.ForwardingRegistry(base.registry());
        return base.registry();
    }

    @Override
    public Map<String, String> managementProperties() {
        return base.managementProperties();
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return base.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        base.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val) throws NoSuchPropertyException {
        base.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        base.printManagementProperties(stream);
    }

    @Override
    public void poll() throws IOException {
        base.poll();
    }

    @Override
    public IbisIdentifier identifier() {
        return base.identifier();
    }

    @Override
    public Properties properties() {
        return base.properties();
    }

    /**
     * Determines if the specified port type is one that is implemented by Lrmc
     * ibis.
     * 
     * @param tp the port type
     * @return <code>true</code> if LRMC Ibis deals with this port type,
     *         <code>false</code> if it is to be passed on to an underlying ibis.
     */
    private static boolean ourPortType(PortType tp) {
        return (tp.hasCapability(PortType.CONNECTION_MANY_TO_MANY) || tp.hasCapability(PortType.CONNECTION_ONE_TO_MANY))
                && !tp.hasCapability(PortType.COMMUNICATION_RELIABLE) && !tp.hasCapability(PortType.CONNECTION_UPCALLS)
                && !tp.hasCapability(PortType.CONNECTION_DOWNCALLS) && !tp.hasCapability(PortType.COMMUNICATION_NUMBERED)
                && !tp.hasCapability(PortType.COMMUNICATION_FIFO);
    }
}
