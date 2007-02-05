/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.Ibis;
import ibis.impl.PortType;
import ibis.impl.ReadMessage;
import ibis.impl.ReceivePort;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.ReceivePortConnectionInfo;
import ibis.impl.SendPortIdentifier;
import ibis.io.BufferedArrayInputStream;
import ibis.io.Conversion;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;
import ibis.util.GetLogger;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

class TcpReceivePort extends ReceivePort implements TcpProtocol {

    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.tcp.TcpReceivePort");

    class ConnectionHandler extends ReceivePortConnectionInfo 
            implements Runnable, TcpProtocol {

        private final Socket s;
        
        private boolean upcallCalledFinish = false;

        ConnectionHandler(SendPortIdentifier origin, Socket s, ReceivePort port)
                throws IOException {
            super(origin, port,
                    new BufferedArrayInputStream(s.getInputStream()));
            this.s = s;
        }

        ConnectionHandler(ConnectionHandler orig) {
            super(orig);
            s = orig.s;
            orig.upcallCalledFinish = true;
            ThreadPool.createNew(this, "ConnectionHandler");
            logger.debug("New connection handler, finish called from upcall");
        }

        void close(Throwable e) {
            try {
                in.close();
            } catch (Throwable z) {
                // Ignore.
            }
            in = null;
            try {
                s.close();
            } catch (Throwable x) {
                // ignore
            }
            logger.debug(name + ": connection with " + origin + " terminating");
            lostConnection(origin, e);
        }

        public void run() {
            try {
                reader(false);
            } catch (Throwable e) {
                logger.debug("ConnectionHandler.run, connected "
                        + "to " + origin + ", caught exception", e);
                close(e);
            }
        }

        void reader(boolean returnOnMessage) throws IOException {
            byte opcode = -1;

            while (in != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(name + ": handler for " + origin + " woke up");
                }
                opcode = in.readByte();
                switch (opcode) {
                case NEW_RECEIVER:
                    logger.debug(name + ": Got a NEW_RECEIVER from " + origin);
                    newStream();
                    break;
                case NEW_MESSAGE:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a NEW_MESSAGE from "
                                + origin);
                    }
                    setMessage(message, this);
                    if (returnOnMessage || upcallCalledFinish) {
                        // if upcallCalledFinish is set, a new thread was
                        // created.
                        return;
                    }
                    break;
                case CLOSE_ALL_CONNECTIONS:
                    logger.debug(name + ": Got a CLOSE_ALL_CONNECTIONS from "
                            + origin);
                    close(null);
                    return;
                case CLOSE_ONE_CONNECTION:
                    logger.debug(name + ": Got a CLOSE_ONE_CONNECTION from "
                            + origin);
                    // read the receiveport identifier from which the sendport
                    // disconnects.
                    byte[] length = new byte[Conversion.INT_SIZE];
                    in.readArray(length);
                    byte[] bytes = new byte[Conversion.defaultConversion
                            .byte2int(length, 0)];
                    in.readArray(bytes);
                    if (ident.equals(new ReceivePortIdentifier(bytes))) {
                        // Sendport is disconnecting from me.
                        logger.debug(name + ": disconnect from " + origin);
                        close(null);
                    }
                    break;
                default:
                    throw new IOException(name + ": Got illegal opcode "
                            + opcode + " from " + origin);
                }
            }
        }
    }

    private ReadMessage m = null;

    private boolean delivered = false;

    private final boolean no_connectionhandler_thread;

    private boolean reader_busy = false;

    private boolean fromDoUpcall = false;

    TcpReceivePort(Ibis ibis, TcpPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) {
        super(ibis, type, name, upcall, connUpcall, connectionAdministration);

        no_connectionhandler_thread = upcall == null && connUpcall == null
                && !type.manyToOne
                && !type.props.isProp("communication", "Poll")
                && !type.props.isProp("communication", "ReceiveTimeout");
    }

    void doUpcall(ReadMessage msg, ConnectionHandler con) {
        synchronized (this) {
            // Wait until the previous message was finished.
            while (this.m != null || !allowUpcalls) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
            this.m = msg;
        }

        try {
            upcall.upcall(msg);
        } catch (Throwable e) {
            // An error occured on receiving (or finishing!) the message during
            // the upcall.
            logger.error("Got Exception in upcall()", e);
            if (e instanceof IOException && ! con.upcallCalledFinish) {
                finishMessage(msg, (IOException) e);
                return;
            }
        }

        if (! con.upcallCalledFinish) {
            // It wasn't finished, so finish the message now.
            fromDoUpcall = true;
            try {
                msg.finish();
            } catch(Throwable e) {
                // Should not happen.
            } finally {
                fromDoUpcall = false;
            }
        }
    }

    protected synchronized void finishMessage(ReadMessage r) {
        m = null;
        notifyAll();

        if (! fromDoUpcall && upcall != null) {
            // Create a new connection handler.
            // The old one will terminate itself.
            new ConnectionHandler((ConnectionHandler) r.getInfo());
        }
    }

    protected synchronized void finishMessage(ReadMessage r, IOException e) {
        ((ConnectionHandler) r.getInfo()).close(e);
        m = null;
        notifyAll();
    }

    void setMessage(ReadMessage m, ConnectionHandler con) throws IOException {
        m.setFinished(false);
        if (type.numbered) {
            m.setSequenceNumber(m.readLong());
        }
        if (upcall != null) {
            doUpcall(m, con);
            return;
        }

        synchronized(this) {
            // Wait until the previous message was finished.
            if (!no_connectionhandler_thread) {
                while (this.m != null) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }

            this.m = m;
            delivered = false;

            if (!no_connectionhandler_thread) {
                notifyAll(); // now handle this message.

                // Wait until the receiver thread finishes this message.
                // We must wait here, because the thread that calls this method 
                // wants to read an opcode from the stream.
                // It can only read this opcode after the whole message is gone
                // first.
                while (this.m != null) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }
        }
    }

    protected ReadMessage getMessage(long timeout)
            throws IOException {

        if (no_connectionhandler_thread) {
            // Allow only one reader in.
            synchronized(this) {
                while (reader_busy && ! closed) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // ignored
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
                synchronized(this) {
                    while (connections.size() == 0 && ! closed) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }

                    // Wait until the current message is done
                    while (m != null && ! closed) {
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
                ((ConnectionHandler)conns[0]).reader(true);
                synchronized(this) {
                    if (m != null) {
                        reader_busy = false;
                        notifyAll();
                        return m;
                    }
                }
            }
        } else {
            synchronized(this) {
                while ((m == null || delivered) && !closed) {
                    try {
                        if (timeout > 0) {
                            wait(timeout);
                        } else {
                            wait();
                        }
                    } catch (Exception e) {
                        throw new ReceiveTimedOutException( "timeout expired in receive()");
                    }
                }
                delivered = true;
                return m;
            }
        }
    }

    protected ReadMessage doPoll() throws IOException {

        Thread.yield(); // Give connection handler thread a chance to deliver

        if (upcall != null) {
            return null;
        }

        synchronized (this) { // Connection handler thread may modify data.
            if (m == null || delivered) {
                return null;
            }
            if (m != null) {
                delivered = true;
            }
            return m;
        }
    }

    protected synchronized void closePort(long timeout) {
        ReceivePortConnectionInfo conns[] = connections();
        if (no_connectionhandler_thread && conns.length > 0) {
            ThreadPool.createNew((ConnectionHandler) conns[0], "ConnectionHandler");
        }
        if (timeout == 0) {
            while (connections.size() > 0) {
                try {
                    wait();
                } catch(Exception e) {
                    // ignored
                }
            }
        } else {
            long endTime = System.currentTimeMillis() + timeout;
            while (connections.size() > 0 && timeout > 0) {
                try {
                    wait(timeout);
                } catch(Exception e) {
                    // ignored
                }
                timeout = endTime - System.currentTimeMillis();
            }
            conns = connections();
            for (int i = 0; i < conns.length; i++) {
                ConnectionHandler conn = (ConnectionHandler) conns[i];
                conn.close(new IOException(
                            "receiver forcibly closed connection"));
            }
        }
        logger.debug(name + ":done receiveport.close");
    }

    synchronized void connect(SendPortIdentifier origin, Socket s)
            throws IOException {
        ConnectionHandler conn = new ConnectionHandler(origin, s, this);
        if (! no_connectionhandler_thread) {
            ThreadPool.createNew(conn, "ConnectionHandler");
        }
    }
}
