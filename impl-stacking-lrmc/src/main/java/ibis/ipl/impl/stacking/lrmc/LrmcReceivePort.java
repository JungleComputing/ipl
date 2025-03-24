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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.util.ThreadPool;

public class LrmcReceivePort implements ibis.ipl.ReceivePort, Runnable {

    private final LrmcReceivePortIdentifier identifier;
    private final MessageUpcall upcall;
    private final Multicaster om;
    private LrmcReadMessage message = null;
    private boolean closed = false;
    private boolean messageIsAvailable = false;
    private boolean upcallsEnabled = false;

    public LrmcReceivePort(Multicaster om, LrmcIbis ibis, MessageUpcall upcall, Properties properties) throws IOException {
        this.om = om;
        identifier = new LrmcReceivePortIdentifier(ibis.identifier(), om.name);
        this.upcall = upcall;
        if (upcall != null && !om.portType.hasCapability(PortType.RECEIVE_AUTO_UPCALLS)) {
            throw new IbisConfigurationException("no connection upcalls requested for this port type");
        }
        ThreadPool.createNew(this, "ReceivePort");
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        om.removeReceivePort();
        notifyAll();
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        close();
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        throw new IbisConfigurationException("connection downcalls not supported");
    }

    @Override
    public void disableConnections() {
        // throw new IbisConfigurationException("connection upcalls not supported");
    }

    @Override
    public synchronized void disableMessageUpcalls() {
        upcallsEnabled = false;
    }

    @Override
    public void enableConnections() {
        // throw new IbisConfigurationException("connection upcalls not supported");
    }

    @Override
    public synchronized void enableMessageUpcalls() {
        upcallsEnabled = true;
        notifyAll();
    }

    @Override
    public PortType getPortType() {
        return om.portType;
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return identifier;
    }

    @Override
    public SendPortIdentifier[] lostConnections() {
        throw new IbisConfigurationException("connection downcalls not supported");
    }

    @Override
    public String name() {
        return identifier.name;
    }

    @Override
    public SendPortIdentifier[] newConnections() {
        throw new IbisConfigurationException("connection downcalls not supported");
    }

    @Override
    public synchronized ReadMessage poll() throws IOException {
        if (closed) {
            throw new IOException("port is closed");
        }
        if (messageIsAvailable) {
            messageIsAvailable = false;
            return message;
        }
        return null;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    @Override
    public ReadMessage receive(long timeout) throws IOException {
        if (upcall != null) {
            throw new IbisConfigurationException("Configured Receiveport for upcalls, downcall not allowed");
        }
        boolean hasTimeout = false;
        if (timeout < 0) {
            throw new IOException("timeout must be a non-negative number");
        }
        if (timeout > 0 && !om.portType.hasCapability(PortType.RECEIVE_TIMEOUT)) {
            throw new IbisConfigurationException("This port is not configured for receive() with timeout");
        }

        synchronized (this) {
            while (!messageIsAvailable) {
                if (closed) {
                    throw new IOException("port is closed");
                }
                if (timeout > 0) {
                    hasTimeout = true;
                    long tm = System.currentTimeMillis();
                    try {
                        wait(timeout);
                    } catch (Throwable e) {
                        // ignored
                    }
                    long tm1 = System.currentTimeMillis();
                    timeout -= (tm1 - tm);
                } else if (hasTimeout) {
                    // timeout expired
                    throw new ReceiveTimedOutException();
                } else {
                    try {
                        wait();
                    } catch (Throwable e) {
                        // ignored
                    }
                }
            }
            messageIsAvailable = false;
            return message;
        }
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCReceivePort");
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
        throw new NoSuchPropertyException("No properties in LRMCReceivePort");
    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in LRMCReceivePort");
    }

    synchronized void doFinish() {
        message = null;
        notifyAll();
    }

    private boolean doUpcall(LrmcReadMessage msg) {
        synchronized (this) {
            // Wait until upcalls are enabled.
            while (!upcallsEnabled) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
        try {
            msg.setInUpcall(true);
            upcall.upcall(msg);
        } catch (IOException e) {
            if (!msg.isFinished) {
                msg.finish(e);
                return false;
            }
        } catch (ClassNotFoundException e) {
            if (!msg.isFinished) {
                IOException ioex = new IOException("Got ClassNotFoundException: " + e.getMessage());
                ioex.initCause(e);
                msg.finish(ioex);
                return false;
            }
            return true;
        } catch (Throwable e) {
            System.exit(1);
        } finally {
            msg.setInUpcall(false);
        }

        if (!msg.isFinished) {
            try {
                msg.finish();
            } catch (IOException e) {
                msg.finish(e);
            }
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        while (true) {
            if (closed) {
                return;
            }
            LrmcReadMessage m = om.receive();
            if (m == null) {
                return;
            }
            synchronized (this) {
                while (message != null) {
                    try {
                        wait();
                    } catch (Throwable e) {
                        // ignored
                    }
                }
                messageIsAvailable = true;
                message = m;
                if (upcall == null) {
                    notifyAll();
                }
            }
            if (upcall != null) {
                if (doUpcall(m)) {
                    // The upcall method called finish.
                    // return this thread.
                    return;
                }
            }
        }
    }
}
