/* $Id$ */

package ibis.impl.net;

import java.util.HashMap;
import java.util.Random;

/**
 * Provides a repository to store global objects while reasonably preventing
 * buggy accesses/overwrites from other modules.
 *
 * Objects are stored in a {@link HashMap} objects using 64bit {@link
 * Long} keys. Every method is synchronized. <BR><B>Note:</B>&nbsp;
 * the {@link Random} object used is not safely 'seeded' at object
 * creation time. The seed used is a static attribute
 * initialized at 0 and incremented at each new bank creation to
 * prevent two process-local instances of the bank to generate the same
 * sequences of keys to which the {@link System#currentTimeMillis}
 * value is added in order to (reasonnably) prevent two distributed
 * instances to generate the same sequence of keys. 
 */
public final class NetBank {

    /**
     * The next seed to use.
     */
    static volatile long seed = 0;

    /**
     * The {@link HashMap} used to store objects.
     *
     * <BR><B>Note:</B>&nbsp; the use of an {@link HashMap} as the
     * backing object for the bank implies that <code>null</code>
     * values can be stored in the stack.
     */
    HashMap map = null;

    /**
     * The {@link Random} object used to generate keys.
     */
    Random rnd = null;

    /**
     * The constructor.
     */
    public NetBank() {
        map = new HashMap();
        rnd = new Random((seed++) + System.currentTimeMillis());
    }

    /**
     * Generates a new key and initializes the associated bank account.
     *
     * The corresponding bank 'account' is initialized to <code>null<code>
     *
     * @return the new key.
     */
    public synchronized Long getUniqueKey() {
        Long key = null;
        do {
            key = new Long(rnd.nextLong());
        } while (map.containsKey(key));

        map.put(key, null);

        return key;
    }

    /**
     * Change the contents of the bank 'account'.
     *
     * <BR><B>Note:</B>&nbsp; the account must have been
     * initialized before this method is called.
     *
     * @param key the account key.
     * @param obj the object to store in the account.
     */
    public synchronized void put(Long key, Object obj) {
        if (!map.containsKey(key)) {
            __.abort__("invalid key");
        }

        map.put(key, obj);
    }

    /**
     * Initializes a new account and store an object in it.
     *
     * @param obj the object to store in the new account.
     * @return the key of the new account.
     */
    public synchronized Long put(Object obj) {
        Long key = getUniqueKey();
        map.put(key, obj);
        return key;
    }

    /**
     * Returns the contents of the account corresponding to the key.
     *
     * <BR><B>Note:</B>&nbsp; the account must have been
     * initialized before this method is called.
     *
     * @param key the account key.
     * @return the contents of the account corresponding to the key.
     */
    public synchronized Object get(Long key) {
        if (!map.containsKey(key)) {
            __.abort__("invalid key");
        }

        return map.get(key);
    }

    /**
     * Atomically exchanges the contents of account corresponding
     * to the key with a new value.
     *
     * <BR><B>Note:</B>&nbsp; the account must have been
     * initialized before this method is called.
     *
     * @param key the account key.
     * @param obj the object to put in place of the current account contents.
     * @return the previous contents of the account.
     */
    public synchronized Object exchange(Long key, Object obj) {
        if (!map.containsKey(key)) {
            __.abort__("invalid key");
        }

        Object previousObj = map.get(key);
        map.put(key, obj);

        return previousObj;
    }

    /**
     * Closes a formely opened account.
     *
     * <BR><B>Note:</B>&nbsp; the account must have been
     * initialized before this method is called.
     *
     * <BR><B>Note 2:</B>&nbsp; the account is not valid anymore
     * once this function returns.
     *
     * <BR><B>Note 3:</B>&nbsp; the probability that the key value
     * might get reused later in the life of the process is not 0.
     *
     * @param key the account key.
     * @return the former contents of the account.
     */
    public synchronized Object discardKey(Long key) {
        if (!map.containsKey(key)) {
            __.abort__("invalid key");
        }

        return map.remove(key);
    }

}
