package ibis.ipl.impl.stacking.cc.io;

import ibis.ipl.PortType;
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
        final ReadMessage msg;
        private int remaining;

        public DataOfferingThread(UpcallBufferedDataInputStream in,
                boolean isLastPart, int remaining,
                ReadMessage msg) {
            logger.debug("Thread created for filling up "
                    + "the buffer with data.");
            this.in = in;
            this.msg = msg;
            this.remaining = remaining;
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
                            + " Got bufferSize={}", remaining);

                    assert in.index + in.buffered_bytes <= in.capacity;
                    assert in.len <= in.capacity;

                    while (remaining > 0) {
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
                            if (remaining <= 0) {
                                /*
                                 * I'm done with this message.
                                 */
                                logger.debug("Message drained."
                                        + " Current buffered bytes = {}, len requested = {}",
                                        new Object[]{in.buffered_bytes, in.len});
                                in.notifyAll();
                                return;
                            }
                            /*
                             * I have at least some remaining bytes from which
                             * to read from.
                             */
                            int n = Math.min(in.capacity - (in.index + in.buffered_bytes),
                                    remaining);
                            byte[] temp = new byte[n];
                            msg.readArray(temp, 0, temp.length);
                            System.arraycopy(temp, 0, in.buffer,
                                    in.index + in.buffered_bytes, n);
                            in.buffered_bytes += n;
                            in.bytes += n;
                            remaining -= n;
                        }
                    }

                    logger.debug("Message drained."
                            + " Current buffered bytes = {}, len requested = {}",
                            new Object[]{in.buffered_bytes, in.len});
                    
                    in.notifyAll();
                } // end sync
            } catch (Exception ex) {
                logger.debug("Message closed "
                        + "most likely because the user upcall "
                        + "has finished and exited the wrapper upcall:\t{}", ex.toString());
            } finally {
                /*
                 * Notify the end of this message, so we may pick up another
                 * upcall.
                 * This is always executed: even if the message was depleted,
                 * or a close was forced on the read message.
                 */
                synchronized (in) {
                    in.port.msgUpcall.messageDepleted = true;
                    try {
                        msg.finish();
                    } catch (IOException ex) {
                        logger.debug("Base message finish threw:\t", ex);
                    }
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
    protected ExecutorService ex;
    /*
     * Length required to be in the buffer at a given time.
     */
    public int len;

    public UpcallBufferedDataInputStream(CCReceivePort port) {
        super(port);
        this.ex = Executors.newSingleThreadExecutor();
    }

    @Override
    public void offerToBuffer(boolean isLastPart, int remaining, ReadMessage msg) {
        currentBaseMsg = msg;
        try {
            ex.submit(new DataOfferingThread(this, isLastPart, remaining, msg));
        } catch (Exception e) {
            logger.warn("Couldn''t submit another data"
                    + " offering thread. The executor was shutdown because"
                    + " the user shutdown the read message."
                    + " and any other following streaming msgs are discarded:\t{}",
                    e.toString());
            synchronized (this) {
                port.msgUpcall.messageDepleted = true;
                try {
                    msg.finish();
                } catch (IOException ignoreMe) {
                }
                notifyAll();
            }
        }
    }

    @Override
    protected void requestFromBuffer(int n) {
        synchronized (this) {
            len = n;
            while (buffered_bytes < len) {
                try {
                    logger.debug("Waiting for buffer "
                            + "to fill: currBufBytes={}, "
                            + "requestedLen={}", new Object[]{buffered_bytes, len});
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
            logger.debug("Closing current read message: "
                    + "{}. gotLastPart={}", new Object[] {port.currentLogicalReadMsg,
                    port.msgUpcall.gotLastPart});
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
                currentBaseMsg.finish();
            } catch(Throwable t) {
                logger.debug("Caught exception when"
                            + " trying to finish CCReadMsg; maybe "
                            + "the user upcall finished it. Details:", t);
            }
            /*
             * Now wait until we have got all the messages, so we can
             * properly finish.
             */
            while (!port.msgUpcall.gotLastPart) {
                try {
                    logger.debug("Waiting for the last part from "
                            + "{}", port.currentLogicalReadMsg.origin());
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            logger.debug("Closed the current read message.");
            
            /*
             * reset the variable.
             */
            closed = false;
        }
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
        this.ex.shutdownNow();
    }
}