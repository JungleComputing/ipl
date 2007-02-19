/* $Id$ */

package ibis.impl;

import ibis.io.Replacer;
import ibis.io.SerializationBase;
import ibis.io.SerializationOutput;
import ibis.io.DataOutputStream;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.SendPortConnectUpcall;
import ibis.util.GetLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Implementation of the {@link ibis.ipl.SendPort} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class SendPort implements ibis.ipl.SendPort {

    /** Debugging output. */
    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.SendPort");

    /** Number of bytes written to messages of this port. */
    private long count = 0;

    /** The type of this port. */
    protected final PortType type;

    /** The name of this port. */
    protected final String name;

    /** The identification of this sendport. */
    protected final SendPortIdentifier ident;

    /** Set when connection downcalls are supported. */
    private final boolean connectionDowncalls;

    /** Connection upcall handler, or <code>null</code>. */
    protected final SendPortConnectUpcall connectUpcall;

    /** Map for implementing the dynamic properties. */
    protected Map<String, Object> props = new HashMap<String, Object>();

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

    /** The Ibis instance of this receive port. */
    protected Ibis ibis;

    /** Object replacer for serialization streams. */
    private final Replacer replacer;

    /** The serialization output stream. */
    protected SerializationOutput out;

    /** The underlying data output stream. */
    protected DataOutputStream dataOut;

    /** The write message for this port. */
    protected final WriteMessage w;

    /**
     * Constructs a <code>SendPort</code> with the specified parameters.
     * Note that all property checks are already performed in the
     * <code>PortType.createSendPort</code> methods.
     * @param ibis the ibis instance.
     * @param type the port type.
     * @param name the name of the <code>SendPort</code>.
     * @param connectUpcall the connection upcall object, or <code>null</code>.
     * @param connectionDowncalls set when connection downcalls must be
     * supported.
     * @exception IOException is thrown in case of trouble.
     */
    protected SendPort(Ibis ibis, PortType type, String name,
            SendPortConnectUpcall connectUpcall, boolean connectionDowncalls)
            throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = new SendPortIdentifier(name, ibis.ident);
        this.connectionDowncalls = connectionDowncalls;
        this.connectUpcall = connectUpcall;
        if (type.replacerClass != null) {
            try {
                this.replacer = (Replacer) type.replacerClass.newInstance();
            } catch(Throwable e) {
                throw new IOException("Could not instantiate replacer class "
                        + type.replacerClass.getName());
            }
        } else {
            this.replacer = null;
        }
        ibis.register(this);
        logger.debug(ibis.identifier() + ": Sendport '" + name + "' created");
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
        out = SerializationBase.createSerializationOutput(type.serialization,
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

    public PortType getType() {
        return type;
    }

    public synchronized Map<String, Object> properties() {
        return props;
    }

    public synchronized void setProperties(Map<String, Object> properties) {
        props = properties;
    }
    
    public synchronized Object getProperty(String key) {
        return props.get(key);
    }
    
    public synchronized void setProperty(String key, Object val) {
        props.put(key, val);
    }
    
    public synchronized ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        if (! connectionDowncalls) {
            throw new IbisConfigurationException("SendPort.lostConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.ReceivePortIdentifier[] result = lostConnections.toArray(
                new ibis.ipl.ReceivePortIdentifier[0]);
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
        logger.debug("Sendport '" + this.name + "' connecting to "
                + name + " at " + id);
        return connect(id, name, 0);
    }

    private void checkConnect(ReceivePortIdentifier r) throws IOException {

        if (receivers.size() > 0 && ! type.oneToMany) {
            throw new IbisConfigurationException("Sendport already has a "
                    + "connection and OneToMany not requested");
        }

        if (getInfo(r) != null) {
            throw new AlreadyConnectedException(
                    "This sendport was already connected to " + r);
        }
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier receiver,
            long timeout) throws IOException {

        logger.debug("Sendport '" + name + "' connecting to " + receiver);

        if (aMessageIsAlive) {
            throw new IOException(
                "A message was alive while adding a new connection");
        }

        ReceivePortIdentifier r = (ReceivePortIdentifier) receiver;

        checkConnect(r);

        addConnectionInfo(r, doConnect(r, timeout));
    }

    public synchronized ibis.ipl.ReceivePortIdentifier connect(
            ibis.ipl.IbisIdentifier id, String name, long timeout)
            throws IOException {
        ReceivePortIdentifier r = new ReceivePortIdentifier(name,
                (IbisIdentifier) id);

        logger.debug("Sendport '" + name + "' connecting to " + r);

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
        logger.debug("SendPort '" + name + "': added connection to " + ri);
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
        logger.debug("SendPort '" + name + "': start close()");
        if (aMessageIsAlive) {
            throw new IOException(
                "Trying to close a sendport port while a message is alive!");
        }
        if (closed) {
            throw new IOException("Port already closed");
        }
        try {
            closePort();
        } finally {
            ReceivePortIdentifier[] ports = receivers.keySet().toArray(
                    new ReceivePortIdentifier[0]);
            for (int i = 0; i < ports.length; i++) {
                SendPortConnectionInfo c = removeInfo(ports[i]);
                c.closeConnection();
            }
            closed = true;
            ibis.deRegister(this);
        }
        logger.debug("SendPort '" + name + "': close() done");
    }

    public synchronized void disconnect(ibis.ipl.ReceivePortIdentifier receiver)
            throws IOException {
        ReceivePortIdentifier r = (ReceivePortIdentifier) receiver;
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
                new ibis.ipl.ReceivePortIdentifier[0]);
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
    protected synchronized SendPortConnectionInfo[] connections() {
        return receivers.values().toArray(new SendPortConnectionInfo[0]);
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
     * method and then rethrows the exception unless the port has
     * connection upcalls.
     */
    protected void gotSendException(WriteMessage w, IOException e)
            throws IOException {
        handleSendException(w, e);
        if (connectUpcall == null) {
            // otherwise upcalls were done.
            throw e;
        }
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * This method must be called by an implementation when it detects that a
     * connection to a particular receive port is lost.  It takes care of the
     * user upcall if requested, and updates the administration accordingly.
     * @param id identifies the receive port.
     * @param cause the exception that describes the reason for the loss of
     * the connection.
     */
    protected void lostConnection(ReceivePortIdentifier id, Throwable cause) {
        if (connectionDowncalls) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } else if (connectUpcall != null) {
            connectUpcall.lostConnection(this, id, cause);
        }
        removeInfo(id);
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
    protected void initStream(DataOutputStream dataOut) {
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
