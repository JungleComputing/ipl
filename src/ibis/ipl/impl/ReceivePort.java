/* $Id$ */

package ibis.ipl.impl;

import ibis.io.SerializationInput;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ibis.ipl.ReceivePort} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class ReceivePort extends Manageable
        implements ibis.ipl.ReceivePort {

    /** Debugging output. */
    private static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.ReceivePort");

    // Possible results of a connection attempt.

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
    
    final static Set<Thread> threadsInUpcallSet
        = Collections.synchronizedSet(new HashSet<Thread>());


    /** The type of this port. */
    public final PortType type;

    /** The name of this port. */
    public final String name;

    /** Set when connections are enabled. */
    private boolean connectionsEnabled = false;

    /** Set when connection downcalls are supported. */
    private boolean connectionDowncalls = false;

    /** The identification of this receiveport. */
    public final ReceivePortIdentifier ident;

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

    /** Message upcall, if specified, or <code>null</code>. */
    protected MessageUpcall upcall;

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

    /** The current message. */
    protected ReadMessage message = null;

    /**
     * Set when the current message has been delivered. Only used for
     * explicit receive.
     */
    protected boolean delivered = false;
    
    /** Properties. */
    protected final Properties properties;

    private long nMessages = 0;
    private long messageBytes = 0;
    private long bytes = 0;
    private long nConnections = 0;
    private long nLostConnections = 0;
    private long nClosedConnections = 0;

    /**
     * Constructs a <code>ReceivePort</code> with the specified parameters.
     * Note that all property checks are already performed in the
     * <code>Ibis.createReceivePort</code> methods.
     * @param ibis the ibis instance.
     * @param type the port type.
     * @param name the name of the <code>ReceivePort</code>.
     * @param upcall the message upcall object, or <code>null</code>.
     * @param connectUpcall the connection upcall object, or <code>null</code>.
     * @param properties the port properties.
     */
    @SuppressWarnings("unchecked")
	protected ReceivePort(Ibis ibis, PortType type, String name,
            MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties) throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = ibis.createReceivePortIdentifier(name, ibis.ident);
        this.upcall = upcall;
        this.connectUpcall = connectUpcall;
        this.connectionDowncalls = type.hasCapability(PortType.CONNECTION_DOWNCALLS);
        this.numbered
                = type.hasCapability(PortType.COMMUNICATION_NUMBERED);
        this.properties = ibis.properties();
        if (properties != null) {
            for (Enumeration<String> e = (Enumeration<String>)properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = properties.getProperty(key);
                this.properties.setProperty(key, value);
            }
        }
        if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
            serialization = "data";    
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
            serialization = "sun";            
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
            serialization = "ibis";            
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }
        ibis.register(this);
        if (logger.isDebugEnabled()) {
            logger.debug(ibis.ident + ": ReceivePort '" + name + "' created");
        }
        addValidKey("Messages");
        addValidKey("MessageBytes");
        addValidKey("Bytes");
        addValidKey("Connections");
        addValidKey("LostConnections");
        addValidKey("ClosedConnections");
    }

    protected ReadMessage createReadMessage(SerializationInput in, ReceivePortConnectionInfo info) {
        return new ReadMessage(in, info);
    }
    
    public synchronized void enableMessageUpcalls() {
        allowUpcalls = true;
        notifyAll();
    }

    public static String getString(int result) {
        switch(result) {
        case ACCEPTED:
            return "ACCEPTED";
        case DENIED:
            return "DENIED";
        case NO_MANY_TO_X:
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

    public synchronized void disableMessageUpcalls() {
        allowUpcalls = false;
    }

    public synchronized ibis.ipl.SendPortIdentifier[] connectedTo() {
        return connections.keySet().toArray(new ibis.ipl.SendPortIdentifier[0]);
    }

    public PortType getPortType() {
        return type;
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
        return receive(0);
    }

    public ReadMessage receive(long timeout) throws IOException {
        if (upcall != null) {
            throw new IbisConfigurationException(
                    "Configured Receiveport for upcalls, downcall not allowed");
        }

        if (timeout < 0) {
            throw new IOException("timeout must be a non-negative number");
        }
        if (timeout > 0 &&
                ! type.hasCapability(PortType.RECEIVE_TIMEOUT)) {
            throw new IbisConfigurationException(
                    "This port is not configured for receive() with timeout");
        }

        return getMessage(timeout);
    }

    public final void close() throws IOException {
        close(0L);
    }

    public void close(long timeout) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Receiveport " + name + ": closing");
        }
        synchronized(this) {
            if (closed) {
                throw new IOException("Port already closed");
            }
            connectionsEnabled = false;
            closed = true;
            notifyAll();
        }
        closePort(timeout);
        ibis.deRegister(this);
    }

    public synchronized byte connectionAllowed(SendPortIdentifier id,
            PortType sp) {
        byte retval = ACCEPTED;

        if (isConnectedTo(id)) {
            retval = ALREADY_CONNECTED;
        } else if (! sp.equals(type)) {
            retval = TYPE_MISMATCH;
        } else if (! connectionsEnabled) {
            retval = DISABLED;
        } else if (connections.size() != 0 &&
            ! (type.hasCapability(PortType.CONNECTION_MANY_TO_ONE)
                || type.hasCapability(PortType.CONNECTION_MANY_TO_MANY))) {
            retval = NO_MANY_TO_X;
        } else if (connectUpcall != null) {
            retval = DENIED;
            try {
                if (connectUpcall.gotConnection(this, id)) {
                    retval = ACCEPTED;
                }
            } catch(Throwable e) {
                logger.error("Unexpected exception in gotConnection(), "
                        + "this Java instance will be terminated" , e);
                System.exit(1);
            }
        }
        if (retval == ACCEPTED && connectionDowncalls) {
            newConnections.add(id);
        } 
        if (logger.isDebugEnabled()) {
            logger.debug("Connection attempt from " + id + ": "
                    + getString(retval));
        }
        return retval;
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
        if (! type.hasCapability(PortType.RECEIVE_POLL)) {
            throw new IbisConfigurationException(
                    "Receiveport not configured for polls");
        }
        return doPoll();
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Public methods, may be called or redefined by implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Notifies this receiveport that the connection associated with the
     * specified sendport must be assumed to be lost, due to the specified
     * reason. It updates the administration, and performs the
     * lostConnection upcall, if required.
     * @param id the identification of the sendport.
     * @param e the cause of the lost connection.
     */
    public void lostConnection(SendPortIdentifier id, Throwable e) {
        if (connectionDowncalls) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } 
        if (connectUpcall != null) {
            try {
                connectUpcall.lostConnection(this, id, e);
            } catch(Throwable e2) {
                logger.error("Unexpected exception in lostConnection(), "
                        + "this Java instance will be terminated" , e2);
                System.exit(1);
            }
        }
        if (e != null) {
            nLostConnections++;
        } else {
            nClosedConnections++;
        }
        removeInfo(id);
    }

    /**
     * Returns the connection information for the specified sendport identifier.
     * @param id the identification of the sendport.
     * @return the connection information, or <code>null</code> if not
     * present.
     */
    public synchronized ReceivePortConnectionInfo getInfo(
            SendPortIdentifier id) {
        return connections.get(id);
    }

    /**
     * Adds a connection entry for the specified sendport identifier,
     * and notifies, for possible waiters on a new connection.
     * @param id the identification of the sendport.
     * @param info the associated connection information.
     */
    public synchronized void addInfo(SendPortIdentifier id,
            ReceivePortConnectionInfo info) {
        nConnections++;
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
    public synchronized ReceivePortConnectionInfo removeInfo(
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
    public synchronized ReceivePortConnectionInfo[] connections() {
        return connections.values().toArray(new ReceivePortConnectionInfo[0]);
    }

    /**
     * Waits for a new message and returns it. If the specified timeout is
     * larger than 0, the implementation waits for the specified time.
     * This method gets called by {@link #receive()} or {@link #receive(long)},
     * and may be redefined by implementations, for instance when there
     * is no separate thread delivering the messages.
     * @param timeout the timeout in milliseconds.
     * @exception ReceiveTimedOutException is thrown on timeout.
     * @exception IOException is thrown in case of trouble.
     * @return the new message, or <code>null</code>.
     */
    public ReadMessage getMessage(long timeout) throws IOException {

        long deadLine = System.currentTimeMillis() + timeout;

        synchronized(this) {
            // Wait until a new message is delivered or the port is closed.
            while ((message == null || delivered) && ! closed) {
                try {
                    if (timeout > 0) {
                        long time = System.currentTimeMillis();
                        if (time >= deadLine) {
                            throw new ReceiveTimedOutException(
                                    "timeout expired in receive()");
                        }
                        time = deadLine - time;
                        wait(time);
                    } else {
                        wait();
                    }
                } catch(InterruptedException e) {
                    // ignored
                }
                if (closed) {
                    throw new ConnectionClosedException("receive() on closed port");
                }
            }
            delivered = true;
            return message;
        }
    }

    /**
     * This method is called when a message arrived and the port is configured
     * for upcalls.
     * The assumption here is that when the upcall does an explicit
     * {@link ReadMessage#finish()}, a new message is allocated, because
     * at that point new messages can be delivered to the receive port.
     * This method may be redefined, for instance when there is a separate
     * thread for dealing with upcalls.
     * @param msg the message.
     */
    public void doUpcall(ReadMessage msg) {
        synchronized(this) {
            // Wait until upcalls are enabled.
            while (! allowUpcalls) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    // ignored
                }
            }
        }
        msg.setInUpcall(true);
        try {
            // Notify the message that is is processed from an upcall,
            // so that finish() calls can be detected.
            threadsInUpcallSet.add(Thread.currentThread());
            upcall.upcall(msg);
        } catch(IOException e) {
            if (! msg.isFinished()) {
                msg.finish(e);
                return;
            }
            logger.error("Got unexpected exception in upcall, continuing ...", e);
        } catch(ClassNotFoundException e) {
            if (! msg.isFinished()) {
                IOException ioex =
                    new IOException("Got ClassNotFoundException: "
                        + e.getMessage());
                ioex.initCause(e);
                msg.finish(ioex);
            }
            return;
        } catch(Throwable e) {
            logger.error("Got unexpected throwable in upcall(), "
                    + "this Java instance will be terminated", e);
            System.exit(1);

        } finally {
            msg.setInUpcall(false);
        }

        if (! msg.isFinished()) {
            try {
                msg.finish();
            } catch(IOException e) {
                msg.finish(e);
            }
        }
    }

    public void messageArrived(ReadMessage msg) {
        // Wait until the previous message was finished.
        synchronized(this) {
            while (message != null) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    // ignored.
                }
            }
            message = msg;
            delivered = false;
            notifyAll();
        }
        if (upcall != null) {
            doUpcall(message);
        }
    }

    /**
     * Notifies the port that {@link ReadMessage#finish()} was called on the
     * specified message. The port should prepare for a new message.
     * @param r the message.
     * @param cnt the byte count of this message.
     */
    public synchronized void finishMessage(ReadMessage r, long cnt) {
        nMessages++;
        messageBytes += cnt;
        message = null;
        threadsInUpcallSet.remove(Thread.currentThread());
        notifyAll();
    }

    /**
     * Notifies the port that {@link ReadMessage#finish(IOException)}
     * was called on the specified message.
     * The port should close the connection, with the specified reason.
     * @param r the message.
     * @param e the Exception.
     */
    public synchronized void finishMessage(ReadMessage r, IOException e) {
        r.getInfo().close(e);
        message = null;
        threadsInUpcallSet.remove(Thread.currentThread());
        notifyAll();
    }

    /**
     * Waits for all connections to close. If the specified timeout is larger
     * than 0, the implementation waits for the specified time, and then
     * forcibly closes all connections.
     * @param timeout the timeout in milliseconds.
     */
    public synchronized void closePort(long timeout) {
        if (timeout == 0) {
            while (connections.size() > 0) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    // ignored
                }
            }
        } else {
            long endTime = System.currentTimeMillis() + timeout;
            while (connections.size() > 0 && timeout > 0) {
                try {
                    wait(timeout);
                } catch(InterruptedException e) {
                    // ignored
                }
                timeout = endTime - System.currentTimeMillis();
            }
            ReceivePortConnectionInfo[] conns = connections();
            for (int i = 0; i < conns.length; i++) {
                conns[i].close(new IOException(
                            "receiver forcibly closed connection"));
            }
            nClosedConnections += conns.length;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(name + ":done receiveport.close");
        }
    }
    
    protected synchronized void updateProperties() {
        setProperty("Bytes", "" + bytes);
        setProperty("ClosedConnections", "" + nClosedConnections);
        setProperty("Connections", "" + nConnections);
        setProperty("Messages", "" + nMessages);
        setProperty("MessageBytes", "" + messageBytes);
        setProperty("LostConnections", "" + nLostConnections);
    }

    void addDataIn(long cnt) {
        bytes += cnt;
    }

    /**
     * Implementation-dependent part of the {@link #poll()} method.
     * This version assumes that other threads deliver messages and
     * do upcalls.
     * @exception IOException is thrown in case of trouble.
     * @return a new {@link ReadMessage} or <code>null</code>.
     */
    protected ReadMessage doPoll() throws IOException {
        Thread.yield(); // Give other thread a chance to deliver

        if (upcall != null) {
            return null;
        }

        synchronized (this) { // Other thread may modify data.
            if (message == null || delivered) {
                return null;
            }
            if (message != null) {
                delivered = true;
            }
            return message;
        }
    }

    synchronized long getMessageCount() {
        return nMessages;
    }
    
    synchronized long getBytesReceived() {
        return bytes;
    }
    
    synchronized long getBytesRead() {
        return messageBytes;
    }

}
