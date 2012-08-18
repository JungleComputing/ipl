package ibis.ipl.impl.stacking.cc.io;

import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class UpcallBufferedDataInputStream extends BufferedDataInputStream {

    private static class DataOfferingThread implements Runnable {

        final UpcallBufferedDataInputStream in;
        final ReadMessage msg;
        private int remaining;

        public DataOfferingThread(UpcallBufferedDataInputStream in,
                boolean isLastPart, int remaining,
                ReadMessage msg) {
            Loggers.readMsgLog.log(Level.INFO, "Thread created for filling up "
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
                    Loggers.readMsgLog.log(Level.INFO, "Thread started."
                            + " Got bufferSize={0}", remaining);

                    assert in.index + in.buffered_bytes <= in.capacity;
                    assert in.len <= in.capacity;

                    while (remaining > 0) {
                        /*
                         * I have enough info for the guy. It's ok. I can let
                         * him take over.
                         */
                        while (in.buffered_bytes >= in.len) {
                            if (in.closed) {
                                Loggers.readMsgLog.log(Level.INFO, "Closing the current"
                                        + " supplying thread. bufBytes={0}, "
                                        + "lenReq={1}", new Object[] {in.buffered_bytes,
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
                                Loggers.readMsgLog.log(Level.INFO, "Message drained."
                                        + " Current buffered bytes = {0}, len requested = {1}",
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
                            msg.readArray(
                                    in.buffer, in.index + in.buffered_bytes, n);
                            in.buffered_bytes += n;
                            in.bytes += n;
                            remaining -= n;
                        }
                    }

                    Loggers.readMsgLog.log(Level.INFO, "Message drained."
                            + " Current buffered bytes = {0}, len requested = {1}",
                            new Object[]{in.buffered_bytes, in.len});
                    
                    in.notifyAll();
                } // end sync
            } catch (Exception ex) {
                Loggers.readMsgLog.log(Level.INFO, "Message closed "
                        + "most likely because the user upcall "
                        + "has finished and exited the wrapper upcall:\t{0}", ex.toString());
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
                        Loggers.readMsgLog.log(Level.WARNING, "Base message"
                                + " finish threw:\t", ex);
                    }
                    in.notifyAll();
                }
                Loggers.readMsgLog.log(Level.INFO, "Data offering thread finishing...");
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
            Loggers.readMsgLog.log(Level.WARNING, "Couldn''t submit another data"
                    + " offering thread. The executor was shutdown because"
                    + " the user shutdown the read message."
                    + " and any other following streaming msgs are discarded:\t{0}",
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
                    Loggers.readMsgLog.log(Level.INFO, "Waiting for buffer "
                            + "to fill: currBufBytes={0}, "
                            + "requestedLen={1}", new Object[]{buffered_bytes, len});
                    notifyAll();
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            Loggers.readMsgLog.log(Level.INFO, "Got my data. bufferedBytes = {0},"
                    + " lenRequested = {1}", new Object[]{buffered_bytes, len});
        }
    }

    @Override
    public void finish() throws IOException {
        /*
         * Wait for the last part.
         */
        synchronized (this) {
            Loggers.readMsgLog.log(Level.INFO, "Closing current read message: "
                    + "{0}. gotLastPart={1}", new Object[] {port.currentLogicalReadMsg,
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
                Loggers.upcallLog.log(Level.WARNING, "Exception when"
                            + " trying to finish CCReadMsg; maybe "
                            + "the user upcall finished it.", t);
            }
            /*
             * Now wait until we have got all the messages, so we can
             * properly finish.
             */
            while (!port.msgUpcall.gotLastPart) {
                try {
                    Loggers.readMsgLog.log(Level.INFO, "Waiting for the last part from "
                            + "{0}", port.currentLogicalReadMsg.origin());
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            Loggers.readMsgLog.log(Level.INFO, "Closed the current read message.");
            
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