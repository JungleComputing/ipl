package ibis.satin.impl;

import java.util.HashMap;

/**
 * This class represents a counter of spawning events. Access to its internals
 * is package-protected.
 * Use incr and decr if you want to test. Otherwise you can just use value++
 * and value--.
 */
public final class SpawnCounter {
	int value = 0;

	SpawnCounter next;

	/* For debugging purposes ... */
	HashMap m = null;
	Throwable lastIncr = null;
	Throwable lastDecr = null;
	int lastvalue = 0;

	synchronized void incr(InvocationRecord r) {
	    Throwable e = new Throwable();
	    Throwable x;
	    if (m == null) m = new HashMap();
	    if (value != lastvalue) {
		System.out.println("Incr: lastvalue != value!");
		if (lastIncr != null) {
		    System.out.println("Last increment: ");
		    lastIncr.printStackTrace();
		}
		if (lastDecr != null) {
		    System.out.println("Last decrement: ");
		    lastDecr.printStackTrace();
		}
	    }
	    value++;
	    lastvalue = value;
	    lastIncr = e;
	    x = (Throwable) m.remove(r);
	    if (x != null) {
		System.out.println("Incr: already present from here: ");
		x.printStackTrace();
		System.out.println("Now here: ");
		e.printStackTrace();
	    }
	    m.put(r, e);
	    if (m.size() != value) {
		System.out.println("Incr: hashmap size = " + m.size() +
				    ", value = " + value);
		e.printStackTrace();
	    }
	}

	synchronized void decr(InvocationRecord r) {
	    if (m == null) m = new HashMap();
	    if (value != lastvalue) {
		System.out.println("Decr: lastvalue != value!");
		if (lastIncr != null) {
		    System.out.println("Last increment: ");
		    lastIncr.printStackTrace();
		}
		if (lastDecr != null) {
		    System.out.println("Last decrement: ");
		    lastDecr.printStackTrace();
		}
	    }
	    value--;
	    lastvalue = value;
	    Throwable x;
	    lastDecr = new Throwable();
	    x = (Throwable) m.remove(r);
	    if (x == null) {
		System.out.println("Decr: not present: ");
		lastDecr.printStackTrace();
	    }
	    if (m.size() != value) {
		System.out.println("Decr: hashmap size = " + m.size() +
				    ", value = " + value);
		lastDecr.printStackTrace();
	    }
	}
}
