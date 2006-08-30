/* $Id$ */

package ibis.rmi.server;

import ibis.rmi.RemoteException;

/**
 * An <code>ExportException</code> is thrown if exporting a remote object
 * fails for some reason. An object that extends
 * {@link ibis.rmi.server.UnicastRemoteObject} is automatically exported.
 * Any <code>Remote</code> object can be exported by using the
 * {@link ibis.rmi.server.UnicastRemoteObject#exportObject} method.
 */
public class ExportException extends RemoteException {
    /**
     * Constructs an <code>ExportException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public ExportException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>ExportException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param e the nested exception
     */
    public ExportException(String s, Exception e) {
        super(s, e);
    }
}