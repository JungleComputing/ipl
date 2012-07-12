package ibis.ipl.impl.stacking.cache.manager;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheStatistics {
    
    public final Map<Connection, Integer> timesCached;
//    public final Map<Connection, Long> avgMillisCache;
    public final Map<Connection, Long> createdOn;
    public final Map<Connection, Long> destroyedOn;
    public final Map<Connection, Long> totalAliveTime;
    public final Map<Connection, Long> lastAliveOn;

    public CacheStatistics() {
        timesCached = new HashMap<Connection, Integer>();
//        avgMillisCache = new HashMap<Connection, Long>();
        createdOn = new HashMap<Connection, Long>();
        destroyedOn = new HashMap<Connection, Long>();
        totalAliveTime = new HashMap<Connection, Long>();
        lastAliveOn = new HashMap<Connection, Long>();
    }
    

    public void printStatistics(PrintStream out) {
        out.println(toString());
    }
    
    public void printStatistics(Logger log) {
        log.log(Level.INFO, toString());
    }
    
    @Override
    public String toString() {
        String border = "\n=====================================================";
        StringBuilder s = new StringBuilder();
        s.append(border);
        s.append("\n\t\t\tCaching Statistics:\n\n");
        
        s.append("Connection\t\t\t\tTimes cached\t\t% time alive\n\n");
        
        
        for(Connection con : createdOn.keySet()) {
            s.append(con).append("\t").append(timesCached.get(con))
                    .append("\t\t")
//                    .append(avgMillisCache.get(con))
                    .append("\t\t").append(perCentAlive(con))
                    .append("\n\n");
        }
        
        s.append(border);        
        return s.toString();
    }
    
    private int perCentAlive(Connection con) {
        if(!destroyedOn.containsKey(con)) {
            destroyedOn.put(con, System.nanoTime());
        }
        long totalTime = destroyedOn.get(con) - createdOn.get(con);
        if(timesCached.get(con) == 0) {
            return 100;
        }
        return (int) (((double) totalAliveTime.get(con) / totalTime) * 100);
    }

    void cache(Connection con) {
        timesCached.put(con, timesCached.get(con) + 1);
        long time = System.nanoTime() - lastAliveOn.get(con);
        totalAliveTime.put(con, totalAliveTime.get(con) + time);
    }

    void remove(Connection con) {
        destroyedOn.put(con, System.nanoTime());
        long time = System.nanoTime() - lastAliveOn.get(con);
        totalAliveTime.put(con, totalAliveTime.get(con) + time);
    }

    void add(Connection con) {
        createdOn.put(con, System.nanoTime());
        lastAliveOn.put(con, System.nanoTime());
        totalAliveTime.put(con, 0L);
        timesCached.put(con, 0);
//        avgMillisCache.put(con, 0L);
    }

    void restore(Connection con) {
        lastAliveOn.put(con, System.nanoTime());
    }
}
