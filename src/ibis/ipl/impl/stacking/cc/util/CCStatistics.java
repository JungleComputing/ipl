package ibis.ipl.impl.stacking.cc.util;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cc.manager.Connection;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CCStatistics {
    
    public static final Map<Connection, Integer> timesCached;
//    public final Map<Connection, Long> avgMillisCache;
    public static final Map<Connection, Long> createdOn;
    public static final Map<Connection, Long> destroyedOn;
    public static final Map<Connection, Long> totalAliveTime;
    public static final Map<Connection, Long> lastAliveOn;
    
    public static boolean worthPrinting = false;

    static {
        timesCached = new HashMap<Connection, Integer>();
//        avgMillisCache = new HashMap<Connection, Long>();
        createdOn = new HashMap<Connection, Long>();
        destroyedOn = new HashMap<Connection, Long>();
        totalAliveTime = new HashMap<Connection, Long>();
        lastAliveOn = new HashMap<Connection, Long>();
    }
    

    public static void printStatistics(PrintStream out) {
        out.println(getString());
    }
    
    public static void printStatistics(Logger log) {
        log.log(Level.INFO, getString());
        
//        for (Timers timer : Timers.list) {
//            timer.log(log);
//        }
    }

    private synchronized static String getString() {
        String border = "\n=====================================================";
        StringBuilder s = new StringBuilder();
        s.append(border);
        s.append("\n\t\t\tCaching Statistics for cached connections:\n\n");
        
        s.append("Connection\t\t\t\tTimes cached\t\t% time alive\n\n");
        
        boolean worth = false;
        for(Connection con : createdOn.keySet()) {
            if(timesCached.containsKey(con) &&
                timesCached.get(con) != 0) {
                worth = true;
            s.append(con).append("\t").append(timesCached.get(con))
                    .append("\t\t")
//                    .append(avgMillisCache.get(con))
                    .append("\t\t").append(perCentAlive(con))
                    .append("\n\n");
            }
        }
        
        s.append(border);     
        
        return worth ? s.toString() : "";
    }
    
    private static int perCentAlive(Connection con) {
        if(!destroyedOn.containsKey(con)) {
            destroyedOn.put(con, System.nanoTime());
        }
        long totalTime = destroyedOn.get(con) - createdOn.get(con);
        if(!timesCached.containsKey(con) ||
            timesCached.get(con) == 0) {
            return 100;
        }
        return (int) (((double) totalAliveTime.get(con) / totalTime) * 100);
    }

    public static void cache(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        worthPrinting = true;
        Connection con = new Connection(spi, rpi);
        cache(con);
    }
    
    public static void cache(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        worthPrinting = true;
        Connection con = new Connection(rpi, spi);
        cache(con);
    }
    
    static synchronized void cache(Connection con) {
        timesCached.put(con, timesCached.get(con) + 1);
        long time = System.nanoTime() - lastAliveOn.get(con);
        totalAliveTime.put(con, totalAliveTime.get(con) + time);
    }

    public static void remove(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        remove(con);
    }
    
    public static void remove(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        remove(con);
    }
    
    static synchronized void remove(Connection con) {
        destroyedOn.put(con, System.nanoTime());
        long time = System.nanoTime() - lastAliveOn.get(con);
        totalAliveTime.put(con, totalAliveTime.get(con) + time);
    }

    public static void connect(SendPortIdentifier spi, ReceivePortIdentifier rpi) {
        Connection con = new Connection(spi, rpi);
        connect(con);
    }
    
    public static void connect(ReceivePortIdentifier rpi, SendPortIdentifier spi) {
        Connection con = new Connection(rpi, spi);
        connect(con);
    }

    static synchronized void connect(Connection con) {
        if(createdOn.containsKey(con)) {
            lastAliveOn.put(con, System.nanoTime());
        } else {
            createdOn.put(con, System.nanoTime());
        lastAliveOn.put(con, System.nanoTime());
        totalAliveTime.put(con, 0L);
        timesCached.put(con, 0);
//        avgMillisCache.put(con, 0L);
        }
    }
}
