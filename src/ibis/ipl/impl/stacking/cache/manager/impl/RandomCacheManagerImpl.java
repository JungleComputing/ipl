package ibis.ipl.impl.stacking.cache.manager.impl;

import ibis.ipl.impl.stacking.cache.CacheIbis;
import ibis.ipl.impl.stacking.cache.manager.CacheManagerImpl;
import ibis.ipl.impl.stacking.cache.manager.Connection;
import java.util.Random;

public class RandomCacheManagerImpl extends CacheManagerImpl {

    /*
     * When caching, the recv port doesn't know we will cache the connection.
     */
    private boolean heKnows = false;
    private Random r;

    public RandomCacheManagerImpl(CacheIbis ibis) {
        super(ibis);
        r = new Random();
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

        int idx = r.nextInt(fromSPLiveConns.size() + fromRPLiveConns.size());
        if (idx < fromSPLiveConns.size()) {
            Connection con = fromSPLiveConns.get(idx);
            con.cache(heKnows);
            fromSPLiveConns.remove(con);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.get(idx - fromSPLiveConns.size());
            con.cache(heKnows);
            fromRPLiveConns.remove(con);
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

        int idx = r.nextInt(fromSPLiveConns.size() + fromRPLiveConns.size());
        if (idx < fromSPLiveConns.size()) {
            Connection con = fromSPLiveConns.get(idx);
            con.cache(heKnows);
            fromSPLiveConns.remove(con);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.get(idx - fromSPLiveConns.size());
            con.cache(heKnows);
            fromRPLiveConns.remove(con);
            fromRPCacheConns.add(con);
            return con;
        }
    }
}
