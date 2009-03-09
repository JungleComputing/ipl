/* $Id$ */

package ibis.ipl;

/**
 * Describes the upcalls that are generated for the Ibis group management of a
 * pool. At most one of the methods in this interface will be active at any time
 * (they are serialized by ibis). These upcalls must be explicitly enabled, by
 * means of the {@link Registry#enableEvents()} method. The following also
 * holds: <BR>
 * - For any given Ibis identifier, at most one {@link #joined(IbisIdentifier)
 * joined()} call will be generated. <BR>
 * - For any given Ibis identifier, at most one {@link #left(IbisIdentifier)
 * left()} call will be generated. <BR>
 * - An Ibis instance will also receive a {@link #joined(IbisIdentifier)
 * joined()} upcall for itself.
 * <p>
 * If the {@link IbisCapabilities#MEMBERSHIP_TOTALLY_ORDERED} is specified for
 * this Ibis, all Ibis instances receive all {@link #joined(IbisIdentifier)
 * joined()}, {@link #left(IbisIdentifier) left()}, and
 * {@link #died(IbisIdentifier) died()}, upcalls in exactly the same order.
 */
public interface RegistryEventHandler {
    /**
     * Upcall generated when an Ibis instance joined the pool. Note: an Ibis
     * instance may also receive a <code>joined</code> upcall for itself. If the
     * {@link IbisCapabilities#MEMBERSHIP_TOTALLY_ORDERED} is specified for this
     * Ibis, all Ibis instances receive the <code>joined</code> upcalls in the
     * same order. If {@link IbisCapabilities#MEMBERSHIP_UNRELIABLE} is
     * specified, some Ibis instances may be missed, and the order of the
     * upcalls may not be the same.
     * 
     * @param joinedIbis
     *            the ibis identifier of the Ibis instance that joined the pool.
     */
    public void joined(IbisIdentifier joinedIbis);

    /**
     * Upcall generated when an Ibis instance voluntarily left the pool. If the
     * {@link IbisCapabilities#MEMBERSHIP_TOTALLY_ORDERED} is specified for this
     * Ibis, all Ibis instances receive the <code>left</code> upcalls in the
     * same order. If {@link IbisCapabilities#MEMBERSHIP_UNRELIABLE} is
     * specified, some Ibis instances may be missed, and the order of the
     * upcalls may not be the same.
     * 
     * @param leftIbis
     *            the ibis identifier of the Ibis instance that left the pool.
     */
    public void left(IbisIdentifier leftIbis);

    /**
     * Upcall generated when an Ibis instance crashed or was killed, implicitly
     * removing it from the pool. If the
     * {@link IbisCapabilities#MEMBERSHIP_TOTALLY_ORDERED} is specified for this
     * Ibis, all Ibis instances receive the <code>died</code> upcalls in the
     * same order. If {@link IbisCapabilities#MEMBERSHIP_UNRELIABLE} is
     * specified, some Ibis instances may be missed, and the order of the
     * upcalls may not be the same.
     * 
     * @param corpse
     *            the ibis identifier of the dead Ibis instance.
     */
    public void died(IbisIdentifier corpse);

    /**
     * Upcall generated when one or more Ibisses are sent a signal.
     * 
     * This call can only be the result of a
     * {@link Registry#signal(String, IbisIdentifier[])} call. It is always the
     * result of a call by the application. How the receiver of this upcall
     * reacts to this is up to the application.
     * 
     * @experimental
     * 
     * @param signal
     *            the value of the signal supplied by the user.
     */
    public void gotSignal(String signal, IbisIdentifier source);

    /**
     * Upcall generated when a new result for an election is available.
     * 
     * @param electionName
     *            the name of the election.
     * @param winner
     *            the winner of the election. This parameter may be null if the
     *            previous winner of an election died or left.
     */
    public void electionResult(String electionName, IbisIdentifier winner);

    /**
     * Upcall generated when a pool closes.
     */
    public void poolClosed();

    /**
     * Upcall generated when a pool terminates.
     * 
     * @param source
     *            Ibis which terminated the pool.
     * 
     * @experimental
     */
    public void poolTerminated(IbisIdentifier source);

}
