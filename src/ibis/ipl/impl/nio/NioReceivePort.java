/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class NioReceivePort extends ibis.ipl.impl.ReceivePort implements
        Runnable, Protocol {

    private static Logger logger = LoggerFactory.getLogger(NioReceivePort.class);

    private boolean reader_busy = false;

    static class ConnectionInfo extends ReceivePortConnectionInfo {
        ConnectionInfo(SendPortIdentifier origin, NioReceivePort port,
                NioDissipator dissipator) throws IOException {
            super(origin, port, dissipator);
            dissipator.info = this;
        }

        protected void upcallCalledFinish() {
            super.upcallCalledFinish();
            ThreadPool.createNew((NioReceivePort) port,
                    "NioReceivePort with upcall");
        }
    }

    NioReceivePort(Ibis ibis, PortType type, String name, MessageUpcall upcall,
            ReceivePortConnectUpcall connUpcall, Properties properties) throws IOException {
        super(ibis, type, name, upcall, connUpcall, properties);

        if (upcall != null) {
            ThreadPool.createNew(this, "NioReceivePort with upcall");
        }
    }

    void addConnection(SendPortIdentifier id, NioDissipator dissipator)
            throws IOException {
        ConnectionInfo info = new ConnectionInfo(id, this, dissipator);
        addInfo(id, info);
    }

    /**
     * Sees if the user is ok with a new connection from "spi" Called by the
     * connection factory.
     * 
     * @return the reply for the send port
     */
    byte connectionRequested(SendPortIdentifier spi, PortType capabilities,
            Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("handling connection request");
        }
        byte r = connectionAllowed(spi, capabilities);
        if (r != ACCEPTED) {
            return r;
        }

        try {
            newConnection(spi, channel);
        } catch (IOException e) {
            lostConnection(spi, e);
            logger.error("newConnection() failed");
            return DENIED;
        }

        if (logger.isInfoEnabled()) {
            logger.info("new incoming connection from " + spi + " to " + ident);
        }

        return r;
    }

    /**
     * Waits for someone to wake us up. Waits: - not at all if deadline == -1 -
     * until System.getTimeMillis >= deadline if deadline > 0 - for(ever) if
     * deadline == 0
     * 
     * This method assumes that the caller holds the monitor on this instance.
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
     * gets a new message from the network. Will block until the deadline has
     * passed, or not at all if deadline = -1, or indefinitely if deadline = 0.
     * Only used when upcalls are disabled. Uses global message "m" to ensure
     * only one message is alive at any time
     * 
     */
    public ReadMessage getMessage(long timeout) throws IOException {
        NioDissipator dissipator;
        long deadline = timeout;

        if (deadline > 0) {
            deadline += System.currentTimeMillis();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("trying to fetch message");
        }

        synchronized(this) {
            while (reader_busy && ! closed) {
                if (!waitForNotify(deadline)) {
                    logger.error("timeout while waiting on previous message");
                    throw new ReceiveTimedOutException("previous message"
                            + " not finished yet");
                }
            }

            // Wait until there is a connection            
            while (connections.size() == 0 && ! closed) {
                if (!waitForNotify(deadline)) {
                    logger.error("timeout while waiting for connection");
                    throw new ReceiveTimedOutException("no connection yet");
                }
            }

            // Wait until the current message is done
            while (message != null && ! closed) {
                if (!waitForNotify(deadline)) {
                    logger.error(
                            "timeout while waiting on previous message");
                    throw new ReceiveTimedOutException("previous message"
                            + " not finished yet");
                }
            }
            if (closed) {
                throw new IOException("receive() on closed port");
            }

            reader_busy = true;
        }

        try {
            dissipator = getReadyDissipator(deadline);
            ReceivePortConnectionInfo info = dissipator.info;

            info.message.setFinished(false);

            if (numbered) {
                try {
                     info.message.setSequenceNumber(info.message.readLong());
                } catch (IOException e) {
                    errorOnRead(dissipator, e);
                    // do recursive call
                    reader_busy = false;
                    return getMessage(deadline);
                }
            }

            messageArrived(info.message);

            if (logger.isDebugEnabled()) {
                logger.debug("new message received");
            }

            return message;

        } catch (ReceiveTimedOutException e) {
            logger.debug("timeout while waiting on dissipator with message");
            throw e;
        } catch (ConnectionClosedException e) {
            logger.debug("receiveport closed while waiting on message");
            throw e;
        } finally {
            synchronized (this) {
                reader_busy = false;
                notifyAll();
            }
        }
    }

    protected ReadMessage doPoll() throws IOException {
        try {
            return getMessage(-1);
        } catch (ReceiveTimedOutException e) {
            // IGNORE
        }
        return null;
    }

    public void closePort(long timeout) {
        closing(); // signal the subclass we are closing down
        if (upcall != null) {
            super.closePort(timeout);
        } else {
            try {
                getMessage(timeout);
            } catch(ConnectionClosedException e) {
                // OK
            } catch(IOException e2) {
                super.closePort(1);
            }
        }
    }

    public void run() {
        NioDissipator dissipator;

        Thread.currentThread().setName(this + " upcall thread");

        while (true) {
            try {
                dissipator = getReadyDissipator(0);
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

            ReceivePortConnectionInfo info = dissipator.info;

            info.message.setFinished(false);

            if (numbered) {
                try {
                     info.message.setSequenceNumber(info.message.readLong());
                } catch (IOException e) {
                    errorOnRead(dissipator, e);
                    continue;
                }
            }

            ReadMessage m = info.message;

            messageArrived(info.message);

            if (m.finishCalledInUpcall()) {
                // a new thread was started to handle the next message,
                // exit
                return;
            }
        }
    }

    /**
     * A new connection has been established.
     */
    abstract void newConnection(SendPortIdentifier spi, Channel channel)
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
     * this receiveport is closing down.
     */
    abstract void closing();
}
