package ibis.satin.impl;

/** The random work-stealing distributed computing algorithm. */

final class RandomWorkStealing extends Algorithm implements Config {

    RandomWorkStealing(Satin s) {
	super(s);
    }

    public InvocationRecord clientIteration() {
	Victim v;

	synchronized (satin) {
	    v = satin.victims.getRandomVictim();
	    /*
	     * Used for fault tolerance we must know who the current victim is
	     * in case it crashes..
	     */
	    if (FAULT_TOLERANCE) {
		if (v != null) {
		    satin.currentVictim = v.ident;
		}
	    }
	}
	if (v == null)
	    return null; //can happen with open world if nobody joined.

	return satin.stealJob(v, false);
    }
}
