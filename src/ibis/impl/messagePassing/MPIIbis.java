package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

public class MPIIbis extends Ibis {

    public MPIIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IbisIOException {
	System.loadLibrary("ibis_mp_mpi");
	super.init();
    }

}
