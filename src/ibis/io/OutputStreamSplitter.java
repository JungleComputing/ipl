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

    ArrayList out = new ArrayList();

    public OutputStreamSplitter() {
        // empty constructor
    }

    public OutputStreamSplitter(boolean removeOnException) {
        this();
        this.removeOnException = removeOnException;
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
        SplitterException e = null;
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
                if (e == null) {
                    e = new SplitterException();
                }
                e.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (e != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            throw e;
        }
    }

    public void write(byte[] b) throws IOException {
        SplitterException e = null;
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
                if (e == null) {
                    e = new SplitterException();
                }
                e.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (e != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            throw e;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        SplitterException e = null;
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
                if (e == null) {
                    e = new SplitterException();
                }
                e.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (e != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            throw e;
        }
    }

    public void flush() throws IOException {
        SplitterException e = null;
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
                if (e == null) {
                    e = new SplitterException();
                }
                e.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (e != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            throw e;
        }
    }

    public void close() throws IOException {
        SplitterException e = null;
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
                if (e == null) {
                    e = new SplitterException();
                }
                e.add(((OutputStream) out.get(i)), e2);
                if (removeOnException) {
                    out.remove(i);
                    i--;
                }
            }
        }

        if (e != null) {
            if (DEBUG) {
                System.err.println("splitter throwing exception");
            }
            throw e;
        }
    }
}