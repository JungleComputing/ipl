package ibis.rmi;

/**
 * An <code>UnmarshalException</code> is thrown when an unmarshalling error
 * occurs.
 * It is thrown on one of the following conditions:
 * <ul>
 * <li> an exception occurs while unmarshalling the call header
 * <li> an <code>java.io.IOException</code> or
 * <code>java.lang.ClassNotFoundException</code> occurs while unmarshalling
 * parameters or unmarshalling the return value
 * <li> the method identification is wrong (server side)
 * <li> a remote reference object for an unmarshalled stub cannot be
 * created
 * </ul>
 */
public class UnmarshalException extends RemoteException {

    /**
     * Constructs an <code>UnmarshalException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public UnmarshalException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>UnmarshalException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public UnmarshalException(String s, Exception ex) {
        super(s, ex);
    }
}