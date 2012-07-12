package ibis.ipl.impl.stacking.cache.manager;

import ibis.ipl.impl.stacking.cache.CacheIbis;
import java.io.IOException;
import java.util.logging.Level;

public class LruCacheManagerImpl extends CacheManagerImpl {

    public LruCacheManagerImpl(CacheIbis ibis) {
        super(ibis);
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


        if (!fromSPLiveConns.isEmpty()) {
            /*
             * Try to get first from the connection from the send port side.
             * Faster to cache.
             */
            Connection con = fromSPLiveConns.remove(0);
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
            Connection con = fromRPLiveConns.remove(0);
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


        if (!fromSPLiveConns.isEmpty()) {
            /*
             * Try to get first from the connection from the send port side.
             * Faster to cache.
             */
            Connection con = fromSPLiveConns.remove(0);
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
            Connection con = fromRPLiveConns.remove(0);
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
