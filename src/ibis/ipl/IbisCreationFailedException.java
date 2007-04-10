/* $Id$ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Signals that no Ibis could be found whose constructor did not throw
 * an exception.
 * <code>IbisCreationFailedException</code> is thrown to indicate
 * that no matching Ibis could be found in
 * {@link ibis.ipl.IbisFactory#createIbis(IbisCapabilities,
 * java.util.Properties, RegistryEventHandler, PortType...) Ibis.createIbis}.
 */
public class IbisCreationFailedException extends Exception {

    /** 
     * Generated
     */
    private static final long serialVersionUID = -387342205084916635L;

    private ArrayList<Throwable> throwables = new ArrayList<Throwable>();

    private ArrayList<String> throwerIDs = new ArrayList<String>();

    /**
     * Constructs a <code>IbisCreationFailedException</code> with
     * the specified detail message.
     *
     * @param s         the detail message
     */
    public IbisCreationFailedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with no specified detail
     * message.
     */
    public IbisCreationFailedException() {
        super();
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with no specified detail
     * message, and adds the specified <String, Throwable> pair to the
     * list of exceptions.
     * @param throwerID some identification of the exception thrower.
     * @param t the exception.
     */
    public IbisCreationFailedException(String throwerID, Throwable t) {
        super();
        add(throwerID, t);
    }

    /**
     * Adds the specified <String, Throwable> pair to the list of exceptions.
     * @param throwerID some identification of the exception thrower.
     * @param t the exception.
     */
    public void add(String throwerID, Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = t.getCause();
        }

        throwables.add(t);
        throwerIDs.add(throwerID);
    }

    public String toString() {
        String res = "";

        if (throwables.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF NESTED EXCEPTION ---\n";
        for (int i = 0; i < throwables.size(); i++) {
            if (throwerIDs.get(i) != null) {
                res += "*** " + throwerIDs.get(i)
                    + " failed because of: ";
            }
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
            if (throwerIDs.get(i) != null) {
                s.println("*** stack trace of " + throwerIDs.get(i));
            }
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
            if (throwerIDs.get(i) != null) {
                s.println("*** stack trace of " + throwerIDs.get(i));
            }
            throwables.get(i).printStackTrace(s);
        }
        s.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }
}
