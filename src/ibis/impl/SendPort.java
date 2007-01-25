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
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortConnectUpcall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public abstract class SendPort implements ibis.ipl.SendPort, Config {

    private long count = 0;

    protected final PortType type;

    protected final String name;

    private final SendPortIdentifier ident;

    private final boolean connectionAdministration;

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

    protected final WriteMessage w;

    protected SendPort(Ibis ibis, PortType type, String name, IbisIdentifier id,
            boolean connectionAdministration,
            SendPortConnectUpcall connectUpcall) throws IOException {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = new SendPortIdentifier(name, type.props, id);
        this.connectionAdministration = connectionAdministration;
        this.connectUpcall = connectUpcall;
        this.numbered = type.props.isProp("communication", "Numbered");
        this.serialization = type.serialization;
        if (type.replacerClass != null) {
            try {
                this.replacer = (Replacer) type.replacerClass.newInstance();
            } catch(Throwable e) {
                throw new IOException("Could not instantiate replacer class "
                        + type.replacerClass.getName());
            }
        }
        if (DEBUG) {
            System.out.println(ibis.identifier() + ": Sendport '" + name
                    + "' created");
        }
        w = new WriteMessage(this);
    }

    protected void initStream(DataOutputStream dataOut) {
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

    public synchronized ReceivePortIdentifier[] connectedTo() {
        return receivers.keySet().toArray(new ReceivePortIdentifier[0]);
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

    protected synchronized void addCount(long cnt) {
        count += cnt;
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
    
    public synchronized ReceivePortIdentifier[] lostConnections() {
        if (! type.props.isProp("communication", "connectiondowncalls")) {
            throw new IbisConfigurationException("SendPort.lostConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ReceivePortIdentifier[] result = lostConnections.toArray(
                new ReceivePortIdentifier[0]);
        lostConnections.clear();
        return result;
    }

    protected void lostConnection(ReceivePortIdentifier id,
            Throwable cause) {
        if (connectionAdministration) {
            synchronized(this) {
                lostConnections.add(id);
            }
        } else if (connectUpcall != null) {
            connectUpcall.lostConnection(this, id, cause);
        }
        removeInfo(id);
    }

    public String name() {
        return name;
    }

    public ibis.ipl.SendPortIdentifier identifier() {
        return ident;
    }

    public void connect(ReceivePortIdentifier receiver) throws IOException {
        connect(receiver, 0);
    }

    public ReceivePortIdentifier connect(ibis.ipl.IbisIdentifier id,
            String name) throws IOException {
        if (DEBUG) {
            System.err.println("Sendport '" + this.name
                     + "' connecting to " + name + " at " + id);
        }
        return connect(id, name, 0);
    }

    public synchronized void connect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {
        if (DEBUG) {
            System.err.println("Sendport '" + name + "' connecting to "
                    + receiver);
        }
        if (!type.properties().equals(receiver.type())) {
            throw new PortMismatchException(
                "Cannot connect ports of different PortTypes");
        }

        if (aMessageIsAlive) {
            throw new IOException(
                "A message was alive while adding a new connection");
        }

        doConnect(receiver, timeoutMillis);
    }

    public synchronized ReceivePortIdentifier connect(
            ibis.ipl.IbisIdentifier id, String name, long timeoutMillis)
            throws IOException {
        if (aMessageIsAlive) {
            throw new IOException(
                     "A message was alive while adding a new connection");
        }
        return doConnect((ibis.impl.IbisIdentifier) id, name, timeoutMillis);
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

    protected abstract void announceNewMessage() throws IOException;

    protected abstract void doConnect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException;

    protected abstract ReceivePortIdentifier doConnect(IbisIdentifier id,
            String name, long timeoutMillis) throws IOException;

    protected synchronized void finishMessage(WriteMessage w, long cnt)
            throws IOException {
        aMessageIsAlive = false;
        if (waitingForMessage > 0) {
            notify();
        }
        count += cnt;
    }

    protected void finishMessage(WriteMessage w, IOException e) {
        try {
            finishMessage(w, 0L);
        } catch(IOException ex) {
            // ignored
        }
    }

    public synchronized void close() throws IOException {
        if (DEBUG) {
            System.err.println("SendPort '" + name + "': start close()");
        }
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
            ReceivePortIdentifier[] ports = connectedTo();
            for (int i = 0; i < ports.length; i++) {
                SendPortConnectionInfo c = removeInfo(ports[i]);
                c.closeConnection();
            }
            closed = true;
        }
        if (DEBUG) {
            System.err.println("SendPort '" + name + "': close() done");
        }
    }

    public synchronized void disconnect(ReceivePortIdentifier receiver)
            throws IOException {
        SendPortConnectionInfo c = removeInfo(receiver);
        if (c == null) {
            throw new IOException("Cannot disconnect from " + receiver
                    + " since we are not connected with it");
        }
        try {
            disconnectPort(receiver, c);
        } finally {
            c.closeConnection();
        }
    }

    /**
     * This method may be called by an implementation to add a new
     * connection to the connection table.
     * A call to this method may result from a connection attempt.
     * @param ri indicates the receive port being connected to.
     * @param connection The connection info, or <code>null</code> if the
     * connection was refused.
     * @exception IOException may be thrown when the connection fails.
     */
    protected synchronized void addConnectionInfo(ReceivePortIdentifier ri,
            SendPortConnectionInfo connection) throws IOException {
        if (DEBUG) {
            System.err.println("SendPort '" + name + "': adding connection to "
                    + ri);
        }
        if (getInfo(ri) != null) {
            throw new AlreadyConnectedException(
                    "This sendport was already connected to " + ri);
        }
        if (connection == null) {
            throw new ConnectionRefusedException("Could not connect");
        }
        addInfo(ri, connection);
    }

    protected abstract void disconnectPort(ReceivePortIdentifier receiver,
            SendPortConnectionInfo c) throws IOException;

    /**
     * Thie method should notify the connected receiveport(s) that this
     * sendport is being closed.
     * @exception IOException may be thrown when communication with the
     * receiveport(s) fails for some reason.
     */
    protected abstract void closePort() throws IOException;

    protected abstract long bytesWritten(WriteMessage w);

    protected abstract void handleSendException(WriteMessage w, IOException e)
            throws IOException;
}
