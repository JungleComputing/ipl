/**
 * 
 */
package ibis.ipl.impl.mx;

import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;


public class MxIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger
    = Logger.getLogger("ibis.ipl.impl.mx.MxIbisStarter");

static final IbisCapabilities ibisCapabilities = new IbisCapabilities( 
	IbisCapabilities.ELECTIONS_STRICT,
	"nickname.mx"
);

/* unimplemented capabilities:
    IbisCapabilities.CLOSED_WORLD,
    IbisCapabilities.MEMBERSHIP_UNRELIABLE,
    IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
    IbisCapabilities.SIGNALS,
    IbisCapabilities.ELECTIONS_UNRELIABLE,
    
 */


static final PortType portCapabilities = new PortType(
	PortType.SERIALIZATION_BYTE,
	PortType.SERIALIZATION_DATA,
	PortType.COMMUNICATION_FIFO,
	PortType.COMMUNICATION_RELIABLE,
    PortType.CONNECTION_ONE_TO_ONE,
    PortType.RECEIVE_EXPLICIT
); 

/* unimplemented portcaps:
    PortType.SERIALIZATION_OBJECT_SUN,
    PortType.SERIALIZATION_OBJECT_IBIS, 
    PortType.SERIALIZATION_OBJECT,
    PortType.COMMUNICATION_RELIABLE,
    PortType.COMMUNICATION_NUMBERED,
    PortType.CONNECTION_DOWNCALLS,
    PortType.CONNECTION_UPCALLS,
    PortType.CONNECTION_TIMEOUT,
    PortType.CONNECTION_MANY_TO_MANY,
    PortType.CONNECTION_MANY_TO_ONE,
    PortType.CONNECTION_ONE_TO_MANY,
    PortType.RECEIVE_POLL,
    PortType.RECEIVE_AUTO_UPCALLS,
    PortType.RECEIVE_POLL_UPCALLS,
    PortType.RECEIVE_TIMEOUT
 */
	
private boolean matching;
private int unmatchedPortTypes;

	public MxIbisStarter() {
	}

	@Override
	public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) {
		this.capabilities = capabilities;
        this.portTypes = portTypes.clone();
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

	@Override
	public boolean isSelectable() {
		// TODO: decide later whether it should be selectable
		return false;
	}
	
	@Override
	public CapabilitySet unmatchedIbisCapabilities() {
		return capabilities.unmatchedCapabilities(ibisCapabilities);
	}

	@Override
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

	@Override
	public Ibis startIbis(RegistryEventHandler handler,
			Properties userProperties) {
		return new MxIbis(handler, capabilities, portTypes,
                userProperties);
	}

}
