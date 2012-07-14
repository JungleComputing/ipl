package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.impl.stacking.cache.CacheIbis;
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
            Connection con = fromSPLiveConns.remove(idx);
            con.cache(heKnows);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(idx - fromSPLiveConns.size());
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

        int idx = r.nextInt(fromSPLiveConns.size() + fromRPLiveConns.size());
        if (idx < fromSPLiveConns.size()) {
            Connection con = fromSPLiveConns.remove(idx);
            con.cache(heKnows);
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(idx - fromSPLiveConns.size());
            con.cache(heKnows);
            fromRPCacheConns.add(con);
            return con;
        }
    }
}
