package ibis.satin;

/** The base class of all double-ended queue implementations. */

// No need to delete aborted invocation records, the spawner keeps an
// outstandingJobs list.

public abstract class DEQueue implements Config {

	public abstract InvocationRecord getFromHead();

	public abstract InvocationRecord getFromTail();

	public abstract void addToHead(InvocationRecord o);

	public abstract void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner);
}
