package ibis.satin;

/** Constants for the configuration of Satin.
 *  This interface is public because it is also used in code generated
 *  by the Satin frontend.
 */

public interface Config {
	
	/** Enable or disable statistics for spawns. **/
	static final boolean SPAWN_STATS = true;

	/** Enable or disable statistics for job stealing. **/
	static final boolean STEAL_STATS = true;

	/** Enable or disable statistics for aborts. **/
	static final boolean ABORT_STATS = true;

	/** Enable or disable statistics for the tuple space. **/
	static final boolean TUPLE_STATS = true;

	/** Enable or disable steal timings. **/
	static final boolean STEAL_TIMING = true;

	/** Enable or disable abort timings. **/
	static final boolean ABORT_TIMING = true;

	/** Enable or disable idle timing. **/
	static final boolean IDLE_TIMING = false;

	/** Enable or disable poll timing. **/
	static final boolean POLL_TIMING = false;

	/** Enable or disable tuple space timing. **/
	static final boolean TUPLE_TIMING = true;

	/** The poll frequency in nanoseconds.
	 * A frequency of 0 means do not poll.
	 * A frequency smaller than 0 means poll every sync.
	 */
	static final long POLL_FREQ = 0;
//	static final long POLL_FREQ = -1L;
//	static final long POLL_FREQ = 100*1000000L;

	/** When polling, poll the satin receiveport. **/
	static final boolean POLL_RECEIVEPORT = true;

	/** Enable or disable asserts. **/
	static final boolean ASSERTS = false;

	/** Enable or disable aborts and inlets. **/
	static final boolean ABORTS = true;

	/** Enable or disable an optimization for handling delayed messages. */
	static final boolean HANDLE_MESSAGES_IN_LATENCY = false;

	/** Use multicast to update the tuple space */
	static final boolean SUPPORT_TUPLE_MULTICAST = true;

	/** Enable or disable debug prints concerning communication. **/
	static final boolean COMM_DEBUG  = false;

	/** Enable or disable debug prints concerning job stealing. **/
	static final boolean STEAL_DEBUG = false;

	/** Enable or disable debug prints concerning spawns. **/
	static final boolean SPAWN_DEBUG = false;

	/** Enable or disable debug prints concerning inlets
	    (exception handling).
	**/
	static final boolean INLET_DEBUG = false;

	/** Enable or disable debug prints concerning aborts. **/
	static final boolean ABORT_DEBUG = false;

	/** Enable or disable debug prints concerning the tuple space. **/
	static final boolean TUPLE_DEBUG = false;
}
