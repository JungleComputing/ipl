package ibis.satin.impl;

abstract class Algorithm {

    Satin satin;

    protected Algorithm(Satin s) {
	satin = s;
    }

	/**
	 * Handler that is called when new work is added to the queue. Default
	 * implementation does nothing.
	 */
	void jobAdded() {
	}

	/**
	 * Called in every iteration of the client loop. It decides which jobs are
	 * run, and what kind(s) of steal requests are done.
	 * returns a job an success, null on failure.
	 */
	abstract public InvocationRecord clientIteration();

	/**
	 * This one is called for each steal reply by the MessageHandler, so the
	 * algorithm knows about the reply (this is needed with asynchronous
	 * communication)
	 */
    public void stealReplyHandler(InvocationRecord ir, int opcode) {
	satin.gotJobResult(ir);
    }

	/**
	 * This one is called in the exit procedure so the algorithm can clean up,
	 * e.g., wait for pending (async) messages Default implementation does
	 * nothing.
	 */
	public void exit() {
		synchronized (satin) {
			satin.notifyAll();
		}
	}

	/**
	 * This one allows an implementation to print some statistics. Default
	 * implementation does nothing.
	 */
	public void printStats(java.io.PrintStream out) {
	}
}
