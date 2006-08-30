/* $Id$ */

package ibis.satin.impl.faultTolerance;

import ibis.satin.impl.Satin;

public class DeleteClusterThread extends Thread {

    int milis;

    public DeleteClusterThread(int time) {
        super("SatinDeleteClusterThread");
        this.milis = 1000 * time;
    }

    public void run() {
        try {
            sleep(milis);
        } catch (InterruptedException e) {
            //ignore
        }
        Satin satin = Satin.getSatin();
        satin.ft.deleteCluster(satin.ident.cluster());
    }

}
