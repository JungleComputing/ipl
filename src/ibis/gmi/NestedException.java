/* $Id: NestedException.java 5248 2007-03-22 21:39:28Z ceriel $ */

package ibis.gmi;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * This is a container class for a list of exceptions.
 */
public class NestedException extends Exception {

    /** 
     * Generated
     */
    private static final long serialVersionUID = -387342205084916635L;

    private ArrayList<Throwable> throwables = new ArrayList<Throwable>();

    /**
     * Constructs a <code>NestedException</code> with
     * the specified detail message.
     *
     * @param s         the detail message
     */
    public NestedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>NestedException</code> with no specified detail
     * message.
     */
    public NestedException() {
        super();
    }

    /**
     * Constructs a <code>NestedException</code> with no specified detail
     * message, and adds the specified Throwable to the
     * list of exceptions.
     * @param t the exception.
     */
    public NestedException(Throwable t) {
        super();
        add(t);
    }

    /**
     * Adds the specified Throwable to the list of exceptions.
     * @param t the exception.
     */
    public void add(Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = t.getCause();
        }

        throwables.add(t);
    }

    public String toString() {
        String res = "";

        if (throwables.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF NESTED EXCEPTION ---\n";
        for (int i = 0; i < throwables.size(); i++) {
            Throwable t = throwables.get(i);
            res += t.getClass().getName();
            res += ": ";
            String msg = t.getMessage();
            if (msg == null) {
                msg = t.toString();
            }
            res += msg;
            res += "\n";
        }
        res += "--- END OF NESTED EXCEPTION ---\n";
        return res;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s) {
        if (throwables.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF NESTED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            throwables.get(i).printStackTrace(s);
        }
        s.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter s) {
        if (throwables.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF NESTED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            throwables.get(i).printStackTrace(s);
        }
        s.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }
}
