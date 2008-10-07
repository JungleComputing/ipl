/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public class SplitterException extends IOException {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 9005051418523286737L;

    private Vector streams = new Vector();

    private Vector exceptions = new Vector();

    public SplitterException() {
        // empty constructor
    }

    public void add(OutputStream s, Exception e) {
        if (streams.contains(s)) {
            System.err.println("AAA, stream was already in splitter exception");
        }

        streams.addElement(s);
        exceptions.addElement(e);
    }

    public int count() {
        return streams.size();
    }

    public OutputStream[] getStreams() {
    	OutputStream[] ret = new OutputStream[streams.size()];
    	for (int i = 0; i < streams.size(); i++) {
    		ret[i] = (OutputStream)streams.elementAt(i);
    	}
        return ret;
    }

    public Exception[] getExceptions() {
    	Exception[] ret = new Exception[exceptions.size()];
    	for (int i = 0; i < exceptions.size(); i++) {
    		ret[i] = (Exception)exceptions.elementAt(i);
    	}
        return ret;
    }

    public OutputStream getStream(int pos) {
        return (OutputStream)streams.elementAt(pos);
    }

    public Exception getException(int pos) {
        return (Exception)exceptions.elementAt(pos);
    }

    public String toString() {
        String res = "got " + streams.size() + " exceptions: ";
        for (int i = 0; i < streams.size(); i++) {
            res += "   " + exceptions.elementAt(i) + "\n";
        }

        return res;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace()
     */
    public void printStackTrace() {
        for (int i = 0; i < streams.size(); i++) {
            System.err.println("Exception: " + exceptions.elementAt(i));
            ((Exception) exceptions.elementAt(i)).printStackTrace();
        }
    }
}
