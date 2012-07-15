package ibis.ipl.impl.stacking.cache.manager.impl;

import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.manager.CacheManagerImpl;
import ibis.ipl.impl.stacking.cache.manager.Connection;

public class LruCacheManagerImpl extends CacheManagerImpl {
    
    /*
     * When caching, the recv port doesn't know we will cache the connection.
     */
    private boolean heKnows = false;

    public LruCacheManagerImpl(CacheIbis ibis) {
        super(ibis);
    }

    @Override
    protected Connection cacheOneConnection() {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while (fromSPLiveConns.size() + fromRPLiveConns.size() == 0) {
            try {
                super.noLiveConnCondition.await();
            } catch (InterruptedException ignoreMe) {
            }
        }


        if (!fromSPLiveConns.isEmpty()) {
            /*
             * Try to get first from the connection from the send port side.
             * Faster to cache.
             */
            Connection con = fromSPLiveConns.remove(0);
            con.cache(heKnows);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(0);
            con.cache(heKnows);
            fromRPCacheConns.add(con);
            return con;
        }
    }

    @Override
    protected Connection cacheOneConnectionFor(Connection conn) {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while ((fromSPLiveConns.size() + fromRPLiveConns.size() == 0)
                && (!canceledReservations.contains(conn))) {
            try {
                super.noLiveConnCondition.await();
            } catch (InterruptedException ignoreMe) {
            }
        }

        if (canceledReservations.contains(conn)) {
            return null;
        }


        if (!fromSPLiveConns.isEmpty()) {
            /*
             * Try to get first from the connection from the send port side.
             * Faster to cache.
             */
            Connection con = fromSPLiveConns.remove(0);
            con.cache(heKnows);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(0);
            con.cache(heKnows);
            fromRPCacheConns.add(con);
            return con;
        }
    }
}
