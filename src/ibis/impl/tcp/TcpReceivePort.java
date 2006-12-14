/* $Id$ */

package ibis.impl.tcp;

import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// why was shouldLeave here?
// If I create a receiveport, do a receive, and someone leaves, 
// the user gets an upcall, or can find out with a downcall.
// why should the receivePort die? --Rob
// That is not what it does: it makes sure readers go away when the
// receive port is closed. --Ceriel

final class TcpReceivePort implements ReceivePort, TcpProtocol, Config {
    TcpPortType type;

    String name; // needed to unbind

    private TcpIbis ibis;

    private TcpReceivePortIdentifier ident;

    private ConnectionHandler[] connections;

    private int connectionsIndex;

    private boolean allowUpcalls = false;

    private Upcall upcall;

    private ReceivePortConnectUpcall connUpcall;

    private boolean started = false;

    private boolean connection_setup_present = false;

    private TcpReadMessage m = null;

    private boolean shouldLeave = false;

    private boolean delivered = false;

    private ArrayList lostConnections = new ArrayList();

    private ArrayList newConnections = new ArrayList();

    private boolean connectionAdministration = false;

    private boolean no_connectionhandler_thread = false;

    private Map props = new HashMap();

    long count = 0;

    private boolean reader_busy = false;

    TcpReceivePort(TcpIbis ibis, TcpPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) {

        this.type = type;
        this.upcall = upcall;
        this.connUpcall = connUpcall;
        this.ibis = ibis;
        this.connectionAdministration = connectionAdministration;
        if (connUpcall != null) {
            this.connectionAdministration = true;
        }

        this.name = name;

        connections = new ConnectionHandler[2];
        connectionsIndex = 0;

        int port = ibis.tcpPortHandler.register(this);
        ident = new TcpReceivePortIdentifier(name, type.props, ibis.ident,
                port);
        if (upcall == null && connUpcall == null
                && !type.props.isProp("communication", "ManyToOne")
                && !type.props.isProp("communication", "Poll")
                && !type.props.isProp("communication", "ReceiveTimeout")) {
            no_connectionhandler_thread = true;
        }
    }

    /** returns the type that was used to create this port */
    public PortType getType() {
        return type;
    }

    // returns:  was the message already finised?
    private boolean doUpcall(TcpReadMessage msg) {
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
            if (! msg.isFinished) {
                finishMessage(e);
                return false;
            }
            return true;
        } catch(Throwable e2) {
            System.err.println("Got exception in upcall(): " + e2);
            e2.printStackTrace();
            if (msg.isFinished) {
                return true;
            }
        }

        /* The code below was so terribly wrong.
         * You cannot touch m here anymore if it indeed
         * was finished, because it might represent another message now!
         * And, if that is not yet finished, things go terribly wrong ....
         * On the other hand, if m is not finished yet, it is valid here.
         * Problem here is, we don't know what is the case.
         *
         * The problem is fixed now, by allocating a new TcpReadMessage
         * in the finish() call.
         */
        synchronized (this) {
            if (!msg.isFinished) {
                // It wasn't finished. Cool, this means that we don't have to
                // start a new thread!
                this.m = null;
                if (STATS) {
                    long after = msg.getHandler().bufferedInput.bytesRead();
                    count += after - msg.before;
                    msg.before = after;
                }
                notifyAll();

                return false;
            }
        }
        return true;
    }

    synchronized void finishMessage() throws IOException {
        TcpReadMessage old = m;

        if (m == null || m.isFinished) {
            throw new IOException(
                    "Finish is called twice on this message, port = " + name);
        }

        m.isFinished = true;
        m = null;
        notifyAll();

        if (upcall != null) {
            /* We need to create a new TcpReadMessage here.
             * Otherwise, there is no way to find out later if a message
             * was finished or not. The code at the end of doUpcall() (after
             * the upcall itself) would be very wrong indeed (it was!)
             * if we would not allocate a new message. The point is, after
             * a finish(), the TcpReadMessage is used for new
             * messages!
             */
            ConnectionHandler h = old.getHandler();
            h.m = new TcpReadMessage(old);
            ThreadPool.createNew(h, "ConnectionHandler");
        }
    }

    synchronized void finishMessage(IOException e) {

        m.getHandler().die(); // tell the handler to stop handling new messages
        m.getHandler().close(e); // tell the handler to clean up
        leave(m.getHandler(), e); // tell the user the connection failed

        m.isFinished = true;
        m = null;
        notifyAll();
    }

    boolean setMessage(TcpReadMessage m) throws IOException {

        // We're not allowed to read from the message until 
        // the isFinished flag is set to true, so start by 
        // resetting it here.
        m.isFinished = false;

        if (type.numbered) {
            m.setSequenceNumber(m.readLong());
        }
        if (upcall != null) {
            return doUpcall(m);
        }
        setBlockingReceiveMessage(m);
        return no_connectionhandler_thread;
    }

    private synchronized void setBlockingReceiveMessage(TcpReadMessage m) {
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

    public long getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }

    private TcpReadMessage getMessage(long timeout)
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
                    return null;
                }
                reader_busy = true;
            }
            // Since we don't have any threads or timeout here, this 'reader' 
            // call directly handles the receive.              
            for (;;) {
                // Wait until there is a connection            
                synchronized(this) {
                    while (connectionsIndex == 0 && ! shouldLeave) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                    
                    // Wait until the current message is done
                    while (m != null && !m.isFinished && ! shouldLeave) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                    if (shouldLeave) {
                        reader_busy = false;
                        notifyAll();
                        return null;
                    }
                }
                
                // Note: This call does NOT always result in a message!
                // Since there is no backgroud thread, this call may handle some
                // 'administration traffic' (like a disconnect), instead of  
                // receiving message. We must therefore keep trying until we see
                // that there really is a message waiting in m!
                connections[0].reader();
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

    public synchronized void enableConnections() {
        // Set 'starting' to true. This is always OK.
        started = true;
    }

    public synchronized void disableConnections() {
        // We may only set 'starting' to false if there is no 
        // connection being set up at the moment.

        while (connection_setup_present) {
            try {
                wait();
            } catch (Exception e) {
                // Ignore
            }
        }
        started = false;
    }

    synchronized int connectionAllowed(TcpSendPortIdentifier id) {
        if (started) {
            if (connectionAdministration) {
                if (connUpcall != null) {
                    if (!connUpcall.gotConnection(this, id)) {
                        return RECEIVER_DENIED;
                    }
                } else {
                    newConnections.add(id);
                }
            }
            connection_setup_present = true;
            notifyAll();
            return RECEIVER_ACCEPTED;
        }
        return RECEIVER_DISABLED;
    }

    public synchronized void enableUpcalls() {
        allowUpcalls = true;
        notifyAll();
    }

    public synchronized void disableUpcalls() {
        allowUpcalls = false;
    }

    public ReadMessage poll() throws IOException {
        if (! type.props.isProp("communication", "Poll")) {
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

    public ReadMessage receive() throws IOException {
        if (upcall != null) {
            throw new IOException(
                    "Configured Receiveport for upcalls, downcall not allowed");
        }

        ReadMessage msg = getMessage(-1);

        if (msg == null) {
            throw new IOException("receive port closed");
        }
        return msg;
    }

    public ReadMessage receive(long timeoutMillis) throws IOException {
        if (upcall != null) {
            throw new IOException(
                    "Configured Receiveport for upcalls, downcall not allowed");
        }

        return getMessage(timeoutMillis);
    }

    public Map properties() {
        return props;
    }

    public Object getProperty(String key) {
        return props.get(key);
    }
    
    public void setProperties(Map properties) {
        props = properties;
    }
    
    public void setProperty(String key, Object val) {
        props.put(key, val);
    }
    
    public String name() {
        return name;
    }

    public ReceivePortIdentifier identifier() {
        return ident;
    }

    // called from the connectionHander.
    void leave(ConnectionHandler leaving, Throwable e) {

        // First update connection administration.
        synchronized (this) {
            boolean found = false;
            if (DEBUG) {
                System.err.println("TcpReceivePort.leave: " + name);
                (new Throwable()).printStackTrace();
            }
            for (int i = 0; i < connectionsIndex; i++) {
                if (connections[i] == leaving) {
                    connections[i] = connections[connectionsIndex - 1];
                    connections[connectionsIndex - 1] = null;
                    connectionsIndex--;
                    found = true;
                    break;
                }
            }

            if (!found) {
                // throw new Error("TcpReceivePort: Connection handler "
                //         + "not found in leave");
                // Ignored
            }
            // Notify threads that might be blocked in a close
            notifyAll();
        }

        // Don't hold the lock when calling user upcall functions. --Rob
        if (connectionAdministration) {
            if (connUpcall != null) {
                if (e == null) {
                    e = new Exception("sender closed connection");
                }
                connUpcall.lostConnection(this, leaving.origin, e);
            } else {
                lostConnections.add(leaving.origin);
            }
        }
    }

    private synchronized ConnectionHandler removeConnection(int index) {
        ConnectionHandler res = connections[index];
        connections[index] = connections[connectionsIndex - 1];
        connections[connectionsIndex - 1] = null;
        connectionsIndex--;

        return res;
    }

    public void close(long timeout) {
        if (timeout == 0L) {
            close();
        } else if (timeout > 0L) {
            forcedClose(timeout);
        } else {
            forcedClose();
        }

    }

    public synchronized void close() {
        if (DEBUG) {
            System.err.println("TcpReceivePort.close: " + name + ": Starting");
        }

        if (m != null) {
            // throw new Error("Doing close while a msg is alive, port = "
            //         + name + " fin = " + m.isFinished);
            // No, this can happen when an application closes after
            // processing an upcall. Just let it go.
        }

        disableConnections();

        shouldLeave = true;
        notifyAll();

        while (connectionsIndex > 0) {
            if (DEBUG) {
                System.err.println(name
                        + " waiting for all connections to close ("
                        + connectionsIndex + ")");
            }
            if (no_connectionhandler_thread) {
                try {
                    connections[0].reader();
                } catch (IOException e) {
                    connectionsIndex = 0;
                }
            } else {
                try {
                    wait();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }

        if (DEBUG) {
            System.err.println(name + " all connections closed");
        }

        /* unregister with porthandler */
        ibis.tcpPortHandler.deRegister(this);

        if (DEBUG) {
            System.err.println(name + ":done receiveport.close");
        }
    }

    synchronized void connect(TcpSendPortIdentifier origin, Socket s) {
        try {
            ConnectionHandler con = new ConnectionHandler(ibis, origin, this, s);

            if (connections.length == connectionsIndex) {
                ConnectionHandler[] temp
                        = new ConnectionHandler[2 * connections.length];
                for (int i = 0; i < connectionsIndex; i++) {
                    temp[i] = connections[i];
                }

                connections = temp;
            }

            connections[connectionsIndex++] = con;
            if (!no_connectionhandler_thread) {
                ThreadPool.createNew(con, "ConnectionHandler");
            }

            connection_setup_present = false;
            notifyAll();
        } catch (Exception e) {
            System.err.println("Got exception " + e);
            e.printStackTrace();
        }
    }

    private synchronized void forcedClose() {
        // this may be ok with a forced close.
        if (m != null) {
            // throw new Error(
            //         "Doing forced close while a msg is alive, port = " + name
            //                 + " fin = " + m.isFinished);
            // No, this can happen when an application closes after
            // processing an upcall. Just let it go.
        }

        disableConnections();

        /* unregister with porthandler */
        ibis.tcpPortHandler.deRegister(this);

        while (connectionsIndex > 0) {
            ConnectionHandler conn = removeConnection(0);
            conn.die();

            if (connectionAdministration) {
                if (connUpcall != null) {
                    connUpcall.lostConnection(this, conn.origin, new Exception(
                            "receiver forcibly closed connection"));
                } else {
                    lostConnections.add(conn.origin);
                }
            }
        }
        shouldLeave = true;
        notifyAll();
    }

    private synchronized void forcedClose(long timeoutMillis) {
        // @@@ this is of course "sub optimal" --Rob
        try {
            wait(timeoutMillis);
        } catch (Exception e) {
            // Ignore.
        }

        forcedClose();
    }

    public synchronized SendPortIdentifier[] connectedTo() {
        SendPortIdentifier[] res = new SendPortIdentifier[connectionsIndex];
        for (int i = 0; i < connectionsIndex; i++) {
            res[i] = connections[i].origin;
        }

        return res;
    }

    synchronized boolean isConnectedTo(SendPortIdentifier id) {
        for (int i = 0; i < connectionsIndex; i++) {
            if (connections[i].origin.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public synchronized SendPortIdentifier[] lostConnections() {
        SendPortIdentifier[] res
                = (SendPortIdentifier[]) lostConnections.toArray(new SendPortIdentifier[0]);
        lostConnections.clear();
        return res;
    }

    public synchronized SendPortIdentifier[] newConnections() {
        SendPortIdentifier[] res
                = (SendPortIdentifier[]) newConnections.toArray();
        newConnections.clear();
        return res;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof TcpReceivePort) {
            TcpReceivePort other = (TcpReceivePort) obj;
            return name.equals(other.name);
        } else if (obj instanceof String) {
            String s = (String) obj;
            return s.equals(name);
        } else {
            return false;
        }
    }
}
