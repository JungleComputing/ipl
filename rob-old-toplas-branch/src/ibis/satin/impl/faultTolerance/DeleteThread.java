/* $Id$ */

package ibis.satin.impl.faultTolerance;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Satin;

class DeleteThread extends Thread {

    private int milis;

//    private String cluster = null;

    DeleteThread(int time) {
        super("SatinDeleteThread");
        this.milis = 1000 * time;
    }

    public void run() {
        try {
            sleep(milis);
        } catch (InterruptedException e) {
            //ignore
        }
        Satin satin = Satin.getSatin();
        satin.ft.ftComm.mustLeave(new IbisIdentifier[] { satin.ident });
    }
}
