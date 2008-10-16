package ibis.ipl.impl.registry.central.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.util.ThreadPool;

public class Heartbeat implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(Heartbeat.class);

    private final Pool pool;
    private final CommunicationHandler commHandler;
    
    private final long heartbeatInterval;

    private long heartbeatDeadline;
    
    Heartbeat(CommunicationHandler commHandler, Pool pool, long heartbeatInterval) {
        this.commHandler = commHandler;
        this.pool = pool;
        this.heartbeatInterval = heartbeatInterval;
        
        
        ThreadPool.createNew(this, "heartbeat thread");
    }

    
    synchronized void updateHeartbeatDeadline() {
        heartbeatDeadline = System.currentTimeMillis()
                + (long) (heartbeatInterval * 0.9 * Math.random());

        logger.debug("heartbeat deadline updated");

        // no need to wake up heartbeat thread, deadline will only be later
    }
    
    synchronized void nudge() {
        notifyAll();
    }

    synchronized void waitForHeartbeatDeadline() {
        while (true) {
            int timeout = (int) (heartbeatDeadline - System.currentTimeMillis());

            if (timeout <= 0) {
                return;
            }

            try {
                logger.debug("waiting " + timeout + " for heartbeat");
                wait(timeout);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }
    
    public void run() {
        while (!pool.isStopped()) {
            waitForHeartbeatDeadline();

            commHandler.sendHeartBeat();
            //update deadline, if we succeeded or not...
            updateHeartbeatDeadline();
        }
    }


}
