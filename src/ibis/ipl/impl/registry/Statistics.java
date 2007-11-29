package ibis.ipl.impl.registry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public final class Statistics {
    private static final Logger logger =
        Logger.getLogger(Statistics.class);

    private final long start;

    private final int opcodes;

    private final double[] totalTimes;

    private final long[] incomingRequestCounter;

    private final long[] outgoingRequestCounter;

    private final long[] bytesIn;

    private final long[] bytesOut;

    List<DataPoint> poolSizeHistory;

    int currentPoolSize;

    List<DataPoint> electionEventHistory;
    
    public Statistics(int opcodes) {
        this.opcodes = opcodes;

        start = System.currentTimeMillis();

        totalTimes = new double[opcodes];
        incomingRequestCounter = new long[opcodes];
        outgoingRequestCounter = new long[opcodes];
        bytesIn = new long[opcodes];
        bytesOut = new long[opcodes];

        poolSizeHistory = new LinkedList<DataPoint>();
        electionEventHistory = new LinkedList<DataPoint>();

        currentPoolSize = 0;

    }

    public Statistics(DataInputStream in, long timeOffset) throws IOException {
        opcodes = in.readInt();

        start = in.readLong();

        totalTimes = new double[opcodes];
        incomingRequestCounter = new long[opcodes];
        outgoingRequestCounter = new long[opcodes];
        bytesIn = new long[opcodes];
        bytesOut = new long[opcodes];

        for (int i = 0; i < opcodes; i++) {
            totalTimes[i] = in.readDouble();
            incomingRequestCounter[i] = in.readLong();
            outgoingRequestCounter[i] = in.readLong();
            bytesIn[i] = in.readLong();
            bytesOut[i] = in.readLong();
        }
        
        poolSizeHistory = new LinkedList<DataPoint>();
        electionEventHistory = new LinkedList<DataPoint>();
        
        int nrOfSizeDataPoints = in.readInt();
        if (nrOfSizeDataPoints < 0) {
            throw new IOException("negative list size");
        }
        for(int i = 0; i < nrOfSizeDataPoints; i++) {
            poolSizeHistory.add(new DataPoint(in.readLong() + timeOffset, in.readLong()));
        }

        int nrOfElectionDataPoints = in.readInt();
        if (nrOfElectionDataPoints < 0) {
            throw new IOException("negative list size");
        }
        
        for(int i = 0; i < nrOfElectionDataPoints; i++) {
            electionEventHistory.add(new DataPoint(in.readLong() + timeOffset, in.readLong()));
        }
        
        currentPoolSize = in.readInt();
    }

    public synchronized void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(opcodes);

        out.writeLong(start);

        for (int i = 0; i < opcodes; i++) {
            out.writeDouble(totalTimes[i]);
            out.writeLong(incomingRequestCounter[i]);
            out.writeLong(outgoingRequestCounter[i]);
            out.writeLong(bytesIn[i]);
            out.writeLong(bytesOut[i]);
        }
        
        out.writeInt(poolSizeHistory.size());
        
        for(DataPoint point: poolSizeHistory) {
            out.writeLong(point.getTime());
            out.writeLong(point.getValue());
        }

        out.writeInt(electionEventHistory.size());
        
        for(DataPoint point: electionEventHistory) {
            out.writeLong(point.getTime());
            out.writeLong(point.getValue());
        }
        
        out.writeInt(currentPoolSize);
    }

    public synchronized void add(byte opcode, long time, long bytesReceived,
            long bytesSend, boolean incoming) {
        if (opcode >= opcodes) {
            logger.error("unknown opcode in handling stats: " + opcode);
        }

        totalTimes[opcode] = totalTimes[opcode] + time;
        if (incoming) {
            incomingRequestCounter[opcode]++;
        } else {
            outgoingRequestCounter[opcode]++;
        }
        bytesIn[opcode] += bytesReceived;
        bytesOut[opcode] += bytesSend;
    }

    synchronized void clear() {
        for (int i = 0; i < opcodes; i++) {
            totalTimes[i] = 0;
            incomingRequestCounter[i] = 0;
            outgoingRequestCounter[i] = 0;
        }
    }

    public synchronized boolean empty() {
        for (byte i = 0; i < opcodes; i++) {
            if (totalTimes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public synchronized void printCommStats(Formatter formatter, String[] opcodeNames) {
        long totalTraffic = 0;

        formatter.format("#statistics at %.2f seconds:\n",
            (System.currentTimeMillis() - start) / 1000.0);
        formatter.format("#TYPE          IN_COUNT OUT_COUNT BYTES_IN BYTES_OUT TOTAL_TIME   AVG_TIME\n");
        formatter.format("#                                                        (sec)       (ms)\n");
        for (byte i = 0; i < opcodes; i++) {
            totalTraffic += bytesIn[i] + bytesOut[i];

            double average =
                totalTimes[i]
                        / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }

            formatter.format("#%-12s %9d %9d %8d %9d %10.2f %10.2f\n",
                opcodeNames[i], incomingRequestCounter[i],
                outgoingRequestCounter[i], bytesIn[i], bytesOut[i],
                totalTimes[i] / 1000.0, average);
        }
    }

    public synchronized void ibisJoined() {
        currentPoolSize++;
        
        poolSizeHistory.add(new DataPoint(currentPoolSize));
        
        logger.debug("ibis joined, size now: " + currentPoolSize);
    }

    public synchronized void ibisLeft() {
        currentPoolSize--;

        poolSizeHistory.add(new DataPoint(currentPoolSize));
        
        logger.debug("ibis left, size now: " + currentPoolSize);
    }

    public synchronized void ibisDied() {
        currentPoolSize--;

        poolSizeHistory.add(new DataPoint(currentPoolSize));
        
        logger.debug("ibis died, size now: " + currentPoolSize);
    }

    public synchronized void unElect() {
        electionEventHistory.add(new DataPoint(electionEventHistory.size() + 1));
    }

    public synchronized void newElection() {
        electionEventHistory.add(new DataPoint(electionEventHistory.size() + 1));
    }

    public synchronized long getStartTime() {
        return start;
    }
    
    public synchronized long getEndTime() {
        long result = start;
        
        if (poolSizeHistory.size() > 0) {
            long time = poolSizeHistory.get(poolSizeHistory.size() - 1).getTime();
            
            if (time > result) {
                result = time;
            }
        }

        if (electionEventHistory.size() > 0) {
            long time = electionEventHistory.get(electionEventHistory.size() - 1).getTime();
            
            if (time > result) {
                result = time;
            }
        }
        
        return result;
    }
    
    public synchronized long poolSizeAt(long time) {
        if (poolSizeHistory.size() == 0) {
            return 0;
        }
        
        long result = 0;

        for(DataPoint point: poolSizeHistory) {
            if (point.getTime() > time) {
                //previous point is result
                return result;
            } else {
                result = point.getValue();
            }
        }
        //return value of last point
        return result;
    }
    
    public synchronized DataPoint[] getPoolSizeData() {
        return poolSizeHistory.toArray(new DataPoint[0]);
    }
    
    public synchronized double totalTraffic() {
        double totalTraffic = 0;
        
        for (byte i = 0; i < opcodes; i++) {
            totalTraffic = totalTraffic + bytesIn[i] + bytesOut[i];
        }
        
        return totalTraffic / 1024.0 / 1024.0;
    }
        
}