package ibis.ipl.impl.stacking.cc;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cc.manager.CCManager;
import ibis.util.TypedProperties;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * A ConnectionCachingIbis. It offers the property that at most a given number
 * of connections will be alive at any point in time, but transparently to the
 * user.
 * @author maricel
 */
public class CCIbis implements Ibis {

    public Ibis baseIbis;
    public IbisStarter starter;
    public CCManager ccManager;
    public final int buffer_capacity;
    
    private boolean ended = false;
    
    /*
     * Maximum number of alive connections at any time per ibis.
     */
    public static final int default_max_conns = 1000;
    
    /*
     * Default caching implementation.
     */
    public static final String default_caching_version = "lru";
    
    /*
     * Default value for buffer size. The buffer is used to store the message
     * which is to be sent to receive ports.
     */
    private static final int default_buffer_capacity = 1 << 16;
    
    public static final String s_prefix = "ipl.stacking.connection.caching";
    public static final String s_max_conns = s_prefix + ".maxConns";
    public static final String s_caching_version = s_prefix + ".version";
    public static final String s_buffer_size = s_prefix + ".bufferSize";
    
    /*
     * I need to remove some capabilities in order to put the ones I need;
     * for example: removing any serialization, and add byte_serialization.
     */
    static final Set<String> removablePortCapabilities;
    /**
     * These capabilities need to be added to the list of capabilites
     * the user requests.
     * i.e. the caching ports require CONNECTION_UPCALLS.
     */
    static final Set<String> additionalPortCapabilities;
    static final Set<PortType> additionalPortTypes;
    static final Set<String> offeredIbisCapabilities;
    static final Set<String> offeredPortCapabilities;
    
    static final Map<String, String> qualNamesMap;
    
    static {
        /*
         * Enforce these capabilities on the ibis creation.
         */
        removablePortCapabilities = new HashSet<String>();
        offeredIbisCapabilities = new HashSet<String>();
        offeredPortCapabilities = new HashSet<String>();
        additionalPortCapabilities = new HashSet<String>();
        additionalPortTypes = new HashSet<PortType>();
        
        removablePortCapabilities.add(PortType.SERIALIZATION_DATA);
        removablePortCapabilities.add(PortType.SERIALIZATION_OBJECT);
        removablePortCapabilities.add(PortType.SERIALIZATION_OBJECT_IBIS);
        removablePortCapabilities.add(PortType.SERIALIZATION_OBJECT_SUN);
        
        /*
         * The buffers are only bytes.
         */
        additionalPortCapabilities.add(PortType.SERIALIZATION_BYTE);
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
        additionalPortTypes.add(CCManager.ultraLightPT);
        /*
         * These are the ibis capabilities offered by the CCIbis.
         * These capabilities are to be removed when constructing 
         * the under-the-hood ibis.
         */
        offeredIbisCapabilities.add(IbisCapabilities.CONNECTION_CACHING);
        offeredPortCapabilities.add(PortType.COMMUNICATION_TOTALLY_ORDERED_MULTICASTS);
        
        qualNamesMap = new HashMap<String, String>();
        qualNamesMap.put("lru",
                "ibis.ipl.impl.stacking.cc.manager.impl.LruCCManagerImpl");
        qualNamesMap.put("mru",
                "ibis.ipl.impl.stacking.cc.manager.impl.MruCCManagerImpl");
        qualNamesMap.put("random",
                "ibis.ipl.impl.stacking.cc.manager.impl.RandomCCManagerImpl");
    }

    public CCIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities ibisCapabilities,
            Credentials credentials, byte[] applicationTag, PortType[] portTypes,
            String specifiedSubImplementation,
            CCIbisStarter ccIbisStarter)
            throws IbisCreationFailedException {

        starter = ccIbisStarter;
        
        int newNoPorts = portTypes.length + additionalPortTypes.size();
        PortType[] newPorts = new PortType[newNoPorts];
        
        /**
         * Need to take all ports and create new ones with the 
         * additional port capabilities.
         */
        for(int i = 0; i < portTypes.length; i++) {
            Set<String> portCap = new HashSet<String>(Arrays.asList(
                    portTypes[i].getCapabilities()));
            portCap.removeAll(removablePortCapabilities);
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

        try {
            TypedProperties typedProps = new TypedProperties(baseIbis.properties());
            
            int maxConns = typedProps.getIntProperty(s_max_conns,
                    default_max_conns);
            
            String cachingImplVersion = typedProps.getProperty(s_caching_version,
                    default_caching_version);
            
            buffer_capacity = typedProps.getIntProperty(s_buffer_size,
                    default_buffer_capacity);
            
            
            String fullyQualName = qualNamesMap.get(cachingImplVersion);
            
            if(fullyQualName == null) {
                StringBuilder msg = new StringBuilder();
                msg.append("Specified caching implementation version unavailable."
                        + " Try one from the following:\t");
                for(String impl : qualNamesMap.keySet()) {
                    msg.append(impl).append(", ");
                }
                throw new IbisCreationFailedException(msg.toString());
            }

            Class clazz = Class.forName(fullyQualName);
            Class[] paramTypes = {CCIbis.class, int.class};
            Object[] params = {this, maxConns};
            @SuppressWarnings("unchecked") 
            Constructor c = clazz.getConstructor(paramTypes);
            ccManager = (CCManager) c.newInstance(params);
        } catch (Exception ex) {
            throw new IbisCreationFailedException(ex);
        }
    }

    @Override
    public void end() throws IOException {        
        synchronized (this) {
            if (ended) {
                return;
            }
            ended = true;
        }
        
        ccManager.end();
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
        return new CCSendPort(portType, this, name, cU, props);
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
        return new CCReceivePort(portType, this, name, u, cU, props);
    }
}
