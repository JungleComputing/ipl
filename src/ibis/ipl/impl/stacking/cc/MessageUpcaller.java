package ibis.ipl.impl.stacking.cc;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles message upcalls.
 */
public final class MessageUpcaller implements MessageUpcall {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(MessageUpcaller.class);

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
     * Lock to make sure at most 1 logical msg is alive.
     */
    final Lock newLogicalMsgLock = new ReentrantLock();

    public MessageUpcaller(MessageUpcall upcaller, CCReceivePort port) {
        this.upcaller = upcaller;
        this.recvPort = port;
    }

    @Override
    public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
        if(logger.isDebugEnabled()) {
        logger.debug("\n\tGot message upcall from {}.",
                new Object[]{m.origin()});        
        }
        
        boolean isLastPart = m.readByte() == 1 ? true : false;
        int bufSize = CCReadMessage.readIntFromBytes(m);

        if(logger.isDebugEnabled()) {
        logger.debug("\n\tMessage upcall: "
                + "isLastPart={}, bufSize={}. I am {}",
                new Object[]{isLastPart, bufSize, recvPort.name()});        
        }

        /*
         * This is a logically new message.
         */
        if (wasLastPart) {
            if(logger.isDebugEnabled()) {
            logger.debug("\n\tNew logical message from {}", m.origin());
            }
            CCReadMessage localCurrentLogicalReadMsg;
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
                localCurrentLogicalReadMsg = recvPort.currentLogicalReadMsg;
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
                        try {
                            m.finish();
                            if(logger.isDebugEnabled()) {
                            logger.debug("Closed base read message.");
                            }
                        } catch (IOException ex) {
                            if(logger.isWarnEnabled()) {
                            logger.warn("Base message"
                                    + " finish threw:\t", ex);
                            }
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
                if(logger.isDebugEnabled()) {
                logger.debug("Calling user upcall...");
                }
                upcaller.upcall(recvPort.currentLogicalReadMsg);
                if(logger.isDebugEnabled()) {
                logger.debug("User upcall finished.");
                }
                
                /*
                 * User upcall finished. Either the message was finished inside
                 * the upcall or we have to do it here.
                 *
                 * The finish method of the CC read message will wait for any
                 * remaining streaming intermediate upcall messages.
                 */
                try {
                    synchronized (recvPort.upcallDataIn) {                        
                        while (!localCurrentLogicalReadMsg.isCompletelyFinished
                                && (!gotLastPart
                                    || recvPort.upcallDataIn.buffered_bytes > 0)) {
                            if(logger.isDebugEnabled()) {
                            logger.debug("I want to finish"
                                    + " the read message, but gotLastPart={},"
                                    + " bufferedBytes={}, remainingBytes={}",
                                    new Object[]{gotLastPart,
                                        recvPort.upcallDataIn.buffered_bytes,
                                    recvPort.upcallDataIn.remainingBytes});
                            }

                            if (recvPort.upcallDataIn.buffered_bytes == 0
                                    && recvPort.upcallDataIn.remainingBytes == 0) {
                                try {
                                    if(!recvPort.upcallDataIn.currentBaseMsgFinished) {
                                        recvPort.upcallDataIn.currentBaseMsgFinished = true;
                                        recvPort.upcallDataIn.currentBaseMsg.finish();
                                        if(logger.isDebugEnabled()) {
                                        logger.debug("BufBytes==0 and remainBytes==0."
                                            + " Closed the current base read msg."
                                            + " Can receive the next message.");
                                        }
                                    } else {
                                        if(logger.isDebugEnabled()) {
                                        logger.debug("BufBytes==0 and remainBytes==0."
                                            + " Base read msg already closed.");
                                        }
                                    }
                                } catch (Exception ex) {
                                    if(logger.isDebugEnabled()) {
                                    logger.debug("Finished user upcall and now"
                                            + " trying to close the last base message."
                                            + " If the user closed it manually,"
                                            + " this should appear.", ex);
                                    }
                                }
                            }

                            try {
                                recvPort.upcallDataIn.wait();
                            } catch (Exception ignoreMe) {
                            }
                        }
                    }
                    if(logger.isDebugEnabled()) {
                    logger.debug("Finishing 1 logical message upcall.\n");
                    }
                    localCurrentLogicalReadMsg.finish();
                } catch (Throwable t) {
                    if(logger.isDebugEnabled()) {
                    logger.debug("Exception when"
                            + " trying to finish CCReadMsg; maybe "
                            + "the user upcall finished it. Details: ", t);
                    }
                }
                /*
                 * We are done.
                 */
            } finally {
                /*
                 * The lock is released in currentLogicalReadMsg.finish().
                 * But in case of some exception, just make sure we unlock it.
                 */
                try {
                    newLogicalMsgLock.unlock();
                } catch(Exception ex) {}
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
                        try {
                            m.finish();
                        } catch (IOException ex) {
                            if(logger.isWarnEnabled()) {
                            logger.warn("Base message"
                                    + " finish threw:\t", ex);
                            }
                        }
                        if(logger.isDebugEnabled()) {
                        logger.debug("Closed base read message.");
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
                if(logger.isDebugEnabled()) {
                logger.debug("\n\tGot lock on object:\t{}."
                        + "\n\tSecondary upcall offered its message to the thread."
                        + " Now it waits for the message to be depleted:"
                        + " buffBytes={}, remainingBytes={}",
                        new Object[]{
                            recvPort.upcallDataIn,
                            recvPort.upcallDataIn.buffered_bytes,
                            recvPort.upcallDataIn.remainingBytes});
                }
                while (recvPort.upcallDataIn.buffered_bytes > 0
                        || recvPort.upcallDataIn.remainingBytes > 0) {
                    try {
                        if(logger.isDebugEnabled()) {
                        logger.debug("Waiting on current "
                                + "buffer to be depleted...");
                        }
                        recvPort.upcallDataIn.wait();
                    } catch (InterruptedException ignoreMe) {
                    }
                }
                
                if(logger.isDebugEnabled()) {
                logger.debug("Going to exit this upcall. These values should both"
                        + " be 0: buffBytes={}, remainingBytes={}",
                        new Object[]{
                            recvPort.upcallDataIn.buffered_bytes,
                            recvPort.upcallDataIn.remainingBytes});
                }
                
                assert recvPort.upcallDataIn.buffered_bytes == 0;
                assert recvPort.upcallDataIn.remainingBytes == 0;
                
                try {
                    /*
                     * This has to be here, otherwise we can close
                     * the next read message in another thread
                     * thinking it's this message.
                     */
                    if (!recvPort.upcallDataIn.currentBaseMsgFinished) {
                        recvPort.upcallDataIn.currentBaseMsgFinished = true;
                        recvPort.upcallDataIn.currentBaseMsg.finish();                        
                    }
                } catch (Exception ex) {
                    if(logger.isDebugEnabled()) {
                    logger.debug("Depleted everything. Tried to close the read message."
                            + " If the user closed it manually,"
                            + " this should appear.", ex);
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
