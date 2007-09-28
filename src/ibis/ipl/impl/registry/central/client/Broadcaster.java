package ibis.ipl.impl.registry.central.client;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.central.Connection;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.Protocol;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class Broadcaster {

    private static final Logger logger = Logger.getLogger(Broadcaster.class);
    
    private final Pool pool;
    private final CommunicationHandler commHandler;
    
    Broadcaster(CommunicationHandler commHandler, Pool pool) {
        this.commHandler = commHandler;
        this.pool = pool;
        
    }
    
    void eventsReceived(Event[] events) {
//        ArrayList<IbisIdentifier> children;
//
//        logger.debug("received new broadcasted events");
//
//        synchronized (this) {
//            newEventsReceived(events, true);
//
//            // get the children of this Ibis in the broadcast tree
//            children = getChildren();
//
//        }
//
//        String message = identifier + " forwarding to: ";
//        for (IbisIdentifier ibis : children) {
//            message += ibis + " ";
//        }
//        logger.info(message);
//
//        for (IbisIdentifier ibis : children) {
//            forwardEvents(ibis, events);
//        }
    }


}
