package ibis.satin;

interface Algorithm {
	/**
	 * Called in every iteration of the client loop. It decides which
	 * jobs are run, and what kind(s) of steal requests are done. */
	public void clientIteration();

	/**
	 * This one is called for each steal reply by the MessageHandler, so the
	 * algorithm knows about the reply (this is needed with asynchronous
	 * communication)
	 */
	public void stealReplyHandler(InvocationRecord ir, int opcode);

	/**
	 * This one is called in the exit procedure so the algorithm can clean up,
	 * e.g., wait for pending (async) messages
	 */
	public void exit();

	public void printStats(java.io.PrintStream out);
}
