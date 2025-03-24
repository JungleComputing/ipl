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
package ibis.ipl.impl.smartsockets;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.VirtualSocketAddress;

public class SmartSocketsUltraLightSendPort implements SendPort {

    protected static final Logger logger = LoggerFactory.getLogger(SmartSocketsUltraLightSendPort.class);

    private final PortType type;
    private final String name;
    final Properties properties;
    private final SmartSocketsIbis ibis;

    private final SendPortIdentifier sid;

    private boolean closed = false;

    private SmartSocketsUltraLightWriteMessage message;

    private boolean messageInUse = false;

    private final Set<ReceivePortIdentifier> connections = new HashSet<>();

    private final byte[][] messageToHub;

    SmartSocketsUltraLightSendPort(SmartSocketsIbis ibis, PortType type, String name, Properties props) throws IOException {

        this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.properties = props;

        sid = new ibis.ipl.impl.SendPortIdentifier(name, ibis.ident);

        messageToHub = new byte[2][];
        messageToHub[0] = ibis.ident.toBytes();
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        notifyAll();
    }

    @Override
    public synchronized void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {
        connections.add(receiver);
    }

    @Override
    public void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        connect(receiver);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName) throws ConnectionFailedException {
        ReceivePortIdentifier id = new ibis.ipl.impl.ReceivePortIdentifier(receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier);
        connect(id);
        return id;
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier, String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        return connect(ibisIdentifier, receivePortName);
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers) throws ConnectionsFailedException {

        LinkedList<ConnectionFailedException> tmp = null;
        LinkedList<ReceivePortIdentifier> success = new LinkedList<>();

        for (ReceivePortIdentifier id : receivePortIdentifiers) {
            try {
                connect(id);
                success.add(id);
            } catch (ConnectionFailedException e) {

                if (tmp == null) {
                    tmp = new LinkedList<>();
                }

                tmp.add(e);
            }
        }

        if (tmp != null && tmp.size() > 0) {
            ConnectionsFailedException c = new ConnectionsFailedException("Failed to connect");

            for (ConnectionFailedException ex : tmp) {
                c.add(ex);
            }

            c.setObtainedConnections(success.toArray(new ReceivePortIdentifier[success.size()]));
            throw c;
        }
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
        connect(receivePortIdentifiers);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {

        ReceivePortIdentifier[] tmp = new ReceivePortIdentifier[ports.size()];

        int index = 0;

        for (Entry<IbisIdentifier, String> e : ports.entrySet()) {
            tmp[index++] = new ibis.ipl.impl.ReceivePortIdentifier(e.getValue(), (ibis.ipl.impl.IbisIdentifier) e.getKey());
        }

        connect(tmp);
        return tmp;
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        return connect(ports);
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        return connections.toArray(new ReceivePortIdentifier[0]);
    }

    @Override
    public synchronized void disconnect(ReceivePortIdentifier receiver) throws IOException {
        if (!connections.remove(receiver)) {
            throw new IOException("Not connected to " + receiver);
        }
    }

    @Override
    public void disconnect(IbisIdentifier ibisIdentifier, String receivePortName) throws IOException {
        disconnect(new ibis.ipl.impl.ReceivePortIdentifier(receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier));
    }

    @Override
    public PortType getPortType() {
        return type;
    }

    @Override
    public SendPortIdentifier identifier() {
        return sid;
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        return new ReceivePortIdentifier[0];
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized WriteMessage newMessage() throws IOException {

        while (!closed && messageInUse) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (closed) {
            throw new IOException("Sendport is closed");
        }

        messageInUse = true;
        message = new SmartSocketsUltraLightWriteMessage(this);
        return message;
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> managementProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        // TODO Auto-generated method stub

    }

    private void send(byte[] data) throws UnknownHostException, MalformedAddressException {

        ServiceLink link = ibis.getServiceLink();

        if (link == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No servicelink available");
            }

            return;
        }

        // messageToHub[1] = Arrays.copyOfRange(buffer, 0, len);
        // This is Java 6 speak. Modified to equivalent code that
        // is acceptable to Java 1.5. --Ceriel
        messageToHub[1] = new byte[data.length];
        System.arraycopy(data, 0, messageToHub[1], 0, data.length);

        for (ReceivePortIdentifier id : connections) {

            ibis.ipl.impl.IbisIdentifier dst = (ibis.ipl.impl.IbisIdentifier) id.ibisIdentifier();
            VirtualSocketAddress a = VirtualSocketAddress.fromBytes(dst.getImplementationData(), 0);

            if (logger.isDebugEnabled()) {
                logger.debug("Sending message to " + a);
            }

            link.send(a.machine(), a.hub(), id.name(), 0xDEADBEEF, messageToHub);
        }
    }

    public synchronized void finishedMessage(byte[] m) throws IOException {

        // int len = (int) message.bytesWritten();

        try {
            send(m);
        } catch (Exception e) {
            logger.debug("Failed to send message to " + connections, e);
        }

        // message.reset(); No longer needed. --Ceriel
        messageInUse = false;
        notifyAll();
    }

    /*
     * private void send(ReceivePortIdentifier id, byte [] data) throws
     * UnknownHostException, MalformedAddressException {
     *
     * ServiceLink link = ibis.getServiceLink();
     *
     * if (link != null) { ibis.ipl.impl.IbisIdentifier dst =
     * (ibis.ipl.impl.IbisIdentifier) id.ibisIdentifier(); VirtualSocketAddress a =
     * VirtualSocketAddress.fromBytes(dst.getImplementationData(), 0);
     *
     * byte [][] message = new byte[2][];
     *
     * message[0] = ibis.ident.toBytes(); message[1] = data;
     *
     * if (logger.isDebugEnabled()) { logger.debug("Sending message to " + a); }
     *
     * link.send(a.machine(), a.hub(), id.name(), 0xDEADBEEF, message); } else {
     *
     * if (logger.isDebugEnabled()) { logger.debug("No servicelink available"); } }
     * }
     *
     *
     * public synchronized void finishedMessage() throws IOException {
     *
     * int len = (int) message.bytesWritten();
     *
     * byte [] m = buffer;
     *
     * if (len < buffer.length) { m = Arrays.copyOfRange(buffer, 0, len); }
     *
     * for (ReceivePortIdentifier id : connections) { try { send(id, m); } catch
     * (Exception e) { logger.debug("Failed to send message to " + id, e); } }
     *
     * message.reset(); messageInUse = false; notifyAll(); }
     */

    public synchronized void finishedMessage(IOException exception) throws IOException {
        message.reset();
        messageInUse = false;
        notifyAll();
    }
}
