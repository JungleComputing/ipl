package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.SendPortConnectionInfo;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PSendPort implements SendPort {
	private PortType type;
	private final HashMap<IbisIdentifier, ReceivePortIdentifier[]> connections = new HashMap<IbisIdentifier, ReceivePortIdentifier[]>();
	private SendPortIdentifier sid;
	private P2PIbis ibis;
	private final String name;
	private boolean closed = false;
	private boolean messageInUse = false;
	private P2PWriteMessage message;
	private final byte[] buffer;
	private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

	protected static final Logger logger = LoggerFactory
			.getLogger("ibis.ipl.impl.smartsockets.SendPort");

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
		}

		this.ibis = ibis;
		this.type = type;
		this.name = name;
		this.sid = new ibis.ipl.impl.SendPortIdentifier(name,
				(ibis.ipl.impl.IbisIdentifier) ibis.identifier());

		buffer = new byte[DEFAULT_BUFFER_SIZE];
		message = new P2PWriteMessage(this, buffer);

	}

	@Override
	public synchronized void close() throws IOException {
		closed = true;
		notifyAll();
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

	private void addReceiver(IbisIdentifier ibisIdentifier,
			ReceivePortIdentifier receiver) {
		if (!connections.containsKey(ibisIdentifier)) {
			ReceivePortIdentifier[] temp = new ibis.ipl.impl.ReceivePortIdentifier[1];
			temp[0] = receiver;
			connections.put(ibisIdentifier, temp);
		} else {
			ReceivePortIdentifier[] temp = connections.get(ibisIdentifier);
			temp = Arrays.copyOf(temp, temp.length + 1);
			temp[temp.length - 1] = receiver;
		}
	}

	@Override
	public synchronized ReceivePortIdentifier connect(
			IbisIdentifier ibisIdentifier, String receivePortName,
			long timeoutMillis, boolean fillTimeout)
			throws ConnectionFailedException {
		ReceivePortIdentifier receiver = new ibis.ipl.impl.ReceivePortIdentifier(
				receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier);
		
		ibis.connect(ibisIdentifier, receivePortName, sid,
				type, timeoutMillis, fillTimeout);
		
		Byte response = ibis.getConnectionResponse();
		
		switch (response.byteValue()) {
		case P2PReceivePort.ALREADY_CONNECTED:
			throw new AlreadyConnectedException("Already connected",
					ibisIdentifier, receivePortName);
		case P2PReceivePort.NOT_PRESENT:
			throw new ConnectionFailedException("Receive Port not present",
					receiver);
		case P2PReceivePort.DENIED:
			throw new ConnectionRefusedException("Connection refused", receiver);
		case P2PReceivePort.TYPE_MISMATCH:
			throw new PortMismatchException("Ports of different tyoes",
					receiver);
		case P2PReceivePort.DISABLED:
			throw new ConnectionFailedException("Connections not enabled at receiver's side",
					receiver);
		case P2PReceivePort.NO_MANY_TO_X:
			throw new ConnectionFailedException("Many to X capability not enabled at receiver's side",
					receiver);
		}
		
		checkConnect(receiver);
		
		addReceiver(ibisIdentifier, receiver);
		return receiver;
	}

	private void checkConnect(ReceivePortIdentifier receiver) throws AlreadyConnectedException {
		if (connections.size() > 0
                && ! type.hasCapability(PortType.CONNECTION_ONE_TO_MANY)
                && ! type.hasCapability(PortType.CONNECTION_MANY_TO_MANY)) {
            throw new IbisConfigurationException("Sendport already has a "
                    + "connection and OneToMany or ManyToMany are not set");
        }

        if (isReceiverConnected(receiver)) {
            throw new AlreadyConnectedException("Already connected", receiver);
        }
		
	}

	@Override
	public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
			throws ConnectionsFailedException {
		connect(receivePortIdentifiers, 0L, true);
	}

	@Override
	public void connect(
			ReceivePortIdentifier[] receivePortIdentifiers, long timeoutMillis,
			boolean fillTimeout) throws ConnectionsFailedException {
		try {
			for (ReceivePortIdentifier id : receivePortIdentifiers) {
				connect(id.ibisIdentifier(), id.name(), timeoutMillis,
						fillTimeout);
			}
		} catch (ConnectionFailedException ex) {
			// FIXME: repair this, should throw an exception for failed
			// connections
			throw new ConnectionsFailedException();
		}
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

		ReceivePortIdentifier[] tmp = new ReceivePortIdentifier[ports.size()];

		int index = 0;

		for (Entry<IbisIdentifier, String> e : ports.entrySet()) {
			tmp[index++] = new ibis.ipl.impl.ReceivePortIdentifier(
					e.getValue(), (ibis.ipl.impl.IbisIdentifier) e.getKey());
		}

		connect(tmp);
		return tmp;
	}

	@Override
	public ReceivePortIdentifier[] connectedTo() {
		ReceivePortIdentifier[] receivers = new ibis.ipl.impl.ReceivePortIdentifier[1];

		Collection<ReceivePortIdentifier[]> c = connections.values();
		Iterator<ReceivePortIdentifier[]> itr = c.iterator();
		while (itr.hasNext()) {
			ReceivePortIdentifier[] temp = itr.next();
			int oldLength = receivers.length;
			receivers = Arrays
					.copyOf(receivers, receivers.length + temp.length);

			for (int i = 0; i < temp.length; i++) {
				receivers[i + oldLength] = temp[i];
			}
		}

		return receivers;
	}

	@Override
	public synchronized void disconnect(ReceivePortIdentifier receiver)
			throws IOException {
		if (connections.remove(receiver.ibisIdentifier()) == null) {
			throw new IOException("Not connected to:" + receiver);
		}
	}

	@Override
	public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName)
			throws IOException {
		disconnect(new ibis.ipl.impl.ReceivePortIdentifier(receivePortName,
				(ibis.ipl.impl.IbisIdentifier) ibisIdentifier));
	}

	@Override
	public PortType getPortType() {
		return type;
	}

	@Override
	public SendPortIdentifier identifier() {
		return sid;
	}

	@Override
	public ReceivePortIdentifier[] lostConnections() {
		// TODO: repair this, the same is done in smart sockets implementation
		return new ReceivePortIdentifier[0];
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public synchronized WriteMessage newMessage() throws IOException {
		while (!closed && messageInUse) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}

		if (closed) {
			throw new IOException("SendPort is closed");
		}

		messageInUse = false;
		return message;
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		// TODO: fix this, the same is done in SmartSockets
		return null;
	}

	@Override
	public Map<String, String> managementProperties() {
		// TODO: fix this, the same is done in SmartSockets
		return null;
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		// TODO: fix this, the same is done in SmartSockets
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		// TODO: fix this, the same is done in SmartSockets
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		// TODO: fix this, the same is done in SmartSockets
	}

	protected synchronized void finishedMessage() throws IOException {
		int length = (int) message.bytesWritten();
		byte[] buffer = this.buffer;

		try {
			send(buffer, length);
		} catch (Exception e) {
			logger.debug("Failed to send message to " + connections, e);
		}

		message.reset();
		messageInUse = false;
		notifyAll();

	}

	private void send(byte[] buffer, int length) throws IOException {
		try {
			ibis.send(buffer, length, this.sid, connections);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected synchronized boolean isReceiverConnected(
            ReceivePortIdentifier id) {
		ReceivePortIdentifier[] receivePorts = connections.get(id.ibisIdentifier());
        if (receivePorts != null) {
		for (ReceivePortIdentifier receivePort : receivePorts)
        	if (receivePort.equals(id)) {
        		return true;
        	}
        }
        return false;
    }

}
