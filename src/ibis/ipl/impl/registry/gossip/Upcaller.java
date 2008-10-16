package ibis.ipl.impl.registry.gossip;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.util.ThreadPool;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Upcaller implements Runnable {

    private static final class Event {
        static final int JOIN = 1;
        static final int LEAVE = 2;
        static final int DIED = 3;
        static final int SIGNAL = 4;
        static final int ELECT = 5;
        
        int type;

        IbisIdentifier ibis;

        String string;

        Event(int type, IbisIdentifier ibis, String string) {
            this.type = type;
            this.ibis = ibis;
            this.string = string;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Upcaller.class);

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
                switch (event.type) {
                case Event.JOIN:
                        handler.joined(event.ibis);
                    break;
                case Event.LEAVE:
                        handler.left(event.ibis);
                    break;
                case Event.DIED:
                        handler.died(event.ibis);
                    break;
                case Event.SIGNAL:
                    handler.gotSignal(event.string, event.ibis);
                    break;
                case Event.ELECT:
                    handler.electionResult(event.string, event
                            .ibis);
                    break;
                default:
                    logger.error("unknown event type: " + event.type);
                }
            } catch (Throwable t) {
                logger.error("error on handling event", t);
            }

            logger.debug("upcall for event " + event + " done");

            clearBusyUpcaller();
        }
    }

    synchronized void ibisJoined(IbisIdentifier ibis) {
        pendingEvents.add(new Event(Event.JOIN, ibis, null));
    }

    synchronized void ibisLeft(IbisIdentifier ibis) {
        pendingEvents.add(new Event(Event.LEAVE, ibis, null));
    }

    synchronized void ibisDied(IbisIdentifier ibis) {
        pendingEvents.add(new Event(Event.DIED, ibis, null));
    }

    synchronized void signal(String signal, IbisIdentifier source) {
        pendingEvents.add(new Event(Event.SIGNAL, source, signal));
    }
    
    synchronized void electionResult(String name, IbisIdentifier winner) {
        pendingEvents.add(new Event(Event.ELECT, winner, name));
    }

}
