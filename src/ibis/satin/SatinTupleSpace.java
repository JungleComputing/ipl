/* $Id$ */

package ibis.satin;

import ibis.satin.impl.TupleSpace;

import java.io.Serializable;

/**
 * This class implements an immutable global tuple space. A tuple consists of a
 * key (a String) and its associated data (a serializable object). Note that the
 * data is <strong>not </strong> immutable, because the
 * {@link #get(String) get()}method does not make a copy.
 * @deprecated The Satin TupleSpace is deprecated, in favor of shared objects
 * (see {@link SharedObject}).
 */
public class SatinTupleSpace {

    static {
        TupleSpace.initTupleSpace();
    }

    /** Prevent construction. */
    private SatinTupleSpace() {
    	// nothing here
    }

    /**
     * Adds an element with the specified key to the global tuple space. If a
     * tuple with this key already exists, it is overwritten with the new
     * element. The propagation to other processors can take an arbitrary amount
     * of time, but it is guaranteed that after multiple updates by the same
     * processor, eventually all processors will have the latest value.
     * <p>
     * However, if multiple processors update the value of the same key, the
     * value of an updated key can be different on different processors.
     * 
     * @param key
     *            The key of the new tuple.
     * @param data
     *            The data associated with the key.
     */
    public static void add(String key, Serializable data) {
        TupleSpace.addTuple(key, data);
    }

    /**
     * Retrieves an element from the tuple space. If the element is not in the
     * space yet, this operation blocks until the element is inserted.
     * 
     * @param key
     *            the key of the element retrieved.
     * @return the data associated with the key.
     */
    public static Serializable get(String key) {
        return TupleSpace.getTuple(key);
    }

    /**
     * Retrieves an element from the tuple space. If the element is not in the
     * space yet, this operation returns null.
     * 
     * @param key
     *            the key of the element retrieved.
     * @return the data associated with the key.
     */
    public static Serializable peek(String key) {
        return TupleSpace.peekTuple(key);
    }

    /**
     * Removes an element from the tuple space.
     * 
     * @param key
     *            the key of the tuple to be removed.
     */
    public static void remove(String key) {
        TupleSpace.removeTuple(key);
    }
}
