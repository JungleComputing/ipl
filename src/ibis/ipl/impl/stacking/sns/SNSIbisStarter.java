package ibis.ipl.impl.stacking.sns;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SNSIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.stacking.sns.SNSIbisStarter");

    public SNSIbisStarter(String nickName, String iplVersion,
            String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

    public boolean matches(IbisCapabilities capabilities, PortType[] types) {
        return true;
    }

    public CapabilitySet unmatchedIbisCapabilities(
            IbisCapabilities capabilities, PortType[] types) {
        return new CapabilitySet();
    }

    public PortType[] unmatchedPortTypes(IbisCapabilities capabilities,
            PortType[] types) {
        return new PortType[0];
    }

    public Ibis startIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,  Credentials credentials,
            byte[] applicationTag, PortType[] portTypes, String specifiedSubImplementation) throws IbisCreationFailedException {
        return new SNSIbis(factory, registryEventHandler,
                userProperties, capabilities, credentials, applicationTag, portTypes, specifiedSubImplementation, this);
    }
}
