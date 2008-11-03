/* $Id: LrmcIbisStarter.java 6500 2007-10-02 18:28:50Z ceriel $ */

package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LrmcIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.stacking.lrmc.LrmcIbisStarter");

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            "nickname.lrmc");

    private boolean matching;

    private PortType[] unmatched;

    public LrmcIbisStarter() {
        matching = false;
    }

    static boolean ourPortType(PortType tp) {
        return (tp.hasCapability(PortType.CONNECTION_MANY_TO_MANY) || tp
                .hasCapability(PortType.CONNECTION_ONE_TO_MANY))
                && !tp.hasCapability(PortType.COMMUNICATION_RELIABLE)
                && !tp.hasCapability(PortType.CONNECTION_UPCALLS)
                && !tp.hasCapability(PortType.CONNECTION_DOWNCALLS)
                && !tp.hasCapability(PortType.COMMUNICATION_NUMBERED)
                && !tp.hasCapability(PortType.COMMUNICATION_FIFO);
    }

    public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) {
        ArrayList<PortType> types = new ArrayList<PortType>();
        for (PortType tp : portTypes) {
            types.add(tp);
        }

        this.capabilities = capabilities;

        for (int i = 0; i < types.size(); i++) {
            PortType tp = types.get(i);
            if (ourPortType(tp)) {
                // Yes, we can do this ...
                if (!matching) {
                    // But we need this ....
                    types.add(LrmcIbis.baseType);
                }
                types.remove(i);
                i--;
                matching = true;
            }
        }
        this.portTypes = portTypes.clone();
        // See if the nickname is specified. If so, this ibis is selected,
        // regardless of the port types.
        if (ibisCapabilities.matchCapabilities(capabilities)) {
            matching = true;
        }
        unmatched = types.toArray(new PortType[types.size()]);
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
        return unmatched.clone();
    }

    public Ibis startIbis(List<IbisStarterInfo> stack,
            RegistryEventHandler registryEventHandler, Properties userProperties, String version) {
        return new LrmcIbis(stack, registryEventHandler, capabilities,
                portTypes, userProperties);
    }
}
