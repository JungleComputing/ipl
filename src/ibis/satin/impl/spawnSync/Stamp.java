/* $Id$ */

package ibis.satin.impl.spawnSync;

/**
 * Implements globally-unique identifications for Satin jobs.
 */
public final class Stamp implements java.io.Serializable {

    private static boolean ENABLE_CACHE = true;

    /** I found that there actually is no need to start with larger stamps arrays.
     * This is thanks to the order divide-and-conquer jobs are executed.  
     */
    private static final int DEFAULT_STAMPS_SIZE = 1;

    private static Stamp stampCache;

    private transient Stamp cacheNext;

    private int[] stamps;

    private int stampLength;

    private transient int counter = 0;

    private static int rootCounter = 0;

    /**
     * Creates a new unique stamp from the specified parent
     * stamp.
     * @param parentStamp stamp of the spawner, or <code>null</code> if there
     * is no parent.
     */
    public static Stamp createStamp(Stamp parentStamp) {
        if (!ENABLE_CACHE || stampCache == null) {
            return new Stamp(parentStamp);
        }

        // get it from the cache
        Stamp res = stampCache;
        stampCache = stampCache.cacheNext;
        res.init(parentStamp);
        return res;
    }

    public static void deleteStamp(Stamp s) {
        if (!ENABLE_CACHE) return;
        s.cacheNext = stampCache;
        stampCache = s;
    }

    /**
     * Constructor. Creates a new unique stamp from the specified parent
     * stamp.
     * @param parentStamp stamp of the spawner, or <code>null</code> if there
     * is no parent.
     */
    private Stamp(Stamp parentStamp) {
        if (parentStamp == null) {
            stampLength = 1;
            stamps = new int[DEFAULT_STAMPS_SIZE];
            stamps[0] = rootCounter++;
        } else {
            stampLength = parentStamp.stampLength + 1;
            if (stampLength > DEFAULT_STAMPS_SIZE) {
                stamps = new int[stampLength];
            } else {
                stamps = new int[DEFAULT_STAMPS_SIZE];
            }
            System.arraycopy(parentStamp.stamps, 0, stamps, 0, stampLength - 1);
            stamps[stampLength - 1] = parentStamp.counter++;
        }
    }

    private void init(Stamp parentStamp) {
        counter = 0;

        if (parentStamp == null) {
            stampLength = 1;
            stamps[0] = rootCounter++;
        } else {
            stampLength = parentStamp.stampLength + 1;
            if (stamps.length < stampLength) {
                // does not fit, alloc new one
                stamps = new int[stampLength];
            }

            // for loop is slightly faster than arraycopy for these small sizes. 
            for (int i = 0; i < stampLength - 1; i++) {
                stamps[i] = parentStamp.stamps[i];
            }
            stamps[stampLength - 1] = parentStamp.counter++;
        }
    }

    /**
     * Compares two stamps.
     * @param other the stamp to compare to.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public boolean stampEquals(Stamp other) {
        if (other == null) {
            return false;
        }
        if (other.stampLength != stampLength) {
            return false;
        }
        for (int i = 0; i < stampLength; i++) {
            if (stamps[i] != other.stamps[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if this stamp is a descendent of the specified stamp.
     * @param other the stamp to compare to.
     * @return <code>true</code> if this stamp is a descendent.
     */
    public boolean isDescendentOf(Stamp other) {
        if (other == null) {
            return true;
        }
        if (other.stampLength > stampLength) {
            return false;
        }
        for (int i = 0; i < other.stampLength; i++) {
            if (stamps[i] != other.stamps[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Stamp)) {
            return false;
        }
        return stampEquals((Stamp) other);
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0; i < stampLength; i++) {
            h = (h << 4) + stamps[i];
        }
        return h;
    }

    /**
     * Computes a String representation of this Stamp.
     * @return the String representation.
     */
    public String toString() {
        String str = "";
        for (int i = 0; i < stampLength; i++) {
            str = str + stamps[i];
            if (i < stampLength - 1) {
                str += ".";
            }
        }
        return str;
    }
}
