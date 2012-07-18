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

    public LruCacheManagerImpl(CacheIbis ibis) {
        super(ibis);
    }

    @Override
    protected Connection cacheOneConnectionFor(Connection conn) {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while ((fromSPLiveConns.size() + fromRPLiveConns.size() == 0)
                && (!canceledReservations.contains(conn))
                && super.fullConns()) {
            try {
                Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                        + " waiting for a live connection to be available.");
                super.noLiveConnCondition.await();
                Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
            } catch (InterruptedException ignoreMe) {
            }
        }

        if (canceledReservations.contains(conn)
                || !super.fullConns()) {
            return null;
        }
        
        if (!fromSPLiveConns.isEmpty()) {
            /*
             * Try to get first from the connection from the send port side.
             * Faster to cache.
             */
            Connection con = fromSPLiveConns.get(0);
            con.cache(heKnows);
            fromSPLiveConns.remove(con);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.get(0);
            con.cache(heKnows);
            fromRPLiveConns.remove(con);
            fromRPCacheConns.add(con);
            return con;
        }
    }
}
