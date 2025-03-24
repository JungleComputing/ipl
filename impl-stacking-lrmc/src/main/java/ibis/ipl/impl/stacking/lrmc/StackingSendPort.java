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
import java.util.Map;
import java.util.Properties;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

/**
 * Forwarding sendport, for porttypes not handled by this Ibis.
 */
public class StackingSendPort implements SendPort {

    final SendPort base;

    /**
     * Forwards a lostConnection upcall to the user, with the proper sendport.
     */
    private static final class DisconnectUpcaller implements SendPortDisconnectUpcall {
        StackingSendPort port;
        SendPortDisconnectUpcall upcaller;

        public DisconnectUpcaller(StackingSendPort port, SendPortDisconnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        @Override
        public void lostConnection(SendPort me, ReceivePortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }

    public StackingSendPort(PortType type, LrmcIbis ibis, String name, SendPortDisconnectUpcall connectUpcall, Properties props) throws IOException {

        if (connectUpcall != null) {
            connectUpcall = new DisconnectUpcaller(this, connectUpcall);
            base = ibis.base.createSendPort(type, name, connectUpcall, props);
        } else {
            base = ibis.base.createSendPort(type, name, null, props);
        }
    }

    @Override
    public void close() throws IOException {
        base.close();
    }

    @Override
    public void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {
        connect(receiver, 0L, true);
    }

    @Override
    public void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        base.connect(receiver, timeoutMillis, fillTimeout);

    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier id, String name) throws ConnectionFailedException {
        return connect(id, name, 0L, true);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        return base.connect(id, name, timeoutMillis, fillTimeout);
    }

    @Override
    public void connect(ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        connect(ports, 0L, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] ports, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
        base.connect(ports, timeoutMillis, fillTimeout);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0L, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        return base.connect(ports, timeoutMillis, fillTimeout);
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        return base.connectedTo();
    }

    @Override
    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
        base.disconnect(receiver);
    }

    @Override
    public void disconnect(IbisIdentifier id, String name) throws IOException {
        base.disconnect(id, name);
    }

    @Override
    public PortType getPortType() {
        return base.getPortType();
    }

    @Override
    public SendPortIdentifier identifier() {
        return base.identifier();
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    @Override
    public String name() {
        return base.name();
    }

    @Override
    public WriteMessage newMessage() throws IOException {
        return new StackingWriteMessage(base.newMessage(), this);
    }

    @Override
    public Map<String, String> managementProperties() {
        return base.managementProperties();
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return base.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        base.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val) throws NoSuchPropertyException {
        base.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        base.printManagementProperties(stream);
    }
}
