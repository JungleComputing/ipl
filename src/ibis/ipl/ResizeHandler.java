package ibis.ipl;

public interface ResizeHandler {
	/** This upcall is generated when a Ibis joines the current run.
	    Only one join/leave at a time will be active at one time (they are
	    serialized by ibis).
	    Join/leave calls are totally ordered. This implies that a join
	    upcall for the current ibis is also generated locally.
	    The following also holds:
	    <BR>
	      - For any given ident, the join call will always be 
	        generated before the leave call. 
	    <BR>
	      - For any given ident, only a single join will be generated
	        before a leave call.
	    <BR>
	      - For any given ident, only a single leave will be generated
	        before a join call.

		@@@ must also invoke join on local machine! @@@
	**/
	public void join(IbisIdentifier ident);
	public void leave(IbisIdentifier ident);
}
