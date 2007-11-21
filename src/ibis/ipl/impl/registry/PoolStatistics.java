package ibis.ipl.impl.registry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class PoolStatistics {

    private static class DataPoint implements Comparable<DataPoint> {
        long time;
        long value;

        DataPoint(long time, long value) {
            this.time = time;
            this.value = value;
        }

        
        DataPoint(long value) {
            time = System.currentTimeMillis();
            this.value = value;
        }

        public int compareTo(DataPoint other) {
            return (int) (time - other.time);
        }
        
        public String toString() {
            return "(" + time + ", " + value + ")";
        }
    }

    SortedSet<DataPoint> poolSizeHistory;
    
    SortedSet<DataPoint> electionEventHistory;

    int currentPoolSize;

    public PoolStatistics() {
        poolSizeHistory = new TreeSet<DataPoint>();
        electionEventHistory = new TreeSet<DataPoint>();

        currentPoolSize = 0;
    }

    public PoolStatistics(DataInputStream in) throws IOException {
        poolSizeHistory = new TreeSet<DataPoint>();
        electionEventHistory = new TreeSet<DataPoint>();
        
        int nrOfSizeDataPoints = in.readInt();
        if (nrOfSizeDataPoints < 0) {
            throw new IOException("negative list size");
        }
        for(int i = 0; i < nrOfSizeDataPoints; i++) {
            poolSizeHistory.add(new DataPoint(in.readLong(), in.readLong()));
        }

        int nrOfElectionDataPoints = in.readInt();
        if (nrOfElectionDataPoints < 0) {
            throw new IOException("negative list size");
        }
        for(int i = 0; i < nrOfElectionDataPoints; i++) {
            electionEventHistory.add(new DataPoint(in.readLong(), in.readLong()));
        }
        
        currentPoolSize = in.readInt();
    }
    
    public synchronized void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(poolSizeHistory.size());
        
        for(DataPoint point: poolSizeHistory) {
            out.writeLong(point.time);
            out.writeLong(point.value);
        }

        out.writeInt(electionEventHistory.size());
        
        for(DataPoint point: electionEventHistory) {
            out.writeLong(point.time);
            out.writeLong(point.value);
        }
        
        out.writeInt(currentPoolSize);
    }

    public synchronized void ibisJoined() {
        currentPoolSize++;
        
        poolSizeHistory.add(new DataPoint(currentPoolSize));
    }

    public synchronized void ibisLeft() {
        currentPoolSize--;

        poolSizeHistory.add(new DataPoint(currentPoolSize));
    }

    public synchronized void ibisDied() {
        currentPoolSize--;

        poolSizeHistory.add(new DataPoint(currentPoolSize));
    }

    public synchronized void unElect() {
        electionEventHistory.add(new DataPoint(electionEventHistory.size() + 1));
    }

    public synchronized void newElection() {
        electionEventHistory.add(new DataPoint(electionEventHistory.size() + 1));
    }
    
    public String toString() {
        String result = "pool size history:";
        for (DataPoint point: poolSizeHistory) {
            result += " " + point;
        }
        
        result += "\n";

        result += "election event history:";
        for (DataPoint point: electionEventHistory) {
            result += " " + point;
        }
        
        return result;
    }
        

}
