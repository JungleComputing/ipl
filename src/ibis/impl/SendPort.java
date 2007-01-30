/* $Id: SendPort.java 4945 2006-12-15 13:19:22Z ceriel $ */

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

public abstract class SendPort implements ibis.ipl.SendPort {

    private static final Logger logger = GetLogger.getLogger("ibis.impl.SendPort");

    private long count = 0;

    protected final PortType type;

    protected final String name;

    public final SendPortIdentifier ident;

    private final boolean connectionDowncalls;

    protected final SendPortConnectUpcall connectUpcall;

    protected Map<String, Object> props = new HashMap<String, Object>();

    protected ArrayList<ReceivePortIdentifier> lostConnections
            = new ArrayList<ReceivePortIdentifier>();

    protected HashMap<ReceivePortIdentifier, SendPortConnectionInfo> receivers
            = new HashMap<ReceivePortIdentifier, SendPortConnectionInfo>();

    private boolean aMessageIsAlive = false;

    private int waitingForMessage = 0;

    private boolean closed = false;

    protected boolean numbered;

    protected String serialization;

    protected Ibis ibis;

    private Replacer replacer;

    protected SerializationOutput out;

    protected DataOutputStream dataOut;

    protected final WriteMessage w;

    private boolean oneToOne;

    private boolean oneToMany;

    protected SendPort(Ibis ibis, PortType type, String name, boolean connectionDowncalls,
            SendPortConnectUpcall connectUpcall) throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = new SendPortIdentifier(name, type.props, ibis.ident);
        this.connectionDowncalls = connectionDowncalls;
        this.connectUpcall = connectUpcall;
        this.numbered = type.props.isProp("communication", "Numbered");
        this.serialization = type.serialization;
        this.oneToMany = type.props.isProp("communication", "OneToMany");
        this.oneToOne = oneToMany
            || type.props.isProp("communication", "OneToOne")
            || type.props.isProp("communication", "ManyToOne");
        if (type.replacerClass != null) {
            try {
                this.replacer = (Replacer) type.replacerClass.newInstance();
            } catch(Throwable e) {
                throw new IOException("Could not instantiate replacer class "
                        + type.replacerClass.getName());
            }
        }
        logger.debug(ibis.identifier() + ": Sendport '" + name + "' created");
        w = new WriteMessage(this);
    }

    protected void initStream(DataOutputStream dataOut) {
        this.dataOut = dataOut;
        if (out != null) {
            try {
                out.close();
            } catch(Throwable e) {
                // ignored
            }
        }
        out = SerializationBase.createSerializationOutput(serialization,
                dataOut);
        if (replacer != null) {
            try {
                out.setReplacer(replacer);
            } catch(Exception e) {
                throw new Error("Exception in setReplacer should not happen", e);
            }
        }
    }

    protected synchronized SendPortConnectionInfo getInfo(
            ReceivePortIdentifier id) {
        return receivers.get(id);
    }

    private synchronized void addInfo(ReceivePortIdentifier id,
            SendPortConnectionInfo info) {
        receivers.put(id, info);
    }

    protected synchronized SendPortConnectionInfo removeInfo(
            ReceivePortIdentifier id) {
        return receivers.remove(id);
    }

    public synchronized ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        return receivers.keySet().toArray(
                new ibis.ipl.ReceivePortIdentifier[0]);
    }

    protected synchronized SendPortConnectionInfo[] connections() {
        return receivers.values().toArray(new SendPortConnectionInfo[0]);
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

    private void checkConnect() {
        if (! oneToOne) {
            throw new IbisConfigurationException("This sendport cannot connect "
                    + "because OneToOne is not requested");
        }

        if (receivers.size() > 0 && ! oneToMany) {
            throw new IbisConfigurationException("Sendport already has a "
                    + "connection and OneToMany not requested");
        }
    }

    public synchronized void connect(ibis.ipl.ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {
        logger.debug("Sendport '" + name + "' connecting to " + receiver);
        if (!type.properties().equals(receiver.type())) {
            throw new PortMismatchException(
                "Cannot connect ports of different PortTypes");
        }

        if (aMessageIsAlive) {
            throw new IOException(
                "A message was alive while adding a new connection");
        }

        checkConnect();

        doConnect((ReceivePortIdentifier) receiver, timeoutMillis);
    }

    public synchronized ibis.ipl.ReceivePortIdentifier connect(
            ibis.ipl.IbisIdentifier id, String name, long timeoutMillis)
            throws IOException {
        if (aMessageIsAlive) {
            throw new IOException(
                     "A message was alive while adding a new connection");
        }

        checkConnect();

        return doConnect((IbisIdentifier) id, name, timeoutMillis);
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        if (closed) {
            throw new IOException("newMessage call on closed sendport");
        }

        synchronized(this) {
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

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, internal use. May be redefined by an implementation.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

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
            notify();
        }
        count += cnt;
    }

    /**
     * Implements the SendPort side of a message finish with exception.
     * This method is called by the {@link WriteMessage#finish(java.io.IOException)}
     * implementation.
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

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * This method must be called by an implementation when it detects that a
     * connection to a particular receive port is lost.
     * It takes care of the user upcall if requested, and updates the administration
     * accordingly.
     * @param id the identification of the receive port.
     * @param cause the exception that describes the reason for the loss of the connection.
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
     * This method must be called by an implementation to add a new
     * connection to the connection table.
     * A call to this method may result from a connection attempt.
     * @param ri indicates the receive port being connected to.
     * @param connection The connection info, or <code>null</code> if the
     * connection was refused.
     * @exception IOException may be thrown when the connection fails.
     */
    protected synchronized void addConnectionInfo(ReceivePortIdentifier ri,
            SendPortConnectionInfo connection) throws IOException {
        logger.debug("SendPort '" + name + "': adding connection to " + ri);
        if (getInfo(ri) != null) {
            throw new AlreadyConnectedException(
                    "This sendport was already connected to " + ri);
        }
        if (connection == null) {
            throw new ConnectionRefusedException("Could not connect");
        }
        addInfo(ri, connection);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be implemented by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Must notify the receiveports that a new message is coming, if needed.
     * This method must also deal with sequencing, if implemented/required.
     * @exception IOException may be thrown when this notification fails.
     */
    protected abstract void announceNewMessage() throws IOException;

    protected abstract void doConnect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException;

    protected abstract ReceivePortIdentifier doConnect(IbisIdentifier id,
            String name, long timeoutMillis) throws IOException;

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
     * {@link #lostConnection(ibis.ipl.ReceivePortIdentifier, Throwable)}
     * method for each of them. After that, it should rethrow the exception
     * unless the port has connection upcalls.
     */
    protected abstract void handleSendException(WriteMessage w, IOException e)
            throws IOException;
}
