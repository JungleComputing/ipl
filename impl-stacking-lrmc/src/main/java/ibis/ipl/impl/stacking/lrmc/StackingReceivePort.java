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

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class StackingReceivePort implements ReceivePort {

    final ReceivePort base;

    /**
     * This class forwards upcalls with the proper receive port.
     */
    private static final class ConnectUpcaller implements
            ReceivePortConnectUpcall {
        StackingReceivePort port;
        ReceivePortConnectUpcall upcaller;

        public ConnectUpcaller(StackingReceivePort port,
                ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
        }

        public boolean gotConnection(ReceivePort me,
                SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe,
                Throwable reason) {
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

        public void upcall(ReadMessage m) throws IOException,
                ClassNotFoundException {
            upcaller.upcall(new StackingReadMessage(m, port));
        }
    }

    public StackingReceivePort(PortType type, LrmcIbis ibis, String name,
            MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties) throws IOException {
        if (connectUpcall != null) {
            connectUpcall = new ConnectUpcaller(this, connectUpcall);
        }
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }
        base = ibis.base.createReceivePort(type, name, upcall, connectUpcall,
                properties);
    }

    public void close() throws IOException {
        base.close();
    }

    public void close(long timeoutMillis) throws IOException {
        base.close(timeoutMillis);
    }

    public SendPortIdentifier[] connectedTo() {
        return base.connectedTo();
    }

    public void disableConnections() {
        base.disableConnections();
    }

    public void disableMessageUpcalls() {
        base.disableMessageUpcalls();
    }

    public void enableConnections() {
        base.enableConnections();
    }

    public void enableMessageUpcalls() {
        base.enableMessageUpcalls();
    }

    public PortType getPortType() {
        return base.getPortType();
    }

    public ReceivePortIdentifier identifier() {
        return base.identifier();
    }

    public SendPortIdentifier[] lostConnections() {
        return base.lostConnections();
    }

    public String name() {
        return base.name();
    }

    public SendPortIdentifier[] newConnections() {
        return base.newConnections();
    }

    public ReadMessage poll() throws IOException {
        ReadMessage m = base.poll();
        if (m != null) {
            m = new StackingReadMessage(m, this);
        }
        return m;
    }

    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    public ReadMessage receive(long timeoutMillis) throws IOException {
        return new StackingReadMessage(base.receive(timeoutMillis), this);
    }

    public Map<String, String> managementProperties() {
        return base.managementProperties();
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return base.getManagementProperty(key);
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        base.setManagementProperties(properties);
    }

    public void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        base.setManagementProperty(key, val);
    }

    public void printManagementProperties(PrintStream stream) {
        base.printManagementProperties(stream);
    }
}
