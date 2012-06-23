package ibis.ipl.impl.smartsockets;

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.servicelink.CallBack;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartSocketsUltraLightReceivePort implements ReceivePort, CallBack, Runnable {

	protected static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.smartsockets.ReceivePort");

	private final PortType type;
	private final String name;
	private final MessageUpcall upcall;	
// 	private final Properties properties;	
	private final ReceivePortIdentifier id;
// 	private final SmartSocketsIbis ibis;
	
	private boolean allowUpcalls = false;
	private boolean closed = false;


	private final LinkedList<SmartSocketsUltraLightReadMessage> messages = 
		new LinkedList<SmartSocketsUltraLightReadMessage>();

	SmartSocketsUltraLightReceivePort(SmartSocketsIbis ibis, PortType type, 
			String name, MessageUpcall upcall, Properties properties) throws IOException {

// 		this.ibis = ibis;
		this.type = type;
		this.name = name; 
		this.upcall = upcall;
		// this.properties = properties;		
		this.id = new ibis.ipl.impl.ReceivePortIdentifier(name, ibis.ident);

		ServiceLink link = ibis.getServiceLink();
		
		if (link == null) { 
			throw new IOException("No ServiceLink available");
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Registering ultralight receive port " + name);
		}
		
		link.register(name, this);
		
		if (type.hasCapability(PortType.RECEIVE_AUTO_UPCALLS) && upcall != null) { 
			ThreadPool.createNew(this, "ConnectionHandler");
		}		
	}

	public synchronized void close() {
		closed = true;
		notifyAll();
	}

	public void close(long timeoutMillis) {
		close();
	}

	private synchronized boolean getClosed() {
		return closed;
	}

	public SendPortIdentifier[] connectedTo() {
		return new SendPortIdentifier[0];
	}

	public void disableConnections() {
		// empty ? 
	}

	public void enableConnections() {
		// empty ? 
	}

	public synchronized void disableMessageUpcalls() {
		allowUpcalls = false;
	}

	public synchronized void enableMessageUpcalls() {
		// TODO Auto-generated method stub
		allowUpcalls = true;
		notifyAll();
	}

	public PortType getPortType() {
		return type;
	}

	public ReceivePortIdentifier identifier() {
		return id;
	}

	public SendPortIdentifier[] lostConnections() {
		return new SendPortIdentifier[0];
	}

	public String name() {
		return name;
	}

	public SendPortIdentifier[] newConnections() {
		return new SendPortIdentifier[0];
	}

	public ReadMessage poll() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public ReadMessage receive() throws IOException {
		return receive(0L);
	}

	public ReadMessage receive(long timeoutMillis) throws IOException {
		return getMessage(timeoutMillis);
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

	public void gotMessage(DirectSocketAddress src, DirectSocketAddress srcProxy, int opcode, 
			boolean returnToSender, byte [][] message) {

		logger.debug("Got message from + " + src);
		
		if (returnToSender || opcode != 0xDEADBEEF || message == null || message.length == 0 
				|| message[0] == null || message[0].length == 0) {			
			logger.warn("Received malformed message from " + src.toString() + " (" 
					+ returnToSender + ", " + opcode + ", " + (message== null) + ", " 
					+ message.length + ", " + (message[0] == null) + ", " + message[0].length + ")");
			return;
		}

		IbisIdentifier source = null;

		try { 
			source = new IbisIdentifier(message[0]);
	
			if (logger.isDebugEnabled()) {
				logger.debug("Message was send by " + source);
			} 
		
		} catch (Exception e) {
			logger.warn("Message from contains malformed IbisIdentifier", e);
			return;
		}

		SmartSocketsUltraLightReadMessage rm = null;

		try { 
			rm = new SmartSocketsUltraLightReadMessage(this, 
					new SendPortIdentifier("anonymous", source), message[1]);
		} catch (Exception e) {
			logger.warn("Message from contains malformed data", e);
			return;
		}

		synchronized (this) {
			messages.addLast(rm);
			notifyAll();
		}
	}    

	private synchronized SmartSocketsUltraLightReadMessage getMessage(long timeout) { 

		long endTime = System.currentTimeMillis() + timeout;

		while (!closed && messages.size() == 0) {			
			if (timeout > 0) { 			
				long waitTime = endTime - System.currentTimeMillis();
				
				if (waitTime <= 0) { 
					break;
				}
				
				try { 
					wait(waitTime);
				} catch (InterruptedException e) {
					// ignore
				}				
			} else { 
				try { 
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

		if (closed || messages.size() == 0) { 
			return null;
		}

		return messages.removeFirst();		
	}

	private synchronized boolean waitUntilUpcallAllowed() { 

		while (!closed && !allowUpcalls) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignored
			}
		}

		return !closed;
	}

	private void performUpcall(SmartSocketsUltraLightReadMessage message) {

		if (waitUntilUpcallAllowed()) { 
			try {
				// Notify the message that is is processed from an upcall,
				// so that finish() calls can be detected.
				message.setInUpcall(true);
				upcall.upcall(message);
			} catch(IOException e) {
				if (!message.isFinished()) {
					message.finish(e);
					return;
				}
				logger.error("Got unexpected exception in upcall, continuing ...", e);
			} catch(Throwable t) {
				if (!message.isFinished()) {
					IOException ioex = 
						new IOException("Got Throwable: " + t.getMessage());
					ioex.initCause(t);
					message.finish(ioex);
				}
				return;
			} finally {
				message.setInUpcall(false);
			}
		}
	}

	protected void newUpcallThread() {
		ThreadPool.createNew(this, "ConnectionHandler");
	}

	public void run() { 
		while (!getClosed()) { 
			SmartSocketsUltraLightReadMessage message = getMessage(0L);

			if (message != null) { 
				performUpcall(message);

				if (message.finishCalledInUpcall()) {
					// A new thread has take our place
					return;
				}
			}
		}
	}
}
