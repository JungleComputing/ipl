/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.InvocationRecord;

/** The random work-stealing distributed computing algorithm. */

public final class RandomWorkStealing extends LoadBalancingAlgorithm implements
        Config {

    public RandomWorkStealing(Satin s) {
        super(s);
    }

    public InvocationRecord clientIteration() {
        Victim v;

        synchronized (satin) {
            v = satin.victims.getRandomVictim();
            /*
             * Used for fault tolerance; we must know who the current victim is
             * in case it crashes..
             */
            if (v != null) {
                satin.lb.setCurrentVictim(v.getIdent());
            }
        }
        if (v == null) {
            return null; //can happen with open world if nobody joined.
        }

        return satin.lb.stealJob(v, false);
    }
}
