package ibis.satin;

public interface Config {
	
	/* Enable or disable statistics. */
	static final boolean SPAWN_STATS = true;
	static final boolean STEAL_STATS = true;

	/* Enable or disable asserts. */
	static final boolean ASSERTS = false;

	/* Enable or disable aborts and inlets. */
	static final boolean ABORTS = true;

	
	/* Enable or disable debug prints. */
	static final boolean COMM_DEBUG  = false;
	static final boolean STEAL_DEBUG = false;
	static final boolean SPAWN_DEBUG = false;
	static final boolean INLET_DEBUG = false;
	static final boolean ABORT_DEBUG = false;
}
