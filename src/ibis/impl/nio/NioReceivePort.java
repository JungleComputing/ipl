/* $Id$ */

package ibis.impl.nio;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

abstract class NioReceivePort implements ReceivePort, Runnable, Config,
        Protocol {

    private static Logger logger = ibis.util.GetLogger.getLogger(NioReceivePort.class);

    protected NioPortType type;

    protected NioIbis ibis;

    private String name;

    NioReceivePortIdentifier ident;

    private Upcall upcall;

    private ReceivePortConnectUpcall connUpcall;

    private boolean connectionAdministration = false;

    private long count = 0;

    private boolean upcallsEnabled = false;

    private boolean connectionsEnabled = false;

    private ArrayList lostConnections = new ArrayList();

    private ArrayList newConnections = new ArrayList();

    private boolean upcallThreadRunning = false;

    private NioReadMessage m = null; // only used when upcalls are off

    /**
     * Fake readmessage used to indicate someone is trying to get a real
     * message. This makes sure only one thread is trying to receive a new
     * message.
     */
    private final NioReadMessage dummy;

    NioReceivePort(NioIbis ibis, NioPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) throws IOException {

        this.type = type;
        this.upcall = upcall;
        this.connUpcall = connUpcall;
        this.ibis = ibis;
        this.connectionAdministration = connectionAdministration;

        this.name = name;

        InetSocketAddress address = ibis.factory.register(this);

        ident = new NioReceivePortIdentifier(name, type.name(),
                (NioIbisIdentifier) type.ibis.identifier(), address);

        ibis.nameServer.bind(name, ident);

        dummy = new NioReadMessage(null, null, -1);

        if (upcall != null) {
            ThreadPool.createNew(this, "NioReceivePort with upcall");
        }

    }

    /**
     * Sees if the user is ok with a new connection from "spi" Called by the
     * connection factory.
     * 
     * @return the reply for the send port
     */
    byte connectionRequested(NioSendPortIdentifier spi, Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("handling connection request");
        }

        synchronized (this) {
            if (!connectionsEnabled) {
                logger.error("connections disabled");
                return CONNECTIONS_DISABLED;
            }
        }

        if (!type.manyToOne && (connectedTo().length > 0)) {
            // many2one not supported...
            logger.error("many2one not supported");
            return CONNECTION_DENIED;
        }

        if (connUpcall != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("passing connection request to user");
            }
            if (!connUpcall.gotConnection(this, spi)) {
                logger.error("user denied connection");
                return CONNECTION_DENIED;
            }
        }

        try {
            newConnection(spi, channel);
        } catch (IOException e) {
            if (connUpcall != null) {
                connUpcall.lostConnection(this, spi, e);
            }
            logger.error("newConnection() failed");
            return CONNECTION_DENIED;
        }

        synchronized (this) {
            if (connectionAdministration) {
                newConnections.add(spi);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("new incoming connection from " + spi + " to " + ident);
        }

        return CONNECTION_ACCEPTED;
    }

    /**
     * Waits for someone to wake us up. Waits: - not at all if deadline == -1 -
     * until System.getTimeMillis >= deadline if deadline > 0 - for(ever) if
     * deadline == 0
     * 
     * @return true we (might have been) notified, or false if the deadline
     *         passed
     */
    private boolean waitForNotify(long deadline) {
        if (deadline == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
            return true;
        } else if (deadline == -1) {
            return false; // deadline always passed
        }

        long time = System.currentTimeMillis();

        if (time >= deadline) {
            return false;
        }

        try {
            wait(deadline - time);
        } catch (InterruptedException e) {
            // IGNORE
        }
        return true; // don't know if we have been notified, but could be...
    }

    /**
     * Called by the subclass to let us know a connection failed, and we should
     * report this to the user somehow
     */
    void connectionLost(NioDissipator dissipator, Exception cause) {
        synchronized (this) {
            if (connectionAdministration) {
                lostConnections.add(dissipator.peer);
            }
        }

        if (connUpcall != null) {
            connUpcall.lostConnection(this, dissipator.peer, cause);
        }
    }

    /**
     * gets a new message from the network. Will block until the deadline has
     * passed, or not at all if deadline = -1, or indefinitely if deadline = 0.
     * Only used when upcalls are disabled. Uses global message "m" to ensure
     * only one message is alive at any time
     * 
     */
    private NioReadMessage getMessage(long deadline) throws IOException {
        NioDissipator dissipator;
        long time;
        NioReadMessage message;
        long sequencenr = -1;

        if (logger.isDebugEnabled()) {
            logger.debug("trying to fetch message");
        }

        synchronized (this) {
            while (m != null) {
                if (!waitForNotify(deadline)) {
                    logger.error("timeout while waiting on previous message");
                    throw new ReceiveTimedOutException("previous message"
                            + " not finished yet");
                }
            }
            m = dummy; // reserve the global message so no-one will
            // try to receive a message while we are too.
        }

        try {
            dissipator = getReadyDissipator(deadline);
        } catch (ReceiveTimedOutException e) {
            synchronized (this) {
                m = null; // give up the lock
            }
            logger.error("!timeout while waiting on dissipator with message");
            throw e;
        } catch (ConnectionClosedException e) {
            synchronized (this) {
                m = null; // give up the lock
            }
            logger.error("receiveport closed while waiting on message");
            throw e;
        }

        try {
            if (type.numbered) {
                sequencenr = dissipator.sis.readLong();
            }

            message = new NioReadMessage(this, dissipator, sequencenr);
        } catch (IOException e) {
            errorOnRead(dissipator, e);
            // do recursive call
            return getMessage(deadline);
        }

        synchronized (this) {
            m = message;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("new message received (#" + sequencenr + ")");
        }

        return message;
    }

    /**
     * called by the readMessage. Finishes message. Also wakes up everyone who
     * was waiting for it
     */
    void finish(NioReadMessage m, long messageCount) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("finishing read message");
        }

        synchronized (this) {
            if (upcall == null) {
                if (this.m != m) {
                    throw new IOException(
                            "finish called on non-current message");
                }

                // no (global)message alive
                this.m = null;
                // wake up everybody who was waiting for this message to finish
                notifyAll();
            }
            count += messageCount;
        }

        if (upcall != null) {
            // this finish was called from an upcall! Create a new thread to
            // fetch the next message (this upcall might not exit for a while)
            ThreadPool.createNew(this, "NioReceivePort with upcall");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("finished read message, received " + messageCount
                    + " bytes");
        }
    }

    /**
     * the message ended on an error. Consider the connection to the SendPort
     * lost. Close it.
     */
    synchronized void finish(NioReadMessage m, Exception e) {
        logger.error("finishing read message with error");
        m.isFinished = true;

        // inform the subclass an error occured
        errorOnRead(m.dissipator, e);

        if (upcall == null) {
            if (this.m == m) {
                this.m = null;
            }

            // wake up everybody who was waiting for this message to finish
            notifyAll();
        } else {
            // this finish was called from an upcall! Create a new thread to
            // fetch the next message (this upcall might not exit for a while)
            ThreadPool.createNew(this, "NioReceivePort with upcall");
        }
    }

    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    public ReadMessage receive(long timeoutMillis) throws IOException {

        long deadline, time;
        ReadMessage m = null;

        if (upcall != null) {
            throw new IOException("explicit receive not allowed with upcalls");
        }

        if (timeoutMillis < 0) {
            throw new IOException("timeout must be a non-negative number");
        } else if (timeoutMillis > 0) {
            deadline = System.currentTimeMillis() + timeoutMillis;
        } else { // timeoutMillis == 0
            deadline = 0;
        }

        return getMessage(deadline);
    }

    public ReadMessage poll() throws IOException {
        try {
            return getMessage(-1);
        } catch (ReceiveTimedOutException e) {
            // IGNORE
        }
        return null;
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized void resetCount() {
        count = 0;
    }

    public DynamicProperties properties() {
        return null;
    }

    public ReceivePortIdentifier identifier() {
        return ident;
    }

    public String name() {
        return name;
    }

    public synchronized void enableConnections() {
        connectionsEnabled = true;
    }

    public synchronized void disableConnections() {
        connectionsEnabled = false;
    }

    public synchronized void enableUpcalls() {
        upcallsEnabled = true;
        notifyAll();
    }

    public synchronized void disableUpcalls() {
        upcallsEnabled = false;
    }

    public synchronized SendPortIdentifier[] lostConnections() {
        SendPortIdentifier[] result;
        result = (SendPortIdentifier[]) lostConnections.toArray();

        lostConnections.clear();

        return result;
    }

    public synchronized SendPortIdentifier[] newConnections() {
        SendPortIdentifier[] result;
        result = (SendPortIdentifier[]) newConnections.toArray();

        newConnections.clear();

        return result;
    }

    /**
     * Free resourced held by receiport AFTER waiting for all the connections to
     * close down
     */
    public void close() throws IOException {
        doClose(0);
    }

    /**
     * Free resourced held by receiport AFTER waiting for all the connections to
     * close down, or the timeout to pass.
     */
    public void close(long timeout) throws IOException {
        if (timeout > 0L) {
            doClose(System.currentTimeMillis() + timeout);
        } else {
            // -1 or 0
            doClose(timeout);
        }
    }

    /**
     * closes all connections after waiting for the deadline to pass.
     */
    void doClose(long deadline) throws IOException {
        ReadMessage m;
        long time;

        disableConnections();
        ibis.nameServer.unbind(ident.name);
        ibis.factory.deRegister(this);

        closing(); // signal the subclass we are closing down

        if (upcall == null) {
            synchronized (this) {
                if (this.m != null) {
                    throw new IOException("Message alive while doing close");
                }
            }
            try {
                m = getMessage(deadline);
                throw new IOException(
                        "message received while closing receiveport");
            } catch (ConnectionClosedException e) {
                // this is _excactly_ wat we want
            } catch (ReceiveTimedOutException e2) {
                // there are some connections left, kill them
                closeAllConnections();
            }
        } else {
            if (deadline != -1) {
                synchronized (this) {
                    if (deadline == 0L) {
                        while (connectedTo().length > 0) {
                            try {
                                wait();
                            } catch (Exception e) {
                                // IGNORE
                            }
                        }
                    } else if (deadline > 0L) {
                        time = System.currentTimeMillis();
                        while (time < deadline && connectedTo().length > 0) {
                            try {
                                wait(deadline - time);
                            } catch (Exception e) {
                                //
                            }
                            time = System.currentTimeMillis();
                        }
                    }
                }
            }
            closeAllConnections();
        }
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof NioReceivePort) {
            NioReceivePort other = (NioReceivePort) obj;
            return name.equals(other.name);
        } else if (obj instanceof String) {
            String s = (String) obj;
            return s.equals(name);
        } else {
            return false;
        }
    }

    public String toString() {
        return name;
    }

    public void run() {
        NioReadMessage m;
        NioDissipator dissipator;
        long sequencenr;

        Thread.currentThread().setName(this + " upcall thread");

        while (true) {
            try {
                dissipator = getReadyDissipator(0);
            } catch (ReceiveTimedOutException e) {
                throw new IbisError("ReceiveTimedOutException caught while"
                        + " doing a getMessage(0)! : " + e);
            } catch (ConnectionClosedException e2) {
                synchronized (this) {
                    // the receiveport was closed, exit
                    notifyAll();
                    return;
                }
            } catch (IOException e) {
                // FIXME: this is not very nice
                continue;
            }

            try {
                if (type.numbered) {
                    sequencenr = dissipator.sis.readLong();
                } else {
                    sequencenr = 0;
                }

                m = new NioReadMessage(this, dissipator, sequencenr);
            } catch (IOException e) {
                errorOnRead(dissipator, e);
                continue;
            }

            synchronized (this) {
                while (!upcallsEnabled) {
                    try {
                        logger.info("waiting for upcall to be enabled");
                        wait();
                    } catch (InterruptedException e) {
                        // IGNORE
                    }
                }
            }

            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("doing upcall");
                }
                upcall.upcall(m);
            } catch (IOException e) {
                errorOnRead(m.dissipator, e);
            }

            if (m.isFinished) {
                // a new thread was started to handle the next message,
                // exit
                return;
            }

            // implicitly finish message

            long messageCount = m.dissipator.bytesRead();
            m.dissipator.resetBytesRead();
            m.isFinished = true;
        }
    }

    /**
     * A new connection has been established.
     */
    abstract void newConnection(NioSendPortIdentifier spi, Channel channel)
            throws IOException;

    abstract void errorOnRead(NioDissipator dissipator, Exception cause);

    /**
     * Searches for a dissipator with a message waiting
     * 
     * Will block until the deadline has passed, or not at all if deadline = -1,
     * or indefinitely if deadline = 0
     * 
     * @param deadline
     *            the deadline after which searching has failed
     * 
     * @throws ReceiveTimedOutException
     *             If no connections are ready after the deadline has passed
     * 
     * @throws ConnectionClosedException
     *             if there a no more connections left and the receiveport is
     *             closing down.
     */
    abstract NioDissipator getReadyDissipator(long deadline) throws IOException;

    /**
     * Generate an array of all connections to this receiveport.
     */
    public abstract SendPortIdentifier[] connectedTo();

    /**
     * this receiveport is closing down.
     */
    abstract void closing();

    /**
     * Drop all open connections.
     */
    abstract void closeAllConnections();

}
