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
/* $Id$ */

package ibis.ipl.impl.smartsockets;

import java.io.IOException;
import java.util.Properties;

import ibis.io.BufferedArrayInputStream;
import ibis.io.Conversion;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.util.ThreadPool;

class SmartSocketsReceivePort extends ReceivePort implements SmartSocketsProtocol {

    class ConnectionHandler extends ReceivePortConnectionInfo implements Runnable, SmartSocketsProtocol {

        private final VirtualSocket s;

        ConnectionHandler(SendPortIdentifier origin, VirtualSocket s, ReceivePort port, BufferedArrayInputStream in) throws IOException {
            super(origin, port, in);
            this.s = s;
        }

        @Override
        public void close(Throwable e) {
            super.close(e);
            try {
                s.close();
            } catch (Throwable x) {
                // ignore
            }
        }

        @Override
        public String connectionType() {
            return s.toString();
        }

        @Override
        public void run() {
            logger.info("Started connection handler thread");
            try {
                if (lazy_connectionhandler_thread) {
                    int interval = 10;
                    // For disconnects, there must be a reader thread, but we
                    // don't really want that. So, we have a thread that only
                    // checks every second.
                    for (;;) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("lazy handler sleeping " + interval + " ms.");
                        }
                        synchronized (this) {
                            // Wait on this handler. We cannot use the port for this,
                            // since that one wakes up when finish() is called on a message.
                            try {
                                wait(interval);
                            } catch (Throwable e) {
                                // ignore
                            }
                        }
                        synchronized (port) {
                            // If there is a reader, or a message is active,
                            // continue.
                            if (reader_busy || ((SmartSocketsReceivePort) port).getPortMessage() != null) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("lazy handler woke up, continues");
                                }
                                if (interval < 1000) {
                                    interval += 10;
                                }
                                continue;
                            }
                            if (closed) {
                                return;
                            }
                            reader_busy = true;
                            interval = 10;
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("lazy handler starting read ...");
                        }
                        reader(true);
                        synchronized (port) {
                            reader_busy = false;
                            port.notifyAll();
                        }
                    }
                } else {
                    reader(true);
                }
            } catch (Throwable e) {
                if (logger.isInfoEnabled()) {
                    logger.info("ConnectionHandler.run, connected " + "to " + origin + ", caught exception", e);
                }
                close(e);
            }
        }

        @Override
        protected void upcallCalledFinish() {
            super.upcallCalledFinish();
            ThreadPool.createNew(this, "ConnectionHandler");
        }

        void reader(boolean fromHandlerThread) throws IOException {
            byte opcode = -1;

            // Moved here to prevent deadlocks and timeouts when using sun
            // serialization -- Jason
            if (in == null) {
                newStream();
            }

            while (in != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(name + ": handler for " + origin + " woke up");
                }
                opcode = in.readByte();
                switch (opcode) {
                case NEW_RECEIVER:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a NEW_RECEIVER from " + origin);
                    }
                    newStream();
                    break;
                case NEW_MESSAGE:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a NEW_MESSAGE from " + origin);
                    }
                    message.setFinished(false);
                    if (numbered) {
                        message.setSequenceNumber(message.readLong());
                    }
                    ReadMessage m = message;
                    messageArrived(m, fromHandlerThread);
                    // Note: if upcall calls finish, a new message is
                    // allocated, so we cannot look at "message" anymore.
                    if (lazy_connectionhandler_thread || !fromHandlerThread || m.finishCalledInUpcall()) {
                        return;
                    }
                    break;
                case CLOSE_ALL_CONNECTIONS:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a CLOSE_ALL_CONNECTIONS from " + origin);
                    }
                    close(null);
                    if (lazy_connectionhandler_thread && !fromHandlerThread) {
                        // Wake up the connection handler thread so that it can die.
                        synchronized (this) {
                            notifyAll();
                        }
                    }
                    return;
                case CLOSE_ONE_CONNECTION:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a CLOSE_ONE_CONNECTION from " + origin);
                    }
                    // read the receiveport identifier from which the sendport
                    // disconnects.
                    byte[] length = new byte[Conversion.INT_SIZE];
                    in.readArray(length);
                    byte[] bytes = new byte[Conversion.defaultConversion.byte2int(length, 0)];
                    in.readArray(bytes);
                    ReceivePortIdentifier identifier = new ReceivePortIdentifier(bytes);
                    if (ident.equals(identifier)) {
                        // Sendport is disconnecting from me.
                        if (logger.isDebugEnabled()) {
                            logger.debug(name + ": disconnect from " + origin + ", fromHandlerThread = " + fromHandlerThread);
                        }

                        // FIXME!
                        //
                        // This is here to make sure the close is processed before a new
                        // connections can be made (by the same sendport). Without this ack,
                        // an application that uses a single sendport that connects/disconnects
                        // for each message may get an 'AlreadyConnectedException', because the
                        // connect overtakes the disconnect...
                        //
                        // Unfortunately, it also causes a deadlock in 1-to-1 explict receive
                        // applications -- J

                        // Fixed by lazy connection handler thread. --Ceriel
                        try {
                            in.close();
                        } catch (Throwable z) {
                            // ignore
                        }

                        closed = true;
                        in = null;
                        if (logger.isDebugEnabled()) {
                            logger.debug(port.name + ": connection with " + origin + " closing");
                        }

                        port.lostConnection(origin, null);

                        s.getOutputStream().write(0);

                        try {
                            dataIn.close();
                        } catch (Throwable z) {
                            // ignore
                        }
                        try {
                            s.close();
                        } catch (Throwable x) {
                            // ignore
                        }
                        if (lazy_connectionhandler_thread && !fromHandlerThread) {
                            // Wake up the connection handler thread so that it can die.
                            synchronized (this) {
                                notifyAll();
                            }
                        }
                    }
                    break;
                default:
                    throw new IOException(name + ": Got illegal opcode " + opcode + " from " + origin);
                }
            }
        }
    }

    private final boolean lazy_connectionhandler_thread;

    private boolean reader_busy = false;

    SmartSocketsReceivePort(Ibis ibis, PortType type, String name, MessageUpcall upcall, ReceivePortConnectUpcall connUpcall, Properties props)
            throws IOException {
        super(ibis, type, name, upcall, connUpcall, props);

        lazy_connectionhandler_thread = upcall == null && connUpcall == null
                && (type.hasCapability(PortType.CONNECTION_ONE_TO_ONE) || type.hasCapability(PortType.CONNECTION_ONE_TO_MANY))
                && !type.hasCapability(PortType.RECEIVE_POLL) && !type.hasCapability(PortType.RECEIVE_TIMEOUT);
    }

    Properties getProperties() {
        return properties;
    }

    private ReadMessage getPortMessage() {
        return message;
    }

    public void messageArrived(ReadMessage msg, boolean fromHandlerThread) {
        super.messageArrived(msg);
        if (fromHandlerThread && upcall == null) {
            synchronized (this) {
                // Wait until the message is finished before starting to
                // read from the stream again ...
                while (!msg.isFinished()) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
        }
    }

    @Override
    public ReadMessage getMessage(long timeout) throws IOException {
        if (lazy_connectionhandler_thread) {
            // Allow only one reader in.
            synchronized (this) {
                // First check if the lazy thread delivered a message.
                if (message != null && !delivered) {
                    return super.getMessage(timeout);
                }
                while (reader_busy && !closed) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // ignored
                    }
                    // Check lazy thread again.
                    if (message != null && !delivered) {
                        return super.getMessage(timeout);
                    }
                }
                if (closed) {
                    throw new IOException("receive() on closed port");
                }
                reader_busy = true;
            }
            // Since we don't have any threads or timeout here, this 'reader'
            // call directly handles the receive.
            for (;;) {
                // Wait until there is a connection
                synchronized (this) {
                    while (connections.size() == 0 && !closed) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }

                    // Wait until the current message is done
                    while (message != null && !closed) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                    if (closed) {
                        reader_busy = false;
                        notifyAll();
                        throw new IOException("receive() on closed port");
                    }
                }

                ReceivePortConnectionInfo conns[] = connections();
                // Note: This call does NOT always result in a message!
                ((ConnectionHandler) conns[0]).reader(false);
                synchronized (this) {
                    if (message != null) {
                        reader_busy = false;
                        notifyAll();
                        return message;
                    }
                }
            }
        } else {
            return super.getMessage(timeout);
        }
    }

    void connect(SendPortIdentifier origin, VirtualSocket s, BufferedArrayInputStream in) throws IOException {
        ConnectionHandler conn;

        synchronized (this) {
            conn = new ConnectionHandler(origin, s, this, in);
        }
        // ThreadPool.createNew(conn, "ConnectionHandler");
        // We are already in a dedicated thread, so no need to create a new
        // one!
        // But this method was synchronized!!! Fixed (Ceriel).
        conn.run();
    }

    @Override
    public synchronized void closePort(long timeout) {
        ReceivePortConnectionInfo conns[] = connections();
        if (lazy_connectionhandler_thread && conns.length > 0) {
            // Wakeup connection handler thread, otherwise this may take a second ...
            synchronized (conns[0]) {
                conns[0].notifyAll();
            }
        }
        super.closePort(timeout);
    }

}
