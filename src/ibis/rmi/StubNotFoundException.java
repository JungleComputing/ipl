package ibis.rmi;

import ibis.rmi.RemoteException;

/**
 * A <code>StubNotFoundException</code> is thrown if no valid stub class
 * could be found while exporting a remote object.
 */
public class StubNotFoundException extends RemoteException {

    /**
     * Constructs a <code>StubNotFoundException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public StubNotFoundException(String s) {
	super(s);
    }

    /**
     * Constructs a <code>StubNotFoundException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public StubNotFoundException(String s, Exception ex) {
	super(s, ex);
    }
}
