package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

public class PandaIbis extends ibis.ipl.impl.messagePassing.Ibis {

    public PandaIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IbisIOException {
System.err.println("Gonna load libibis_mp_panda.so");
	System.loadLibrary("ibis_mp_panda");
System.err.println("Loaded libibis_mp_panda.so");
	super.init();
    }

}
