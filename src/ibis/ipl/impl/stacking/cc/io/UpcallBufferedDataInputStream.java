package ibis.ipl.impl.stacking.cc.io;

import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpcallBufferedDataInputStream extends BufferedDataInputStream {
    
    protected final static Logger logger = 
            LoggerFactory.getLogger(UpcallBufferedDataInputStream.class);

    private static class DataOfferingThread implements Runnable {

        final UpcallBufferedDataInputStream in;

        public DataOfferingThread(UpcallBufferedDataInputStream in) {
            logger.debug("Thread created for filling up "
                    + "the buffer with data.");
            this.in = in;
        }

        @Override
        public void run() {
            try {
                /*
                 * Need this sync to protect the buffer from adding data and
                 * removing it simultaneously.
                 */
                synchronized (in) {
                    logger.debug("Thread started."
                            + " Got bufferSize={}", in.remainingBytes);

                    assert in.index + in.buffered_bytes <= in.capacity;
                    assert in.len <= in.capacity;

                    while (in.remainingBytes > 0) {
                        /*
                         * I have enough info for the guy. It's ok. I can let
                         * him take over.
                         */
                        while (in.buffered_bytes >= in.len) {
                            if (in.closed) {
                                logger.debug("Closing the current"
                                        + " supplying thread. bufBytes={}, "
                                        + "lenReq={}", new Object[] {in.buffered_bytes,
                                        in.len});
                                /*
                                 * Execute finally and get out.
                                 */
                                return;
                            }
                            try {
                                in.notifyAll();
                                in.wait();
                            } catch (InterruptedException ignoreMe) {
                            }
                        }

                        if (in.buffered_bytes == 0) {
                            in.index = 0;
                        } else if (in.index + in.buffered_bytes + in.len > in.capacity) {
                            // not enough space for "len" more bytes
                            // move index to 0, and we have enough space.
                            System.arraycopy(in.buffer, in.index, in.buffer, 0, in.buffered_bytes);
                            in.index = 0;
                        }
                        /*
                         * Fill up the buffer with some data from the
                         * currentMsg, but at most what currentMsg has left.
                         */
                        while (in.buffered_bytes < in.len) {
                            if (in.remainingBytes <= 0) {
                                /*
                                 * I'm done with this message.
                                 */
                                logger.debug("Message drained."
                                        + " Current buffered bytes = {}, len requested = {}",
                                        new Object[]{in.buffered_bytes, in.len});
                                return;
                            }
                            /*
                             * I have at least some remaining bytes from which
                             * to read from.
                             */
                            int n = Math.min(in.capacity - (in.index + in.buffered_bytes),
                                    in.remainingBytes);
                            logger.debug("Going to read {} bytes from {}.", 
                                    new Object[] {n, in.currentBaseMsg});
                            byte[] temp = new byte[n];
                            in.currentBaseMsg.readArray(temp, 0, temp.length);
                            System.arraycopy(temp, 0, in.buffer,
                                    in.index + in.buffered_bytes, n);
                            in.buffered_bytes += n;
                            in.bytes += n;
                            in.remainingBytes -= n;
                            logger.debug("got some data: bufferedBytes={}, remainingBytes={}",
                                    new Object[] {in.buffered_bytes, in.remainingBytes});
                        }
                    }

                    logger.debug("Message drained."
                            + " Current buffered bytes = {}",
                            new Object[]{in.buffered_bytes});
                } // end sync
            } catch (Exception ex) {
                logger.debug("Message closed "
                        + "most likely because the user upcall "
                        + "has finished and exited the wrapper upcall.", ex);
            } finally {
                /*
                 * Notify that data is available.
                 */
                synchronized (in) {
                    in.notifyAll();
                }
                logger.debug("Data offering thread finishing...");
            }
        }
    }
    /*
     * Executor which will handle the correct and sequential buffering of the
     * data when upcalls are enabled. There will be at most one thread alive at
     * any time in this executor.
     */
    protected ExecutorService execServ;
    /*
     * Length required to be in the buffer at a given time.
     */
    public int len;

    public UpcallBufferedDataInputStream(CCReceivePort port) {
        super(port);
        this.execServ = Executors.newSingleThreadExecutor();
    }

    @Override
    public void offerToBuffer(boolean isLastPart, int remaining, ReadMessage msg) {
        synchronized(this) {
            remainingBytes = remaining;
            currentBaseMsg = msg;
            currentBaseMsgFinished = false;
            logger.debug("Receive port {} has current base message:\t{}",
                    new Object[] {this.recvPort.name(), currentBaseMsg});
        }
        try {
            execServ.submit(new DataOfferingThread(this));
        } catch (Exception ex) {
            logger.error("Couldn''t submit another data"
                    + " offering thread.", ex);
        }
    }

    @Override
    protected void requestFromBuffer(int n) {
        synchronized (this) {
            len = n;
            while (buffered_bytes < len) {
                if(buffered_bytes == 0 && remainingBytes == 0) {
                    try {
                        logger.debug("Got lock on object:\t{}\n."
                                + "bufBytes==0 and remainingBytes==0."
                                + " Finishing the current read message:\t{}",
                                new Object[] {this, currentBaseMsg});
                        if(!currentBaseMsgFinished) {
                            currentBaseMsgFinished = true;
                            currentBaseMsg.finish();                            
                        }
                    } catch(Exception ex) {
                        logger.debug("This should not happen. This is the only"
                                + " place where the currentBaseMsg should"
                                + " be closed when more data is requested."
                                + " Check where it was closed before and why.",
                                ex);
                    }
                }
                try {
                    logger.debug("Waiting for buffer "
                            + "to fill: currBufBytes={}, remainingBytes={} "
                            + "requestedLen={}", new Object[]{buffered_bytes,
                                remainingBytes, len});
                    notifyAll();
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            logger.debug("Got my data. bufferedBytes = {},"
                    + " lenRequested = {}", new Object[]{buffered_bytes, len});
        }
    }

    @Override
    public void finish() throws IOException {
        /*
         * Wait for the last part.
         */
        synchronized (this) {
            logger.debug("Port {} is closing the current read message. gotLastPart={}",
                    new Object[] {recvPort.name(), recvPort.msgUpcall.gotLastPart});
            /*
             * Start sending any future data to a sink.
             */
            closed = true;
            /*
             * Notify (if any) the thread standing by with data to be taken out.
             * This close was called forcefully and any other data remaining in
             * the buffer/pipe will be discarded.
             */
            notifyAll();

            try {
                if (!currentBaseMsgFinished) {
                    currentBaseMsgFinished = true;
                    currentBaseMsg.finish();                    
                }
            } catch (Throwable t) {
                logger.warn("Caught exception when"
                        + " trying to finish CCReadMsg; maybe "
                        + "the user upcall finished it. Details:", t);
            }
            /*
             * Now wait until we have got all the messages, so we can
             * properly finish.
             */
            while (!recvPort.msgUpcall.gotLastPart) {
                try {
                    logger.debug("Port {} is waiting for the last part "
                            + "of the message from {}", 
                            new Object[] {recvPort.name(), 
                                recvPort.currentLogicalReadMsg.origin()});
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            logger.debug("Port {} has closed the current read message.",
                    recvPort.name());
            
            /*
             * reset the variable.
             */
            closed = false;
            
            /*
             * I need this for an extremely stupid and rare case:
             * - message is finished in upcall;
             * - exiting user upcall - pause now.
             * - new upcall comes, rewrites gotLastPart.
             * - old thread is blocked thinking it didn't get the last part 
             *          --- although the message is quite finished.
             * - new thread waits for the lock on the new message.
             * 
             * Deadlock. trust me, i've got this!
             */
            recvPort.currentLogicalReadMsg.isCompletelyFinished = true;
            notifyAll();
        }
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
        this.execServ.shutdownNow();
    }
}