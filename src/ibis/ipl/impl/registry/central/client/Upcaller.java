package ibis.ipl.impl.registry.central.client;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.registry.central.Event;
import ibis.util.ThreadPool;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

final class Upcaller implements Runnable {

    private static final Logger logger = Logger.getLogger(Upcaller.class);

    private RegistryEventHandler handler;

    private final List<Event> pendingEvents;

    /** Set when registry upcalls are enabled. */
    private boolean registryUpcallerEnabled = false;

    /** Set when processing a registry upcall. */
    private boolean busyUpcaller = false;

    Upcaller(RegistryEventHandler handler) {
        this.handler = handler;

        pendingEvents = new LinkedList<Event>();

        ThreadPool.createNew(this, "upcaller");
    }

    synchronized void enableEvents() {
        registryUpcallerEnabled = true;
        notifyAll();
    }

    synchronized void disableEvents() {
        registryUpcallerEnabled = false;
        while (busyUpcaller) {
            try {
                wait();
            } catch (Exception e) {
                // nothing
            }
        }
    }

    private synchronized void setBusyUpcaller() {
        busyUpcaller = true;
    }

    private synchronized void clearBusyUpcaller() {
        busyUpcaller = false;
        notifyAll();
    }

    private synchronized Event waitForEvent() {
        while (!(registryUpcallerEnabled && !pendingEvents.isEmpty())) {
            try {
                wait();
            } catch (Exception e) {
                // nothing
            }
        }
        return pendingEvents.remove(0);
    }

    synchronized void newEvent(Event event) {
        logger.debug("newEvent: " + event);
        pendingEvents.add(event);
        notifyAll();
    }

    synchronized void stop() {
        pendingEvents.add(null);
        notifyAll();
    }

    public void run() {
        while (true) {

            Event event = waitForEvent();

            if (event == null) {
                // registry stopped
                return;
            }

            logger.debug("doing upcall for event: " + event);

            setBusyUpcaller();

            try {
                IbisIdentifier[] ibisses = event.getIbises();
                switch (event.getType()) {
                case Event.JOIN:
                    for (IbisIdentifier identifier : ibisses) {
                        handler.joined(identifier);
                    }
                    break;
                case Event.LEAVE:
                    for (IbisIdentifier identifier : ibisses) {
                        handler.left(identifier);
                    }
                    break;
                case Event.DIED:
                    for (IbisIdentifier identifier : ibisses) {
                        handler.died(identifier);
                    }
                    break;
                case Event.SIGNAL:
                    handler.gotSignal(event.getDescription(), event.getIbis());
                    break;
                case Event.ELECT:
                    handler.electionResult(event.getDescription(), event
                            .getIbis());
                    break;
                case Event.UN_ELECT:
                    handler.electionResult(event.getDescription(), null);
                    break;
                case Event.POOL_CLOSED:
                	handler.poolClosed();
                    break;
                case Event.POOL_TERMINATED:
                	handler.poolTerminated(event.getIbis());
                    break;
                default:
                    logger.error("unknown event type: " + event.getType());
                }
            } catch (Throwable t) {
                logger.error("error on handling event", t);
            }

            logger.debug("upcall for event " + event + " done");

            clearBusyUpcaller();
        }
    }

}
