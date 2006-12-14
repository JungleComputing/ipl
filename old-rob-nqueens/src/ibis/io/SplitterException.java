/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class SplitterException extends IOException {

    private ArrayList streams = new ArrayList();

    private ArrayList exceptions = new ArrayList();

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

    public OutputStream getStream(int pos) {
        return (OutputStream) streams.get(pos);
    }

    public Exception getException(int pos) {
        return (Exception) exceptions.get(pos);
    }

    public String toString() {
        String res = "got " + streams.size() + " exceptions: ";
        for (int i = 0; i < streams.size(); i++) {
            res += "   " + exceptions.get(i) + "\n";
        }

        return res;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace()
     */
    public void printStackTrace() {
        for (int i = 0; i < streams.size(); i++) {
            System.err.println("Exception: " + exceptions.get(i));
            ((Exception) exceptions.get(i)).printStackTrace();
        }
    }
}