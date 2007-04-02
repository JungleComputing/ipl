/* $Id$ */

package ibis.impl;

import ibis.io.DataOutputStream;
import ibis.io.Replacer;
import ibis.io.SerializationBase;
import ibis.io.SerializationOutput;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.SendPortDisconnectUpcall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Implementation of the {@link ibis.ipl.SendPort} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class SendPort extends Managable implements ibis.ipl.SendPort,
            ibis.ipl.PredefinedCapabilities {

    /** Debugging output. */
    private static final Logger logger = Logger.getLogger("ibis.impl.SendPort");

    /** Number of bytes written to messages of this port. */
    private long count = 0;

    /** The type of this port. */
    public final CapabilitySet type;

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

    /**
     * Constructs a <code>SendPort</code> with the specified parameters.
     * Note that all property checks are already performed in the
     * <code>Ibis.createSendPort</code> methods.
     * @param ibis the ibis instance.
     * @param type the port type.
     * @param name the name of the <code>SendPort</code>.
     * @param connectUpcall the connection upcall object, or <code>null</code>.
     * @param connectionDowncalls set when connection downcalls must be
     * supported.
     * @exception IOException is thrown in case of trouble.
     */
    protected SendPort(Ibis ibis, CapabilitySet type, String name,
            SendPortDisconnectUpcall connectUpcall, boolean connectionDowncalls)
            throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = ibis.createSendPortIdentifier(name, ibis.ident);
        this.connectionDowncalls = connectionDowncalls;
        this.connectUpcall = connectUpcall;
        
        String replacerName = type.getCapability("serialization.replacer");
        if (replacerName != null) {
            try {
                Class replacerClass = Class.forName(replacerName);
                this.replacer = (Replacer) replacerClass.newInstance();
            } catch(Throwable e) {
                throw new IOException("Could not instantiate replacer class "
                        + replacerName);
            }
        } else {
            this.replacer = null;
        }
        ibis.register(this);
        if (logger.isDebugEnabled()) {
            logger.debug(ibis.identifier() + ": Sendport '" + name
                    + "' created");
        }
        w = createWriteMessage();
    }

    /**
     * Creates a new write message. May be redefined.
     * @return the new write message.
     */
    protected WriteMessage createWriteMessage() {
        return new WriteMessage(this);
    }

    private void createOut() {
        String serialization;
        if (type.hasCapability(SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (type.hasCapability(SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }
        out = SerializationBase.createSerializationOutput(serialization,
                dataOut);
        if (replacer != null) {
            try {
                out.setReplacer(replacer);
            } catch(Exception e) {
                throw new Error("Exception in setReplacer should not happen",
                        e);
            }
        }
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized void resetCount() {
        count = 0;
    }

    public CapabilitySet getType() {
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
            throws IOException {
        connect(receiver, 0);
    }

    public ibis.ipl.ReceivePortIdentifier connect(ibis.ipl.IbisIdentifier id,
            String name) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + this.name + "' connecting to "
                    + name + " at " + id);
        }
        return connect(id, name, 0);
    }

    private void checkConnect(ReceivePortIdentifier r) throws IOException {

        if (receivers.size() > 0
                && ! type.hasCapability(CONNECTION_ONE_TO_MANY)
                && ! type.hasCapability(CONNECTION_MANY_TO_MANY)) {
            throw new IbisConfigurationException("Sendport already has a "
                    + "connection and OneToMany or ManyToMany not requested");
        }

        if (getInfo(r) != null) {
            throw new AlreadyConnectedException(
                    "This sendport was already connected to " + r);
        }
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier receiver,
            long timeout) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + name + "' connecting to " + receiver);
        }

        if (aMessageIsAlive) {
            throw new IOException(
                "A message was alive while adding a new connection");
        }

        if (timeout < 0) {
            throw new IOException("connect(): timeout must be >= 0");
        }

        ReceivePortIdentifier r = (ReceivePortIdentifier) receiver;

        checkConnect(r);

        addConnectionInfo(r, doConnect(r, timeout));
    }

    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0);
    }

    public synchronized ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports, long timeout)
            throws ConnectionsFailedException {
        ConnectionsFailedException ex = null;
        ArrayList<ibis.ipl.ReceivePortIdentifier> portIds
                = new ArrayList<ibis.ipl.ReceivePortIdentifier>();

        int count = ports.size();
        long endTime = 0;
        if (timeout > 0) {
            endTime = System.currentTimeMillis() + timeout;
        }

        for (Map.Entry<ibis.ipl.IbisIdentifier, String> entry : ports.entrySet()) {
            long t = 0;
            if (endTime != 0) {
                long now = System.currentTimeMillis();
                if (endTime < now) {
                    IOException e = 
                            new ConnectionTimedOutException(
                            "Out of time, connection to (" +  entry.getKey()
                            + ", " + entry.getValue() + ") not even tried");
                    if (ex == null) {
                        ex = new ConnectionsFailedException();
                    }
                    ex.add(entry.getKey(), entry.getValue(), e);
                    continue;
                }              
                t = (endTime - now) / count;
                if (t <= 0) {
                    t = 1;
                }
            }
            count--;
            try {
                portIds.add(connect(entry.getKey(), entry.getValue(), t));
            } catch(IOException e) {
                if (ex == null) {
                    ex = new ConnectionsFailedException();
                }
                ex.add(entry.getKey(), entry.getValue(), e);
            }
        }

        ibis.ipl.ReceivePortIdentifier[] result = portIds.toArray(
                new ibis.ipl.ReceivePortIdentifier[portIds.size()]);

        if (ex != null) {
            ex.setObtainedConnections(result);
            throw ex;
        }

        return result;
    }

    public void connect(ibis.ipl.ReceivePortIdentifier[] ports)
            throws ConnectionsFailedException {
        connect(ports, 0);
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier[] ports,
            long timeout) throws ConnectionsFailedException {

        ArrayList<ibis.ipl.ReceivePortIdentifier> portIds
                = new ArrayList<ibis.ipl.ReceivePortIdentifier>();
        ConnectionsFailedException ex = null;

        int count = ports.length;
        long endTime = 0;
        if (timeout > 0) {
            endTime = System.currentTimeMillis() + timeout;
        }
        for (int i = 0; i < ports.length; i++) {
            long t = 0;
            if (endTime != 0) {
                long now = System.currentTimeMillis();
                if (endTime < now) {
                    IOException e = new ConnectionTimedOutException(
                            "Out of time, connection to (" +  ports[i].ibis()
                            + ", " + ports[i].name() + ") not even tried");
                    if (ex == null) {
                        ex = new ConnectionsFailedException();
                    }
                    ex.add(ports[i], e);
                    continue;
                }              
                t = (endTime - now) / count;
                if (t <= 0) {
                    t = 1;
                }
            }
            count--;
            try {
                connect(ports[i], t);
                portIds.add(ports[i]);
            } catch(IOException e) {
                if (ex == null) {
                    ex = new ConnectionsFailedException();
                }
                ex.add(ports[i], e);
            }
        }

        if (ex != null) {
            ibis.ipl.ReceivePortIdentifier[] result = portIds.toArray(
                    new ibis.ipl.ReceivePortIdentifier[portIds.size()]);
            ex.setObtainedConnections(result);
            throw ex;
        }
    }

    public synchronized ibis.ipl.ReceivePortIdentifier connect(
            ibis.ipl.IbisIdentifier id, String name, long timeout)
            throws IOException {
        ReceivePortIdentifier r = ibis.createReceivePortIdentifier(name,
                (IbisIdentifier) id);

        if (logger.isDebugEnabled()) {
            logger.debug("Sendport '" + name + "' connecting to " + r);
        }

        if (aMessageIsAlive) {
            throw new IOException(
                "A message was alive while adding a new connection");
        }

        checkConnect(r);

        addConnectionInfo(r, doConnect(r, timeout));

        return r;
    }

    private synchronized void addConnectionInfo(ReceivePortIdentifier ri,
            SendPortConnectionInfo connection) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("SendPort '" + name + "': added connection to " + ri);
        }
        if (connection == null) {
            throw new ConnectionRefusedException("Could not connect");
        }
        addInfo(ri, connection);
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        synchronized(this) {
            if (closed) {
                throw new IOException("newMessage call on closed sendport");
            }

            if (out == null) {
                createOut();
            }

            waitingForMessage++;
            while (aMessageIsAlive) {
                try {
                    wait();
                } catch(Exception e) {
                    // ignored
                }
            }
            waitingForMessage--;
            aMessageIsAlive = true;
        }
        announceNewMessage();
        w.initMessage(out);
        return w;
    }


    public synchronized void close() throws IOException {
        boolean alive = receivers.size() > 0 && aMessageIsAlive;
        if (logger.isDebugEnabled()) {
            logger.debug("SendPort '" + name + "': start close()");
        }
        if (closed) {
            throw new IOException("Port already closed");
        }
        try {
            closePort();
        } finally {
            ReceivePortIdentifier[] ports = receivers.keySet().toArray(
                    new ReceivePortIdentifier[receivers.size()]);
            for (int i = 0; i < ports.length; i++) {
                SendPortConnectionInfo c = removeInfo(ports[i]);
                c.closeConnection();
            }
            closed = true;
            ibis.deRegister(this);
            if (alive) {
                throw new IOException(
                    "Closed a sendport port while a message is alive!");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("SendPort '" + name + "': close() done");
            }
        }
    }

    public synchronized void disconnect(ibis.ipl.ReceivePortIdentifier receiver)
            throws IOException {
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
            disconnectPort(r, c);
        } finally {
            c.closeConnection();
        }
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
        count += cnt;
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
        if (type.hasCapability(CONNECTION_ONE_TO_ONE)) {
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
        if (connectionDowncalls) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } else if (connectUpcall != null) {
            connectUpcall.lostConnection(this, id, cause);
        }
        SendPortConnectionInfo c = removeInfo(id);
        if (c != null) {
            try {
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
     * {@link #doConnect(ReceivePortIdentifier, long)} call.
     * @param dataOut the {@link ibis.io.DataOutputStream} to be used when
     * creating a new serialization stream is created.
     */
    public void initStream(DataOutputStream dataOut) {
        this.dataOut = dataOut;
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
            ReceivePortIdentifier receiver, long timeout) throws IOException;

    /**
     * This method must notify the specified receive port that this sendport
     * has disconnected from it.
     * @param receiver identifies the receive port.
     * @param c the connection information.
     * @exception IOException is thrown in case of trouble.
     */
    protected abstract void disconnectPort(ReceivePortIdentifier receiver,
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
}
