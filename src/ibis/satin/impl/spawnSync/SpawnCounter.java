/* $Id$ */

package ibis.satin.impl.spawnSync;

import ibis.satin.impl.Satin;

import java.util.HashMap;

/**
 * This class represents a counter of spawning events. Access to its internals
 * is package-protected.
 */
public final class SpawnCounter {
    private static SpawnCounter spawnCounterCache = null;

    private int value = 0;

    private SpawnCounter next;

    /** For debugging purposes ... */
    private HashMap m = null;

    /** For debugging purposes ... */
    private Throwable lastIncr = null;

    /** For debugging purposes ... */
    private Throwable lastDecr = null;

    /** For debugging purposes ... */
    private int lastvalue = 0;

    /**
     * Obtains a new spawn counter. This does not need to be synchronized, only
     * one thread spawns.
     * 
     * @return a new spawn counter.
     */
    static public final SpawnCounter newSpawnCounter() {
        if (spawnCounterCache == null) {
            return new SpawnCounter();
        }

        SpawnCounter res = spawnCounterCache;
        spawnCounterCache = res.next;

        return res;
    }

    /**
     * Makes a spawn counter available for recycling. This does not need to be
     * synchronized, only one thread spawns.
     * 
     * @param s
     *            the spawn counter made available.
     */
    static public final void deleteSpawnCounter(SpawnCounter s) {
        if (Satin.ASSERTS && s.value < 0) {
            Satin.spawnLogger.fatal(
                "deleteSpawnCounter: spawncouner < 0, val =" + s.value,
                new Throwable());
            System.exit(1); // Failed assertion
        }

        // Only put it in the cache if its value is 0.
        // If not, there may be references to it yet.
        if (s.value == 0) {
            s.next = spawnCounterCache;
            spawnCounterCache = s;
        } else {
            System.err.println("EEK, deleteSpawnCounter, while counter > 0");
            new Exception().printStackTrace();
            // we can continue, but I don't know how this can ever happen --Rob
        }
    }

    public void incr(InvocationRecord r) {
        if (Satin.ASSERTS && Satin.spawnLogger.isDebugEnabled()) {
            debugIncr(r);
        } else {
            value++;
        }
    }

    private synchronized void debugIncr(InvocationRecord r) {
        Throwable e = new Throwable();
        Throwable x;
        if (m == null) {
            m = new HashMap();
        }
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
            System.out.println("Incr: hashmap size = " + m.size()
                + ", value = " + value);
            e.printStackTrace();
        }
    }

    public void decr(InvocationRecord r) {
        if (Satin.ASSERTS && Satin.spawnLogger.isDebugEnabled()) {
            decrDebug(r);
        } else {
            value--;
        }
        if (Satin.ASSERTS && value < 0) {
            System.err.println("Just made spawncounter < 0");
            new Exception().printStackTrace();
            System.exit(1); // Failed assertion
        }
    }

    private synchronized void decrDebug(InvocationRecord r) {
        if (m == null) {
            m = new HashMap();
        }
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
            System.out.println("Decr: hashmap size = " + m.size()
                + ", value = " + value);
            lastDecr.printStackTrace();
        }
    }

    public int getValue() {
        return value;
    }
}
