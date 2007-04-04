/* $Id: CollectedWriteExceptions.java 5236 2007-03-21 10:05:37Z jason $ */

package ibis.ipl.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Collects IOExceptions for multicast output streams.
 */
public class CollectedWriteException extends IOException {

    /** Added. */
    private static final long serialVersionUID = 5494793976122105110L;
    
    private ArrayList<IOException> exceptions = new ArrayList<IOException>();

    /**
     * Constructs a <code>CollectedWriteException</code> with
     * the specified detail message.
     *
     * @param s         the detail message
     */
    public CollectedWriteException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>CollectedWriteException</code> with
     * <code>null</code> as its error detail message.
     */
    public CollectedWriteException() {
        super();
    }

    /**
     * Adds an exception.
     * @param e the exception to be added.
     */
    public void add(IOException e) {
        exceptions.add(e);
    }

    /**
     * Returns the exceptions.
     * @return an array with one element for each exception.
     */
    public IOException[] getExceptions() {
        return exceptions.toArray(new IOException[exceptions.size()]);
    }

    public String toString() {
        String res = "";

        if (exceptions.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF COLLECTED EXCEPTIONS ---\n";
        for (int i = 0; i < exceptions.size(); i++) {
            IOException f = exceptions.get(i);
            String msg = f.getMessage();
            if (msg == null) {
                msg = f.toString();
            }
            res += msg;
            res += "\n";
        }
        res += "--- END OF COLLECTED EXCEPTIONS ---\n";
        return res;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s) {
        if (exceptions.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF COLLECTED EXCEPTIONS STACK TRACE ---");
        for (int i = 0; i < exceptions.size(); i++) {
            IOException f = exceptions.get(i);
            f.printStackTrace(s);
        }
        s.println("--- END OF COLLECTED EXCEPTIONS STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter s) {
        if (exceptions.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF COLLECTED EXCEPTIONS STACK TRACE ---");
        for (int i = 0; i < exceptions.size(); i++) {
            IOException f = exceptions.get(i);
            f.printStackTrace(s);
        }
        s.println("--- END OF COLLECTED EXCEPTIONS STACK TRACE ---");
    }
}
