package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class RandomCacheManager extends CacheManager {

    /*
     * This list will contain all alive connections.
     */
    private final Set<Connection> set;
    /*
     * My random generator.
     */
    private final Random r;

    RandomCacheManager(CacheIbis ibis) {
        super(ibis);
        set = new HashSet<Connection>();
        r = new Random();
    }

    @Override
    protected int cacheAtLeastNConnExcept(int n, Object spiOrRpi) {
        int idx, counter, retVal = 0;
        Connection[] array = (Connection[]) set.toArray();

        while (n-- > 0) {
            // get a random index, and try to cache
            // what's stopping us is that the connections may contain the spi
            // so we fastforward in the list from the idx until we have a 
            // connection without the spi.
            idx = r.nextInt(array.length);
            counter = 0;

            while (counter < array.length &&
                    (array[idx] == null || array[idx].contains(spiOrRpi)) ) {
                idx = (idx + 1) % array.length;
                counter++;
            }

            if (counter == array.length) {
                /*
                 * not good. The list only has connections with the given spi.
                 * How can i cache a connection which is does not contain the
                 * send port with the respective spi, if all my alive
                 * connections are from the mentioned sendport.
                 */
                String s = "";
                for(int i = 0; i < array.length; i++) {
                    s+="("+array[i].toString()+"), ";
                }
                throw new RuntimeException("You ask too much. "
                        + "Array of live connections looks like this:\n"+s);
            }

            Connection conn = array[idx];
            if (conn.atSendPortSide) {
                super.removeConnection(conn.spi, conn.rpi);
            } else {
                super.removeConnection(conn.rpi, conn.spi);
            }
            array[idx] = null;
            retVal++;
        }

        return retVal;
    }

    @Override
    protected void addConnectionsImpl(SendPortIdentifier spi, ReceivePortIdentifier[] rpis) {
        for (ReceivePortIdentifier rpi : rpis) {
            set.add(new Connection(spi, rpi));
        }
    }

    @Override
    protected void removeConnectionImpl(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        set.remove(new Connection(spi, rpi));
    }

    @Override
    protected int removeAllConnectionsImpl(SendPortIdentifier spi) {
        int retVal = 0;
        for (Iterator it = set.iterator(); it.hasNext();) {
            Connection conn = (Connection) it.next();
            if (conn.contains(spi)) {
                it.remove();
                retVal++;
            }
        }
        return retVal;
    }

    @Override
    protected void addConnectionImpl(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        set.add(new Connection(rpi, spi));
    }

    @Override
    protected void removeConnectionImpl(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        set.remove(new Connection(rpi, spi));
    }
}
