/* $Id: TcpReceivePort.java 6259 2007-09-10 11:03:29Z ceriel $ */

package ibis.ipl.impl.smartsockets;

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
import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.Properties;

class SmartSocketsReceivePort extends ReceivePort implements SmartSocketsProtocol {

    class ConnectionHandler extends ReceivePortConnectionInfo 
            implements Runnable, SmartSocketsProtocol {

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
                    if (numbered) {
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
                        s.getOutputStream().write(0);
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

    SmartSocketsReceivePort(Ibis ibis, PortType type, String name, MessageUpcall upcall,
            ReceivePortConnectUpcall connUpcall, Properties props) throws IOException {
        super(ibis, type, name, upcall, connUpcall, props);

        no_connectionhandler_thread = upcall == null && connUpcall == null
                && type.hasCapability(PortType.CONNECTION_ONE_TO_ONE)
                && !type.hasCapability(PortType.RECEIVE_POLL)
                && !type.hasCapability(PortType.RECEIVE_TIMEOUT);
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

    void connect(SendPortIdentifier origin, IbisSocket s,
            BufferedArrayInputStream in) throws IOException {
        ConnectionHandler conn;

        synchronized(this) {
            conn = new ConnectionHandler(origin, s, this, in);
        }
        
        if (! no_connectionhandler_thread) {
            // ThreadPool.createNew(conn, "ConnectionHandler");
            // We are already in a dedicated thread, so no need to create a new
            // one!
            // But this method was synchronized!!! Fixed (Ceriel).
            conn.run();
        }
    }
}
