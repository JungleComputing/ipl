package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisStarter;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PIbisStarter extends IbisStarter {

	static final Logger logger = LoggerFactory.getLogger(P2PIbisStarter.class);

	public P2PIbisStarter(String nickName, String iplVersion,
			String implementationVersion) {
		super(nickName, iplVersion, implementationVersion);
	}

	@Override
	public boolean matches(IbisCapabilities capabilities, PortType[] portTypes) {
		return true;
	}

	/* 
	 * [TODO] implement here specific node join operations
	 * @see ibis.ipl.IbisStarter#startIbis(ibis.ipl.IbisFactory, ibis.ipl.RegistryEventHandler, java.util.Properties, ibis.ipl.IbisCapabilities, ibis.ipl.Credentials, byte[], ibis.ipl.PortType[], java.lang.String)
	 */
	@Override
	public Ibis startIbis(IbisFactory factory, RegistryEventHandler handler,
			Properties userProperties, IbisCapabilities capabilities,
			Credentials credentials, byte[] applicationTag,
			PortType[] portTypes, String specifiedSubImplementation)
			throws IbisCreationFailedException {
		
		return new P2PIbis(factory, handler, userProperties, capabilities, credentials, applicationTag, portTypes, specifiedSubImplementation, this);
	}

	@Override
	public CapabilitySet unmatchedIbisCapabilities(
			IbisCapabilities capabilities, PortType[] portTypes) {
		// TODO specify unmatched capabilities
		return new CapabilitySet();
	}

	@Override
	public PortType[] unmatchedPortTypes(IbisCapabilities capabilities,
			PortType[] portTypes) {
		// TODO specify unmatched port types
		return new PortType[0];
	}

}
