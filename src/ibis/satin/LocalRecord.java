package ibis.satin;

abstract public class LocalRecord {
	public transient LocalRecord next; /* Used to link the records in the cache. */

	abstract public void handleException(int spawnId, Throwable t, InvocationRecord parent);
}
