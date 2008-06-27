package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;

public class MxIbis extends Ibis {
    
	private static Logger logger = Logger.getLogger(MxIbis.class);
	
	protected MxChannelFactory factory;
	protected IdManager<MxReceivePort> receivePortManager;
	
	public MxIbis(RegistryEventHandler registryHandler,
			IbisCapabilities capabilities, PortType[] portTypes,
			Properties userProperties) {
		super(registryHandler, capabilities, portTypes, userProperties);
		receivePortManager = new IdManager<MxReceivePort>();
		this.properties.checkProperties("ibis.ipl.impl.mx",
                new String[] {"ibis.ipl.impl.mx.mx"}, null, true);
		
	}

	@Override
	protected byte[] getData() throws IOException {
		factory = new MxChannelFactory(this);

		return factory.address.toBytes();
	}

	@Override
	protected void quit() {
       	factory.close();
        logger.info("MxIbis " + ident + " DE-initialized");
	}

	@Override
	protected ReceivePort doCreateReceivePort(PortType tp, String name,
			MessageUpcall u, ReceivePortConnectUpcall cu, Properties properties)
			throws IOException {
		// TODO maybe some portType-specific stuff later
		MxReceivePort result = new MxReceivePort(this, tp, name, u, cu, properties);
		try {
			receivePortManager.insert(result);
		} catch (Exception e) {
			// TODO Error: out of ReceivePortIdentifiers
			e.printStackTrace();
		}
		return result;
	}

	@Override
	protected SendPort doCreateSendPort(PortType tp, String name,
			SendPortDisconnectUpcall cu, Properties properties)
			throws IOException {
		// TODO maybe some portType-specific stuff later
		if (tp.hasCapability(PortType.COMMUNICATION_RELIABLE)) {
			if (logger.isDebugEnabled()) {
				logger.debug("creating reliable channel");
			}
			return new MxSendPort(this, tp, name, cu, properties, true);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("creating unreliable channel");
			}
			return new MxSendPort(this, tp, name, cu, properties, false);
		}
	}

	@Override
	public void poll() {
		// TODO empty implementation (just like the default)
		// Maybe we can come up with something smarts
	}
	
}
