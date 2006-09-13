/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.InvocationRecord;

/** The master-worker distribution algorithm. */

public final class MasterWorker extends LoadBalancingAlgorithm implements
        Config {

    public MasterWorker(Satin s) {
        super(s);
    }

    public InvocationRecord clientIteration() {
        Victim v;

        if (satin.isMaster()) {
            return null;
        }

        synchronized (satin) {
            v = satin.victims.getVictim(satin.getMasterIdent());
        }

        if (v == null) return null; // node might have crashed

        return satin.lb.stealJob(v, true); // blocks at the server side
    }

    public void jobAdded() {
        synchronized (satin) {
            if (!satin.ident.equals(satin.masterIdent)) {
                spawnLogger.fatal("with the master/worker algorithm, "
                    + "work can only be spawned on the master!");
                System.exit(1); // Failed assertion
            }

            satin.notifyAll();
        }
    }

    public void exit() {
        synchronized (satin) {
            satin.notifyAll();
        }
    }
}
