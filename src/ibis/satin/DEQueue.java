package ibis.satin;

/** The base class of all double-ended queue implementations. */

// No need to delete aborted invocation records, the spawner keeps an
// outstandingJobs list.

abstract class DEQueue implements Config {

	abstract InvocationRecord getFromHead();
	abstract InvocationRecord getFromTail();
	abstract void addToHead(InvocationRecord o);
	abstract void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner);
	abstract int size();
}
