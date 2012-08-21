package ibis.ipl.impl.stacking.cc.manager.impl;

import ibis.ipl.impl.stacking.cc.CCIbis;
import ibis.ipl.impl.stacking.cc.manager.CCManagerImpl;
import ibis.ipl.impl.stacking.cc.manager.Connection;

public class MruCCManagerImpl extends CCManagerImpl {

    /*
     * When caching, the recv port doesn't know we will cache the connection.
     */
    private boolean heKnows = false;

    public MruCCManagerImpl(CCIbis ibis, int maxConns) {
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
                logger.debug("Lock will be released:"
                        + " waiting for: "
                        + "a live connection to be available for caching"
                        + " OR "
                        + "an empty slot"
                        + " OR "
                        + "for a cancelation.");
                super.gotSpaceCondition.await();
                logger.debug("Lock reaquired.");
            } catch (InterruptedException ignoreMe) {
            }
        }

        if (canceledReservations.contains(conn)
                || !super.fullConns()) {
            return null;
        }

        /*
         * Get the last alive connection used.
         */
        Connection con = aliveConns.get(aliveConns.size() - 1);
        con.cache(heKnows);
        aliveConns.remove(con);
        cachedConns.add(con);
        return con;
    }
}