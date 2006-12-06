/* $Id$ */

package ibis.ipl;

import java.io.IOException;

/**
 * A <code>Registry</code> provides methods for storing and retrieving
 * {@link ibis.ipl.ReceivePortIdentifier ReceivePortIdentifier}s or
 * {@link ibis.ipl.IbisIdentifier}s with arbitrary names.
 */
public interface Registry {

    /**
     * Locates the {@link ibis.ipl.ReceivePortIdentifier ReceivePortIdentifier}
     * that has been bound to the specified <code>name</code>.
     * The method blocks until a receiveport with the specified name is found.
     * Receiveports that are created anonymously can never be obtained in this
     * way, unless the user binds them in the registry explicitly.
     *
     * @param name name of the receiveport.
     * @return the identifier.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier lookupReceivePort(String name)
            throws IOException;

    /**
     * Locates the {@link ibis.ipl.ReceivePortIdentifier ReceivePortIdentifier}
     * that has been bound to the specified <code>name</code>.
     * The method blocks until a receiveport with the specified name is found,
     * or the timeout expires.
     * If timeout is 0, the method behaves as if no timeout was given.
     * Receiveports that are created anonymously can never be obtained in this
     * way, unless the user binds them in the registry explicitly.
     *
     * @param name name of the receiveport.
     * @param timeout the timeout, in milliseconds.
     * @return the identifier.
     * @exception ConnectionTimedOutException is thrown when the timeout
     * 	expires.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier lookupReceivePort(String name, long timeout)
            throws IOException;

    /**
     * Locates the {@link ibis.ipl.ReceivePortIdentifier ReceivePortIdentifiers}
     * that have been bound to the specified <code>names</code>.
     * The method blocks until the receiveports with the specified names are
     * found.
     * Receiveports that are created anonymously can never be obtained in this
     * way, unless the user binds them in the registry explicitly.
     *
     * @param names names of the receiveports.
     * @return the identifiers.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier[] lookupReceivePorts(String[] names)
            throws IOException;

    /**
     * Locates the {@link ibis.ipl.ReceivePortIdentifier ReceivePortIdentifiers}
     * that have been bound to the specified <code>names</code>.
     * The method blocks until the receiveport with the specified names are
     * found, or the timeout expires. When not all identifier can be resolved  
     * in the given time, the method either returns an exception (when 
     * allowPartialResults is false) or any results it did find (when 
     * allowPartialResults is true). If timeout is 0, the method behaves as if 
     * no timeout was given.
     * Receiveports that are created anonymously can never be obtained in this
     * way, unless the user binds them in the registry explicitly.
     *
     * @param names names of the receiveports.
     * @param timeout the timeout, in milliseconds.
     * @param allowPartialResults can the method return a partial result  
     * @return the identifiers.
     * @exception ConnectionTimedOutException is thrown when the timeout
     * 	expires.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier[] lookupReceivePorts(String[] names,
            long timeout, boolean allowPartialResults) throws IOException;

    /**
     * Locates the {@link ibis.ipl.IbisIdentifier IbisIdentifier}
     * that has been bound to the specified <code>name</code>.
     * The method blocks until an Ibis with the specified
     * name is found.
     *
     * @param name name of the ibis.
     * @return the identifier
     * @exception ClassNotFoundException is thrown if an IbisIdentifier
     * 	is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier lookupIbis(String name) throws IOException,
            ClassNotFoundException;

    /**
     * Locates the {@link ibis.ipl.IbisIdentifier IbisIdentifier}
     * that has been bound to the specified <code>name</code>.
     * The method blocks until an Ibis with the specified
     * name is found, or the timeout expires.
     * If timeout is 0, the method behaves as if no timeout was given.
     *
     * @param name name of the ibis.
     * @param timeout the timeout, in milliseconds.
     * @return the identifier
     * @exception ClassNotFoundException is thrown if an IbisIdentifier
     * 	is returned whose class is locally unknown.
     * @exception ConnectionTimedOutException is thrown when the timeout
     * 	expires.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier lookupIbis(String name, long timeout)
            throws IOException, ClassNotFoundException;

    /**
     * Returns the list of receiveport identifiers that are registered
     * with this registry, and have the specified ibis as their
     * controlling ibis.
     * @exception ClassNotFoundException is thrown if an any receiveport
     *  identifier is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier[] listReceivePorts(IbisIdentifier ident)
            throws IOException, ClassNotFoundException;

    /**
     * Elects a single candidate from a number of candidates calling this
     * method with a specified election name.
     * Note that this has nothing to do with a real election: it is not like
     * "most votes count". It is more like: "the first one in gets it".
     *
     * When it is detected the winner of the election has left or has died
     * a new winner will automatically be elected and returned on the next 
     * call of this function.
     *
     * @param election the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception ClassNotFoundException is thrown if an Ibis identifier
     * 	is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier elect(String election) throws IOException,
            ClassNotFoundException;

    /**
     * Gets the result of an election, without being a candidate.
     *
     * @param election the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception ClassNotFoundException is thrown if an Ibis identifier
     * 	is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier getElectionResult(String election)
            throws IOException, ClassNotFoundException;

    /**
     * Notifies that an Ibis instance is suspected to be dead.
     *
     * @param ibis the Ibis identifier of the Ibis instance suspected
     *   to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void maybeDead(IbisIdentifier ibis) throws IOException;

    /**
     * Notifies that an Ibis instance must be assumed to be dead.
     *
     * @param ibis the Ibis identifier of the Ibis instance that must
     *   be assumed to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void dead(IbisIdentifier ibis) throws IOException;

    /**
     * Requests some Ibis instances to leave.
     * This results in a {@link ResizeHandler#mustLeave(IbisIdentifier[])} upcall on
     * all Ibis instances in the current run. It is up to the application
     * to react accordingly.
     * @param ibisses the ibisses which are told to leave. Multiple ibisses
     * may be ordered to leave when, for instance, an entire cluster is killed.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void mustLeave(IbisIdentifier[] ibisses) throws IOException;
}
