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
    public ReceivePortIdentifier lookup(String name)
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
    public ReceivePortIdentifier lookup(String name, long timeout)
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
    public IbisIdentifier locate(String name)
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
    public IbisIdentifier locate(String name, long timeout)
	throws IOException, ClassNotFoundException;

    /**
     * Returns the list of receiveport identifiers that are registered
     * with this registry, and have the specified ibis as their
     * controlling ibis.
     * @exception ClassNotFoundException is thrown if an any receiveport
     *  identifier is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier[] query(IbisIdentifier ident)
	throws IOException, ClassNotFoundException;

    /**
     * Elects a single candidate from a number of candidates calling this
     * method with a specified election name.
     * Note that this has nothing to do with a real election: it is not like
     * "most votes count". It is more like: "the first one in gets it".
     *
     * @param election the name of this election.
     * @param candidate a candidate for this election.
     * @return the object elected.
     * @exception ClassNotFoundException is thrown if an object
     * 	is returned whose class is locally unknown.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public Object elect(String election, Object candidate)
	throws IOException, ClassNotFoundException;

    public Object reelect(String election, Object candidate, Object formerRuler) 
	throws IOException, ClassNotFoundException;		
	
    /**
     * Binds the specified name to the specified identifier.
     * If the name already is bound, an exception is thrown.
     *
     * @param name the name to which a port is being bound.
     * @param port the receiveport identifier to which a binding is made.
     * @exception ConnectionRefusedException is thrown when the specified name
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
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void unbind(String name)
	throws IOException;

    /**
     * Returns an array of strings, each starting with <code>pattern</code>,
     * that have a receiveport identifier bound to it.
     *
     * @param pattern all strings in the result must start with this.
     * @return an array of names starting with <code>pattern</code> that have
     *  a receiveport identifier bound to it.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public String[] list(String pattern)
	throws IOException;
}
