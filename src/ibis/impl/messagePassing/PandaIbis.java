package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

public class PandaIbis extends Ibis {

    public PandaIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IbisIOException {
	System.loadLibrary("ibis_mp_panda");
	super.init();
	// new InterruptCatcher().start();
    }

}
