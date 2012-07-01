package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LruCacheManager extends CacheManager {

    /*
     * This list will contain all alive connections.
     */
    private final List<Connection> list;

    LruCacheManager(CacheIbis ibis) {
        super(ibis);
        list = new LinkedList<Connection>();
    }

    @Override
    protected int cacheAtLeastNConnExcept(int n, Object spiOrRpi) {
        int retVal = 0, counter = 0;

        while (counter < list.size() && n > 0) {
            counter++;
            Connection conn = list.get(0);
            if (conn.contains(spiOrRpi)) {
                continue;
            }
            if (conn.atSendPortSide) {
                super.removeConnection(conn.spi, conn.rpi);
            } else {
                super.removeConnection(conn.rpi, conn.spi);
            }
            retVal++;
            n--;
        }
        if (counter == list.size() || n > 0) {
            String s = "";
            for (int i = 0; i < list.size(); i++) {
                s += "(" + list.get(i).toString() + "), ";
            }
            throw new RuntimeException("You ask too much. "
                    + "Array of live connections looks like this:\n" + s);
        }

        return retVal;
    }

    @Override
    protected void addConnectionsImpl(SendPortIdentifier spi, ReceivePortIdentifier[] rpis) {
        for (ReceivePortIdentifier rpi : rpis) {
            list.add(new Connection(spi, rpi));
        }
    }

    @Override
    protected void removeConnectionImpl(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        list.remove(new Connection(spi, rpi));
    }

    @Override
    protected int removeAllConnectionsImpl(SendPortIdentifier spi) {
        int retVal = 0;
        for (Iterator it = list.iterator(); it.hasNext();) {
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
        list.add(new Connection(rpi, spi));
    }

    @Override
    protected void removeConnectionImpl(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        list.remove(new Connection(rpi, spi));
    }
}
