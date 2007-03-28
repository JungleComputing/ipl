package ibis.impl.registry.central;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

final class Upcaller implements Runnable {

    private static final Logger logger = Logger.getLogger(Upcaller.class);

    private RegistryEventHandler handler;
    
    private IbisIdentifier ibisId;

    private Registry registry;

    public Upcaller(RegistryEventHandler handler, IbisIdentifier id, Registry registry) {
        this.handler = handler;
        this.ibisId = id;
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
                    handler.joined(event.getFirstIbis());
                    break;
                case Event.LEAVE:
                    handler.left(event.getFirstIbis());
                    break;
                case Event.DIED:
                    handler.died(event.getFirstIbis());
                    break;
                case Event.SIGNAL:
                    IbisIdentifier[] ibisses = event.getIbisses();
                    for (IbisIdentifier identifier: ibisses) {
                        if (identifier.equals(ibisId)) {
                            handler.gotSignal(event.getDescription());
                        }
                    }
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
