package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

class MPIIbis extends ibis.ipl.impl.messagePassing.Ibis {

    public MPIIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IbisIOException {
	super.init();
	System.loadLibrary("ibis_mp_mpi");
    }

}
