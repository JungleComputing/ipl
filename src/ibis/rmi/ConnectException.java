package ibis.rmi;

/**
 * A <code>ConnectException</code> is thrown if a connection is
 * refused.
 */
public class ConnectException extends RemoteException {

    /**
     * Constructs an <code>ConnectException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public ConnectException(String s) {
	super(s);
    }

    /**
     * Constructs an <code>ConnectException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public ConnectException(String s, Exception ex) {
	super(s, ex);
    }
}
