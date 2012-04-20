package ibis.ipl.registry.central.client;

import ibis.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heartbeat implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(Heartbeat.class);

    private final Pool pool;
    private final CommunicationHandler commHandler;

    private final long heartbeatInterval;

    private final boolean exitOnServerFailure;

    private long heartbeatDeadline;
    
    private long serverFailureDeadline;

    Heartbeat(CommunicationHandler commHandler, Pool pool,
            long heartbeatInterval, boolean exitOnServerFailure) {
        this.commHandler = commHandler;
        this.pool = pool;
        this.heartbeatInterval = heartbeatInterval;
        this.exitOnServerFailure = exitOnServerFailure;

        ThreadPool.createNew(this, "heartbeat thread");
    }

    synchronized void resetServerDeadline() {
        serverFailureDeadline = System.currentTimeMillis()
                + (heartbeatInterval * 5);
    }

    synchronized void resetHeartbeatDeadline() {
        heartbeatDeadline = System.currentTimeMillis()
                + (long) (heartbeatInterval * (0.3 + Math.random()/2.0));
    }

    synchronized void resetDeadlines() {
        resetHeartbeatDeadline();
        resetServerDeadline();

        if (logger.isDebugEnabled()) {
            logger.debug("deadlines reset");
        }

        // no need to wake up heartbeat thread, deadline will only be later
    }
    
    synchronized boolean serverDeadlineExpired() {
        return System.currentTimeMillis() > serverFailureDeadline;
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
        	if (logger.isDebugEnabled()) {
        	    logger.debug("waiting " + timeout + " for heartbeat");
        	}
                wait(timeout);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    public void run() {
        resetDeadlines();
        while (!pool.isStopped()) {
            waitForHeartbeatDeadline();

            boolean success = commHandler.sendHeartBeat();
            if (success) {
                resetServerDeadline();
                resetHeartbeatDeadline();
            } else {
                resetHeartbeatDeadline();
            }

            if (serverDeadlineExpired()) {
                if (exitOnServerFailure) {
                    logger.error("Registry: contact with server lost, terminating JVM");
                    System.exit(1);
                } else {
                    logger.warn("Registry: contact with server lost");
                }
            }
        }
    }

}
