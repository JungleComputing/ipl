/* $Id: ConnectionsFailedException.java 5236 2007-03-21 10:05:37Z jason $ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Signals a failure to connect to one or more ReceivePorts. Besides 
 * failed connections, also has a list of succeeded connection attempts. 
 */
public class ConnectionsFailedException extends java.io.IOException {
    
    private static final long serialVersionUID = 1L;
    
    private ArrayList<ConnectionFailedException> failures = new ArrayList<ConnectionFailedException>();

    private ReceivePortIdentifier[] obtainedConnections;

    /**
     * Constructs a <code>ConnectionsFailedException</code> with
     * the specified detail message.
     *
     * @param s         the detail message
     */
    public ConnectionsFailedException(String s) {
        super(s);
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
     * @param exception the connection failure exception.
     */
    public void add(ConnectionFailedException exception) {
        failures.add(exception);
    }

    /**
     * Sets the obtained connections.
     * @param ports the obtained connections.
     */
    public void setObtainedConnections(ReceivePortIdentifier[] ports) {
        obtainedConnections = ports;
    }

    /**
     * Returns the obtained connections.
     * @return the obtained connections.
     */
    public ReceivePortIdentifier[] getObtainedConnections() {
        return obtainedConnections;
    }

    /**
     * Returns the connection attempts that failed, including the exception that
     * caused the failure.
     * @return an array with one element for each failure.
     */
    public ConnectionFailedException[] getFailures() {
        return failures.toArray(new ConnectionFailedException[failures.size()]);
    }

    public String toString() {
        String res = "";

        if (failures.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF CONNECTIONS FAILED EXCEPTION ---\n";
        for (int i = 0; i < failures.size(); i++) {
            ConnectionFailedException f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                res += "Connection to <" + f.receivePortIdentifier()
                        + "> failed: ";
            } else {
                res += "Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ";
            }
            res += f.getMessage() + "\n";
            Throwable t = f.getCause();
            if (t != null) {
                res += t.getClass().getName();
                res += ": ";
                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.toString();
                }
                res += msg;
                res += "\n";
            }
        }
        res += "--- END OF CONNECTIONS FAILED EXCEPTION ---\n";
        return res;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s) {
        if (failures.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");

        for (int i = 0; i < failures.size(); i++) {
            ConnectionFailedException f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                s.println("Connection to <" + f.receivePortIdentifier()
                        + "> failed: ");
            } else {
                s.println("Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ");
            }
            s.println(f.getMessage());
            f.printStackTrace(s);
        }
        s.println("--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter s) {
        if (failures.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < failures.size(); i++) {
            ConnectionFailedException f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                s.println("Connection to <" + f.receivePortIdentifier()
                        + "> failed: ");
            } else {
                s.println("Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ");
            }
            s.println(f.getMessage());
            f.printStackTrace();
        }
        s.println("--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }
}
