/* $Id$ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Signals a failure to connect to one or more ReceivePorts. Besides 
 * failed connections, also has a list of succeeded connection attempts.
 */
public class ConnectionsFailedException extends IbisIOException {
    
    private static final long serialVersionUID = 1L;
    
    private ArrayList<ConnectionFailedException> failures
            = new ArrayList<ConnectionFailedException>();

    private ReceivePortIdentifier[] obtainedConnections;

    /**
     * Constructs a <code>ConnectionsFailedException</code> with
     * the specified detail message.
     *
     * @param detailMessage
     *          the detail message
     */
    public ConnectionsFailedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>ConnectionsFailedException</code> with
     * <code>null</code> as its error detail message.
     */
    public ConnectionsFailedException() {
        super();
    }

    /**
     * Adds a failed connection attempt.
     * 
     * @param exception
     *          the connection failure exception.
     */
    public void add(ConnectionFailedException exception) {
        failures.add(exception);
    }

    /**
     * Sets the obtained connections.
     * @param receivePortIdentifiers
     *          the obtained connections.
     */
    public void setObtainedConnections(
            ReceivePortIdentifier[] receivePortIdentifiers) {
        obtainedConnections = receivePortIdentifiers.clone();
    }

    /**
     * Returns the obtained connections.
     * @return
     *          the obtained connections.
     */
    public ReceivePortIdentifier[] getObtainedConnections() {
        return obtainedConnections.clone();
    }

    /**
     * Returns the connection attempts that failed, including the exception that
     * caused the failure.
     * @return
     *          an array with one element for each failure.
     */
    public ConnectionFailedException[] getFailures() {
        return failures.toArray(new ConnectionFailedException[failures.size()]);
    }

    public String toString() {
        if (failures.size() == 0) {
            return super.toString();
        }

        StringBuffer result = new StringBuffer();

        result.append("\n--- START OF CONNECTIONS FAILED EXCEPTION ---\n");
        for (ConnectionFailedException failure : failures) {
            result.append("Connection to <");
            if (failure.receivePortIdentifier() != null) {
                result.append(failure.receivePortIdentifier().toString());
            } else {
                result.append(failure.ibisIdentifier().toString());
                result.append(", ");
                result.append(failure.receivePortName());
            }
            result.append("> failed: ");
            result.append(failure.getMessage());
            result.append("\n");
            Throwable throwable = failure.getCause();
            if (throwable != null) {
                result.append(throwable.getClass().getName());
                result.append(": ");
                String message = throwable.getMessage();
                if (message == null) {
                    message = throwable.toString();
                }
                result.append(message);
                result.append("\n");
            }
        }
        result.append("--- END OF CONNECTIONS FAILED EXCEPTION ---\n");
        return result.toString();
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream printStream) {
        if (failures.size() == 0) {
            super.printStackTrace(printStream);
            return;
        }

        printStream.println(
                "--- START OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");

        for (ConnectionFailedException failure : failures) {
            if (failure.receivePortIdentifier() != null) {
                printStream.println("Connection to <"
                        + failure.receivePortIdentifier() + "> failed: ");
            } else {
                printStream.println("Connection to <"
                        + failure.ibisIdentifier() + ", "
                        + failure.receivePortName() + "> failed: ");
            }
            printStream.println(failure.getMessage());
            failure.printStackTrace(printStream);
        }
        printStream.println(
                "--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter printWriter) {
        if (failures.size() == 0) {
            super.printStackTrace(printWriter);
            return;
        }

        printWriter.println(
                "--- START OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
        for (ConnectionFailedException failure : failures) {
            if (failure.receivePortIdentifier() != null) {
                printWriter.println("Connection to <"
                        + failure.receivePortIdentifier() + "> failed: ");
            } else {
                printWriter.println("Connection to <"
                        + failure.ibisIdentifier() + ", "
                        + failure.receivePortName() + "> failed: ");
            }
            printWriter.println(failure.getMessage());
            failure.printStackTrace();
        }
        printWriter.println(
                "--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }
}
