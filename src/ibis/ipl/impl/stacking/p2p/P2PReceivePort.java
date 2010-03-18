package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class P2PReceivePort implements ReceivePort{
	final ReceivePort base;
	
	/**
     * This class forwards upcalls with the proper receive port.
     */
    private static final class ConnectUpcaller
            implements ReceivePortConnectUpcall {
        P2PReceivePort port;
        ReceivePortConnectUpcall upcaller;

        public ConnectUpcaller(P2PReceivePort port,
                ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        public boolean gotConnection(ReceivePort me,
                SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        public void lostConnection(ReceivePort me,
                SendPortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }
    
    /**
     * This class forwards message upcalls with the proper message.
     */
    private static final class Upcaller implements MessageUpcall {
        MessageUpcall upcaller;
        P2PReceivePort port;

        public Upcaller(MessageUpcall upcaller, P2PReceivePort port) {
            this.upcaller = upcaller;
            this.port = port;
        }

        public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
            upcaller.upcall(new P2PReadMessage(m, port));
        }
    }
    
	public P2PReceivePort(PortType type, P2PIbis ibis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties) throws IOException{
		if (connectUpcall != null) {
            connectUpcall = new ConnectUpcaller(this, connectUpcall);
        }
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }
        base = ibis.base.createReceivePort(type, name, upcall, connectUpcall, properties);
	}
	
	@Override
	public void close() throws IOException {
		base.close();
	}

	@Override
	public void close(long timeoutMillis) throws IOException {
		base.close(timeoutMillis);
	}

	@Override
	public SendPortIdentifier[] connectedTo() {
		return base.connectedTo();
	}

	@Override
	public void disableConnections() {
		base.disableConnections();
	}

	@Override
	public void disableMessageUpcalls() {
		base.disableMessageUpcalls();
	}

	@Override
	public void enableConnections() {
		base.enableConnections();
	}

	@Override
	public void enableMessageUpcalls() {
		base.enableMessageUpcalls();
	}

	@Override
	public PortType getPortType() {
		return base.getPortType();
	}

	@Override
	public ReceivePortIdentifier identifier() {
		return base.identifier();
	}

	@Override
	public SendPortIdentifier[] lostConnections() {
		return base.lostConnections();
	}

	@Override
	public String name() {
		return base.name();
	}

	@Override
	public SendPortIdentifier[] newConnections() {
		return base.newConnections();
	}

	@Override
	public ReadMessage poll() throws IOException {
		return base.poll();
	}

	@Override
	public ReadMessage receive() throws IOException {
		return base.receive();
	}

	@Override
	public ReadMessage receive(long timeoutMillis) throws IOException {
		return base.receive(timeoutMillis);
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
