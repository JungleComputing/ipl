package ibis.satin.impl;

/**
 * This class represents a counter of spawning events. Access to its internals
 * is package-protected.
 */
public final class SpawnCounter {
	int value = 0;
	SpawnCounter next;
}