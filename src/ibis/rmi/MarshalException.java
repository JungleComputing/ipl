package ibis.rmi;

/**
 * A <code>MarshalException</code> is thrown if an IOException occurs
 * while marshalling the remote call or return value. The call may
 * or may not have reached the server.
 */
public class MarshalException extends RemoteException {

    /**
     * Constructs an <code>MarshalException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public MarshalException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>MarshalException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public MarshalException(String s, Exception ex) {
        super(s, ex);
    }
}