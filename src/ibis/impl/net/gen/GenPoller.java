/* $Id$ */

package ibis.impl.net.gen;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * Provides a generic multiple network input poller.
 */
public class GenPoller extends NetPoller {

    public GenPoller(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
    }

    public GenPoller(NetPortType pt, NetDriver driver, String context,
            boolean decouplePoller, NetInputUpcall inputUpcall)
            throws IOException {
        super(pt, driver, context, decouplePoller, inputUpcall);
    }

}
