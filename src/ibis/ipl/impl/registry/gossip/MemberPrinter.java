package ibis.ipl.impl.registry.gossip;

import org.apache.log4j.Logger;

import ibis.util.ThreadPool;

public class MemberPrinter implements Runnable {
    
    public static final int INTERVAL = 10000;
    
    private static final Logger logger = Logger.getLogger(MemberPrinter.class);

    private final Pool pool;
    
    MemberPrinter(Pool pool) {
        this.pool = pool;
        
        ThreadPool.createNew(this, "member printer");
    }
    
    public synchronized void run() {
        while (pool.isAlive()) {
            pool.printMembers();
            
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                //IGNORE
            }
        }
    }


}
