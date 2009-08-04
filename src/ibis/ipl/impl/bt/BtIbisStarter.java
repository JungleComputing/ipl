/* $Id: TcpIbisStarter.java 10913 2009-05-14 16:45:26Z nick $ */

package ibis.ipl.impl.bt;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BtIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.tcp.TcpIbisStarter");

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
        IbisCapabilities.CLOSED_WORLD,
        IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
        IbisCapabilities.MEMBERSHIP_UNRELIABLE,
        IbisCapabilities.SIGNALS,
        IbisCapabilities.ELECTIONS_UNRELIABLE,
        IbisCapabilities.ELECTIONS_STRICT,
        IbisCapabilities.MALLEABLE,
        IbisCapabilities.TERMINATION
    );

    static final PortType portCapabilities = new PortType(
        PortType.SERIALIZATION_OBJECT_SUN,
        PortType.SERIALIZATION_OBJECT_IBIS, 
        PortType.SERIALIZATION_OBJECT,
        PortType.SERIALIZATION_DATA,
        PortType.SERIALIZATION_BYTE,
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


    public BtIbisStarter(String nickName, String iplVersion,
            String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

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

    public CapabilitySet unmatchedIbisCapabilities(
            IbisCapabilities capabilities, PortType[] types) {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

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
    
    public Ibis startIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler, Properties userProperties,
            IbisCapabilities capabilities, Credentials credentials, byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation) 
    throws IbisCreationFailedException {
    	return new BtIbis(registryEventHandler, capabilities, credentials,
                applicationTag, portTypes, userProperties, this);
    }
}
