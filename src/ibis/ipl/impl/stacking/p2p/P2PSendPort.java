package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class P2PSendPort implements SendPort {
	final SendPort base;

	private static final class DisconnectUpcaller implements
			SendPortDisconnectUpcall {
		P2PSendPort port;
		SendPortDisconnectUpcall upcaller;

		public DisconnectUpcaller(P2PSendPort port,
				SendPortDisconnectUpcall upcaller) {
			this.port = port;
			this.upcaller = upcaller;
		}

		public void lostConnection(SendPort me, ReceivePortIdentifier johnDoe,
				Throwable reason) {
			upcaller.lostConnection(port, johnDoe, reason);
		}
	}

	public P2PSendPort(PortType type, P2PIbis ibis, String name,
			SendPortDisconnectUpcall connectUpcall, Properties props)
			throws IOException {
		if (connectUpcall != null) {
			connectUpcall = new DisconnectUpcaller(this, connectUpcall);
			base = ibis.base.createSendPort(type, name, connectUpcall, props);
		} else {
			base = ibis.base.createSendPort(type, name, null, props);
		}
	}

	@Override
	public void close() throws IOException {
		base.close();
	}

	@Override
	public void connect(ReceivePortIdentifier receiver)
			throws ConnectionFailedException {
		connect(receiver, 0L, true);
	}

	@Override
	public void connect(ReceivePortIdentifier receiver, long timeoutMillis,
			boolean fillTimeout) throws ConnectionFailedException {
		connect(receiver, timeoutMillis, fillTimeout);
	}

	@Override
	public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
			String receivePortName) throws ConnectionFailedException {
		return connect(ibisIdentifier, receivePortName, 0L, true);
	}

	@Override
	public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
			String receivePortName, long timeoutMillis, boolean fillTimeout)
			throws ConnectionFailedException {
		return base.connect(ibisIdentifier, receivePortName, timeoutMillis,
				fillTimeout);
	}

	@Override
	public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
			throws ConnectionsFailedException {
		connect(receivePortIdentifiers, 0L, true);
	}

	@Override
	public void connect(ReceivePortIdentifier[] receivePortIdentifiers,
			long timeoutMillis, boolean fillTimeout)
			throws ConnectionsFailedException {
		base.connect(receivePortIdentifiers, timeoutMillis, fillTimeout);

	}

	@Override
	public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
			throws ConnectionsFailedException {
		return connect(ports, 0L, true);
	}

	@Override
	public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports,
			long timeoutMillis, boolean fillTimeout)
			throws ConnectionsFailedException {
		return base.connect(ports, timeoutMillis, fillTimeout);
	}

	@Override
	public ReceivePortIdentifier[] connectedTo() {
		return base.connectedTo();
	}

	@Override
	public void disconnect(ReceivePortIdentifier receiver) throws IOException {
		base.disconnect(receiver);
	}

	@Override
	public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName)
			throws IOException {
		base.disconnect(ibisIdentifier, receivePortName);

	}

	@Override
	public PortType getPortType() {
		return base.getPortType();
	}

	@Override
	public SendPortIdentifier identifier() {
		return base.identifier();
	}

	@Override
	public ReceivePortIdentifier[] lostConnections() {
		return base.lostConnections();
	}

	@Override
	public String name() {
		return base.name();
	}

	@Override
	public WriteMessage newMessage() throws IOException {
		return new P2PWriteMessage(base.newMessage(), this);
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
