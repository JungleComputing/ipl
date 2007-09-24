/* $Id$ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Signals that no Ibis could be created.
 * <code>IbisCreationFailedException</code> is thrown to indicate
 * that no matching Ibis could be found in
 * {@link ibis.ipl.IbisFactory#createIbis(IbisCapabilities,
 * java.util.Properties, boolean, RegistryEventHandler, PortType...)
 * Ibis.createIbis}.
 */
public class IbisCreationFailedException extends Exception {
    private static final long serialVersionUID = 1L;

    private ArrayList<Throwable> throwables = new ArrayList<Throwable>();

    private ArrayList<String> throwerIDs = new ArrayList<String>();

    /**
     * Constructs a <code>IbisCreationFailedException</code> with
     * the specified detail message.
     *
     * @param detailMessage
     *          the detail message
     */
    public IbisCreationFailedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with no specified
     * detail message.
     */
    public IbisCreationFailedException() {
        super();
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with no specified
     * detail message, and adds the specified <String, Throwable> pair to the
     * list of exceptions.
     * @param throwerID
     *          some identification of the exception thrower.
     * @param throwable
     *          the exception.
     */
    public IbisCreationFailedException(String throwerID, Throwable throwable) {
        super();
        add(throwerID, throwable);
    }

    /**
     * Adds the specified <String, Throwable> pair to the list of exceptions.
     * @param throwerID
     *          some identification of the exception thrower.
     * @param throwable
     *          the exception.
     */
    public void add(String throwerID, Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            throwable = throwable.getCause();
        }

        throwables.add(throwable);
        throwerIDs.add(throwerID);
    }

    public String toString() {
        if (throwables.size() == 0) {
            return super.toString();
        }

        StringBuffer result = new StringBuffer();

        result.append("\n--- START OF NESTED EXCEPTION ---\n");
        for (int i = 0; i < throwables.size(); i++) {
            if (throwerIDs.get(i) != null) {
                result.append("*** ");
                result.append(throwerIDs.get(i));
                result.append(" failed because of: ");
            }
            Throwable throwable = throwables.get(i);
            result.append(throwable.getClass().getName());
            result.append(": ");
            String message = throwable.getMessage();
            if (message == null) {
                message = throwable.toString();
            }
            result.append(message);
            result.append("\n");
        }
        result.append("--- END OF NESTED EXCEPTION ---\n");
        return result.toString();
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream printStream) {
        if (throwables.size() == 0) {
            super.printStackTrace(printStream);
            return;
        }

        printStream.println("--- START OF NESTED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            if (throwerIDs.get(i) != null) {
                printStream.println("*** stack trace of " + throwerIDs.get(i));
            }
            throwables.get(i).printStackTrace(printStream);
        }
        printStream.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter printWriter) {
        if (throwables.size() == 0) {
            super.printStackTrace(printWriter);
            return;
        }

        printWriter.println("--- START OF NESTED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            if (throwerIDs.get(i) != null) {
                printWriter.println("*** stack trace of " + throwerIDs.get(i));
            }
            throwables.get(i).printStackTrace(printWriter);
        }
        printWriter.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }
}
