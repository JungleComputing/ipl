/* $Id$ */

package ibis.satin.impl;

/**
 * Implements globally-unique identifications for Satin jobs.
 */
public final class Stamp implements java.io.Serializable {

    private int[] stamps;
    private transient int counter = 0;
    private static int rootCounter = 0;

    /**
     * Constructor. Creates a new unique stamp from the specified parent
     * stamp.
     * @param parentStamp stamp of the spawner, or <code>null</code> if there
     * is no parent.
     */
    public Stamp(Stamp parentStamp) {
        if (parentStamp == null) {
            stamps = new int[1];
            stamps[0] = rootCounter++;
        } else {
            int len = parentStamp.stamps.length;
            stamps = new int[len + 1];
            System.arraycopy(parentStamp.stamps, 0, stamps, 0, len);
            stamps[len] = parentStamp.counter++;
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
        if (other.stamps.length != stamps.length) {
            return false;
        }
        for (int i = 0; i < stamps.length; i++) {
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
        if (other.stamps.length > stamps.length) {
            return false;
        }
        for (int i = 0; i < other.stamps.length; i++) {
            if (stamps[i] != other.stamps[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object other) {
        if (! (other instanceof Stamp)) {
            return false;
        }
        return stampEquals((Stamp) other);
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0; i < stamps.length; i++) {
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
        for (int i = 0; i < stamps.length; i++) {
            str = str + stamps[i];
            if (i < stamps.length-1) {
                str += ".";
            }
        }
        return str;
    }
}
