package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.Manageable;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PReceivePort extends Manageable implements ReceivePort, Runnable {
	private P2PIbis ibis;

	protected static final Logger logger = LoggerFactory
			.getLogger("ibis.ipl.impl.smartsockets.ReceivePort");

	/** current connections **/
	protected Vector<SendPortIdentifier> connections = new Vector<SendPortIdentifier>();

	/** The type of this port. */
	public final PortType type;

	/** The name of this port. */
	public final String name;

	/** The identification of this receiveport. */
	public final ReceivePortIdentifier ident;

	/** Message upcall, if specified, or <code>null</code>. */
	protected MessageUpcall upcall;

	/** Set when upcalls are enabled. */
	protected boolean allowUpcalls = false;

	/** Set when connections are enabled. */
	private boolean connectionsEnabled = false;

	/** Set when connection downcalls are supported. */
	private boolean connectionDowncalls = false;

	/** Set when this port is closed. */
	protected boolean closed = false;

	/** Connection upcall handler, or <code>null</code>. */
	protected ReceivePortConnectUpcall connectUpcall;

	/** The current message. */
	protected ReadMessage message = null;

	private LinkedBlockingQueue<P2PReadMessage> messages = new LinkedBlockingQueue<P2PReadMessage>();

	/**
	 * Set when the current message has been delivered. Only used for explicit
	 * receive.
	 */
	protected boolean delivered = false;

	/**
	 * The connections lost since the last call to {@link #lostConnections()}.
	 */
	private Vector<SendPortIdentifier> lostConnections = new Vector<SendPortIdentifier>();

	/**
	 * The new connections since the last call to {@link #newConnections()}.
	 */
	private Vector<SendPortIdentifier> newConnections = new Vector<SendPortIdentifier>();

	/** connection error codes **/

	/** Connection attempt accepted. */
	public static final byte ACCEPTED = 0;

	/** Connection attempt denied by user code. */
	public static final byte DENIED = 1;

	/** Connection attempt disabled. */
	public static final byte DISABLED = 2;

	/** Sendport was already connected to this receiveport. */
	public static final byte ALREADY_CONNECTED = 3;

	/** Port type does not match. */
	public static final byte TYPE_MISMATCH = 4;

	/** Receiveport not found. */
	public static final byte NOT_PRESENT = 5;

	/** Receiveport already has a connection, and ManyToOne is not specified. */
	public static final byte NO_MANY_TO_X = 6;

	public P2PReceivePort(PortType type, P2PIbis ibis, String name,
			MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
			Properties properties) throws IOException {
		this.upcall = upcall;
		this.connectUpcall = connectUpcall;

		this.ibis = ibis;
		this.type = type;
		this.name = name;
		this.ident = ibis.createReceivePortIdentifier(name, ibis.identifier());

		this.connectionDowncalls = type
				.hasCapability(PortType.CONNECTION_DOWNCALLS);

		ibis.register(this);
	}

	@Override
	public void close() throws IOException {
		close(0L);
	}

	@Override
	public void close(long timeoutMillis) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Receiveport " + name + ": closing");
		}
		synchronized (this) {
			if (closed) {
				throw new IOException("Port already closed");
			}
			connectionsEnabled = false;
			closed = true;
			notifyAll();
		}

		// TODO: wait until all connections are closed, or maybe there is no
		// need for waiting....
		ibis.deRegister(this);
	}

	private synchronized boolean getClosed() {
		return closed;
	}

	@Override
	public SendPortIdentifier[] connectedTo() {
		return connections.toArray(new ibis.ipl.SendPortIdentifier[0]);
	}

	@Override
	public synchronized void disableConnections() {
		connectionsEnabled = false;
	}

	@Override
	public synchronized void disableMessageUpcalls() {
		allowUpcalls = false;
	}

	@Override
	public synchronized void enableConnections() {
		connectionsEnabled = true;

	}

	@Override
	public synchronized void enableMessageUpcalls() {
		allowUpcalls = true;
		notifyAll();
	}

	@Override
	public PortType getPortType() {
		return type;
	}

	@Override
	public ReceivePortIdentifier identifier() {
		return this.ident;

	}

	@Override
	public SendPortIdentifier[] lostConnections() {
		if (!connectionDowncalls) {
			throw new IbisConfigurationException(
					"ReceivePort.lostConnections()"
							+ " called but connectiondowncalls not configured");
		}
		ibis.ipl.SendPortIdentifier[] result = lostConnections
				.toArray(new ibis.ipl.SendPortIdentifier[0]);
		lostConnections.clear();
		return result;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public SendPortIdentifier[] newConnections() {
		if (!connectionDowncalls) {
			throw new IbisConfigurationException("ReceivePort.newConnections()"
					+ " called but connectiondowncalls not configured");
		}
		ibis.ipl.SendPortIdentifier[] result = newConnections
				.toArray(new ibis.ipl.SendPortIdentifier[0]);
		newConnections.clear();
		return result;
	}

	@Override
	public ReadMessage poll() throws IOException {
		if (!type.hasCapability(PortType.RECEIVE_POLL)) {
			throw new IbisConfigurationException(
					"Receiveport not configured for polls");
		}
		return doPoll();
	}

	protected ReadMessage doPoll() throws IOException {
		Thread.yield(); // Give other thread a chance to deliver

		if (upcall != null) {
			return null;
		}

		return messages.poll();
		
	}

	@Override
	public ReadMessage receive() throws IOException {
		return receive(0);
	}

	@Override
	public ReadMessage receive(long timeout) throws IOException {
		if (upcall != null) {
			throw new IbisConfigurationException(
					"Configured Receiveport for upcalls, downcall not allowed");
		}

		if (timeout < 0) {
			throw new IOException("timeout must be a non-negative number");
		}
		if (timeout > 0 && !type.hasCapability(PortType.RECEIVE_TIMEOUT)) {
			throw new IbisConfigurationException(
					"This port is not configured for receive() with timeout");
		}

		if (closed) {
			throw new IOException("receive() on closed port");
		}

		return getMessage(timeout);
	}

	public P2PReadMessage getMessage(long timeout) {

		while (!closed) {
			if (timeout > 0) {
				try {
					return messages.poll(timeout, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					return messages.take();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		return null;
	}

	protected void newUpcallThread() {
		ThreadPool.createNew(this, "ConnectionHandler");
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

	private void performUpcall(P2PReadMessage message) {

		if (waitUntilUpcallAllowed()) {
			try {
				// Notify the message that is is processed from an upcall,
				// so that finish() calls can be detected.
				message.setInUpcall(true);
				upcall.upcall(message);
			} catch (IOException e) {
				if (!message.isFinished()) {
					message.finish(e);
					return;
				}
				logger
						.error(
								"Got unexpected exception in upcall, continuing ...",
								e);
			} catch (Throwable t) {
				if (!message.isFinished()) {
					IOException ioex = new IOException("Got Throwable: "
							+ t.getMessage());
					ioex.initCause(t);
					message.finish(ioex);
				}
				return;
			} finally {
				message.setInUpcall(false);
			}
		}
	}

	/**
	 * deliver a message, if upcalls are enabled, deliver message to upcaller,
	 * otherwise append to queue, when user calls receive, extract first message
	 * grom queue
	 * 
	 * @param source
	 * @param data 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void deliverMessage(SendPortIdentifier source, byte[] data)  {
		try {
			P2PReadMessage msg = new P2PReadMessage(this, source, data);
			messages.put(msg);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void updateProperties() {
		// TODO Ask about management properties
	}

	@Override
	public void run() {
		while (!getClosed()) {
			P2PReadMessage message = getMessage(0L);

			if (message != null) {
				performUpcall(message);

				if (message.finishCalledInUpcall()) {
					// A new thread has take our place
					return;
				}
			}
		}

	}

	public byte handleConnectionRequest(SendPortIdentifier source,
			PortType senderType) {

		if (connections.contains(source)) {
			return P2PReceivePort.ALREADY_CONNECTED;
		}

		if (!type.equals(senderType)) {
			return P2PReceivePort.TYPE_MISMATCH;
		}

		if (!connectionsEnabled) {
			return P2PReceivePort.DISABLED;
		}

		if (connections.size() != 0
				&& !(type.hasCapability(PortType.CONNECTION_MANY_TO_ONE) || type
						.hasCapability(PortType.CONNECTION_MANY_TO_MANY))) {
			return P2PReceivePort.NO_MANY_TO_X;
		}

		if (upcall != null) {
			if (!connectUpcall.gotConnection(this, source)) {
				return P2PReceivePort.DENIED;
			}
		}

		if (connectionDowncalls) {
			newConnections.add(source);
			connections.add(source);
		}

		return P2PReceivePort.ACCEPTED;
	}
}
