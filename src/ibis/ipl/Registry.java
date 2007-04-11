/* $Id$ */

package ibis.ipl;

import java.io.IOException;

/**
 * A <code>Registry</code> coordinates the Pool which an Ibis instance is part of.
 */
public interface Registry {

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
     * @param electionName the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier elect(String electionName) throws IOException;

    /**
     * Gets the result of an election, without being a candidate.
     *
     * @param electionName the name of this election.
     * @return the Ibis identifier of the elected Ibis instance.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public IbisIdentifier getElectionResult(String electionName) throws IOException;

    /**
     * Should be called when an application suspects that a particular
     * Ibis instance is dead. The registry may react by checking this.
     *
     * @param ibisIdentifier the Ibis identifier of the Ibis instance suspected
     *   to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void maybeDead(IbisIdentifier ibisIdentifier) throws IOException;

    /**
     * Instructs the registry to assume that the specified Ibis instance
     * is dead.
     *
     * @param ibisIdentifier the Ibis identifier of the Ibis instance that must
     *   be assumed to be dead.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void assumeDead(IbisIdentifier ibisIdentifier) throws IOException;

    /**
     * Send a signal to one or more Ibisses.
     * This results in a {@link RegistryEventHandler#gotSignal(String)}
     * upcall on all Ibis instances in the given list. It is up to the
     * application to react accordingly.
     * @param signal the value of the signal. Useful if more than one
     * type of signal is needed.
     * @param ibisIdentifiers the ibisses to wich the signal is sent.
     * @exception java.io.IOException is thrown in case of trouble.
     */
    public void signal(String signal, IbisIdentifier... ibisIdentifiers)
            throws IOException;
}
