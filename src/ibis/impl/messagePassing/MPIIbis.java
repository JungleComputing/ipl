package ibis.impl.messagePassing;

import ibis.ipl.IbisException;

import java.io.IOException;

public class MPIIbis extends Ibis {

    public MPIIbis() throws IbisException {
	super();
    }

    protected void init() throws IbisException, IOException {
	ibis.ipl.Ibis.loadLibrary("ibis_mp_mpi");
	super.init();
    }

}
