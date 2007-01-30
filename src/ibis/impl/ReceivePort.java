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

public abstract class ReceivePort implements ibis.ipl.ReceivePort {

    public static final String ANONYMOUS = "__anonymous__";

    private static final Logger logger = GetLogger.getLogger("ibis.impl.ReceivePort");

    // Possible results of a connection attempt.
    public static final byte ACCEPTED = 0;

    public static final byte DENIED = 1;

    public static final byte DISABLED = 2;

    public static final byte ALREADY_CONNECTED = 3;

    public static final byte TYPE_MISMATCH = 4;

    protected final PortType type;

    protected final String name;

    private long count = 0;

    private boolean connectionsEnabled = false;

    private boolean connectionAdministration = false;

    public final ReceivePortIdentifier ident;

    private ArrayList<SendPortIdentifier> lostConnections
        = new ArrayList<SendPortIdentifier>();

    private ArrayList<SendPortIdentifier> newConnections
        = new ArrayList<SendPortIdentifier>();

    private Map<String, Object> props = new HashMap<String, Object>();

    protected Upcall upcall;

    protected ReceivePortConnectUpcall connectUpcall;

    protected HashMap<SendPortIdentifier, ReceivePortConnectionInfo> connections
            = new HashMap<SendPortIdentifier, ReceivePortConnectionInfo>();

    protected boolean allowUpcalls = false;

    private Ibis ibis;

    protected final boolean numbered;

    protected final String serialization;

    public ReceivePort(Ibis ibis, PortType type, String name, Upcall upcall,
            ReceivePortConnectUpcall connectUpcall, boolean connectionAdmimistration) {
        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.ident = new ReceivePortIdentifier(name, type.props, ibis.ident);
        this.upcall = upcall;
        this.connectUpcall = connectUpcall;
        this.connectionAdministration = connectionAdministration;
        this.numbered = type.props.isProp("communication", "Numbered");
        this.serialization = type.serialization;
        ibis.register(this);
        logger.debug(ibis.ident + ": ReceivePort '" + name + "' created");
    }

    public synchronized void enableUpcalls() {
        allowUpcalls = true;
        notifyAll();
    }

    public synchronized void disableUpcalls() {
        allowUpcalls = false;
    }

    protected synchronized ReceivePortConnectionInfo getInfo(
            SendPortIdentifier id) {
        return connections.get(id);
    }

    protected synchronized void addInfo(SendPortIdentifier id,
            ReceivePortConnectionInfo info) {
        connections.put(id, info);
        notifyAll();
    }

    protected synchronized ReceivePortConnectionInfo removeInfo(
            SendPortIdentifier id) {
        ReceivePortConnectionInfo info = connections.remove(id);
        if (connections.size() == 0) {
            notifyAll();
        }
        return info;
    }

    public synchronized ibis.ipl.SendPortIdentifier[] connectedTo() {
        return connections.keySet().toArray(new ibis.ipl.SendPortIdentifier[0]);
    }

    protected synchronized ReceivePortConnectionInfo[] connections() {
        return connections.values().toArray(new ReceivePortConnectionInfo[0]);
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
        if (! connectionAdministration) {
            throw new IbisConfigurationException("ReceivePort.lostConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.SendPortIdentifier[] result = lostConnections.toArray(
                new ibis.ipl.SendPortIdentifier[0]);
        lostConnections.clear();
        return result;
    }

    public synchronized ibis.ipl.SendPortIdentifier[] newConnections() {
        if (! connectionAdministration) {
            throw new IbisConfigurationException("ReceivePort.newConnections()"
                    + " called but connectiondowncalls not configured");
        }
        ibis.ipl.SendPortIdentifier[] result = newConnections.toArray(
                new ibis.ipl.SendPortIdentifier[0]);
        newConnections.clear();
        return result;
    }

    protected void lostConnection(SendPortIdentifier id,
            Throwable e) {
        if (connectionAdministration) {
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

    public ReadMessage receive(long timeoutMillis) throws IOException {
        if (upcall != null) {
            throw new IbisConfigurationException(
                    "Configured Receiveport for upcalls, downcall not allowed");
        }

        return getMessage(timeoutMillis);
    }

    public void close() {
        doClose(0);
        ibis.deRegister(this);
    }

    public void close(long timeout) {
        doClose(timeout);
        ibis.deRegister(this);
    }

    public synchronized byte connectionAllowed(SendPortIdentifier id) {
        if (! id.type().equals(type.props)) {
            return TYPE_MISMATCH;
        }
        if (connectionsEnabled) {
            if (connectionAdministration) {
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

    protected abstract void doClose(long timeout);

    protected abstract ReadMessage getMessage(long timeoutMillis) throws IOException;

    protected abstract void finishMessage(ReadMessage r) throws IOException;

    protected abstract void finishMessage(ReadMessage r, IOException e);

    protected void setFinished(ReadMessage r, boolean val) {
        r.setFinished(val);
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

    protected ReadMessage createMessage(SerializationInput in,
            ReceivePortConnectionInfo info) {
        return new ReadMessage(in, info, this);
    }
}
