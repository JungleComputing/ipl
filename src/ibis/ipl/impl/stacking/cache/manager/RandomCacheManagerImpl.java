package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.impl.stacking.cache.CacheIbis;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

public class RandomCacheManagerImpl extends CacheManagerImpl {

    private Random r;
    public RandomCacheManagerImpl(CacheIbis ibis) {
        super(ibis);
        r = new Random();
    }

    @Override
    protected synchronized Connection cacheOneConnection() {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while (fromSPLiveConns.size() + fromRPLiveConns.size() == 0) {
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }

        
        int idx = r.nextInt(fromSPLiveConns.size() + fromRPLiveConns.size());
        if (idx < fromSPLiveConns.size()) {
            Connection con = fromSPLiveConns.remove(idx);
            try {
                con.cache();
            } catch (IOException ex) {
                CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                        + " to be cached, but still counted."
                        + "\nException occured:\t{1}",
                        new Object[]{con.toString(), ex.toString()});
            }
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(idx - fromSPLiveConns.size());
            try {
                con.cache();
            } catch (IOException ex) {
                CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                        + " to be cached, but still counted."
                        + "\nException occured:\t{1}",
                        new Object[]{con.toString(), ex.toString()});
            }
            fromRPCacheConns.add(con);
            return con;
        }
    }

    @Override
    protected synchronized Connection cacheOneConnectionFor(Connection conn) {
        /*
         * Nothing to cache. Wait until some live connections arive.
         */
        while ((fromSPLiveConns.size() + fromRPLiveConns.size() == 0)
                && (!canceledReservations.contains(conn))){
            try {
                wait();
            } catch (InterruptedException ignoreMe) {
            }
        }
        
        if(canceledReservations.contains(conn)) {
            return null;
        }

        
        int idx = r.nextInt(fromSPLiveConns.size() + fromRPLiveConns.size());
        if (idx < fromSPLiveConns.size()) {
            Connection con = fromSPLiveConns.remove(idx);
            try {
                con.cache();
            } catch (IOException ex) {
                CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                        + " to be cached, but still counted."
                        + "\nException occured:\t{1}",
                        new Object[]{con.toString(), ex.toString()});
            }
            fromSPCacheConns.add(con);
            return con;
        } else {
            /*
             * Get the one from the receive port side.
             */
            Connection con = fromRPLiveConns.remove(idx - fromSPLiveConns.size());
            try {
                con.cache();
            } catch (IOException ex) {
                CacheManager.log.log(Level.SEVERE, "Connection:\t{0} failed"
                        + " to be cached, but still counted."
                        + "\nException occured:\t{1}",
                        new Object[]{con.toString(), ex.toString()});
            }
            fromRPCacheConns.add(con);
            return con;
        }
    }
}
