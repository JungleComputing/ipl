package ibis.satin;

public interface Config {
	
	/* Enable or disable statistics. */
	static final boolean SPAWN_STATS = true;
	static final boolean STEAL_STATS = true;
	static final boolean ABORT_STATS = false;

	/* Enable or disable timings */
	static final boolean STEAL_TIMING = false;
	static final boolean ABORT_TIMING = false;
	static final boolean IDLE_TIMING = false;
	static final boolean POLL_TIMING = false;

	/* The poll frequency in nanoseconds. A frequency of 0 means do not poll. */
	static final long POLL_FREQ = 100*1000000L;
//	static final long POLL_FREQ = 0;

	/* poll Ibis, or poll the satin receiveport */
	static final boolean POLL_RECEIVEPORT = true;

	/* Enable or disable asserts. */
	static final boolean ASSERTS = true;

	/* Enable or disable aborts and inlets. */
	static final boolean ABORTS = true;

	/* Enable or disable an optimization for aborts. */
	static final boolean HANDLE_ABORTS_IN_LATENCY = false;

	/* Enable or disable debug prints. */
	static final boolean COMM_DEBUG  = false;
	static final boolean STEAL_DEBUG = false;
	static final boolean SPAWN_DEBUG = false;
	static final boolean INLET_DEBUG = false;
	static final boolean ABORT_DEBUG = false;
}
