/* $Id$ */

package ibis.ipl;

import java.io.IOException;

/**
 * A <code>Registry</code> coordinates the Pool which an Ibis instance is part
 * of.
 */
public interface Registry extends Manageable {

    /**
     * Elects a single candidate from a number of candidates calling this method
     * with a specified election name. Note that this has nothing to do with a
     * real election: it is not like "most votes count". It is more like: "the
     * first one in gets it".
     * 
     * When it is detected the winner of the election has left or has died a new
     * winner will automatically be elected and returned on the next call of
     * this function.
     * 
     * @param electionName
     *            the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier elect(String electionName) throws IOException;

    /**
     * Elects a single candidate from a number of candidates calling this method
     * with a specified election name. Note that this has nothing to do with a
     * real election: it is not like "most votes count". It is more like: "the
     * first one in gets it".
     * 
     * When it is detected the winner of the election has left or has died a new
     * winner will automatically be elected and returned on the next call of
     * this function.
     * 
     * Blocks for at most the specified timeout. If no winner can be determinted
     * by then, <code>null</code> is returned.
     * 
     * @param electionName
     *            the name of this election.
     * @param timeoutMillis
     *            time to wait. 0 means: wait forever.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier elect(String electionName, long timeoutMillis)
            throws IOException;

    /**
     * Gets the result of an election, without being a candidate. Blocks until
     * there is a winner for the election.
     * 
     * @param electionName
     *            the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier getElectionResult(String electionName)
            throws IOException;

    /**
     * Gets the result of an election, without being a candidate. Blocks for at
     * most the specified timeout. If there is no winner by then,
     * <code>null</code> is returned.
     * 
     * @param electionName
     *            the name of this election.
     * @param timeoutMillis
     *            time to wait. 0 means: wait forever, -1 means: return
     *            immediately.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) throws IOException;

    /**
     * Should be called when an application suspects that a particular Ibis
     * instance is dead. The registry may react by checking this.
     * 
     * @param ibisIdentifier
     *            the Ibis identifier of the Ibis instance suspected to be dead.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public void maybeDead(IbisIdentifier ibisIdentifier) throws IOException;

    /**
     * Instructs the registry to assume that the specified Ibis instance is
     * dead.
     * 
     * @param ibisIdentifier
     *            the Ibis identifier of the Ibis instance that must be assumed
     *            to be dead.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public void assumeDead(IbisIdentifier ibisIdentifier) throws IOException;

    /**
     * Send a signal to one or more Ibisses. This results in a
     * {@link RegistryEventHandler#gotSignal(String,IbisIdentifier)} upcall on
     * all Ibis instances in the given list. It is up to the application to
     * react accordingly.
     * 
     * @param signal
     *            the value of the signal. Useful if more than one type of
     *            signal is needed.
     * @param ibisIdentifiers
     *            the ibisses to wich the signal is sent. if this parameter is
     *            null, the signal is broadcasted to all ibisses currently in
     *            the pool.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public void signal(String signal, IbisIdentifier... ibisIdentifiers)
            throws IOException;

    /**
     * Returns the Ibis instances that joined the pool. Returns the changes
     * since the last joinedIbises call, or, if this is the first call, all Ibis
     * instances that joined. This call only works if this Ibis is configured to
     * support registry downcalls. If no Ibis instances joined, an array with 0
     * entries is returned.
     * 
     * @exception IbisConfigurationException
     *                is thrown when the port was not configured to support
     *                membership administration.
     * @return the joined Ibises.
     */
    public IbisIdentifier[] joinedIbises();

    /**
     * Returns the Ibis instances that left the pool. Returns the changes since
     * the last leftIbises call, or, if this is the first call, all Ibis
     * instances that left. This call only works if this Ibis is configured to
     * support registry downcalls. If no Ibis instances left, an array with 0
     * entries is returned.
     * 
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support
     *                membership administration.
     * @return the left Ibises.
     */
    public IbisIdentifier[] leftIbises();

    /**
     * Returns the Ibis instances that died. Returns the changes since the last
     * diedIbises call, or, if this is the first call, all Ibis instances that
     * died. This call only works if this Ibis is configured to support registry
     * downcalls. If no Ibis instances died, an array with 0 entries is
     * returned.
     * 
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support
     *                membership administration.
     * @return the Ibises that died.
     */
    public IbisIdentifier[] diedIbises();

    /**
     * Returns the signals received. Returns the changes since the last
     * receivedSignals call, or, if this is the first call, all signals received
     * so far. This call only works if this Ibis is configured to support
     * registry downcalls. If no signals were received, an array with 0 entries
     * is returned.
     * 
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support signals.
     * @return the received signals.
     */
    public String[] receivedSignals();

    /**
     * When running closed-world, returns the total number of Ibis instances in
     * the pool.
     * 
     * @return the number of Ibis instances
     * @exception NumberFormatException
     *                is thrown when the property
     *                <code>ibis.pool.total_hosts</code> is not defined or
     *                does not represent a number.
     * @exception IbisConfigurationException
     *                is thrown when this is not a closed-world run.
     */
    public int getPoolSize();

    /**
     * When running closed-world, wait for the pool to close. A pool closes
     * after all Ibisses have joined.
     * 
     * @exception IbisConfigurationException
     *                is thrown when this is not a closed-world run, or when
     *                registry events are not enabled yet.
     */
    public void waitUntilPoolClosed();

    /**
     * Returns if this pool has been closed.
     * 
     * @return true if this pool is closed, false if not.
     */
    public boolean isClosed();

    /**
     * Allows reception of
     * {@link ibis.ipl.RegistryEventHandler RegistryEventHandler} upcalls.
     * Registry events are saved until the event handler is enabled, and are
     * then delivered, one by one. Ibis instances are always started with the
     * registry event handler disabled. This method must be called to allow for
     * the reception of registry handler upcalls.
     */
    public void enableEvents();

    /**
     * Disables reception of
     * {@link ibis.ipl.RegistryEventHandler RegistryEventHandler} upcalls.
     * Registry events will be saved until the handler is enabled again, and
     * then be delivered, one by one.
     */
    public void disableEvents();

    /**
     * Obtains a sequence number from the registry. Each sequencer has a name,
     * which must be provided to this call.
     * 
     * @param name
     *            the name of this sequencer.
     * @exception IOException
     *                may be thrown when communication with the registry fails.
     */
    public long getSequenceNumber(String name) throws IOException;

    /**
     * Send a termination event to all members of the pool, including this Ibis.
     * Also closes the pool, if it has not been closed yet, stopping any new
     * ibisses from joining the pool. Depending on the registry implementation,
     * there may be a delay between this function being called, and the event
     * arriving at all ibisses (including this one)
     * 
     * @exception IOException
     *                may be thrown when communication with the registry fails.
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support
     *                termination.
     * 
     * 
     */
    public void terminate() throws IOException;

    /**
     * Returns if this pool has been terminated.
     * 
     * @return true if this pool has terminated, false if not.
     * 
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support
     *                termination.
     */
    public boolean hasTerminated();

    /**
     * 
     * Waits until this pool has been terminated.
     * 
     * @return The ibis which terminated the pool.
     * 
     * @exception IbisConfigurationException
     *                is thrown when ibis was not configured to support
     *                termination.
     * 
     * 
     */
    public IbisIdentifier waitUntilTerminated();

}
