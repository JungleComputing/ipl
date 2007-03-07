package ibis.impl.registry.central;

import org.apache.log4j.Logger;

import ibis.impl.Ibis;
import ibis.util.ThreadPool;

final class Upcaller implements Runnable {

    private static final Logger logger = Logger.getLogger(Upcaller.class);

    private Ibis ibis;

    private Registry registry;

    public Upcaller(Ibis ibis, Registry registry) {
        this.ibis = ibis;
        this.registry = registry;

        ThreadPool.createNew(this, "upcaller");
    }

    public void run() {
        int eventNr = 0;

        while (true) {
            // wait for and retrieve event from registry
            Event event = registry.waitForEvent(eventNr);
            eventNr++;

            if (event == null) {
                // registry stopped
                return;
            }

            logger.debug("doing upcall for event: " + event);

            try {
                switch (event.getType()) {
                case Event.JOIN:
                    ibis.joined(event.getIbisses());
                    break;
                case Event.LEAVE:
                    ibis.left(event.getIbisses());
                    break;
                case Event.DIED:
                    ibis.died(event.getIbisses());
                    break;
                case Event.MUST_LEAVE:
                    ibis.mustLeave(event.getIbisses());
                    break;
                case Event.ELECT:
                    // Only handled in registry
                    break;
                case Event.UN_ELECT:
                    // Only handled in registry
                    break;
                default:
                    logger.error("unknown event type: " + event.getType());
                }
            } catch (Throwable t) {
                logger.error("error on handling event", t);
            }

        }
    }

}
