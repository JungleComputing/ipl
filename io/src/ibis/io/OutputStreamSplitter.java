/* $Id$ */

package ibis.io;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Contract: write to multiple outputstreams.
 * when an exception occurs, store it and continue.
 * when the data is written to all streams, throw one large exception
 * that contains all previous exceptions.
 * This way, even when one of the streams dies, the rest will receive the data.
 **/
public final class OutputStreamSplitter extends OutputStream {

    private static final int MAXTHREADS = 32;

    private boolean removeOnException = false;
    private boolean saveException = false;
    private SplitterException savedException = null;
    private long bytesWritten = 0;

    ArrayList<OutputStream> out = new ArrayList<OutputStream>();

    private int numSenders = 0;

    private class Sender implements Runnable {
        int offset;
        int len;
        byte[] buf;
        int index;

        Sender(byte[] buf, int index, int offset, int len) {
            this.buf = buf;
            this.offset = offset;
            this.len = len;
            this.index = index;
        }

        public void run() {
            doWrite(buf, offset, len, index);
            finish();
        }
    }

    private class Flusher implements Runnable {
        int index;

        Flusher(int index) {
            this.index = index;
        }

        public void run() {
            doFlush(index);
            finish();
        }
    }

    private class Closer implements Runnable {
        int index;

        Closer(int index) {
            this.index = index;
        }

        public void run() {
            doClose(index);
            finish();
        }
    }

    void doWrite(byte[] buf, int offset, int len, int index) {
        try {
            OutputStream o = out.get(index);
            if (o != null) {
        	o.write(buf, offset, len);
            }
        } catch(IOException e) {
            addException(e, index);
        }
    }

    void doFlush(int index) {
        try {
            OutputStream o = out.get(index);
            if (o != null) {
        	o.flush();
            }
        } catch(IOException e) {
            addException(e, index);
        }
    }

    void doClose(int index) {
        try {
            OutputStream o = out.get(index);
            if (o != null) {
        	o.close();
            }
        } catch(IOException e) {
            addException(e, index);
        }
    }

    private synchronized void finish() {
        numSenders--;
        notifyAll();
    }

    private synchronized void addException(IOException e, int index) {
        if (savedException == null) {
            savedException = new SplitterException();
        }
        savedException.add(out.get(index), e);
        if (removeOnException) {
            out.set(index, null);
        }
    }

    public OutputStreamSplitter() {
        // empty constructor
    }

    public OutputStreamSplitter(boolean removeOnException, boolean saveException) {
        this();
        this.removeOnException = removeOnException;
        this.saveException = saveException;
    }

    public synchronized void add(OutputStream s) {
        out.add(s);
    }

    public synchronized void remove(OutputStream s) throws IOException {
	
        synchronized(this) {
            while (numSenders != 0) {
                try {
                    wait();
                } catch(Exception e) {
                    // Ignored
                }
            }
        }
        
        int i = out.indexOf(s);
        
        if (i == -1) {
            throw new IOException("Removing unknown stream from splitter.");
        }

        out.remove(i);
    }

    public void write(int b) throws IOException {

        synchronized(this) {
            while (numSenders != 0) {
                try {
                    wait();
                } catch(Exception e) {
                    // Ignored
                }
            }
            numSenders++;
        }

        bytesWritten += out.size();

        for (int i = 0; i < out.size(); i++) {
            try {
        	OutputStream o = out.get(i);
        	if (o != null) {
        	    o.write(b);
        	}
            } catch (IOException e2) {
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(out.get(i), e2);
                if (removeOnException) {
                    out.remove(i);
                    --i;
                }
            }
        }

        synchronized(this) {
            numSenders--;
            notifyAll();
        }

        if (savedException != null) {
            if (! saveException) {
                SplitterException e = savedException;
                savedException = null;
                throw e;
            }
        }

    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (out.size() > 0) {
            bytesWritten += len * out.size();
            synchronized(this) {
                while (numSenders != 0) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
                numSenders++;
            }
            for (int i = 1; i < out.size(); i++) {
                Sender s = new Sender(b, i, off, len);
                runThread(s, "Splitter sender");
            }
            doWrite(b, off, len, 0);
            done();
        }
    }

    public void flush() throws IOException {
        if (out.size() > 0) {
            synchronized(this) {
                while (numSenders != 0) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
                numSenders++;
            }
            for (int i = 1; i < out.size(); i++) {
                Flusher f = new Flusher(i);
                runThread(f, "Splitter flusher");
            }
            doFlush(0);
            done();
        }
    }

    public void close() throws IOException {

        if (out.size() > 0) {
            synchronized(this) {
                while (numSenders != 0) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
                numSenders++;
            }

            for (int i = 1; i < out.size(); i++) {
                Closer f = new Closer(i);
                runThread(f, "Splitter closer");
            }
            doClose(0);
            done();
        }
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    public void resetBytesWritten() {
        bytesWritten = 0;
    }

    public SplitterException getExceptions() {
        SplitterException e = savedException;
        savedException = null;
        return e;
    }

    private void runThread(Runnable r, String name) {
        synchronized(this) {
            while (numSenders >= MAXTHREADS) {
                try {
                    wait();
                } catch(Exception e) {
                    // Ignored
                }
            }
            numSenders++;
        }
        ThreadPool.createNew(r, name);
    }

    private void done() throws IOException {
        synchronized(this) {
            numSenders--;
            while (numSenders != 0) {
                try {
                    wait();
                } catch(Exception e) {
                    // Ignored
                }
            }
            notifyAll();

            if (savedException != null) {
                if (removeOnException) {
                    for (int i = 0; i < out.size(); i++) {
                        if (out.get(i) == null) {
                            out.remove(i);
                            i--;
                        }
                    }
                }

                if (! saveException) {
                    SplitterException e = savedException;
                    savedException = null;
                    throw e;
                }
            }
        }
    }
}
