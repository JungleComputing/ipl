package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

class PandaIbis extends ibis.ipl.impl.messagePassing.Ibis {

    public PandaIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IbisIOException {
	super.init();
	System.loadLibrary("ibis_mp_panda");
    }

}
