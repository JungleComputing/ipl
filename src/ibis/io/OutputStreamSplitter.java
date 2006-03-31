/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/** Contract: write to multiple outputstreams.
 when an exception occurs, store it and continue.
 when the data is written to all streams, throw one large exception
 that contains all previous exceptions.
 This way, even when one of the streams dies, the rest will receive the data.
 **/
public final class OutputStreamSplitter extends OutputStream {
    private static final boolean DEBUG = false;

    private boolean removeOnException = false;
    private boolean saveException = false;
    private SplitterException savedException = null;

    ArrayList out = new ArrayList();

    public OutputStreamSplitter() {
        // empty constructor
    }

    public OutputStreamSplitter(boolean removeOnException, boolean saveException) {
        this();
        this.removeOnException = removeOnException;
        this.saveException = saveException;
    }

    public void add(OutputStream s) {
        if (DEBUG) {
            System.err.println("SPLIT: ADDING: " + s);
        }
        out.add(s);
    }

    public void remove(OutputStream s) {
        int i = out.indexOf(s);
        if (i == -1) {
            throw new Error("Removing unknown stream from splitter.");
        }

        out.remove(i);
    }

    public void write(int b) throws IOException {
        if (DEBUG) {
            System.err.println("SPLIT: writing: " + b);
        }

        for (int i = 0; i < out.size(); i++) {
            try {
                ((OutputStream) out.get(i)).write(b);
            } catch (IOException e2) {
                if (DEBUG) {
                    System.err.println("splitter got exception");
                }
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (savedException != null) {
            if (! saveException) {
                if (DEBUG) {
                    System.err.println("splitter throwing exception");
                }
                SplitterException e = savedException;
                savedException = null;
                throw e;
            }
        }
    }

    public void write(byte[] b) throws IOException {
        if (DEBUG) {
            System.err.println("SPLIT: writing: " + b + ", b.lenth = "
                    + b.length);
        }

        for (int i = 0; i < out.size(); i++) {
            try {
                ((OutputStream) out.get(i)).write(b);
            } catch (IOException e2) {
                if (DEBUG) {
                    System.err.println("splitter got exception");
                }
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (savedException != null) {
            if (! saveException) {
                if (DEBUG) {
                    System.err.println("splitter throwing exception");
                }
                SplitterException e = savedException;
                savedException = null;
                throw e;
            }
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("SPLIT: writing: " + b + ", off = " + off
                    + ", len = " + len);
        }

        for (int i = 0; i < out.size(); i++) {
            try {
                ((OutputStream) out.get(i)).write(b, off, len);
            } catch (IOException e2) {
                if (DEBUG) {
                    System.err.println("splitter got exception");
                }
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (savedException != null) {
            if (! saveException) {
                if (DEBUG) {
                    System.err.println("splitter throwing exception");
                }
                SplitterException e = savedException;
                savedException = null;
                throw e;
            }
        }
    }

    public void flush() throws IOException {
        if (DEBUG) {
            System.err.println("SPLIT: flush");
        }

        for (int i = 0; i < out.size(); i++) {
            try {
                ((OutputStream) out.get(i)).flush();
            } catch (IOException e2) {
                if (DEBUG) {
                    System.err.println("splitter got exception");
                }
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (savedException != null) {
            if (! saveException) {
                if (DEBUG) {
                    System.err.println("splitter throwing exception");
                }
                SplitterException e = savedException;
                savedException = null;
                throw e;
            }
        }
    }

    public void close() throws IOException {
        if (DEBUG) {
            System.err.println("SPLIT: close");
        }

        for (int i = 0; i < out.size(); i++) {
            try {
                ((OutputStream) out.get(i)).close();
            } catch (IOException e2) {
                if (DEBUG) {
                    System.err.println("splitter got exception");
                }
                if (savedException == null) {
                    savedException = new SplitterException();
                }
                savedException.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (savedException != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            SplitterException e = savedException;
            savedException = null;
            throw e;
        }
    }

    public SplitterException getExceptions() {
        SplitterException e = savedException;
        savedException = null;
        return e;
    }
}
