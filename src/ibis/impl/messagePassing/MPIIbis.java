package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

public class MPIIbis extends Ibis {

    public MPIIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IOException {
	System.loadLibrary("ibis_mp_mpi");
	super.init();
    }

}
