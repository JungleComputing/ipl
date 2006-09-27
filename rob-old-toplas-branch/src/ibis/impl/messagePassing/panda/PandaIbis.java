/* $Id$ */

package ibis.impl.messagePassing.panda;

import ibis.ipl.IbisException;

import java.io.IOException;

/**
 * Ibis implementation on top of native Panda layer
 */
public class PandaIbis extends ibis.impl.messagePassing.Ibis {

    public PandaIbis() throws IbisException {
        super();
    }

    public boolean broadcastSupported() {
        return true;
    }

    protected void init() throws IbisException, IOException {
        ibis.ipl.Ibis.loadLibrary("ibis_mp_panda");
        super.init();
        // new InterruptCatcher().start();
    }

    private void pandaInit(String[] arg) throws IbisException {
        System.err.println("Cluster gateway: run a stripped "
                + getClass().getName());

        myIbis = this;
        ibmp_init(arg);
        if (myCpu < nrCpus) {
            throw new IbisException(
                    "Use Ibis.main() only for cluster gateways");
        }
        ibmp_start();
        /* The gateway will block in end() until all other participants have
         * left */

        lock();
        try {
            ibmp_end();
        } finally {
            unlock();
        }
        System.exit(0);
    }

    /**
     * Provide a main method. This is the way to start a Panda cluster
     * gateway.
     *
     * We must ensure this does not approach name servers, use world stuff or
     * anything else for a real Ibis.
     * So, we don't call init() but do the Panda stuff ourselves.
     */
    public static void main(String[] arg) {
        ibis.ipl.Ibis.loadLibrary("ibis_mp_panda");
        // System.loadLibrary("ibis_mp");
        try {
            PandaIbis i = new PandaIbis();
            i.pandaInit(arg);
        } catch (IbisException e) {
            System.err.println("Ibis.main: " + e);
        }
    }

}
