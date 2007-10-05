package ibis.ipl.impl.registry.central.client;

import org.apache.log4j.Logger;

public class Broadcaster {

    private static final Logger logger = Logger.getLogger(Broadcaster.class);
    
    private final Pool pool;
    private final CommunicationHandler commHandler;
    
    Broadcaster(CommunicationHandler commHandler, Pool pool) {
        this.commHandler = commHandler;
        this.pool = pool;
        
    }

    
    
}
