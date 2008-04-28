package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.mx.io.*;

/**
 * @author Timo van Kessel
 *
 */
public class MxIbis extends Ibis {

	protected MxEndpoint endpoint;
	
	public MxIbis(RegistryEventHandler registryHandler,
			IbisCapabilities capabilities, PortType[] portTypes,
			Properties userProperties) {
		super(registryHandler, capabilities, portTypes, userProperties);
		// TODO Auto-generated constructor stub
		
		// TODO open endpoint
		endpoint = new MxEndpoint();
		
		// TODO check properties
	}

	@Override
	protected ReceivePort doCreateReceivePort(PortType tp, String name,
			MessageUpcall u, ReceivePortConnectUpcall cu, Properties properties)
			throws IOException {
		// TODO maybe some portType-specific stuff later
		return new MxReceivePort(this, tp, name, u, cu, properties);
	}

	@Override
	protected SendPort doCreateSendPort(PortType tp, String name,
			SendPortDisconnectUpcall cu, Properties properties)
			throws IOException {
		// TODO maybe some portType-specific stuff later
		return new MxSendPort(this, tp, name, cu, properties);
	}

	@Override
	protected byte[] getData() throws IOException {
		// TODO Auto-generated method stub
		endpoint.getMyAddress().toString().getBytes();
		return null;
	}

	@Override
	protected void quit() {
		// TODO Auto-generated method stub

	}

}
