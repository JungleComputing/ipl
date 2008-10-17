package ibis.ipl.impl.smartsockets;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.VirtualSocketAddress;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartSocketsUltraLightSendPort implements SendPort {

	// FIXME: This value is arbitrarily chosen... 
	private static final int DEFAULT_BUFFER_SIZE = 64*1024;
	
	protected static final Logger logger = 
		LoggerFactory.getLogger("ibis.ipl.impl.smartsockets.SendPort");

	private final PortType type;
	private final String name;
	private final Properties properties;	
	private final SmartSocketsIbis ibis;

	private final SendPortIdentifier sid;
	
	private boolean closed = false;

	private final SmartSocketsUltraLightWriteMessage message; 
	private final byte [] buffer;

	private boolean messageInUse = false;

	private final Set<ReceivePortIdentifier> connections = new HashSet<ReceivePortIdentifier>();
	
	SmartSocketsUltraLightSendPort(SmartSocketsIbis ibis, PortType type, String name, 
			Properties props) throws IOException {
		
		this.ibis = ibis;
		this.type = type;
		this.name = name;
		this.properties = props;
		
		sid = new ibis.ipl.impl.SendPortIdentifier(name, ibis.ident);
		
		buffer = new byte[DEFAULT_BUFFER_SIZE];		
		message = new SmartSocketsUltraLightWriteMessage(this, buffer);		
	}	
		
	public synchronized void close() throws IOException {
		closed = true;
		notifyAll();		
	}

	public synchronized void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {
		connections.add(receiver);
	}

	public void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
		connect(receiver);
	}

	public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName) throws ConnectionFailedException {
		ReceivePortIdentifier id = new ibis.ipl.impl.ReceivePortIdentifier(receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier);
		connect(id);
		return id; 
	}

	public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
		return connect(ibisIdentifier, receivePortName);
	}

	public void connect(ReceivePortIdentifier[] receivePortIdentifiers) throws ConnectionsFailedException {
		
		LinkedList<ConnectionFailedException> tmp = null;
		LinkedList<ReceivePortIdentifier> success = new LinkedList<ReceivePortIdentifier>();
		
		for (ReceivePortIdentifier id : receivePortIdentifiers) {
			try { 
				connect(id);
				success.add(id);
			} catch (ConnectionFailedException e) {

				if (tmp == null) { 
					tmp = new LinkedList<ConnectionFailedException>();
				}
				
				tmp.add(e);
			}
		}		

		if (tmp != null && tmp.size() > 0) { 
			ConnectionsFailedException c = new ConnectionsFailedException("Failed to connect");
			
			for (ConnectionFailedException ex : tmp) { 
				c.add(ex);
			}			
	
			c.setObtainedConnections(success.toArray(new ReceivePortIdentifier[success.size()]));
			throw c;
		}
	}

	public void connect(ReceivePortIdentifier[] receivePortIdentifiers, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
		connect(receivePortIdentifiers);
	}

	public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {

		ReceivePortIdentifier [] tmp = new ReceivePortIdentifier[ports.size()];
		
		int index = 0;
		
		for (Entry<IbisIdentifier, String> e : ports.entrySet()) { 
			tmp[index++] = new ibis.ipl.impl.ReceivePortIdentifier(e.getValue(), (ibis.ipl.impl.IbisIdentifier) e.getKey());
		}
		
		connect(tmp);
		return tmp;
	}

	public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
		return connect(ports);
	}

	public ReceivePortIdentifier[] connectedTo() {
		return connections.toArray(new ReceivePortIdentifier[0]);
	}

	public synchronized void disconnect(ReceivePortIdentifier receiver) throws IOException {
		if (!connections.remove(receiver)) { 
			throw new IOException("Not connected to " + receiver);
		}
	}

	public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName) throws IOException {
		disconnect(new ibis.ipl.impl.ReceivePortIdentifier(receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier));
	}

	public PortType getPortType() {
		return type;
	}

	public SendPortIdentifier identifier() {
		return sid;
	}

	public ReceivePortIdentifier[] lostConnections() {
		return new ReceivePortIdentifier[0];
	}

	public String name() {
		return name;
	}

	public synchronized WriteMessage newMessage() throws IOException {
		
		while (!closed && messageInUse) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore
			}			
		}

		if (closed) { 
			throw new IOException("Sendport is closed");
		}
		
		messageInUse = false;		
		return message;
	}

	public String getManagementProperty(String key) throws NoSuchPropertyException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, String> managementProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	public void printManagementProperties(PrintStream stream) {
		// TODO Auto-generated method stub
		
	}

	public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
		// TODO Auto-generated method stub
		
	}

	public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
		// TODO Auto-generated method stub
		
	}
	
	private void send(ReceivePortIdentifier id, byte [] data) throws UnknownHostException, MalformedAddressException { 
		
		ServiceLink link = ibis.getServiceLink();
		
		if (link != null) {
			ibis.ipl.impl.IbisIdentifier dst = (ibis.ipl.impl.IbisIdentifier) id.ibisIdentifier();
			VirtualSocketAddress a = VirtualSocketAddress.fromBytes(dst.getImplementationData(), 0);

			byte [][] message = new byte[2][];
			
			message[0] = ibis.ident.toBytes();
			message[1] = data;
	
			if (logger.isDebugEnabled()) { 
				logger.debug("Sending message to " + a);
			}
			
			link.send(a.machine(), a.hub(), id.name(), 0xDEADBEEF, message);  
		} else { 
			
			if (logger.isDebugEnabled()) { 
				logger.debug("No sericelink available");
			}
		}
	}

	public synchronized void finishedMessage() throws IOException {

		int len = (int) message.bytesWritten();
		
		byte [] m = buffer;
		
		if (len < buffer.length) { 
			m = Arrays.copyOfRange(buffer, 0, len);
		}
		
		for (ReceivePortIdentifier id : connections) { 
			try { 
				send(id, m);
			} catch (Exception e) {
				logger.debug("Failed to send message to " + id, e);
			}
		}
		
		message.reset();	
		messageInUse = false;
		notifyAll();
	}

	public synchronized void finishedMessage(IOException exception) throws IOException {
		message.reset();	
		messageInUse = false;
		notifyAll();
	} 	
}
