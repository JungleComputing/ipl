package ibis.impl.net.gen;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSplitter;

import java.io.IOException;

/**
 * Provides a generic multiple network output poller.
 */
public final class GenSplitter extends NetSplitter {

    /**
     * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
     * @param driver the driver of this poller.
     * @param context the context.
     */
    public GenSplitter(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
    }

}