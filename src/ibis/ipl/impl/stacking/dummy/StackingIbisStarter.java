/* $Id$ */

package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisStarter;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StackingIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.stacking.dummy.StackingIbisStarter");

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
        "nickname.dummy"
    );

    private boolean matching;

    public StackingIbisStarter() {
    }
    
    public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) {
        this.capabilities = capabilities;
        this.portTypes = portTypes.clone();
        // See if the nickname is specified. Otherwise this Ibis is not
        // selected.
        matching = ibisCapabilities.matchCapabilities(capabilities);
        return matching;
    }

    public boolean isSelectable() {
        return true;
    }

    public boolean isStacking() {
        return true;
    }

    public CapabilitySet unmatchedIbisCapabilities() {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

    public PortType[] unmatchedPortTypes() {
        return portTypes.clone();
    }

    public Ibis startIbis(List<IbisStarter> stack,
            RegistryEventHandler registryEventHandler,
            Properties userProperties) {
        IbisStarter s = stack.remove(0);
        Ibis base = s.startIbis(stack, registryEventHandler, userProperties);
        return new StackingIbis(base);
    }
}
