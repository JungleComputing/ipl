package ibis.ipl.impl.stacking.cache;

import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.manager.RandomCacheManager;
import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class CacheIbis implements Ibis {

    public Ibis baseIbis;
    IbisStarter starter;
    CacheManager cacheManager;
    
    /**
     * These capabilities need to be added to the list of capabilites
     * the user requests.
     * i.e. the caching ports require CONNECTION_UPCALLS.
     */
    static final Set<String> additionalPortCapabilities;
    static final Set<PortType> additionalPortTypes;
    static final Set<String> offeredIbisCapabilities;
    
    static {
        /*
         * Enforce these capabilities on the ibis creation.
         */
        offeredIbisCapabilities = new HashSet<String>();
        additionalPortCapabilities = new HashSet<String>();
        additionalPortTypes = new HashSet<PortType>();
        
        /*
         * I require this so I can handle caching connections at the
         * receive port side.
         */
        additionalPortCapabilities.add(PortType.CONNECTION_UPCALLS);
        /*
         * I require this so my partial streamed messages arrive 
         * in the right order.
         */
        additionalPortCapabilities.add(PortType.COMMUNICATION_FIFO);
        /*
         * Add this port type to the ibis constructor - required for
         * the side channel.
         */
        additionalPortTypes.add(CacheManager.ultraLightPT);
        /*
         * These are the ibis capabilities offered by CacheIbis.
         * These capabilities are to be removed when constructing 
         * the under-the-hood ibis.
         */
        offeredIbisCapabilities.add(IbisCapabilities.CONNECTION_CACHING);
    }

    public CacheIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities ibisCapabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes,
            String specifiedSubImplementation,
            CacheIbisStarter cacheIbisStarter)
            throws IbisCreationFailedException {

        starter = cacheIbisStarter;
        
        
        int newNoPorts = portTypes.length + additionalPortTypes.size();
        PortType[] newPorts = new PortType[newNoPorts];
        
        /**
         * Need to take all ports and create new ones with the 
         * additional port capabilities.
         */
        for(int i = 0; i < portTypes.length; i++) {
            Set<String> portCap = new HashSet<String>(Arrays.asList(
                    portTypes[i].getCapabilities()));
            portCap.addAll(additionalPortCapabilities);
            newPorts[i] = new PortType(portCap.toArray(new String[portCap.size()]));
        }
        
        /**
         * Add to the list of port types any other required port types,
         * i.e. the port type used for the side channel.
         */
        int i = portTypes.length;
        for(PortType pt : additionalPortTypes) {
            newPorts[i++] = pt;
        }        
        portTypes = newPorts;
        
        /**
         * Remove any of the offered ibis capabilities for the under the hood
         * ibis instance.
         */
        Set<String> newCapabilities = new HashSet<String>(Arrays.asList(
                ibisCapabilities.getCapabilities()));
        newCapabilities.removeAll(offeredIbisCapabilities);
        ibisCapabilities = new IbisCapabilities(newCapabilities.toArray(
                new String[newCapabilities.size()]));
        
        baseIbis = factory.createIbis(registryEventHandler, ibisCapabilities,
                    userProperties, credentials, applicationTag, portTypes,
                    specifiedSubImplementation);
        
        // TODO: get the implementation version from somewhere
        cacheManager = new RandomCacheManager(this);
    }

    @Override
    public void end() throws IOException {
        cacheManager.end();
        baseIbis.end();
    }

    @Override
    public Registry registry() {
        return baseIbis.registry();
    }

    @Override
    public Map<String, String> managementProperties() {
        return baseIbis.managementProperties();
    }

    @Override
    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return baseIbis.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        baseIbis.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        baseIbis.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        baseIbis.printManagementProperties(stream);
    }

    @Override
    public void poll() throws IOException {
        baseIbis.poll();
    }

    @Override
    public IbisIdentifier identifier() {
        return baseIbis.identifier();
    }

    @Override
    public String getVersion() {
        return starter.getNickName() + " on top of " + baseIbis.getVersion();
    }

    @Override
    public Properties properties() {
        return baseIbis.properties();
    }

    @Override
    public SendPort createSendPort(PortType portType) throws IOException {
        return createSendPort(portType, null, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name)
            throws IOException {
        return createSendPort(portType, name, null, null);
    }

    @Override
    public SendPort createSendPort(PortType portType, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new CacheSendPort(portType, this, name, cU, props);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name)
            throws IOException {
        return createReceivePort(portType, name, null, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u) throws IOException {
        return createReceivePort(portType, name, u, null, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(portType, name, null, cU, null);
    }

    @Override
    public ReceivePort createReceivePort(PortType portType, String name,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        return new CacheReceivePort(portType, this, name, u, cU, props);
    }
}
