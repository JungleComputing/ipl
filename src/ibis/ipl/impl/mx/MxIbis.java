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

/**
 * @author Timo van Kessel
 *
 */
public class MxIbis extends Ibis {

	public MxIbis(RegistryEventHandler registryHandler,
			IbisCapabilities capabilities, PortType[] portTypes,
			Properties userProperties) {
		super(registryHandler, capabilities, portTypes, userProperties);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ReceivePort doCreateReceivePort(PortType tp, String name,
			MessageUpcall u, ReceivePortConnectUpcall cu, Properties properties)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SendPort doCreateSendPort(PortType tp, String name,
			SendPortDisconnectUpcall cu, Properties properties)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected byte[] getData() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void quit() {
		// TODO Auto-generated method stub

	}

}
