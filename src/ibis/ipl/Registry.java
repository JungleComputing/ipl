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
