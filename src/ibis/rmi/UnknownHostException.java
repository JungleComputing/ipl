/* $Id$ */

package ibis.rmi;

/**
 * An <code>UnknownHostException</code> is thrown when the <code>java.net</code>
 * version of same occurs while creating a connection to a remote host.
 */
public class UnknownHostException extends RemoteException {
    /**
     * Constructs an <code>UnknownHostException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public UnknownHostException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>UnknownHostException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param e the nested exception
     */
    public UnknownHostException(String s, Exception e) {
        super(s, e);
    }
}