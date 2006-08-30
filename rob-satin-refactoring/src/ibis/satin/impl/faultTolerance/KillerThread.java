/* $Id$ */

package ibis.satin.impl.faultTolerance;

public class KillerThread extends Thread {

    int milis; //wait that long before dying

    String cluster = null; //die only if your are in this cluster

    public KillerThread(int time) {
        super("SatinKillerThread");
        this.milis = time * 1000;
    }

    public void run() {
        try {
            sleep(milis);
        } catch (InterruptedException e) {
            //ignore
        }
        // Satin satin = Satin.this_satin;
        // if (satin.allIbises.indexOf(satin.ident)
        //         >= (satin.allIbises.size() / 2)) {
        System.exit(1); // Kills this satin on purpose, this is a killerthread!
    }

}
