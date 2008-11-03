package ibis.ipl.impl.stacking.lrmc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

public class LrmcSendPort implements SendPort {

    private final LrmcSendPortIdentifier identifier;
    private final Multicaster om;
    IbisIdentifier[] connectedTo = new IbisIdentifier[0];
    HashSet<IbisIdentifier> connections = new HashSet<IbisIdentifier>();
    boolean connectionsChanged = false;
    boolean closed = false;
    LrmcWriteMessage message = null;

    public LrmcSendPort(Multicaster om, LrmcIbis ibis, Properties props) {
        this.om = om;
        identifier = new LrmcSendPortIdentifier(ibis.identifier(), om.name);
    }

    public synchronized void close() throws IOException {
        closed = true;
        om.sendPort = null;
        connections.clear();
        if (message != null) {
            throw new IOException("Close called while a message is alive");
        }
        om.removeSendPort();
    }

    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    public synchronized void connect(ReceivePortIdentifier receiver,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        if (closed) {
            throw new ConnectionFailedException("Sendport is closed", receiver);
        }
        if (!identifier.name.equals(receiver.name())) {
            throw new ConnectionFailedException(
                    "LRMCIbis sendport connect requires that the "
                            + "receiveport has the same name", receiver);
        }
        if (connections.contains(receiver.ibisIdentifier())) {
            throw new AlreadyConnectedException(
                    "This connection already exists", receiver);
        }
        connections.add(receiver.ibisIdentifier());
        connectionsChanged = true;
    }

    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws ConnectionFailedException {
        ReceivePortIdentifier id = new LrmcReceivePortIdentifier(
                ibisIdentifier, receivePortName);
        connect(id, 0, true);
        return id;
    }

    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        ReceivePortIdentifier id = new LrmcReceivePortIdentifier(
                ibisIdentifier, receivePortName);
        connect(id, timeoutMillis, fillTimeout);
        return id;
    }

    public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    public void connect(ReceivePortIdentifier[] receivePortIdentifiers,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {

        ArrayList<ReceivePortIdentifier> succes = new ArrayList<ReceivePortIdentifier>();

        HashMap<ReceivePortIdentifier, Throwable> results = new HashMap<ReceivePortIdentifier, Throwable>();

        for (ReceivePortIdentifier id : receivePortIdentifiers) {
            try {
                connect(id, 0, true);
                succes.add(id);
            } catch (Throwable e) {
                results.put(id, e);
            }
        }

        // We are done OR we ran out of time OR we tried everyone at once and
        // are not supposed to continue.

        if (succes.size() != receivePortIdentifiers.length) {
            // Some connections have failed. Throw a ConnectionsFailedException
            // to inform the user of this.

            // Gather all exceptions from the result map. Add new once for
            // targets that have not been tried at all.
            ConnectionsFailedException ex = new ConnectionsFailedException();

            for (ReceivePortIdentifier rp : results.keySet()) {

                Throwable tmp = results.get(rp);

                if (tmp instanceof ConnectionFailedException) {
                    ex.add((ConnectionFailedException) tmp);
                } else {
                    ex.add(new ConnectionFailedException("Connection failed",
                            rp, tmp));
                }
            }

            // Add a list of connections that were successful.
            ex
                    .setObtainedConnections(succes
                            .toArray(new ibis.ipl.ReceivePortIdentifier[succes
                                    .size()]));

            throw ex;
        }
    }

    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        ibis.ipl.ReceivePortIdentifier[] ids = new ibis.ipl.ReceivePortIdentifier[ports
                .size()];

        int index = 0;

        for (Map.Entry<ibis.ipl.IbisIdentifier, String> entry : ports
                .entrySet()) {
            ids[index++] = new LrmcReceivePortIdentifier(entry.getKey(), entry
                    .getValue());
        }

        connect(ids, timeoutMillis, fillTimeout); // may throw an exception

        return ids;
    }

    public ReceivePortIdentifier[] connectedTo() {
        // Not supported.
        return null;
    }

    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
        if (closed) {
            throw new IOException("Sendport is closed");
        }
        if (!identifier.name.equals(receiver.name())) {
            throw new IOException(
                    "LRMCIbis sendport disconnect requires that the "
                            + "receiveport has the same name");
        }
        if (!connections.contains(receiver.ibisIdentifier())) {
            throw new IOException("This connection does not exists");
        }
        connections.remove(receiver.ibisIdentifier());
        connectionsChanged = true;
    }

    public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName)
            throws IOException {
        disconnect(new LrmcReceivePortIdentifier(ibisIdentifier,
                receivePortName));
    }

    public PortType getPortType() {
        return om.portType;
    }

    public SendPortIdentifier identifier() {
        return identifier;
    }

    public ReceivePortIdentifier[] lostConnections() {
        // not supported
        return null;
    }

    public String name() {
        return identifier.name;
    }

    public synchronized WriteMessage newMessage() throws IOException {
        if (closed) {
            throw new IOException("Sendport is closed");
        }

        while (message != null) {
            try {
                wait();
            } catch (Throwable e) {
                // ignored
            }
        }

        if (connectionsChanged) {
            connectionsChanged = false;
            connectedTo = connections.toArray(new IbisIdentifier[connections
                    .size()]);
        }

        message = new LrmcWriteMessage(this, om, connectedTo);
        return message;
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCRSendPort");
    }

    public Map<String, String> managementProperties() {
        return new HashMap<String, String>();
    }

    public void printManagementProperties(PrintStream stream) {
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCSendPort");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCSendPort");
    }

}
