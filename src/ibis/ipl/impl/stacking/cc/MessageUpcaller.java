package ibis.ipl.impl.stacking.cc;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * This class handles message upcalls.
 */
public final class MessageUpcaller implements MessageUpcall {

    /*
     * The user defined message upcaller.
     */
    MessageUpcall upcaller;
    /*
     * A ref to our recv Port.
     */
    CCReceivePort recvPort;
    /*
     * Boolean to check if the last base message was the last part.
     */
    public volatile boolean wasLastPart = true;
    /*
     * Boolean to check if I got or not the last part.
     */
    public volatile boolean gotLastPart = false;
    /*
     * If the current true message has been or not emptied.
     */
    public volatile boolean messageDepleted;
    
    /*
     * Lock to make sure at most 1 logical msg is alive.
     */
    final Lock newLogicalMsgLock = new ReentrantLock();

    public MessageUpcaller(MessageUpcall upcaller, CCReceivePort port) {
        this.upcaller = upcaller;
        this.recvPort = port;
    }

    @Override
    public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
        messageDepleted = false;
        boolean isLastPart = m.readBoolean();
        int bufSize = m.readInt();

        Loggers.upcallLog.log(Level.INFO, "\n\tGot message upcall from {0}. "
                + "isLastPart={1}, bufSize={2}",
                new Object[]{m.origin(), isLastPart, bufSize});

        /*
         * This is a logically new message.
         */
        if (wasLastPart) {
            Loggers.upcallLog.log(Level.INFO, "\n\tNew logical message from {0}", m.origin());
            /*
             * Block until the old logical message has finished.
             */
            newLogicalMsgLock.lock();
            /*
             * This is a logically new message. Update wasLastPart.
             */
            wasLastPart = isLastPart;
            try {                
                /*
                 * Initializations.
                 */
                recvPort.currentLogicalReadMsg = new CCReadMessage(m, recvPort);
                recvPort.readMsgRequested = false;
                gotLastPart = isLastPart;
            } finally {
                newLogicalMsgLock.unlock();
            }
            
            synchronized (recvPort.upcallDataIn) {
                if (recvPort.currentLogicalReadMsg.recvPort.dataIn.closed) {
                    /*
                     * The read message has been closed. discard everything.
                     * also, if last part, notify the "main thread" of the
                     * message, the first thread which handles the first part of
                     * the logical message.
                     */
                    if (isLastPart) {
                        gotLastPart = true;
                        messageDepleted = true;
                        try {
                            m.finish();
                        } catch (IOException ex) {
                            Loggers.readMsgLog.log(Level.WARNING, "Base message"
                                    + " finish threw:\t{0}", ex.toString());
                        }
                        recvPort.upcallDataIn.notifyAll();
                    }
                    return;
                }
            }

            newLogicalMsgLock.lock();

            /*
             * Feed the buffer of the DataInputStream with the data from this
             * message. When all data has been extracted, messageDepleted is set
             * to true, and this thread is notified. It will finish and let the
             * next streaming upcall come.
             */
            try {
                recvPort.currentLogicalReadMsg.offerToBuffer(isLastPart, bufSize, m);
            } catch (Exception ex) {
                // nothing should be thrown here, but
                // just to be sure we don't leave the lock locked.
            }

            try {
                /*
                 * User upcall.
                 */
                Loggers.upcallLog.log(Level.INFO, "Calling user upcall...");
                upcaller.upcall(recvPort.currentLogicalReadMsg);
                Loggers.upcallLog.log(Level.INFO, "User upcall finished.");

                /*
                 * User upcall finished. Either the message was finished inside
                 * the upcall or we have to do it here.
                 *
                 * The finish method of the CC read message will wait for any
                 * remaining streaming intermediate upcall messages.
                 */
                try {
                    synchronized (recvPort.upcallDataIn) {
                        while (!gotLastPart || !messageDepleted) {
                            Loggers.upcallLog.log(Level.INFO, "I want to finish"
                                    + " the read message, but gotLastPart={0},"
                                    + " messageDepleted={1}",
                                    new Object[]{gotLastPart, messageDepleted});
                            try {
                                recvPort.upcallDataIn.wait();
                            } catch (Exception ignoreMe) {
                            }
                        }
                    }
                    Loggers.upcallLog.log(Level.INFO, "Finishing 1 logical message upcall.\n");
                    recvPort.currentLogicalReadMsg.finish();
                } catch (Exception ex) {
                    Loggers.upcallLog.log(Level.WARNING, "Exception when"
                            + " trying to finish CCReadMsg; maybe "
                            + "the user upcall finished it.", ex);
                }
                /*
                 * We are done.
                 */
            } finally {
                newLogicalMsgLock.unlock();
            }
        } else {
            /*
             * Update wasLastPart.
             */
            wasLastPart = isLastPart;
            /*
             * This is a non-first part of the entire logical message.
             */
            synchronized (recvPort.upcallDataIn) {
                if (recvPort.currentLogicalReadMsg.recvPort.dataIn.closed) {
                    /*
                     * The read message has been closed. discard everything.
                     * also, if last part, notify the "main thread" of the
                     * message, the first thread which handles the first part of
                     * the logical message.
                     */
                    if (isLastPart) {
                        gotLastPart = true;
                        messageDepleted = true;
                        try {
                            m.finish();
                        } catch (IOException ex) {
                            Loggers.readMsgLog.log(Level.WARNING, "Base message"
                                    + " finish threw:\t{0}", ex.toString());
                        }
                        recvPort.upcallDataIn.notifyAll();
                    }
                    return;
                }
            }

            /*
             * Feed the buffer of the DataInputStream with the data from this
             * message. When all data has been extracted, messageDepleted is set
             * to true, and this thread is notified. It will finish and let the
             * next streaming upcall come.
             */
            try {
                recvPort.currentLogicalReadMsg.offerToBuffer(isLastPart, bufSize, m);
            } catch (Exception ex) {
                // nothing should be thrown here, but
                // just to be sure we don't leave the lock locked.
            }

            /*
             * Block until all data from m has been removed or a close on the
             * read message was called and we fake the message depletion and get
             * out, ignoring any other data received.
             */
            synchronized (recvPort.upcallDataIn) {
                while (!messageDepleted) {
                    try {
                        Loggers.upcallLog.log(Level.INFO, "Waiting on current "
                                + "streamed message to be depleted and "
                                + "current logical message to be finished...");
                        recvPort.upcallDataIn.wait();
                    } catch (InterruptedException ignoreMe) {
                    }
                }

                /*
                 * Also, if this was the last part of the logical message,
                 * notify the main thread of the message of this.
                 */
                if (isLastPart) {
                    gotLastPart = true;
                    recvPort.upcallDataIn.notifyAll();
                }
            }
        }
    }
}
