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
    public IbisIdentifier lookupIbis(String name)
	throws IOException, ClassNotFoundException;

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
    public IbisIdentifier elect(String election)
	throws IOException, ClassNotFoundException;

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
     * Binds the specified name to the specified identifier.
     * If the name already is bound, an exception is thrown.
     *
     * @param name the name to which a port is being bound.
     * @param port the receiveport identifier to which a binding is made.
     * @exception BindingException is thrown when the specified name
     * 	is already bound.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void bind(String name, ReceivePortIdentifier port)
	throws IOException;

    /**
     * Rebinds the specified name to the specified identifier.
     * If the name already is bound, the old binding is discarded.
     *
     * @param name the name to which a port is being bound.
     * @param port the receiveport identifier to which a binding is made.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void rebind(String name, ReceivePortIdentifier port)
	throws IOException;

    /**
     * Removes any binding for the specified name.
     * @param name the name for which any binding is to be removed.
     * @exception BindingException is thrown when the specified name was not
     * bound
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void unbind(String name)
	throws IOException;

    /**
     * Returns an array of strings representing names that have a
     * receiveport identifier bound to it and match the given regular
     * expression.
     *
     * @param regex the regular expression to match with.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public String[] listNames(String regex) throws IOException;

    /**
     * Notifies that an Ibis instance is suspected to be dead.
     * Returns <code>true</code> if this suspicion is found to be true.
     *
     * @param ibis the Ibis identifier of the Ibis instance suspected
     *   to be dead.
     * @return <code>true</code> if this Ibis instance indeed seems to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public boolean isDead(IbisIdentifier ibis) throws IOException;

    /**
     * Notifies that an Ibis instance must be assumed to be dead.
     *
     * @param ibis the Ibis identifier of the Ibis instance that must
     *   be assumed to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void dead(IbisIdentifier ibis) throws IOException;
}
