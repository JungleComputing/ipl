/* $Id$ */

package ibis.satin.impl.faultTolerance;

import ibis.satin.impl.Satin;
import ibis.satin.impl.loadBalancing.Victim;

class DeleteClusterThread extends Thread {

    private int milis;

    DeleteClusterThread(int time) {
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
        satin.ft.deleteCluster(Victim.clusterOf(satin.ident));
    }

}
