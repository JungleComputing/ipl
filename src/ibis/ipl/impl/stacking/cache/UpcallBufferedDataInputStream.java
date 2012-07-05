package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReadMessage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class UpcallBufferedDataInputStream extends BufferedDataInputStream {

    private static class DataOfferingThread implements Runnable {

        final UpcallBufferedDataInputStream in;
        final ReadMessage msg;

        public DataOfferingThread(UpcallBufferedDataInputStream is,
                ReadMessage msg) {
            this.in = is;
            this.msg = msg;
        }

        @Override
        public void run() {
            synchronized (in) {
                try {
                    int remaining = msg.readInt();
                    assert in.index + in.buffered_bytes <= in.capacity;
                    assert in.len <= in.capacity;

                    /*
                     * I have enough info for the guy. It's ok. I can let him
                     * take over.
                     */
                    while (in.buffered_bytes >= in.len) {
                        try {
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
                     * Fill up the buffer with some data from the currentMsg,
                     * but at most what currentMsg has left.
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
                         * I have at least some remaining bytes from which to
                         * read from.
                         */
                        int n = Math.min(in.capacity - (in.index + in.buffered_bytes), 
                                remaining);
                        msg.readArray(in.buffer, in.index + in.buffered_bytes, n);
                        in.buffered_bytes += n;
                        in.bytes += n;
                        remaining -= n;
                    }
                } catch (IOException ignoreMe) {
                }
                in.notify();
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

    public UpcallBufferedDataInputStream(ReadMessage m, CacheReceivePort port) {
        super(port);

        this.ex = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void offer(ReadMessage msg) {
        ex.execute(new DataOfferingThread(this, msg));
    }

    @Override
    protected void fillBuffer(int n) {
        synchronized (this) {
            len = n;
            while (super.buffered_bytes <= len) {
                try {
                    this.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
    }

    @Override
    public void close() {
        super.port.msgUpcall.wasLastPart = true;
    }
}