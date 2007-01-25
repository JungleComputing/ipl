/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.ReadMessage;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.ReceivePortConnectionInfo;
import ibis.io.BufferedArrayInputStream;
import ibis.io.Conversion;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

final class TcpReceivePort extends ibis.impl.ReceivePort
        implements TcpProtocol {

    private class ConnectionHandler extends ReceivePortConnectionInfo 
        implements Runnable, TcpProtocol {

        final BufferedArrayInputStream bufferedInput;

        private InputStream input;

        private Socket s;
        
        ReadMessage m;

        volatile boolean iMustDie = false;

        ConnectionHandler(SendPortIdentifier origin, Socket s)
                throws IOException {
            super(origin);
            this.s = s;
            this.input = s.getInputStream();

            bufferedInput = new BufferedArrayInputStream(input);
            initStream(serialization, bufferedInput);
            m = createMessage(in, this);
        }

        protected long bytesRead() {
            return bufferedInput.bytesRead();
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
                if (DEBUG) {
                    System.err.println("ConnectionHandler.run : " + name()
                            + " Caught exception " + e);
                    System.err.println("I am connected to " + origin);
                    e.printStackTrace();
                }

                close(e);
            }
        }

        void reader() throws IOException {
            byte opcode = -1;

            while (!iMustDie) {
                if (DEBUG) {
                    System.err.println("handler " + this + " for port: "
                            + name() + " woke up");
                }
                opcode = in.readByte();
                if (iMustDie) {
                    // in this case, a forced close was done, and my port is gone..
                    close(null);
                    return;
                }

                if (DEBUG) {
                    System.err.println("handler " + this + " for port: "
                            + name() + ", READ BYTE " + opcode);
                }

                switch (opcode) {
                case NEW_RECEIVER:
                    if (DEBUG) {
                        System.err.println(name() + ": Got a NEW_RECEIVER");
                    }
                    initStream(serialization, bufferedInput);
                    m = createMessage(in, this);
                    break;
                case NEW_MESSAGE:
                    if (DEBUG) {
                        System.err.println("handler " + this
                                + " GOT a new MESSAGE " + m + " on port "
                                + name());
                    }

                    if (setMessage(m)) {
                        // The port created a new reader thread, I must exit.
                        // Also when there is no separate connectionhandler thread.
                        return;
                    }

                    // If the upcall did not release the message, cool, 
                    // no need to create a new thread, we are the reader.
                    break;
                case CLOSE_ALL_CONNECTIONS:
                    if (DEBUG) {
                        System.err.println(name() + ": Got a FREE from "
                                + origin);
                    }
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
                    try {
                        identifier = (ReceivePortIdentifier)
                                Conversion.byte2object(receiverBytes);
                        if (identifier.equals(identifier())) {
                            // Sendport is disconnecting from me.
                            if (DEBUG) {
                                System.err.println(name()
                                        + ": got a disconnect from: " + origin);
                            }
                            close(null);
                            return;
                        }
                    } catch(ClassNotFoundException e) {
                        throw new IOException("TcpIbis: internal error, "
                                + name() + ": disconnect from: " + origin
                                + " failed: " + e);
                    }
                    break;
                default:
                    throw new IOException(name() + " EEK TcpReceivePort: "
                            + "run: got illegal opcode: " + opcode + " from: "
                            + origin);
                }
            }
        }
    }

    private ReadMessage m = null;

    private boolean shouldLeave = false;

    private boolean delivered = false;

    private boolean no_connectionhandler_thread = false;

    private boolean reader_busy = false;

    private boolean fromDoUpcall = false;

    TcpReceivePort(TcpIbis ibis, TcpPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) {
        super(ibis, type, name, (ibis.impl.IbisIdentifier) ibis.identifier(), upcall,
                connUpcall, connectionAdministration);
        StaticProperties props = type.properties();

        if (upcall == null && connUpcall == null
                && !props.isProp("communication", "ManyToOne")
                && !props.isProp("communication", "Poll")
                && !props.isProp("communication", "ReceiveTimeout")) {
            no_connectionhandler_thread = true;
        }
    }

    // returns:  was the message already finised?
    private boolean doUpcall(ReadMessage msg) {
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
        } catch (IOException e) {
            // An error occured on receiving (or finishing!) the message during
            // the upcall.
            System.err.println("Got IO Exception in upcall(): " + e);
            e.printStackTrace();
            if (! msg.isFinished()) {
                finishMessage(msg, e);
                return false;
            }
            return true;
        } catch(Throwable e2) {
            System.err.println("Got exception in upcall(): " + e2);
            e2.printStackTrace();
            if (msg.isFinished()) {
                return true;
            }
        }

        synchronized (this) {
            if (!msg.isFinished()) {
                // It wasn't finished. Cool, this means that we don't have to
                // start a new thread!
                // We can touch msg here, because if it was finished, a new
                // message was created.
                fromDoUpcall = true;
                try {
                    msg.finish();
                } catch(Throwable e) {
                    // Should not happen.
                } finally {
                    fromDoUpcall = false;
                }

                return false;
            }
        }
        return true;
    }

    protected synchronized void finishMessage(ReadMessage r, long cnt)
            throws IOException {
        addCount(cnt);
        m = null;
        notifyAll();

        if (! fromDoUpcall && upcall != null) {
            /* We need to create a new ReadMessage here.
             * Otherwise, there is no way to find out later if a message
             * was finished or not.
             */
            ConnectionHandler h = (ConnectionHandler) r.getInfo();
            h.m = new ReadMessage(r);
            ThreadPool.createNew(h, "ConnectionHandler");
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

    boolean setMessage(ReadMessage m) throws IOException {
        setFinished(m, false);
        if (numbered) {
            m.setSequenceNumber(m.readLong());
        }
        if (upcall != null) {
            return doUpcall(m);
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
        return no_connectionhandler_thread;
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
    void leave(ConnectionHandler leaving, Throwable e) {

        // First update connection administration.
        synchronized (this) {
            if (DEBUG) {
                System.err.println("TcpReceivePort.leave: " + name);
                (new Throwable()).printStackTrace();
            }
            removeInfo(leaving.origin);
            notifyAll();
        }

        lostConnection(leaving.origin, e);
    }

    protected synchronized void doClose(long timeout) {
        if (DEBUG) {
            System.err.println("TcpReceivePort.close: " + name + ": Starting");
        }

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
            SendPortIdentifier[] ids = connectedTo();
            for (int i = 0; i < ids.length; i++) {
                ConnectionHandler conn = (ConnectionHandler) removeInfo(ids[i]);
                conn.die();

                lostConnection(ids[i], new Exception(
                        "receiver forcibly closed connection"));
            }
        } else {
            while (connections.size() > 0) {
                if (DEBUG) {
                    System.err.println(name
                            + " waiting for all connections to close ("
                            + connections.size() + ")");
                }
                if (no_connectionhandler_thread) {
                    ReceivePortConnectionInfo conns[] = connections();
                    try {
                        ((ConnectionHandler)conns[0]).reader();
                    } catch (IOException e) {
                        removeInfo(((ConnectionHandler)conns[0]).origin);
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

        if (DEBUG) {
            System.err.println(name + ":done receiveport.close");
        }
    }

    synchronized void connect(SendPortIdentifier origin, Socket s) {
        try {
            ConnectionHandler con = new ConnectionHandler(origin, s);
            addInfo(origin, con);

            if (!no_connectionhandler_thread) {
                ThreadPool.createNew(con, "ConnectionHandler");
            }

            notifyAll();
        } catch (Exception e) {
            System.err.println("Got exception " + e);
            e.printStackTrace();
        }
    }
}
