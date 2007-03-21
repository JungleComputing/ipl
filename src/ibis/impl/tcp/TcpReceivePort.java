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
import ibis.ipl.Upcall;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

class TcpReceivePort extends ReceivePort implements TcpProtocol {

    private static final Logger logger
            = Logger.getLogger("ibis.impl.tcp.TcpReceivePort");

    class ConnectionHandler extends ReceivePortConnectionInfo 
            implements Runnable, TcpProtocol {

        private final IbisSocket s;

        ConnectionHandler(SendPortIdentifier origin, IbisSocket s,
                ReceivePort port, BufferedArrayInputStream in)
                throws IOException {
            super(origin, port, in);
            this.s = s;
        }

        public void close(Throwable e) {
            super.close(e);
            try {
                s.close();
            } catch (Throwable x) {
                // ignore
            }
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

        protected void upcallCalledFinish() {
            super.upcallCalledFinish();
            ThreadPool.createNew(this, "ConnectionHandler");
        }

        void reader(boolean noThread) throws IOException {
            byte opcode = -1;

            while (in != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(name + ": handler for " + origin + " woke up");
                }
                opcode = in.readByte();
                switch (opcode) {
                case NEW_RECEIVER:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a NEW_RECEIVER from "
                                + origin);
                    }
                    newStream();
                    break;
                case NEW_MESSAGE:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a NEW_MESSAGE from "
                                + origin);
                    }
                    message.setFinished(false);
                    if (type.numbered) {
                        message.setSequenceNumber(message.readLong());
                    }
                    ReadMessage m = message;
                    messageArrived(m);
                    // Note: if upcall calls finish, a new message is
                    // allocated, so we cannot look at "message" anymore.
                    if (noThread || m.finishCalledInUpcall()) {
                        return;
                    }
                    break;
                case CLOSE_ALL_CONNECTIONS:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name
                                + ": Got a CLOSE_ALL_CONNECTIONS from "
                                + origin);
                    }
                    close(null);
                    return;
                case CLOSE_ONE_CONNECTION:
                    if (logger.isDebugEnabled()) {
                        logger.debug(name + ": Got a CLOSE_ONE_CONNECTION from "
                                + origin);
                    }
                    // read the receiveport identifier from which the sendport
                    // disconnects.
                    byte[] length = new byte[Conversion.INT_SIZE];
                    in.readArray(length);
                    byte[] bytes = new byte[Conversion.defaultConversion
                            .byte2int(length, 0)];
                    in.readArray(bytes);
                    ReceivePortIdentifier identifier
                            = new ReceivePortIdentifier(bytes);
                    if (ident.equals(identifier)) {
                        // Sendport is disconnecting from me.
                        if (logger.isDebugEnabled()) {
                            logger.debug(name + ": disconnect from " + origin);
                        }
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

    private final boolean no_connectionhandler_thread;

    private boolean reader_busy = false;

    TcpReceivePort(Ibis ibis, TcpPortType type, String name, Upcall upcall,
            boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) {
        super(ibis, type, name, upcall, connUpcall, connectionAdministration);

        no_connectionhandler_thread = upcall == null && connUpcall == null
                && !type.manyToOne
                && !type.capabilities().hasCapability(RECEIVE_POLL)
                && !type.capabilities().hasCapability(RECEIVE_TIMEOUT);
    }

    public void messageArrived(ReadMessage msg) {
        super.messageArrived(msg);
        if (! no_connectionhandler_thread && upcall == null) {
            synchronized(this) {
                // Wait until the message is finished before starting to
                // read from the stream again ...
                while (! msg.isFinished()) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
            }
        }
    }

    public ReadMessage getMessage(long timeout) throws IOException {
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
                    while (message != null && ! closed) {
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

    public synchronized void closePort(long timeout) {
        ReceivePortConnectionInfo conns[] = connections();
        if (no_connectionhandler_thread && conns.length > 0) {
            ThreadPool.createNew((ConnectionHandler) conns[0],
                    "ConnectionHandler");
        }
        super.closePort(timeout);
    }

    synchronized void connect(SendPortIdentifier origin, IbisSocket s,
            BufferedArrayInputStream in) throws IOException {
        ConnectionHandler conn = new ConnectionHandler(origin, s, this, in);
        if (! no_connectionhandler_thread) {
            ThreadPool.createNew(conn, "ConnectionHandler");
        }
    }
}
