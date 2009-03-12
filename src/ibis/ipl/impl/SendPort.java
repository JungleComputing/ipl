/* $Id$ */

package ibis.ipl.impl;

import ibis.io.DataOutputStream;
import ibis.io.Replacer;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ibis.ipl.SendPort} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class SendPort extends Manageable implements ibis.ipl.SendPort {

    /** Debugging output. */
    private static final Logger logger = LoggerFactory.getLogger("ibis.ipl.impl.SendPort");
    
    private static final String ALLOW_COMM_IN_UPCALL = "ibis.upcall.communication";
    
    private static final String ALLOW_CONN_IN_UPCALL = "ibis.upcall.connections";
    
    private static final String[][] propertiesList = new String[][] {
        { ALLOW_COMM_IN_UPCALL, "false",
            "Boolean: when set, communication is allowed from inside upcalls,"
            + " without first calling finish()."},
        { ALLOW_CONN_IN_UPCALL, "false",
                "Boolean: when set, connection setup is allowed from inside upcalls,"
                + " without first calling finish()."},
    };

    /** The type of this port. */
    public final PortType type;

    /** The name of this port. */
    public final String name;

    /** The identification of this sendport. */
    public final SendPortIdentifier ident;

    /** Set when connection downcalls are supported. */
    private final boolean connectionDowncalls;

    /** Connection upcall handler, or <code>null</code>. */
    public final SendPortDisconnectUpcall connectUpcall;

    /**
     * The connections lost since the last call to {@link #lostConnections()}.
     */
    protected ArrayList<ReceivePortIdentifier> lostConnections
            = new ArrayList<ReceivePortIdentifier>();

    /** The current connections. */
    protected HashMap<ReceivePortIdentifier, SendPortConnectionInfo> receivers
            = new HashMap<ReceivePortIdentifier, SendPortConnectionInfo>();

    /** Set when a message is currently being used. */
    private boolean aMessageIsAlive = false;

    /**
     * Counts the number of threads waiting for a message in
     * a {@link #newMessage()} call.
     */
    private int waitingForMessage = 0;

    /** Set when this port is closed. */
    private boolean closed = false;

    /** The Ibis instance of this send port. */
    protected Ibis ibis;

    /** Object replacer for serialization streams. */
    private final Replacer replacer;

    /** The serialization output stream. */
    protected SerializationOutput out;

    /** The underlying data output stream. */
    protected DataOutputStream dataOut;

    /** The write message for this port. */
    protected final WriteMessage w;

    /** Collected exceptions for multicast ports. */
    private CollectedWriteException collectedExceptions;
    
    /** Properties. */
    protected final Properties properties;

    /**
     * The number of messages sent with this sendport. Actually counts the
     * number of finish() calls.
     */
    private long nMessages = 0;

    /**
     * The number of bytes in these messages. Counted once, even for
     * one-to-many sendports.
     */
    private long messageBytes = 0;

    /**
     * The total number of bytes written for these messages. In contrast
     * to messageBytes, this one counts the number of bytes actually
     * put on the network. If the message goes through an N-way output
     * stream splitter, the message is counted N times. See the counting
     * in ibis.io.
     */
    private long bytes = 0;

    /**
     * Cumulative value of totalWritten(), including in-between calls of
     * resetWritten(). Note that totalWritten() and resetWritten() can be
     * redefined by subclasses.
     */
    private long prevBytes = 0;

    /** Counts the number of connections set up with this sendport. */
    private long nConnections = 0;

    /**
     * Counts the number of times a connection was lost (as opposed to
     * closed).
     */
    private long nLostConnections = 0;

    /** Counts the number of connections that were explicitly closed. */
    private long nClosedConnections = 0;
    
    /** 
     * When set, allow newMessage calls in message upcalls without
     * first calling finish().
     */
    private final boolean allowCommunicationInUpcall;
    
    /** 
     * When set, allow connection setup in message upcalls without
     * first calling finish().
     */
    private final boolean allowConnectionsInUpcall;

    /**
     * Constructs a <code>SendPort</code> with the specified parameters.
     * Note that all property checks are already performed in the
     * <code>Ibis.createSendPort</code> methods.
     * @param ibis the ibis instance.
     * @param type the port type.
     * @param name the name of the <code>SendPort</code>.
     * @param connectUpcall the connection upcall object, or <code>null</code>.
     * @param properties the port properties.
     * @exception IOException is thrown in case of trouble.
     */
    @SuppressWarnings("unchecked")
    protected SendPort(Ibis ibis, PortType type, String name,
            SendPortDisconnectUpcall connectUpcall, Properties properties) throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = ibis.createSendPortIdentifier(name, ibis.ident);
        this.connectionDowncalls = type.hasCapability(PortType.CONNECTION_DOWNCALLS);
        this.connectUpcall = connectUpcall;
        this.properties = ibis.properties();
        if (properties != null) {
            for (Enumeration<String> e = (Enumeration<String>)properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = properties.getProperty(key);
                this.properties.setProperty(key, value);
            }
        }

        String replacerName = this.properties.getProperty("ibis.serialization.replacer");
        if (replacerName != null) {
            try {
                Class<? extends Replacer> replacerClass = (Class<? extends Replacer>) Class.forName(replacerName);
                this.replacer = replacerClass.newInstance();
            } catch(Throwable e) {
                throw new IOException("Could not instantiate replacer class "
                        + replacerName);
            }
        } else {
            this.replacer = null;
        }
        
        TypedProperties tp = new TypedProperties(this.properties);
        allowCommunicationInUpcall = tp.getBooleanProperty(ALLOW_COMM_IN_UPCALL, false);
        allowConnectionsInUpcall = tp.getBooleanProperty(ALLOW_CONN_IN_UPCALL, false);
        ibis.register(this);
        if (logger.isDebugEnabled()) {
            logger.debug(ibis.identifier() + ": Sendport '" + name
                    + "' created");
        }
        w = createWriteMessage();

        addValidKey("Messages");
        addValidKey("MessageBytes");
        addValidKey("Bytes");
        addValidKey("Connections");
        addValidKey("LostConnections");
        addValidKey("ClosedConnections");
    }
    
    /**
     * Returns a map mapping hard-coded property names to their descriptions.
     * 
     * @return the name/description map.
     */
    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

    /**
     * Returns a list of recognized properties.
     */
    public static List<String> getPropertyNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (String[] element : propertiesList) {
            result.add(element[0]);
        }
        return result;
    }
    
    protected synchronized void updateProperties() {
        setProperty("ClosedConnections", "" + nClosedConnections);
        setProperty("LostConnections", "" + nLostConnections);
        setProperty("Connections", "" + nConnections);
        setProperty("Messages", "" + nMessages);
        setProperty("MessageBytes", "" + messageBytes);
        setProperty("Bytes", "" + bytes);
    }

    /**
     * Creates a new write message. May be redefined.
     * @return the new write message.
     */
    protected WriteMessage createWriteMessage() {
        return new WriteMessage(this);
    }

    protected long totalWritten() {
        return dataOut.bytesWritten();
    }

    protected void resetWritten() {
        dataOut.resetBytesWritten();
    }

    private void createOut() throws IOException {
        String serialization;
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
        out = SerializationFactory.createSerializationOutput(serialization,
                dataOut);
        if (replacer != null) {
            out.setReplacer(replacer);
        }
    }

    public PortType getPortType() {
        return type;
    }

    public synchronized ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        if (! connectionDowncalls) {
            throw new IbisConfigurationException("SendPort.lostConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.ReceivePortIdentifier[] result = lostConnections.toArray(
                new ibis.ipl.ReceivePortIdentifier[lostConnections.size()]);
        lostConnections.clear();
        return result;
    }

    public String name() {
        return name;
    }

    public ibis.ipl.SendPortIdentifier identifier() {
        return ident;
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    public ibis.ipl.ReceivePortIdentifier connect(ibis.ipl.IbisIdentifier id,
            String name) throws ConnectionFailedException {
        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + this.name + "' connecting to "
                    + name + " at " + id);
        }
        return connect(id, name, 0, true);
    }

    private void checkConnect(ReceivePortIdentifier r) throws ConnectionFailedException {

        if (receivers.size() > 0
                && ! type.hasCapability(PortType.CONNECTION_ONE_TO_MANY)
                && ! type.hasCapability(PortType.CONNECTION_MANY_TO_MANY)) {
            throw new IbisConfigurationException("Sendport already has a "
                    + "connection and OneToMany or ManyToMany not requested");
        }

        if (getInfo(r) != null) {
            throw new AlreadyConnectedException("Already connected", r);
        }
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier receiver,
            long timeout, boolean fillTimeout) throws ConnectionFailedException {
        
        if (! allowConnectionsInUpcall && ReceivePort.threadsInUpcallSet.contains(Thread.currentThread())) {
            throw new ConnectionFailedException("Connection attempt in upcall is not allowed",
                    receiver);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + name + "' connecting to " + receiver);
        }

        if (aMessageIsAlive) {
            throw new ConnectionFailedException(
                "A message was alive while adding a new connection", receiver);
        }

        if (timeout < 0) {
            throw new ConnectionFailedException(
                    "connect(): timeout must be >= 0", receiver);
        }

        ReceivePortIdentifier r = (ReceivePortIdentifier) receiver;

        checkConnect(r);

        try {
            addConnectionInfo(r, doConnect(r, timeout, fillTimeout));
        } catch(ConnectionFailedException e) {
            throw e;
        } catch(Throwable e1) {
            throw new ConnectionFailedException("Got unexpected exception", r, e1);
        }
        nConnections++;
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports, long timeout, 
            boolean fillTimeout) throws ConnectionsFailedException {

        ibis.ipl.ReceivePortIdentifier [] ids = 
            new ibis.ipl.ReceivePortIdentifier[ports.size()];
        
        int index = 0;
        
        for (Map.Entry<ibis.ipl.IbisIdentifier, String> entry : ports.entrySet()) {
            ids[index++] = ibis.createReceivePortIdentifier(entry.getValue(), 
                    (IbisIdentifier) entry.getKey());
        }

        connect(ids, timeout, fillTimeout); // may throw an exception
        
        return ids; 
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports)
            throws ConnectionsFailedException {
        connect(ports, 0, true);
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier[] ports,
            long timeout, boolean fillTimeout) throws ConnectionsFailedException {

        ArrayList<ibis.ipl.ReceivePortIdentifier> succes
            = new ArrayList<ibis.ipl.ReceivePortIdentifier>();

        LinkedList<ibis.ipl.ReceivePortIdentifier> todo
            = new LinkedList<ibis.ipl.ReceivePortIdentifier>();
        
        HashMap<ibis.ipl.ReceivePortIdentifier, Throwable> results 
            = new HashMap<ibis.ipl.ReceivePortIdentifier, Throwable>();
        
        long deadline = 0;
        
        // Calculate the deadline (if needed).
        if (timeout > 0) {
            deadline = System.currentTimeMillis() + timeout;
        }
   
        // Create a list of the connections that we need to set up.
        for (ibis.ipl.ReceivePortIdentifier rp : ports) {
            todo.add(rp);
        }
        
        if (todo.size() > 0) {
            if (! allowConnectionsInUpcall && ReceivePort.threadsInUpcallSet.contains(Thread.currentThread())) {
                throw new ConnectionsFailedException("Connection attempt in upcall is not allowed");
            }
        }
            
        // Keep iterating over the list of connection to set up until the list 
        // is empty or until we reach the deadline.
        while (todo.size() > 0) { 
            
            long time = 0;
            
            // Check if we have reached the deadline. If so, break out of 
            // the loop.
            if (deadline != 0) {
                
                time = (deadline - System.currentTimeMillis()) / todo.size();
                
                if (time <= 0) {
                    break;
                }
            } 

            // Remove the next target from the list,
            ibis.ipl.ReceivePortIdentifier rp = todo.removeFirst(); 
            
            // Attempt a connection setup. If successful, the target adress is 
            // added to the 'succes' list and any previous exceptions are 
            // discarded. If unsuccessful, the exception is saved for future 
            // reference. Depending on the value of fillTimeout, the target is 
            // put back in the list to be retried later.
            // TODO: do we need an exp. backoff here? 
            try {
                connect(rp, time, false);
                succes.add(rp);
                results.remove(rp);
            } catch(Throwable e) {
                
                results.put(rp, e);
                
                if (fillTimeout) {
                    // We may get another chance!
                    
                    // TODO: should we filter out some exceptions from which we 
                    // can never recover? (e.g. if the ibis doesn't exist 
                    // anymore)?
                    todo.addLast(rp);                
                }
            }
        }

        // We are done OR we ran out of time OR we tried everyone at once and 
        // are not supposed to continue.
        
        if (succes.size() != ports.length) {
            // Some connections have failed. Throw a ConnectionsFailedException
            // to inform the user of this.
            
            // Gather all exceptions from the result map. Add new once for 
            // targets that have not been tried at all.
            ConnectionsFailedException ex = new ConnectionsFailedException();
            
            for (ibis.ipl.ReceivePortIdentifier rp : todo) {

                Throwable tmp = results.get(rp);

                if (tmp == null) { 
                    ex.add(new ConnectionTimedOutException(
                            "Out of time, connection not even tried", rp));
                } else if (tmp instanceof ConnectionFailedException) { 
                    ex.add((ConnectionFailedException) tmp);
                } else { 
                    ex.add(new ConnectionFailedException(
                            "Connection failed", rp, tmp));
                }
            }

            // Add a list of connections that were successful.            
            ex.setObtainedConnections(succes.toArray(
                    new ibis.ipl.ReceivePortIdentifier[succes.size()]));

            throw ex;
        }
    }

    public ibis.ipl.ReceivePortIdentifier connect(
            ibis.ipl.IbisIdentifier id, String name, long timeout, 
            boolean fillTimeout) throws ConnectionFailedException {
     
        ReceivePortIdentifier r = 
            ibis.createReceivePortIdentifier(name, (IbisIdentifier) id);

        connect(r, timeout, fillTimeout);
        
        return r;
    }

    private synchronized void addConnectionInfo(ReceivePortIdentifier ri,
            SendPortConnectionInfo connection) {
        if (logger.isDebugEnabled()) {
            logger.debug("SendPort '" + name + "': added connection to " + ri);
        }
        addInfo(ri, connection);
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        
        if (! allowCommunicationInUpcall && ReceivePort.threadsInUpcallSet.contains(Thread.currentThread())) {
            throw new IOException("Communication in upcall is not allowed");
        }
        synchronized(this) {
            if (closed) {
                throw new IOException("newMessage call on closed sendport");
            }

            if (out == null) {
                try {
                    createOut();
                } catch (Throwable t) {
                    // TODO: is this correct ? -- J.
                    throw new IOException("Lost connection!");
                }
            }

            waitingForMessage++;
            while (aMessageIsAlive) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    // ignored
                }
            }
            waitingForMessage--;
            // We waited, so the port may be closed now ...
            if (closed) {
                throw new IOException("newMessage call on closed sendport");
            }
            aMessageIsAlive = true;
        }
        announceNewMessage();
        w.initMessage(out);
        return w;
    }

    public void close() throws IOException {
        ReceivePortIdentifier[] ports;
        synchronized(this) {
            ports = receivers.keySet().toArray(new ReceivePortIdentifier[receivers.size()]);
            boolean alive = receivers.size() > 0 && aMessageIsAlive;
            if (alive) {
                throw new ConnectionClosedException(
                "Closed a sendport port while a message is alive!");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("SendPort '" + name + "': start close()");
            }

            if (closed) {
                throw new ConnectionClosedException("Port already closed");
            }
            closed = true;
            nClosedConnections += ports.length;
        }
        
        try {
            // We must create the outputstream if it doesn't exist yet, 
            // otherwise the close message may get lost!! -- Jason
            if (out == null) {
                try { 
                    createOut();
                } catch (Throwable t) { 
                    // TODO: Not sure how to handle this... typically occurs 
                    // when the stream is already closed by the other side. 
                    // Let's continue and see what happens -- J.
                }
            }
            
            // closePort should close all streams. (out and dataOut).
            closePort();
        } finally {
            // Don't keep the lock on the sendport while calling ibis.deregister()!
            // Otherwise, deadlocks may arise when someone is calling ibis.printStatistics()
            // at the same time. --Ceriel
            for (int i = 0; i < ports.length; i++) {
                SendPortConnectionInfo c = removeInfo(ports[i]);
                try {
                    c.closeConnection();
                } catch(Throwable e) {
                    // ignore and just continue (?)
                }      
            }
        
            ibis.deRegister(this);
            if (logger.isDebugEnabled()) {
                logger.debug("SendPort '" + name + "': close() done");
            }
        }
    }

    public void disconnect(ibis.ipl.IbisIdentifier id, String name)
            throws IOException {
        disconnect(ibis.createReceivePortIdentifier(name, (IbisIdentifier) id));
    }

    public synchronized void disconnect(ibis.ipl.ReceivePortIdentifier receiver)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + this.name + "' disconnecting from "
                    + receiver.name() + " at " + receiver.ibisIdentifier());
        }
        ReceivePortIdentifier r = (ReceivePortIdentifier) receiver;
        if (aMessageIsAlive) {
            throw new IOException(
                "Trying to disconnect while a message is alive!");
        }
        SendPortConnectionInfo c = removeInfo(r);
        if (c == null) {
            throw new IOException("Cannot disconnect from " + r
                    + " since we are not connected with it");
        }
        try {
            sendDisconnectMessage(r, c);
        } finally {
            c.closeConnection();
        }
        nClosedConnections++;
    }

    public synchronized ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        return receivers.keySet().toArray(
                new ibis.ipl.ReceivePortIdentifier[receivers.size()]);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, internal use. May be redefined or called
    // by an implementation.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Returns the connection information for the specified receiveport
     * identifier.
     * @param id the identification of the receiveport.
     * @return the connection information, or <code>null</code> if not
     * present.
     */
    protected synchronized SendPortConnectionInfo getInfo(
            ReceivePortIdentifier id) {
        return receivers.get(id);
    }

    /**
     * Adds a connection entry for the specified receiveport identifier.
     * @param id the identification of the receiveport.
     * @param info the associated connection information.
     */
    private synchronized void addInfo(ReceivePortIdentifier id,
            SendPortConnectionInfo info) {
        receivers.put(id, info);
    }

    /**
     * Removes the connection entry for the specified receiveport identifier.
     * @param id the identification of the receiveport.
     * @return the removed connection.
     */
    protected synchronized SendPortConnectionInfo removeInfo(
            ReceivePortIdentifier id) {
        return receivers.remove(id);
    }

    /**
     * Returns an array with entries for each connection.
     * @return the connections.
     */
    public synchronized SendPortConnectionInfo[] connections() {
        return receivers.values().toArray(
                new SendPortConnectionInfo[receivers.size()]);
    }

    /**
     * Implements the SendPort side of a message finish.
     * This method is called by the {@link WriteMessage#finish()}
     * implementation.
     * @param w the write message that calls this method.
     * @param cnt the number of bytes written.
     */
    protected synchronized void finishMessage(WriteMessage w, long cnt)
            throws IOException {
        aMessageIsAlive = false;
        if (waitingForMessage > 0) {
            // NotifyAll, because we don't know who is waiting, and what for.
            notifyAll();
        }
        nMessages++;
        messageBytes += cnt;
        bytes = prevBytes + totalWritten();
        if (collectedExceptions != null) {
            IOException e = collectedExceptions;
            collectedExceptions = null;
            throw e;
        }
    }

    /**
     * Implements the SendPort side of a message finish with exception.
     * This method is called by the
     * {@link WriteMessage#finish(java.io.IOException)} implementation.
     * @param w the write message that calls this method.
     * @param e the exception that was passed on to the
     * {@link WriteMessage#finish(java.io.IOException)} call.
     */
    protected void finishMessage(WriteMessage w, IOException e) {
        try {
            finishMessage(w, 0L);
        } catch(IOException ex) {
            // ignored
        }
    }

    /**
     * Returns the number of bytes written to the current data output stream.
     * This method is called by the {@link WriteMessage} implementation.
     */
    protected long bytesWritten() {
        return dataOut.bytesWritten();
    }

    /**
     * Called when a method from {@link WriteMessage} receives an
     * <code>IOException</code>.
     * It calls the implementation-specific {@link #handleSendException}
     * method and then rethrows the exception unless we are dealing with a
     * multicast port, in which case the exception is saved.
     */
    protected void gotSendException(WriteMessage w, IOException e)
            throws IOException {
        handleSendException(w, e);
        if (type.hasCapability(PortType.CONNECTION_ONE_TO_ONE)
                || type.hasCapability(PortType.CONNECTION_MANY_TO_ONE)) {
            // Otherwise exception will be saved until the finish.
            throw e;
        }
        if (collectedExceptions == null) {
            collectedExceptions = new CollectedWriteException();
        }
        collectedExceptions.add(e);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Public methods, to be called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * This method must be called by an implementation when it detects that a
     * connection to a particular receive port is lost.  It takes care of the
     * user upcall if requested, and updates the administration accordingly.
     * @param id identifies the receive port.
     * @param cause the exception that describes the reason for the loss of
     * the connection.
     */
    public void lostConnection(ReceivePortIdentifier id, Throwable cause) {
        if (logger.isDebugEnabled() && cause != null) {
            logger.debug("lostConnection to " + id + ", cause " + cause, cause);
        }
        if (connectionDowncalls) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } 
        if (connectUpcall != null) {
            try {
                connectUpcall.lostConnection(this, id, cause);
            } catch(Throwable e) {
                logger.error("Unexpected exception in lostConnection(), "
                        + "this Java instance will be terminated" , e);
                System.exit(1);
            }
        }
        SendPortConnectionInfo c = removeInfo(id);
        if (c != null) {
            try {
                nLostConnections++;
                c.closeConnection();
            } catch(Throwable e) {
                // ignored
            }
        }
    }

    /**
     * This method (re-)initializes the {@link ibis.io.DataOutputStream}, and
     * closes the serialization stream if there was one.
     * This method should be called from the implementation-specific
     * constructor, and from each
     * {@link #doConnect(ReceivePortIdentifier, long,boolean)} call.
     * @param dataOut the {@link ibis.io.DataOutputStream} to be used when
     * creating a new serialization stream is created.
     */
    public void initStream(DataOutputStream dataOut) {
        this.dataOut = dataOut;
        prevBytes += totalWritten();
        resetWritten();
        // Close the serialization stream. A new one will be created when
        // needed.
        if (out != null) {
            try {
                out.close();
            } catch(Throwable e) {
                // ignored
            }
        }
        out = null;
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be implemented by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * This method must notify the receiveports that a new message is coming,
     * if needed. It must also deal with sequencing, if implemented/required.
     * @exception IOException may be thrown when this notification fails.
     */
    protected abstract void announceNewMessage() throws IOException;

    /**
     * This method must set up a connection with the specified receive port.
     * @param receiver identifies the receive port.
     * @param timeout the timout, in milliseconds.
     * @exception IOException may be thrown when the connection fails.
     * @return the {@link SendPortConnectionInfo} associated with the
     * connection.
     */
    protected abstract SendPortConnectionInfo doConnect(
            ReceivePortIdentifier receiver, long timeout, boolean fillTimeout) 
        throws IOException;

    /**
     * This method must notify the specified receive port that this sendport
     * has disconnected from it.
     * @param receiver identifies the receive port.
     * @param c the connection information.
     * @exception IOException is thrown in case of trouble.
     */
    protected abstract void sendDisconnectMessage(ReceivePortIdentifier receiver,
            SendPortConnectionInfo c) throws IOException;

    /**
     * This method should notify the connected receiveport(s) that this
     * sendport is being closed.
     * @exception IOException may be thrown when communication with the
     * receiveport(s) fails for some reason.
     */
    protected abstract void closePort() throws IOException;

    /**
     * This method is called when a {@link WriteMessage} method receives an
     * <code>IOException</code>. The implementation should try and find out
     * which connection(s) were lost, and call the
     * {@link #lostConnection(ReceivePortIdentifier, Throwable)}
     * method for each of them.
     * @param w the {@link WriteMessage}.
     * @param e the exception that was thrown.
     */
    protected abstract void handleSendException(WriteMessage w, IOException e);
    
    /**
     * Number of messages send
     */
    synchronized long getMessageCount() {
        return nMessages;
    }

    /**
     * Number of bytes written by the user in messages.
     */
    synchronized long getBytesWritten() {
        return messageBytes;
    }
    
    /**
     * Number of bytes send out on the network. Includes extra traffic send
     * if a message is broadcasted.
     */
    synchronized long getBytesSend() {
        return bytes;
    }
}
