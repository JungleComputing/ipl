package ibis.rmi;

/**
 * The <code>AccessException</code> is never actually thrown in Ibis RMI.
 */
public class AccessException extends RemoteException {
    /**
     * Constructs an <code>AccessException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public AccessException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>AccessException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param e the nested exception
     */
    public AccessException(String s, Exception e) {
        super(s, e);
    }
}