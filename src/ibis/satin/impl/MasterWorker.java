package ibis.satin.impl;

/** The master-worker distribution algorithm. */

final class MasterWorker extends Algorithm {

	MasterWorker(Satin s) {
	    super(s);
	}

	public InvocationRecord clientIteration() {
		InvocationRecord r;
		Victim v;

		if (satin.master) {
			return null;
		}

		synchronized (satin) {
			v = satin.victims.getVictim(satin.masterIdent);
		}

		return satin.stealJob(v, true); // blocks at the server side
	}

	void jobAdded() {
		synchronized (satin) {
			satin.notifyAll();
		}
	}

	public void exit() {
		synchronized (satin) {
			satin.notifyAll();
		}
	}
}
