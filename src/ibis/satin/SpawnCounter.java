package ibis.satin;

/** A counter of spawning events. */
/* Make this class final, so it can be inlined. */
public final class SpawnCounter {
    public int value = 0;
    SpawnCounter next;
}
