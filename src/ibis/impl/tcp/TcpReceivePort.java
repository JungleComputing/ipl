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

final class TcpReceivePort extends ReceivePort implements TcpProtocol {

    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.tcp.TcpReceivePort");

    private class ConnectionHandler extends ReceivePortConnectionInfo 
        implements Runnable, TcpProtocol {

        private final Socket s;
        
        volatile boolean iMustDie = false;

        private final boolean noThread;

        private boolean upcallCalledFinish = false;

        ConnectionHandler(SendPortIdentifier origin, Socket s,
                ReceivePort port, boolean noThread) throws IOException {
            super(origin, port,
                    new BufferedArrayInputStream(s.getInputStream()));
            this.noThread = noThread;
            this.s = s;
            if (! noThread) {
                ThreadPool.createNew(this, "ConnectionHandler");
            }
        }

        ConnectionHandler(ConnectionHandler orig) {
            super(orig);
            s = orig.s;
            noThread = false;
            orig.upcallCalledFinish = true;
            ThreadPool.createNew(this, "ConnectionHandler");
            logger.debug("Created new connection handler, finish called from upcall");
        }

        void close(Throwable e) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception z) {
                    // Ignore.
                }
            }
            in = null;
            try {
                s.close();
            } catch (Exception x) {
                // ignore
            }
            if (!iMustDie) {
                // if we came in through a forced close, the port already knows
                // that we are gone.
                leave(this, e);
            }
        }

        /* called by forced close */
        void die() {
            iMustDie = true;
        }

        //	static int msgCounter = 0;
        public void run() {
            try {
                reader();
            } catch (Throwable e) {
                logger.debug("ConnectionHandler.run, connected "
                        + "to " + origin + ", caught exception", e);
                close(e);
            }
        }

        void reader() throws IOException {
            byte opcode = -1;

            while (!iMustDie) {
                if (logger.isDebugEnabled()) {
                    logger.debug("handler " + this + " for port: " + name() + " woke up");
                }
                opcode = in.readByte();
                if (iMustDie) {
                    // in this case, a forced close was done, and my port is gone..
                    close(null);
                    return;
                }

                switch (opcode) {
                case NEW_RECEIVER:
                    logger.debug(name() + ": Got a NEW_RECEIVER");
                    newStream();
                    break;
                case NEW_MESSAGE:
                    if (logger.isDebugEnabled()) {
                        logger.debug("handler " + this + " GOT a new MESSAGE "
                                + message + " on port " + name());
                    }
                    setMessage(message, this);
                    synchronized(this) {
                        if (noThread || upcallCalledFinish) {
                            logger.debug("Terminating ConnectionHandler thread");
                            return;
                        }
                    }
                    // If the upcall did not release the message, cool, 
                    // no need to create a new thread, we are the reader.
                    break;
                case CLOSE_ALL_CONNECTIONS:
                    logger.debug(name() + ": Got a FREE from " + origin);
                    close(null);
                    return;
                case CLOSE_ONE_CONNECTION:
                    ReceivePortIdentifier identifier;
                    byte[] receiverLength = new byte[Conversion.INT_SIZE];
                    byte[] receiverBytes;
                    // identifier of the receiveport from which a sendport is
                    // disconnecting comes next.
                    in.readArray(receiverLength);
                    receiverBytes = new byte[Conversion.defaultConversion.byte2int(
                            receiverLength, 0)];
                    in.readArray(receiverBytes);
                    identifier = new ReceivePortIdentifier(receiverBytes);
                    if (identifier.equals(identifier())) {
                        // Sendport is disconnecting from me.
                        logger.debug(name() + ": disconnect from: " + origin);
                        close(null);
                        return;
                    }
                    break;
                default:
                    throw new IOException(name() + " EEK TcpReceivePort: "
                            + "run: got illegal opcode: " + opcode + " from: " + origin);
                }
            }
        }
    }

    private ReadMessage m = null;

    private boolean shouldLeave = false;

    private boolean delivered = false;

    private final boolean no_connectionhandler_thread;

    private boolean reader_busy = false;

    private boolean fromDoUpcall = false;

    TcpReceivePort(Ibis ibis, TcpPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) {
        super(ibis, type, name, upcall, connUpcall, connectionAdministration);
        StaticProperties props = type.properties();

        if (upcall == null && connUpcall == null
                && !props.isProp("communication", "ManyToOne")
                && !props.isProp("communication", "Poll")
                && !props.isProp("communication", "ReceiveTimeout")) {
            no_connectionhandler_thread = true;
        } else {
            no_connectionhandler_thread = false;
        }
    }

    private void doUpcall(ReadMessage msg, ConnectionHandler con) {
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
            System.err.println("Got Exception in upcall(): " + e);
            e.printStackTrace();
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

    protected synchronized void finishMessage(ReadMessage r) throws IOException {
        m = null;
        notifyAll();

        if (! fromDoUpcall && upcall != null) {
            // Create a new connection handler. The old one will terminate itself.
            new ConnectionHandler((ConnectionHandler) r.getInfo());
        }
    }

    protected synchronized void finishMessage(ReadMessage r, IOException e) {
        ConnectionHandler c = (ConnectionHandler) r.getInfo();

        c.die(); // tell the handler to stop handling new messages
        c.close(e); // tell the handler to clean up
        leave(c, e); // tell the user the connection failed

        m = null;
        notifyAll();
    }

    public void setMessage(ReadMessage m, ConnectionHandler con) throws IOException {
        setFinished(m, false);
        if (numbered) {
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
                while (reader_busy && ! shouldLeave) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // ignored
                    }
                }
                if (shouldLeave) {
                    throw new IOException("receive() on closed receive port");
                }
                reader_busy = true;
            }
            // Since we don't have any threads or timeout here, this 'reader' 
            // call directly handles the receive.              
            for (;;) {
                // Wait until there is a connection            
                synchronized(this) {
                    while (connections.size() == 0 && ! shouldLeave) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                    
                    // Wait until the current message is done
                    while (m != null && !m.isFinished() && ! shouldLeave) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                    if (shouldLeave) {
                        reader_busy = false;
                        notifyAll();
                        throw new IOException("receive() on closed receive port");
                    }
                }
                
                // Note: This call does NOT always result in a message!
                // Since there is no backgroud thread, this call may handle some
                // 'administration traffic' (like a disconnect), instead of  
                // receiving message. We must therefore keep trying until we see
                // that there really is a message waiting in m!
                ReceivePortConnectionInfo conns[] = connections();
                ((ConnectionHandler)conns[0]).reader();
                synchronized(this) {
                    if (m != null) {
                        reader_busy = false;
                        delivered = true;
                        notifyAll();
                        return m;
                    }
                }
            }
        } else {
            synchronized(this) {
                while ((m == null || delivered) && !shouldLeave) {
                    try {
                        if (timeout > 0) {
                            wait(timeout);
                        } else {
                            wait();
                        }
                    } catch (Exception e) {
                        throw new ReceiveTimedOutException(
                                "timeout expired in receive()");
                    }
                }
                delivered = true;
                return m;
            }
        }
    }

    public ReadMessage poll() throws IOException {
        if (! type.properties().isProp("communication", "Poll")) {
            throw new IOException("Receiveport not configured for polls");
        }

        Thread.yield(); // Give connection handler thread a chance to deliver

        if (upcall != null) {
            return null;
        }

        synchronized (this) { // must this be synchronized? --Rob
                              // Yes, connection handler thread. (Ceriel)
            if (m == null || delivered) {
                return null;
            }
            if (m != null) {
                delivered = true;
            }
            return m;
        }
    }

    // called from the connectionHander.
    public void leave(ConnectionHandler leaving, Throwable e) {

        // First update connection administration.
        logger.debug(name() + ": connection with " + leaving.origin() +
                    " terminating");
        lostConnection(leaving.origin(), e);
    }

    protected synchronized void doClose(long timeout) {
        logger.debug("TcpReceivePort.close: " + name + ": Starting");

        if (timeout > 0) {
            // @@@ this is of course "sub optimal" --Rob
            try {
                wait(timeout);
            } catch (Exception e) {
                // Ignore.
            }
        }

        if (m != null) {
            // throw new Error("Doing close while a msg is alive, port = "
            //         + name + " fin = " + m.isFinished());
            // No, this can happen when an application closes after
            // processing an upcall. Just let it go.
        }

        disableConnections();

        shouldLeave = true;
        notifyAll();

        if (timeout != 0L) {

            SendPortIdentifier[] ids = 
                    connections.keySet().toArray(new SendPortIdentifier[0]);
            for (int i = 0; i < ids.length; i++) {
                ConnectionHandler conn = (ConnectionHandler) getInfo(ids[i]);
                conn.die();

                lostConnection(ids[i], new Exception(
                        "receiver forcibly closed connection"));
            }
        } else {
            while (connections.size() > 0) {
                logger.debug(name + " waiting for all connections to close ("
                            + connections.size() + ")");
                if (no_connectionhandler_thread) {
                    ReceivePortConnectionInfo conns[] = connections();
                    try {
                        ((ConnectionHandler)conns[0]).reader();
                    } catch (IOException e) {
                        removeInfo(((ConnectionHandler)conns[0]).origin());
                    }
                } else {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }
        }

        logger.debug(name + ":done receiveport.close");
    }

    synchronized void connect(SendPortIdentifier origin, Socket s) throws IOException {
        new ConnectionHandler(origin, s, this, no_connectionhandler_thread);
    }
}
