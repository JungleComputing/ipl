package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

public class PandaIbis extends Ibis {

    public PandaIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IOException {
	System.loadLibrary("ibis_mp_panda");
	super.init();
	// new InterruptCatcher().start();
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
	System.loadLibrary("ibis_mp_panda");
	// System.loadLibrary("ibis_mp");
	try {
	    Ibis i = new PandaIbis();

	    System.err.println("Cluster gateway: run a stripped " + i.getClass().getName());

	    i.myIbis = i;
	    i.ibmp_init(arg);
	    if (i.myCpu < i.nrCpus) {
		throw new IbisException("Use Ibis.main() only for cluster gateways");
	    }
	    i.ibmp_start();
	    /* The gateway will block in end() until all other participants have
	     * left */
	    i.ibmp_end();
	    System.exit(0);
	} catch (IbisException e) {
	    System.err.println("Ibis.main: " + e);
	}
    }

}
