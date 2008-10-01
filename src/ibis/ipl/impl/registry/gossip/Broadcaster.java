package ibis.ipl.impl.registry.gossip;

import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.util.ThreadPool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broadcasts the leave of an ibis
 */
final class Broadcaster implements Runnable {

    private static final int THREADS = 10;

    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);

    private final CommunicationHandler commHandler;

    private List<VirtualSocketAddress> q;

    private int count;

    Broadcaster(CommunicationHandler commHandler,
            VirtualSocketAddress[] addresses) {
        this.commHandler = commHandler;
        
        // Arrays.asList list does not support remove, so do this "trick"
        q = new LinkedList<VirtualSocketAddress>();
        q.addAll(Arrays.asList(addresses));

        // number of jobs remaining
        count = this.q.size();

        int threads = Math.min(THREADS, count);
        for (int i = 0; i < threads; i++) {
            ThreadPool.createNew(this, "broadcaster");
        }
    }

    synchronized VirtualSocketAddress next() {
        if (q.isEmpty()) {
            return null;
        }

        return q.remove(0);
    }

    synchronized void doneJob() {
        count--;

        if (count <= 0) {
            notifyAll();
        }
    }

    synchronized void waitUntilDone() {
        logger.debug("waiting until done, " + count + " remaining");
        while (count > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        logger.debug("done!");
    }

    public void run() {
        while (true) {
            VirtualSocketAddress address = next();

            if (address == null) {
                // done pushing
                return;
            }

            logger.trace("sending leave to " + address);

            commHandler.sendLeave(address);

            doneJob();
        }
    }
}
