/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class SplitterException extends IOException {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 9005051418523286737L;

    // Transient, because OutputStream is not Serializable. (Thanks, Selmar Smit!)
    // Note that some of the methods here are meaningless after serialization.
    private transient ArrayList<OutputStream> streams = new ArrayList<OutputStream>();

    private ArrayList<Exception> exceptions = new ArrayList<Exception>();

    public SplitterException() {
        // empty constructor
    }

    public void add(OutputStream s, Exception e) {
        if (streams.contains(s)) {
            System.err.println("AAA, stream was already in splitter exception");
        }

        streams.add(s);
        exceptions.add(e);
    }

    public int count() {
        return streams.size();
    }

    public OutputStream[] getStreams() {
        return streams.toArray(new OutputStream[0]);
    }

    public Exception[] getExceptions() {
        return exceptions.toArray(new Exception[0]);
    }

    public OutputStream getStream(int pos) {
        return streams.get(pos);
    }

    public Exception getException(int pos) {
        return exceptions.get(pos);
    }

    public String toString() {
        String res = "got " + exceptions.size() + " exceptions: ";
        for (int i = 0; i < exceptions.size(); i++) {
            res += "   " + exceptions.get(i) + "\n";
        }

        return res;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace()
     */
    public void printStackTrace() {
        for (int i = 0; i < exceptions.size(); i++) {
            System.err.println("Exception: " + exceptions.get(i));
            (exceptions.get(i)).printStackTrace();
        }
    }

    public String getMessage() {
        return toString();
    }
}
