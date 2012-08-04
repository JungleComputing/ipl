package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.util.ArrayList;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CacheIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger = LoggerFactory.getLogger("ibis.ipl.impl.stacking.cache.CacheIbisStarter");
    
    /**
     * The almost same capabilities as SmartSocketsIbisStarter.
     */
    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.CLOSED_WORLD,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
            IbisCapabilities.MEMBERSHIP_UNRELIABLE, IbisCapabilities.SIGNALS,
            IbisCapabilities.ELECTIONS_UNRELIABLE,
            IbisCapabilities.ELECTIONS_STRICT, IbisCapabilities.MALLEABLE,
            IbisCapabilities.TERMINATION, 
            // my capability
            IbisCapabilities.CONNECTION_CACHING);
    static final PortType portCapabilities = new PortType(
            PortType.SERIALIZATION_OBJECT_SUN,
            PortType.SERIALIZATION_OBJECT_IBIS, PortType.SERIALIZATION_OBJECT,
            PortType.SERIALIZATION_DATA, PortType.SERIALIZATION_BYTE,
            PortType.COMMUNICATION_FIFO, 
            // my capability
            PortType.COMMUNICATION_TOTALLY_ORDERED_MULTICASTS,
            PortType.COMMUNICATION_RELIABLE, PortType.CONNECTION_DOWNCALLS,
            PortType.CONNECTION_UPCALLS, PortType.CONNECTION_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_ONE_TO_MANY, PortType.CONNECTION_ONE_TO_ONE,
            PortType.CONNECTION_LIGHT,
            /*
             * can't support ultra_ligth connection, because I enrich the ports
             * with connection upcalls, and this combination doesn't work.
             * PortType.CONNECTION_ULTRALIGHT,
             */
            PortType.CONNECTION_DIRECT, PortType.RECEIVE_POLL,
            PortType.RECEIVE_AUTO_UPCALLS, PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_POLL_UPCALLS, PortType.RECEIVE_TIMEOUT);    

    public CacheIbisStarter(String nickName, String iplVersion,
            String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

    @Override
    public boolean matches(IbisCapabilities capabilities, PortType[] types) {
        if (!capabilities.matchCapabilities(ibisCapabilities)) {
            return false;
        }
        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CapabilitySet unmatchedIbisCapabilities(
            IbisCapabilities capabilities, PortType[] types) {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

    @Override
    public PortType[] unmatchedPortTypes(IbisCapabilities capabilities,
            PortType[] types) {
        ArrayList<PortType> result = new ArrayList<PortType>();

        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                result.add(portType);
            }
        }
        return result.toArray(new PortType[0]);
    }

    @Override
    public Ibis startIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag,
            PortType[] portTypes, String specifiedSubImplementation)
            throws IbisCreationFailedException {
        return new CacheIbis(factory, registryEventHandler, userProperties,
                capabilities, credentials, applicationTag, portTypes,
                specifiedSubImplementation, this);
    }
}
