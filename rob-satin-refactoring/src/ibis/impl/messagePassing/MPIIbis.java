/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.IbisException;

import java.io.IOException;

/**
 * Ibis on top of native MPI layer
 */
public class MPIIbis extends Ibis {

    public MPIIbis() throws IbisException {
        super();
    }

    protected void init() throws IbisException, IOException {
        ibis.ipl.Ibis.loadLibrary("ibis_mp_mpi");
        super.init();
    }

}