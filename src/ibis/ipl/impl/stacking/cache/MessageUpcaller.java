package ibis.ipl.impl.stacking.cache;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cache.util.Loggers;
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
    CacheReceivePort port;
    boolean wasLastPart = true;
    public boolean gotLastPart = false;
    CacheReadMessage currentLogicalMsg;
    public final Object currentLogicalMsgLock = new Object();
    final Lock newLogicalMsgLock = new ReentrantLock();
    /*
     * If the current true message has been or not emptied.
     */
    public boolean messageDepleted;

    public MessageUpcaller(MessageUpcall upcaller, CacheReceivePort port) {
        this.upcaller = upcaller;
        this.port = port;
    }

    @Override
    public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
        messageDepleted = false;
        boolean isLastPart = m.readBoolean();
        int bufSize = m.readInt();

        Loggers.upcallLog.log(Level.INFO, "\n\tGot message upcall from {0}. isLastPart={1}, bufSize={2}", 
                new Object[] {m.origin(), isLastPart, bufSize});

        /*
         * This is a logically new message.
         */
        if (wasLastPart) {
            /*
             * Block until the old logical message has finished.
             */
            newLogicalMsgLock.lock();
            try {
                Loggers.upcallLog.log(Level.INFO, "\n\tNew logical message from {0}", m.origin());
                /*
                 * Initializations.
                 */
                currentLogicalMsg = new CacheReadMessage(m, port);
                gotLastPart = isLastPart;
            } finally {
                newLogicalMsgLock.unlock();
            }
        }

        synchronized (currentLogicalMsgLock) {
            if (currentLogicalMsg.recvPort.dataIn.closed) {
                /*
                 * The read message has been closed. discard everything.
                 * also, if last part, notify the "main thread" of the message,
                 * the first thread which handles the first part of the
                 * logical message.
                 */
                if (isLastPart) {
                    gotLastPart = true;
                    currentLogicalMsgLock.notifyAll();
                }
                wasLastPart = isLastPart;
                return;
            }
        }

        /*
         * Need to aquire the lock before starting the data-offering thread.
         * Otherwise, the thread may deplete and finish the message before we
         * can reach the code below and another upcall may come and overtake us.
         */
        if (wasLastPart) {
            newLogicalMsgLock.lock();
        }

        /*
         * Feed the buffer of the DataInputStream with the data from this
         * message. When all data has been extracted, messageDepleted is set to
         * true, and this thread is notified. It will finish and let the next
         * streaming upcall come.
         */
        try {
            currentLogicalMsg.offerToBuffer(isLastPart, bufSize, m);
        } catch(Exception ex) {
            // nothing should be thrown here, but
            // just to be sure we don't leave the lock locked.
        }


        if (wasLastPart) {
            /*
             * This is a logically new message. Update wasLastPart.
             */
            wasLastPart = isLastPart;

            try {
                /*
                 * User upcall.
                 */
                Loggers.upcallLog.log(Level.INFO, "Calling user upcall...");
                upcaller.upcall(currentLogicalMsg);
                Loggers.upcallLog.log(Level.INFO, "User upcall finished.");

                /*
                 * User upcall finished. Either the message was finished inside
                 * the upcall or we have to do it here.
                 *
                 * The finish method of the cache read message will wait for any
                 * remaining streaming intermediate upcall messages.
                 */
                try {
                    currentLogicalMsg.finish();
                } catch (Exception ex) {
                    Loggers.upcallLog.log(Level.WARNING, "Exception when"
                            + " trying to finish cacheReadMsg; maybe "
                            + "the user upcall finished it:\t{0}", ex.getMessage());
                }
                /*
                 * We are done.
                 */
                return;
            } finally {
                newLogicalMsgLock.unlock();
            }
        }

        /*
         * Intermediate threads with the intermediate messages are here.
         */
        /*
         * Update wasLastPart.
         */
        wasLastPart = isLastPart;

        /*
         * Only the threads which are not the first part of the 
         * logical streamed message get here.
         * 
         * Block until all data from m has been removed 
         * or
         * a close on the read message was called and we fake
         * the message depletion and get out, ignoring any other data received.
         */
        synchronized (currentLogicalMsgLock) {
            while (!messageDepleted) {
                try {
                    Loggers.upcallLog.log(Level.INFO, "Waiting on current "
                            + "streamed message to be depleted and "
                            + "current logical message to be finished...");
                    currentLogicalMsgLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }

            /*
             * Also, if this was the last part of the logical message,
             * notify the main thread of the message of this.
             */
            if (isLastPart) {
                gotLastPart = true;
                currentLogicalMsgLock.notifyAll();
                Loggers.upcallLog.log(Level.INFO, "Finishing 1 logical message upcall.\n");
            }
        }
    }
}
