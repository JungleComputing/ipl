/* $Id: LrmcIbisStarter.java 6500 2007-10-02 18:28:50Z ceriel $ */

package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisStarter;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.IbisFactory.ImplementationInfo;

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

    private final PortType[] unmatched;
    private final boolean matching;

    public LrmcIbisStarter(IbisCapabilities caps, PortType[] portTypes,
            IbisFactory.ImplementationInfo info) {
        super(caps, portTypes, info);
        
        // we match if either one of the porttypes matches, or this
        // Ibis was requested explicitly.
        boolean m = false;
        ArrayList<PortType> types = new ArrayList<PortType>();
        for (PortType tp : portTypes) {
            types.add(tp);
        }
        for (int i = 0; i < types.size(); i++) {
            PortType tp = types.get(i);
            if (ourPortType(tp)) {
                // Yes, we can do this ...
                if (!m) {
                    // But we need this ....
                    types.add(LrmcIbis.baseType);
                }
                types.remove(i);
                i--;
                m = true;
            }
        }
        if (ibisCapabilities.matchCapabilities(capabilities)) {
            m = true;
        }
        matching = m;
        unmatched = types.toArray(new PortType[types.size()]);
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

    public boolean matches() {
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

    public Ibis startIbis(List<IbisStarter> stack,
            RegistryEventHandler registryEventHandler, Properties userProperties,ImplementationInfo info) {
        return new LrmcIbis(stack, registryEventHandler, capabilities,
                portTypes, userProperties, info);
    }
}
