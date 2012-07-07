package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

class UpcallBufferedDataInputStream extends BufferedDataInputStream {

    private static class DataOfferingThread implements Runnable {

        final UpcallBufferedDataInputStream in;
        final ReadMessage msg;

        public DataOfferingThread(UpcallBufferedDataInputStream in,
                ReadMessage msg) {
            CacheManager.log.log(Level.INFO, "Thread created for filling up "
                    + "the buffer with data.");
            this.in = in;
            this.msg = msg;
        }

        @Override
        public void run() {
            CacheManager.log.log(Level.INFO, "Thread started.");
            /*
             * Need this sync to protect the buffer from adding data and
             * removing it.
             */
            synchronized (in) {
                try {
                    int remaining = msg.readInt();
                    CacheManager.log.log(Level.INFO, "bufferSize={0}", remaining);
                    assert in.index + in.buffered_bytes <= in.capacity;
                    assert in.len <= in.capacity;

                    while (remaining > 0) {
                        /*
                         * I have enough info for the guy. It's ok. I can let
                         * him take over.
                         */
                        while (in.buffered_bytes >= in.len) {
                            try {
                                in.notify();
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
                                in.notify();
                                return;
                            }
                            /*
                             * I have at least some remaining bytes from which
                             * to read from.
                             */
                            int n = Math.min(in.capacity - (in.index + in.buffered_bytes),
                                    remaining);
                            CacheManager.log.log(Level.INFO, "Buffering {0} bytes.", n);
                            msg.readArray(
                                    in.buffer, in.index + in.buffered_bytes, n);
                            in.buffered_bytes += n;
                            in.bytes += n;
                            remaining -= n;
                        }
                    }
                    CacheManager.log.log(Level.INFO, "Msg finished, can receive"
                            + "another upcall from now on.");
                } catch (Exception ex) {
                    CacheManager.log.log(Level.INFO, "Got exception when trying "
                            + "to read the buffer:\n\t{0}", ex.toString());
                } finally {
                    /*
                     * Notify the end of this message, so we may pick up another
                     * upcall.
                     */
                    synchronized (in.port.msgUpcall.currentLogicalMsgLock) {
                        in.port.msgUpcall.messageDepleted = true;
                        in.port.msgUpcall.currentLogicalMsgLock.notify();
                    }
                    /*
                     * Notify that there is available data in the buffer.
                     */
                    in.notify();
                }
            }
            CacheManager.log.log(Level.INFO, "Finishing one thread...");
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
    /*
     * The origin of the message we are draining of data.
     */
    private final SendPortIdentifier origin;

    UpcallBufferedDataInputStream(ReadMessage m, CacheReceivePort port) {
        super(port);
        this.origin = m.origin();
        this.ex = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void offerToBuffer(ReadMessage msg) {
        assert origin.equals(msg.origin());
        currentMsg = msg;
        ex.submit(new DataOfferingThread(this, msg));
    }

    @Override
    protected void requestFromBuffer(int n) {
        synchronized (this) {
            len = n;
            while (super.buffered_bytes < len) {
                try {
                    CacheManager.log.log(Level.INFO, "Waiting for buffer "
                            + "to fill: currBufBytes={0}, "
                            + "requestedLen={1}", new Object[]{buffered_bytes, len});
                    notify();
                    wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        CacheManager.log.log(Level.INFO, "Closing the current message");
        ex.shutdownNow();
    }
}