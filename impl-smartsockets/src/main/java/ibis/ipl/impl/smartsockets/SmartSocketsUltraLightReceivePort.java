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
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.servicelink.CallBack;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.util.ThreadPool;

public class SmartSocketsUltraLightReceivePort implements ReceivePort, CallBack, Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(SmartSocketsUltraLightReceivePort.class);

    private final PortType type;
    private final String name;
    private final MessageUpcall upcall;
// 	private final Properties properties;
    private final ReceivePortIdentifier id;
// 	private final SmartSocketsIbis ibis;

    private boolean allowUpcalls = false;
    private boolean closed = false;
    Properties properties;

    private final LinkedList<SmartSocketsUltraLightReadMessage> messages = new LinkedList<>();

    SmartSocketsUltraLightReceivePort(SmartSocketsIbis ibis, PortType type, String name, MessageUpcall upcall, Properties properties)
            throws IOException {

// 		this.ibis = ibis;
        this.type = type;
        this.name = name;
        this.upcall = upcall;
        this.properties = properties;
        // this.properties = properties;
        this.id = new ibis.ipl.impl.ReceivePortIdentifier(name, ibis.ident);

        ServiceLink link = ibis.getServiceLink();

        if (link == null) {
            throw new IOException("No ServiceLink available");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Registering ultralight receive port " + name);
        }

        link.register(name, this);

        if (type.hasCapability(PortType.RECEIVE_AUTO_UPCALLS) && upcall != null) {
            ThreadPool.createNew(this, "ConnectionHandler");
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    @Override
    public void close(long timeoutMillis) {
        close();
    }

    private synchronized boolean getClosed() {
        return closed;
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        return new SendPortIdentifier[0];
    }

    @Override
    public void disableConnections() {
        // empty ?
    }

    @Override
    public void enableConnections() {
        // empty ?
    }

    @Override
    public synchronized void disableMessageUpcalls() {
        allowUpcalls = false;
    }

    @Override
    public synchronized void enableMessageUpcalls() {
        // TODO Auto-generated method stub
        allowUpcalls = true;
        notifyAll();
    }

    @Override
    public PortType getPortType() {
        return type;
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return id;
    }

    @Override
    public SendPortIdentifier[] lostConnections() {
        return new SendPortIdentifier[0];
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SendPortIdentifier[] newConnections() {
        return new SendPortIdentifier[0];
    }

    @Override
    public ReadMessage poll() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0L);
    }

    @Override
    public ReadMessage receive(long timeoutMillis) throws IOException {
        return getMessage(timeoutMillis);
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

    @Override
    public void gotMessage(DirectSocketAddress src, DirectSocketAddress srcProxy, int opcode, boolean returnToSender, byte[][] message) {

        logger.debug("Got message from + " + src);

        if (returnToSender || opcode != 0xDEADBEEF || message == null || message.length == 0 || message[0] == null || message[0].length == 0) {
            logger.warn("Received malformed message from " + src.toString() + " (" + returnToSender + ", " + opcode + ", " + (message == null) + ", "
                    + message.length + ", " + (message[0] == null) + ", " + message[0].length + ")");
            return;
        }

        IbisIdentifier source = null;

        try {
            source = new IbisIdentifier(message[0]);

            if (logger.isDebugEnabled()) {
                logger.debug("Message was send by " + source);
            }

        } catch (Exception e) {
            logger.warn("Message from contains malformed IbisIdentifier", e);
            return;
        }

        SmartSocketsUltraLightReadMessage rm = null;

        try {
            rm = new SmartSocketsUltraLightReadMessage(this, new SendPortIdentifier("anonymous", source), message[1]);
        } catch (Exception e) {
            logger.warn("Message from contains malformed data", e);
            return;
        }

        synchronized (this) {
            messages.addLast(rm);
            notifyAll();
        }
    }

    private synchronized SmartSocketsUltraLightReadMessage getMessage(long timeout) {

        long endTime = System.currentTimeMillis() + timeout;

        while (!closed && messages.size() == 0) {
            if (timeout > 0) {
                long waitTime = endTime - System.currentTimeMillis();

                if (waitTime <= 0) {
                    break;
                }

                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        if (closed || messages.size() == 0) {
            return null;
        }

        return messages.removeFirst();
    }

    private synchronized boolean waitUntilUpcallAllowed() {

        while (!closed && !allowUpcalls) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }

        return !closed;
    }

    private void performUpcall(SmartSocketsUltraLightReadMessage message) {

        if (waitUntilUpcallAllowed()) {
            try {
                // Notify the message that is is processed from an upcall,
                // so that finish() calls can be detected.
                message.setInUpcall(true);
                upcall.upcall(message);
            } catch (IOException e) {
                if (!message.isFinished()) {
                    message.finish(e);
                    return;
                }
                logger.error("Got unexpected exception in upcall, continuing ...", e);
            } catch (Throwable t) {
                if (!message.isFinished()) {
                    IOException ioex = new IOException("Got Throwable: " + t.getMessage());
                    ioex.initCause(t);
                    message.finish(ioex);
                }
                return;
            } finally {
                message.setInUpcall(false);
            }
        }
    }

    protected void newUpcallThread() {
        ThreadPool.createNew(this, "ConnectionHandler");
    }

    @Override
    public void run() {
        while (!getClosed()) {
            SmartSocketsUltraLightReadMessage message = getMessage(0L);

            if (message != null) {
                performUpcall(message);

                if (message.finishCalledInUpcall()) {
                    // A new thread has take our place
                    return;
                }
            }
        }
    }
}
