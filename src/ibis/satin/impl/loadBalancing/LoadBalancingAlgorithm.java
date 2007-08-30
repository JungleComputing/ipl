/* $Id: LoadBalancingAlgorithm.java 3310 2005-12-08 08:52:02Z ceriel $ */

package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.InvocationRecord;

public abstract class LoadBalancingAlgorithm implements Config {

    protected Satin satin;

    protected LoadBalancingAlgorithm(Satin s) {
        satin = s;
    }

    /**
     * Handler that is called when new work is added to the queue. Default
     * implementation does nothing.
     */
    public void jobAdded() {
        // do nothing
    }

    /**
     * Called in every iteration of the client loop. It decides which jobs are
     * run, and what kind(s) of steal requests are done. returns a job an
     * success, null on failure.
     */
    abstract public InvocationRecord clientIteration();

    /**
     * This one is called for each steal reply by the MessageHandler, so the
     * algorithm knows about the reply (this is needed with asynchronous
     * communication)
     */
    public void stealReplyHandler(InvocationRecord ir, IbisIdentifier sender, int opcode) {
        satin.lb.gotJobResult(ir, sender);
    }

    /**
     * This one is called in the exit procedure so the algorithm can clean up,
     * e.g., wait for pending (async) messages Default implementation does
     * nothing.
     */
    public void exit() {
        synchronized (satin) {
            satin.notifyAll();
        }
    }

    public void handleCrash(IbisIdentifier ident) {
        // by default, do nothing
    }
    
    protected void throttle(long count) {
        if(!THROTTLE_STEALS) {
            return;
        }
        int maxTime = 500;
        int time = 1;
        if (count >= 9) {
            time = maxTime;
        } else {
            time <<= count;
        }

        satin.stats.stealThrottleTimer.start();
        
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // ignore
        }

        satin.stats.stealThrottleTimer.stop();
    }
}
