package ibis.satin;

/* Make this class final, so it can be inlined. */
public final class SpawnCounter {
	public volatile int value = 0;
	SpawnCounter next;
}
