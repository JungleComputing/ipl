/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    HashSet<IbisIdentifier> connections = new HashSet<>();
    boolean connectionsChanged = false;
    boolean closed = false;
    LrmcWriteMessage message = null;

    public LrmcSendPort(Multicaster om, LrmcIbis ibis, Properties props) {
        this.om = om;
        identifier = new LrmcSendPortIdentifier(ibis.identifier(), om.name);
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        om.sendPort = null;
        connections.clear();
        if (message != null) {
            throw new IOException("Close called while a message is alive");
        }
        om.removeSendPort();
    }

    @Override
    public void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    @Override
    public synchronized void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        if (closed) {
            throw new ConnectionFailedException("Sendport is closed", receiver);
        }
        if (!identifier.name.equals(receiver.name())) {
            throw new ConnectionFailedException("LRMCIbis sendport connect requires that the " + "receiveport has the same name", receiver);
        }
        if (connections.contains(receiver.ibisIdentifier())) {
            throw new AlreadyConnectedException("This connection already exists", receiver);
        }
        connections.add(receiver.ibisIdentifier());
        connectionsChanged = true;
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName) throws ConnectionFailedException {
        ReceivePortIdentifier id = new LrmcReceivePortIdentifier(ibisIdentifier, receivePortName);
        connect(id, 0, true);
        return id;
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        ReceivePortIdentifier id = new LrmcReceivePortIdentifier(ibisIdentifier, receivePortName);
        connect(id, timeoutMillis, fillTimeout);
        return id;
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers) throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {

        ArrayList<ReceivePortIdentifier> succes = new ArrayList<>();

        HashMap<ReceivePortIdentifier, Throwable> results = new HashMap<>();

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
                    ex.add(new ConnectionFailedException("Connection failed", rp, tmp));
                }
            }

            // Add a list of connections that were successful.
            ex.setObtainedConnections(succes.toArray(new ibis.ipl.ReceivePortIdentifier[succes.size()]));

            throw ex;
        }
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        ibis.ipl.ReceivePortIdentifier[] ids = new ibis.ipl.ReceivePortIdentifier[ports.size()];

        int index = 0;

        for (Map.Entry<ibis.ipl.IbisIdentifier, String> entry : ports.entrySet()) {
            ids[index++] = new LrmcReceivePortIdentifier(entry.getKey(), entry.getValue());
        }

        connect(ids, timeoutMillis, fillTimeout); // may throw an exception

        return ids;
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        // Not supported.
        return null;
    }

    @Override
    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
        if (closed) {
            throw new IOException("Sendport is closed");
        }
        if (!identifier.name.equals(receiver.name())) {
            throw new IOException("LRMCIbis sendport disconnect requires that the " + "receiveport has the same name");
        }
        if (!connections.contains(receiver.ibisIdentifier())) {
            throw new IOException("This connection does not exists");
        }
        connections.remove(receiver.ibisIdentifier());
        connectionsChanged = true;
    }

    @Override
    public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName) throws IOException {
        disconnect(new LrmcReceivePortIdentifier(ibisIdentifier, receivePortName));
    }

    @Override
    public PortType getPortType() {
        return om.portType;
    }

    @Override
    public SendPortIdentifier identifier() {
        return identifier;
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        // not supported
        return null;
    }

    @Override
    public String name() {
        return identifier.name;
    }

    @Override
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
            connectedTo = connections.toArray(new IbisIdentifier[connections.size()]);
        }

        message = new LrmcWriteMessage(this, om, connectedTo);
        return message;
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCRSendPort");
    }

    @Override
    public Map<String, String> managementProperties() {
        return new HashMap<>();
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCSendPort");
    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCSendPort");
    }

}
