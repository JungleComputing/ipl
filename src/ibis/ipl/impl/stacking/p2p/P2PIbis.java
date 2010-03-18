package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class P2PIbis implements Ibis {

	Ibis base;

	public P2PIbis(IbisFactory factory,
			RegistryEventHandler registryEventHandler,
			Properties userProperties, IbisCapabilities capabilities,
			Credentials credentials, byte[] applicationTag,
			PortType[] portTypes, String specifiedSubImplementation,
			P2PIbisStarter p2pIbisStarter) throws IbisCreationFailedException {

		base = factory.createIbis(registryEventHandler, capabilities,
				userProperties, credentials, applicationTag, portTypes,
				specifiedSubImplementation);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName) throws IOException {
		return createReceivePort(portType, receivePortName, null, null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName, MessageUpcall messageUpcall)
			throws IOException {

		return createReceivePort(portType, receivePortName, messageUpcall,
				null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName,
			ReceivePortConnectUpcall receivePortConnectUpcall)
			throws IOException {

		return createReceivePort(portType, receivePortName, null,
				receivePortConnectUpcall, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName, MessageUpcall messageUpcall,
			ReceivePortConnectUpcall receivePortConnectUpcall,
			Properties properties) throws IOException {
		return new P2PReceivePort(portType, this, receivePortName,
				messageUpcall, receivePortConnectUpcall, properties);
	}

	@Override
	public SendPort createSendPort(PortType portType) throws IOException {
		return createSendPort(portType, null, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String sendPortName)
			throws IOException {
		return createSendPort(portType, sendPortName, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String sendPortName,
			SendPortDisconnectUpcall sendPortDisconnectUpcall,
			Properties properties) throws IOException {
		return new P2PSendPort(portType, this, sendPortName,
				sendPortDisconnectUpcall, properties);
	}

	@Override
	public void end() throws IOException {
		base.end();
	}

	@Override
	public String getVersion() {
		return "Stacking P2P Ibis over " + base.getVersion();
	}

	@Override
	public IbisIdentifier identifier() {
		return base.identifier();
	}

	@Override
	public void poll() throws IOException {
		base.poll();
	}

	@Override
	public Properties properties() {
		return base.properties();
	}

	@Override
	public Registry registry() {
		return base.registry();
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return base.getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {
		return base.managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		base.printManagementProperties(stream);
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		base.setManagementProperties(properties);
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		base.setManagementProperty(key, value);
	}
}
