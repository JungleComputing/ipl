package ibis.ipl;

/**
 * Describes the upcalls that are generated for the Ibis group
 * management of a run.
 * At most one of the methods in this interface will be active at any
 * time (they are serialized by ibis).
 * These upcalls must be explicitly enabled, by means of the
 * {@link ibis.ipl.Ibis#enableResizeUpcalls Ibis.enableResizeUpcalls()}
 * method.
 * The following also holds:
 * <BR>
 * - For any given Ibis identifier, at most one
 * {@link #joined(IbisIdentifier) joined()} call will be generated.
 * <BR>
 * - For any given Ibis identifier, at most one
 * {@link #left(IbisIdentifier) left()} call will be generated.
 * <BR>
 * - An Ibis will also receive a {@link #joined(IbisIdentifier) joined()}
 * upcall for itself.
 */
public interface ResizeHandler {
    /**
     * Upcall generated when an Ibis joined the current run.
     * @param ident the ibis identifier of the Ibis that joined the current run.
     */
    public void joined(IbisIdentifier ident);

    /**
     * Upcall generated when an Ibis voluntarily left the current run.
     * @param ident the ibis identifier of the Ibis that left the current run.
     */
    public void left(IbisIdentifier ident);

    /**
     * Upcall generated when an Ibis crashed or was killed, implicitly
     * removing it from the current run.
     * @param ident the ibis identifier of the Ibis leaving the current run.
     */
    public void died(IbisIdentifier ibis);

    /**
     * Upcall generated when one or more Ibisses are ordered to leave.
     *
     * An ibis may be given some time to leave before it is actually
     * forced to quit. A left or died signal will be issued when the node
     * actually leaves the run, depending on if it left in time, or was
     * explicitly killed.
     * @param ibisses the ibisses which are told to leave. Multiple ibisses
     * may be ordered to leave when, for instance, an entire cluster is killed.
     */    
    public void mustLeave(IbisIdentifier[] ibisses);
}
