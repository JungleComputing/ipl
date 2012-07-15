package ibis.ipl.impl.stacking.cache.util;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Timers {
    
    public static final List<Timers> list;
    
    public static final Timers streamTimer;
    public static final Timers ackTimer; 
    
    static{ 
        list = new LinkedList<Timers>();
        
        streamTimer = new Timers("streaming-timer");
        list.add(streamTimer);
        
        ackTimer = new Timers("ack-timer");
        list.add(ackTimer);
    }
    
    public final String name;
    private long start, duration, count;

    public Timers(String name) {
        this.name = name;
    }
    
    
    public void start() {
        start = System.currentTimeMillis();
        count++;
    }
    
    public void stop() {
        duration += System.currentTimeMillis() - start;
    }

    public void print(PrintStream out) {
        out.println(toString());
    }
    
    public void log(Logger log) {
        log.log(Level.INFO, toString());
    }
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Timer ").append(name).append(":\t(count, avgMillis) = (").
                append(count).append(", ").append(duration / count).append(")");

        return s.toString();
    }
}
