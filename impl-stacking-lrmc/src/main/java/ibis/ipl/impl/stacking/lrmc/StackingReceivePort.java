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

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

public class StackingReceivePort implements ReceivePort {

    final ReceivePort base;

    /**
     * This class forwards upcalls with the proper receive port.
     */
    private static final class ConnectUpcaller implements ReceivePortConnectUpcall {
        StackingReceivePort port;
        ReceivePortConnectUpcall upcaller;

        public ConnectUpcaller(StackingReceivePort port, ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        @Override
        public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        @Override
        public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe, Throwable reason) {
            upcaller.lostConnection(port, johnDoe, reason);
        }
    }

    /**
     * This class forwards message upcalls with the proper message.
     */
    private static final class Upcaller implements MessageUpcall {
        MessageUpcall upcaller;
        StackingReceivePort port;

        public Upcaller(MessageUpcall upcaller, StackingReceivePort port) {
            this.upcaller = upcaller;
            this.port = port;
        }

        @Override
        public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
            upcaller.upcall(new StackingReadMessage(m, port));
        }
    }

    public StackingReceivePort(PortType type, LrmcIbis ibis, String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties) throws IOException {
        if (connectUpcall != null) {
            connectUpcall = new ConnectUpcaller(this, connectUpcall);
        }
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }
        base = ibis.base.createReceivePort(type, name, upcall, connectUpcall, properties);
    }

    @Override
    public void close() throws IOException {
        base.close();
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        base.close(timeoutMillis);
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        return base.connectedTo();
    }

    @Override
    public void disableConnections() {
        base.disableConnections();
    }

    @Override
    public void disableMessageUpcalls() {
        base.disableMessageUpcalls();
    }

    @Override
    public void enableConnections() {
        base.enableConnections();
    }

    @Override
    public void enableMessageUpcalls() {
        base.enableMessageUpcalls();
    }

    @Override
    public PortType getPortType() {
        return base.getPortType();
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return base.identifier();
    }

    @Override
    public SendPortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    @Override
    public String name() {
        return base.name();
    }

    @Override
    public SendPortIdentifier[] newConnections() {
        return base.newConnections();
    }

    @Override
    public ReadMessage poll() throws IOException {
        ReadMessage m = base.poll();
        if (m != null) {
            m = new StackingReadMessage(m, this);
        }
        return m;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    @Override
    public ReadMessage receive(long timeoutMillis) throws IOException {
        return new StackingReadMessage(base.receive(timeoutMillis), this);
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
