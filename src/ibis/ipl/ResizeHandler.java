package ibis.ipl;

/**
 * Describes the upcalls that are generated when an Ibis joins or
 * leaves the current run.
 * At most one join/leave will be active at any time (they are
 * serialized by ibis).
 * Join/leave calls are totally ordered. This implies that a join
 * upcall for the current ibis is also generated locally.
 * The following also holds:
 * <BR>
 * - For any given Ibis identifier, the join call will always be 
 *   generated before the leave call. 
 * <BR>
 * - For any given Ibis identifier, only a single join will be generated.
 * <BR>
 * - For any given Ibis identifier, only a single leave will be generated.
 */
public interface ResizeHandler {
    /**
     * Upcall generated when an Ibis joins the current run.
     * @param ident the ibis identifier of the Ibis joining the current run.
     */
    public void join(IbisIdentifier ident);

    /**
     * Upcall generated when an Ibis leaves the current run.
     * @param ident the ibis identifier of the Ibis leaving the current run.
     */
    public void leave(IbisIdentifier ident);
}
