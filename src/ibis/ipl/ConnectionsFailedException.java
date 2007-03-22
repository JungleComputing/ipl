/* $Id: ConnectionsFailedException.java 5236 2007-03-21 10:05:37Z jason $ */

package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Signals a failure to connect to some of the receive ports specified in
 * a {@link SendPort#connect(Map)}, {@link SendPort#connect(Map, long)},
 * {@link SendPort#connect(ReceivePortIdentifier[])} or
 * {@link SendPort#connect(ReceivePortIdentifier[], long)} call.
 */
public class ConnectionsFailedException extends java.io.IOException {

    /** 
     * Generated.
     */
    private static final long serialVersionUID = -387342205084916635L;

    private ArrayList<Throwable> throwables = new ArrayList<Throwable>();
    private ArrayList<IbisIdentifier> ids = new ArrayList<IbisIdentifier>();
    private ArrayList<String> names = new ArrayList<String>();

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
     * Constructs a <code>ConnectionsFailedException</code> with
     * <code>null</code> as its error detail message, and the specified
     * failed connection (including the exception that caused the failure).
     * 
     * @param id the ibis identifier identifying the ibis instance to which
     * a connection attempt was done.
     * @param name the name of the receiveport.
     * @param t the exception that was thrown on this connection attempt.
     */
    public ConnectionsFailedException(IbisIdentifier id, String name,
            Throwable t) {
        super();
        add(id, name, t);
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
        throwables.add(t);
        ids.add(id);
        names.add(name);
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
     * Returns the number of failed connection attempts.
     * @return the number of failures.
     */
    public int numFailures() {
        return throwables.size();
    }

    /**
     * Returns the ibis identifier of the <code>i</code>-th failure.
     * @param i the failure index.
     * @return the ibis identifier.
     */
    public IbisIdentifier getIdentifier(int i) {
        return ids.get(i);
    }

    /**
     * Returns the receiveport name of the <code>i</code>-th failure.
     * @param i the failure index.
     * @return the name.
     */
    public String getName(int i) {
        return names.get(i);
    }

    /**
     * Returns the cause of the <code>i</code>-th failure.
     * @param i the failure index.
     * @return the cause.
     */
    public Throwable getCause(int i) {
        return throwables.get(i);
    }

    public String toString() {
        String res = "";

        if (throwables.size() == 0) {
            return super.toString();
        }

        res = "\n--- START OF CONNECTIONS FAILED EXCEPTION ---\n";
        for (int i = 0; i < throwables.size(); i++) {
            Throwable t = throwables.get(i);
            res += "Connection to <" + ids.get(i) + ", " + names.get(i)
                    + "> failed: ";
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
        if (throwables.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            s.println("Connection to <" + ids.get(i) + ", " + names.get(i)
                    + "> failed: ");
            throwables.get(i).printStackTrace(s);
        }
        s.println("--- END OF CONNECTIONS FAILED EXCEPTION STACK TRACE ---");
    }

    public void printStackTrace(PrintWriter s) {
        if (throwables.size() == 0) {
            super.printStackTrace(s);
            return;
        }

        s.println("--- START OF NESTED EXCEPTION STACK TRACE ---");
        for (int i = 0; i < throwables.size(); i++) {
            s.println("Connection to <" + ids.get(i) + ", " + names.get(i)
                    + "> failed: ");
            throwables.get(i).printStackTrace(s);
        }
        s.println("--- END OF NESTED EXCEPTION STACK TRACE ---");
    }
}
