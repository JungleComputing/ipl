/* $Id: ReceivePort.java 4939 2006-12-14 19:02:20Z ceriel $ */

package ibis.impl;

import ibis.io.SerializationInput;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.Upcall;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.util.GetLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Implementation of the {@link ibis.ipl.ReceivePort} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class ReceivePort implements ibis.ipl.ReceivePort {

    /** Debugging output. */
    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.ReceivePort");

    // Possible results of a connection attempt.

    /** Connection attempt accepted. */
    public static final byte ACCEPTED = 0;

    /** Connection attempt denied by user code. */
    public static final byte DENIED = 1;

    /** Connection attempt disabled. */
    public static final byte DISABLED = 2;

    /** Sendport was already connected to this receiveport. */
    public static final byte ALREADY_CONNECTED = 3;

    /** PortType does not match. */
    public static final byte TYPE_MISMATCH = 4;

    /** Receiveport not found. */
    public static final byte NOT_PRESENT = 5;

    /** Receiveport already has a connection, and ManyToOne is not specified. */
    public static final byte NO_MANYTOONE = 6;

    /** The type of this port. */
    protected final PortType type;

    /** The name of this port. */
    protected final String name;

    /** Number of bytes read from messages of this port. */
    private long count = 0;

    /** Set when connections are enabled. */
    private boolean connectionsEnabled = false;

    /** Set when connection downcalls are supported. */
    private boolean connectionDowncalls = false;

    /** The identification of this receiveport. */
    protected final ReceivePortIdentifier ident;

    /**
     * The connections lost since the last call to {@link #lostConnections()}.
     */
    private ArrayList<SendPortIdentifier> lostConnections
        = new ArrayList<SendPortIdentifier>();

    /**
     * The new connections since the last call to {@link #newConnections()}.
     */
    private ArrayList<SendPortIdentifier> newConnections
        = new ArrayList<SendPortIdentifier>();

    /** Map for implementing the dynamic properties. */
    private Map<String, Object> props = new HashMap<String, Object>();

    /** Message upcall, if specified, or <code>null</code>. */
    protected Upcall upcall;

    /** Connection upcall handler, or <code>null</code>. */
    protected ReceivePortConnectUpcall connectUpcall;

    /** The current connections. */
    protected HashMap<SendPortIdentifier, ReceivePortConnectionInfo> connections
            = new HashMap<SendPortIdentifier, ReceivePortConnectionInfo>();

    /** Set when upcalls are enabled. */
    protected boolean allowUpcalls = false;

    /** The Ibis instance of this receive port. */
    protected Ibis ibis;

    /** Set when messages are numbered. */
    protected final boolean numbered;

    /** The serialization for this receive port. */
    protected final String serialization;

    /** Set when this port is closed. */
    protected boolean closed = false;

    /**
     * Constructs a <code>ReceivePort</code> with the specified parameters.
     * Note that all property checks are already performed in the
     * <code>PortType.createReceivePort</code> methods.
     * @param ibis the ibis instance.
     * @param type the port type.
     * @param name the name of the <code>ReceivePort</code>.
     * @param upcall the message upcall object, or <code>null</code>.
     * @param connectUpcall the connection upcall object, or <code>null</code>.
     * @param connectionDowncalls set when connection downcalls must be
     * supported.
     */
    protected ReceivePort(Ibis ibis, PortType type, String name, Upcall upcall,
            ReceivePortConnectUpcall connectUpcall,
            boolean connectionDowncalls) {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = new ReceivePortIdentifier(name, type.props, ibis.ident);
        this.upcall = upcall;
        this.connectUpcall = connectUpcall;
        this.connectionDowncalls = connectionDowncalls;
        this.numbered = type.props.isProp("communication", "Numbered");
        this.serialization = type.serialization;
        ibis.register(this);
        logger.debug(ibis.ident + ": ReceivePort '" + name + "' created");
    }

    public synchronized void enableUpcalls() {
        allowUpcalls = true;
        notifyAll();
    }

    public static String getString(int result) {
        switch(result) {
        case ACCEPTED:
            return "ACCEPTED";
        case DENIED:
            return "DENIED";
        case NO_MANYTOONE:
            return "NO_MANYTOONE";
        case DISABLED:
            return "DISABLED";
        case ALREADY_CONNECTED:
            return "ALREADY_CONNECTED";
        case TYPE_MISMATCH:
            return "TYPE_MISMATCH";
        case NOT_PRESENT:
            return "NOT_PRESENT";
        }
        return "UNKNOWN";
    }

    public synchronized void disableUpcalls() {
        allowUpcalls = false;
    }

    public synchronized ibis.ipl.SendPortIdentifier[] connectedTo() {
        return connections.keySet().toArray(new ibis.ipl.SendPortIdentifier[0]);
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized void resetCount() {
        count = 0;
    }

    public PortType getType() {
        return type;
    }

    public Map<String, Object> properties() {
        return props;
    }

    public synchronized Object getProperty(String key) {
        return props.get(key);
    }
    
    public synchronized void setProperties(Map<String, Object> properties) {
        props = properties;
    }
    
    public synchronized void setProperty(String key, Object val) {
        props.put(key, val);
    }
    
    public synchronized ibis.ipl.SendPortIdentifier[] lostConnections() {
        if (! connectionDowncalls) {
            throw new IbisConfigurationException("ReceivePort.lostConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.SendPortIdentifier[] result = lostConnections.toArray(
                new ibis.ipl.SendPortIdentifier[0]);
        lostConnections.clear();
        return result;
    }

    public synchronized ibis.ipl.SendPortIdentifier[] newConnections() {
        if (! connectionDowncalls) {
            throw new IbisConfigurationException("ReceivePort.newConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.SendPortIdentifier[] result = newConnections.toArray(
                new ibis.ipl.SendPortIdentifier[0]);
        newConnections.clear();
        return result;
    }

    public String name() {
        return name;
    }

    public ibis.ipl.ReceivePortIdentifier identifier() {
        return ident;
    }

    public synchronized void enableConnections() {
        connectionsEnabled = true;
    }

    public synchronized void disableConnections() {
        connectionsEnabled = false;
    }

    public ReadMessage receive() throws IOException {
        return receive(-1);
    }

    public ReadMessage receive(long timeout) throws IOException {
        if (upcall != null) {
            throw new IbisConfigurationException(
                    "Configured Receiveport for upcalls, downcall not allowed");
        }
        if (timeout > 0 &&
                ! type.props.isProp("communication", "ReceiveTimeout")) {
            throw new IbisConfigurationException(
                    "This port is not configured for receive() with timeout");
        }

        return getMessage(timeout);
    }

    public final void close() {
        close(0L);
    }

    public void close(long timeout) {
        if (logger.isDebugEnabled()) {
            logger.debug("Receiveport " + name + ": closing");
        }
        disableConnections();
        synchronized(this) {
            closed = true;
            notifyAll();
        }
        closePort(timeout);
        ibis.deRegister(this);
    }

    public synchronized byte connectionAllowed(SendPortIdentifier id) {
        if (isConnectedTo(id)) {
            return ALREADY_CONNECTED;
        }
        if (! id.type().equals(type.props)) {
            return TYPE_MISMATCH;
        }
        if (connectionsEnabled) {
            if (connections.size() != 0 &&
                    ! type.props.isProp("communication", "ManyToOne")) {
                return DENIED;
            }
            if (connectionDowncalls) {
                newConnections.add(id);
            } else if (connectUpcall != null) {
                if (!connectUpcall.gotConnection(this, id)) {
                    return DENIED;
                }
            }
            return ACCEPTED;
        }
        return DISABLED;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof ReceivePort) {
            ReceivePort other = (ReceivePort) obj;
            return name.equals(other.name);
        } else if (obj instanceof String) {
            String s = (String) obj;
            return s.equals(name);
        } else {
            return false;
        }
    }

    public synchronized boolean isConnectedTo(SendPortIdentifier id) {
        return getInfo(id) != null;
    }

    public ReadMessage poll() throws IOException {
        if (! type.properties().isProp("communication", "Poll")) {
            throw new IbisConfigurationException(
                    "Receiveport not configured for polls");
        }
        return doPoll();
    }

    synchronized void addCount(long cnt) {
        count += cnt;
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Notifies this receiveport that the connection associated with the
     * specified sendport must be assumed to be lost, due to the specified
     * reason. It updates the administration, and performs the
     * lostConnection upcall, if required.
     * @param id the identification of the sendport.
     * @param e the cause of the lost connection.
     */
    protected void lostConnection(SendPortIdentifier id,
            Throwable e) {
        if (connectionDowncalls) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } else if (connectUpcall != null) {
            if (e == null) {
                e = new Exception("sender closed connection");
            }
            connectUpcall.lostConnection(this, id, e);
        }
        removeInfo(id);
    }

    /**
     * Returns the connection information for the specified sendport identifier.
     * @param id the identification of the sendport.
     * @return the connection information, or <code>null</code> if not
     * present.
     */
    protected synchronized ReceivePortConnectionInfo getInfo(
            SendPortIdentifier id) {
        return connections.get(id);
    }

    /**
     * Adds a connection entry for the specified sendport identifier,
     * and notifies, for possible waiters on a new connection.
     * @param id the identification of the sendport.
     * @param info the associated connection information.
     */
    protected synchronized void addInfo(SendPortIdentifier id,
            ReceivePortConnectionInfo info) {
        connections.put(id, info);
        notifyAll();
    }

    /**
     * Removes the connection entry for the specified sendport identifier.
     * If after this there are no connections left, a notify is done.
     * A {@link #closePort} can wait for this to happen.
     * @param id the identification of the sendport.
     * @return the removed connection.
     */
    protected synchronized ReceivePortConnectionInfo removeInfo(
            SendPortIdentifier id) {
        ReceivePortConnectionInfo info = connections.remove(id);
        if (connections.size() == 0) {
            notifyAll();
        }
        return info;
    }

    /**
     * Returns an array with entries for each connection.
     * @return the connections.
     */
    protected synchronized ReceivePortConnectionInfo[] connections() {
        return connections.values().toArray(new ReceivePortConnectionInfo[0]);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be implemented by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Implementation-dependent part of the {@link #poll()} method.
     * @exception IOException is thrown in case of trouble.
     * @return a new {@link ReadMessage} or <code>null</code>.
     */
    protected abstract ReadMessage doPoll() throws IOException;

    /**
     * Waits for all connections to close. If the specified timeout is larger
     * than 0, the implementation waits for the specified time, and then
     * forcibly closes all connections.
     * @param timeout the timeout in milliseconds.
     */
    protected abstract void closePort(long timeout);

    /**
     * Waits for a new message and returns it. If the specified timeout is
     * larger than 0, the implementation waits for the specified time,
     * and returns <code>null</code> if a message did not arrive within
     * this time.
     * @param timeout the timeout in milliseconds.
     * @exception IOException is thrown in case of trouble.
     * @return the new message, or <code>null</code>.
     */
    protected abstract ReadMessage getMessage(long timeout) throws IOException;

    /**
     * Notifies the port that {@link ReadMessage#finish()} was called on the
     * specified message. The port should prepare for a new message.
     * @param r the message.
     */
    protected abstract void finishMessage(ReadMessage r);

    /**
     * Notifies the port that {@link ReadMessage#finish(IOException)}
     * was called on the specified message.
     * The port should close the connection, with the specified reason.
     * @param r the message.
     * @param e the Exception.
     */
    protected abstract void finishMessage(ReadMessage r, IOException e);
}
