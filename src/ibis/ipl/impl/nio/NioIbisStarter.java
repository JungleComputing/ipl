/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NioIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.nio.NioIbisStarter");

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.CLOSED_WORLD,
            IbisCapabilities.MEMBERSHIP_UNRELIABLE,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
            IbisCapabilities.SIGNALS,
            IbisCapabilities.ELECTIONS_UNRELIABLE,
            IbisCapabilities.ELECTIONS_STRICT,
            "nickname.nio"
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
            PortType.RECEIVE_TIMEOUT,
            "sendport.blocking",
            "sendport.nonblocking",
            "sendport.thread",
            "receiveport.blocking",
            "receivport.nonblocking",
            "receiveport.thread"
    );    
    
    private boolean matching;
    private int unmatchedPortTypes;

    public NioIbisStarter() {
    }
    
    public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) { 
        this.capabilities = capabilities;
        this.portTypes = portTypes;
        matching = true;
        if (! capabilities.matchCapabilities(ibisCapabilities)) {
            matching = false;
        }
        for (PortType pt : portTypes) {
            if (! pt.matchCapabilities(portCapabilities)) {
                unmatchedPortTypes++;
                matching = false;
            }
        }
        return matching;
    }

    public boolean isSelectable() {
        return false;
    }

    public CapabilitySet unmatchedIbisCapabilities() {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

    public PortType[] unmatchedPortTypes() {
        PortType[] unmatched = new PortType[unmatchedPortTypes];
        int i = 0;
        for (PortType pt : portTypes) {
            if (! pt.matchCapabilities(portCapabilities)) {
                unmatched[i++] = pt;
            }
        }
        return unmatched;
    }

    public Ibis startIbis(RegistryEventHandler registryEventHandler,
            Properties userProperties, String version) {
        return new NioIbis(registryEventHandler, capabilities, portTypes,
                userProperties, version);
    }
}
