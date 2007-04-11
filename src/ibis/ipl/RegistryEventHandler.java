/* $Id: RegistryEventHandler.java 5028 2007-02-12 11:43:36Z ceriel $ */

package ibis.ipl;

/**
 * Describes the upcalls that are generated for the Ibis group
 * management of a pool.
 * At most one of the methods in this interface will be active at any
 * time (they are serialized by ibis).
 * These upcalls must be explicitly enabled, by means of the
 * {@link ibis.ipl.Ibis#enableRegistryEvents Ibis.enableRegistryEvents()}
 * method.
 * The following also holds:
 * <BR>
 * - For any given Ibis identifier, at most one
 * {@link #joined(IbisIdentifier) joined()} call will be generated.
 * <BR>
 * - For any given Ibis identifier, at most one
 * {@link #left(IbisIdentifier) left()} call will be generated.
 * <BR>
 * - An Ibis instance will also receive a
 *   {@link #joined(IbisIdentifier) joined()} upcall for itself.
 */
public interface RegistryEventHandler {
    /**
     * Upcall generated when an Ibis instance joined the pool.
     * @param ident the ibis identifier of the Ibis instance that joined the
     * pool. Note: an Ibis instance will also receive a
     * <code>joined</code> upcall for itself.
     * All Ibis instances receive the <code>joined</code> upcalls in the
     * same order.
     */
    public void joined(IbisIdentifier ident);

    /**
     * Upcall generated when an Ibis instance voluntarily left the pool.
     * @param ident the ibis identifier of the Ibis instance that left the
     * pool.
     */
    public void left(IbisIdentifier ident);

    /**
     * Upcall generated when an Ibis instance crashed or was killed, implicitly
     * removing it from the pool.
     * @param corpse the ibis identifier of the dead Ibis instance.
     */
    public void died(IbisIdentifier corpse);

    /**
     * Upcall generated when one or more Ibisses are sent a signal.
     *
     * This call can only be the result of a
     * {@link Registry#signal(String, IbisIdentifier[])}
     * call. It is always the result of a call by the application.
     * How the receiver of this upcall reacts to this is up to the application.
     * @param signal the value of the signal supplied by the user.
     */
    public void gotSignal(String signal);
}
