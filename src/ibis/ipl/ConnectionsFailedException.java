/* $Id: ConnectionsFailedException.java 5236 2007-03-21 10:05:37Z jason $ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Signals a failure to connect to one or more ReceivePorts. Besides 
 * failed connections, also has a list of succeeded connection attempts. 
 */
public class ConnectionsFailedException extends java.io.IOException {

    /**
     * Container class for a single failure.
     */
    public static class Failure {
        private final ReceivePortIdentifier rp;
        private final IbisIdentifier id;
        private final String name;
        private final Throwable cause;

        /**
         * Constructs a container for a specific failed attempt to connect
         * to a specific named receiveport at a specific ibis instance.
         * @param identifier the Ibis identifier of the ibis instance.
         * @param name the name of the receive port.
         * @param cause the cause of the failure.
         */
        Failure(IbisIdentifier identifier, String name, Throwable cause) {
            this.id = identifier;
            this.name = name;
            this.cause = cause;
            this.rp = null;
        }

        /**
         * Constructs a container for a specific failed attempt to connect
         * to a specific receiveport.
         * @param rp the receiveport identifier.
         * @param cause the cause of the failure.
         */
        Failure(ReceivePortIdentifier rp, Throwable cause) {
            this.rp = rp;
            this.cause = cause;
            this.id = rp.ibis();
            this.name = rp.name();
        }

        /**
         * Returns the ibis identifier of the ibis instance running the
         * receive port.
         * @return the ibis identifier.
         */
        public IbisIdentifier identifier() {
            return id;
        }

        /**
         * Returns the receiveport identifier of the failed connection attempt.
         * If the connection attempt specified ibis identifiers and names,
         * this call may return <code>null</code>.
         * @return the receiveport identifier, or <code>null</code>.
         */
        public ReceivePortIdentifier receivePortIdentifier() {
            return rp;
        }

        /**
         * Returns the name of the receive port.
         * @return the name.
         */
        public String name() {
            return name;
        }

        /**
         * Returns the cause of the failure.
         * @return the cause.
         */
        public Throwable cause() {
            return cause;
        }
    }

    /** 
     * Generated.
     */
    private static final long serialVersionUID = -387342205084916635L;

    private ArrayList<Failure> failures = new ArrayList<Failure>();

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
     * @param id the ibis identifier identifying the ibis instance to which
     * a connection attempt was done.
     * @param name the name of the receiveport.
     * @param t the exception that was thrown on this connection attempt.
     */
    public void add(IbisIdentifier id, String name, Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = t.getCause();
        }
        failures.add(new Failure(id, name, t));
    }

    /**
     * Adds a failed connection attempt.
     * 
     * @param rp the receiveport identifier to which a connection was attempted.
     * @param t the exception that was thrown on this connection attempt.
     */
    public void add(ReceivePortIdentifier rp, Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = t.getCause();
        }
        failures.add(new Failure(rp, t));
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
    public Failure[] getFailures() {
        return failures.toArray(new Failure[failures.size()]);
    }

    public String toString() {
        String res = "";

        if (failures.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF CONNECTIONS FAILED EXCEPTION ---\n";
        for (int i = 0; i < failures.size(); i++) {
            Failure f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                res += "Connection to <" + f.receivePortIdentifier()
                        + "> failed: ";
            } else {
                res += "Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ";
            }
            Throwable t = f.cause();
            res += t.getClass().getName();
            res += ": ";
            String msg = t.getMessage();
            if (msg == null) {
                msg = t.toString();
            }
            res += msg;
            res += "\n";
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
            Failure f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                s.println("Connection to <" + f.receivePortIdentifier()
                        + "> failed: ");
            } else {
                s.println("Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ");
            }
            f.cause().printStackTrace(s);
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
            Failure f = failures.get(i);
            if (f.receivePortIdentifier() != null) {
                s.println("Connection to <" + f.receivePortIdentifier()
                        + "> failed: ");
            } else {
                s.println("Connection to <" + f.identifier() + ", " + f.name()
                        + "> failed: ");
            }
        }
        s.println("--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }
}
