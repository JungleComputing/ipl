package ibis.ipl.impl.stacking.cache.manager.impl;

import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.manager.CacheManagerImpl;
import ibis.ipl.impl.stacking.cache.manager.Connection;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import java.util.logging.Level;

public class LruCacheManagerImpl extends CacheManagerImpl {

    /*
     * When caching, the recv port doesn't know we will cache the connection.
     */
    private boolean heKnows = false;

    public LruCacheManagerImpl(CacheIbis ibis, int maxConns) {
        super(ibis, maxConns);
    }

    @Override
    protected Connection cacheOneConnectionFor(Connection conn) {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while (!canCache()
                && (!canceledReservations.contains(conn))
                && super.fullConns()) {
            try {
                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                        + " waiting for: "
                        + "a live connection to be available for caching"
                        + " OR "
                        + "an empty slot"
                        + " OR "
                        + "for a cancelation.");
                super.gotSpaceCondition.await();
                Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
            } catch (InterruptedException ignoreMe) {
            }
        }

        if (canceledReservations.contains(conn)
                || !super.fullConns()) {
            return null;
        }

        /*
         * Get the least recently used (the first) alive connection.
         */
        Connection con = aliveConns.get(0);
        con.cache(heKnows);
        aliveConns.remove(con);
        cachedConns.add(con);
        return con;
    }
}