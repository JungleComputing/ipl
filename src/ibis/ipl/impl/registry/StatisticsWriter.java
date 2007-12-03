package ibis.ipl.impl.registry;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.ipl.impl.IbisIdentifier;

/**
 * Gathers statistics and writes them to disk
 */
public class StatisticsWriter extends Thread {

    private static final Logger logger = Logger
            .getLogger(StatisticsWriter.class);

    private final String poolName;

    private final Long timeout;

    private final Map<IbisIdentifier, Statistics> clientStatistics;

    private final Statistics serverStatistics;

    private final String[] opcodeNames;

    public StatisticsWriter(String poolName, long timeout,
            Statistics serverCommStats, String[] opcodeNames) {
        this.poolName = poolName;
        this.timeout = timeout;
        this.serverStatistics = serverCommStats;
        this.opcodeNames = opcodeNames;

        clientStatistics = new HashMap<IbisIdentifier, Statistics>();
    }

    public synchronized void addStatistics(Statistics statistics,
            IbisIdentifier clientIdentifier) {
        logger.debug("new statistics from: " + clientIdentifier);
        clientStatistics.put(clientIdentifier, statistics);
        write();
    }

    private synchronized long getStartTime() {
        long result = serverStatistics.getStartTime();

        for (Statistics statistics : clientStatistics.values()) {
            if (statistics.getStartTime() < result) {
                result = statistics.getStartTime();
            }
        }

        return result;
    }

    private synchronized long getEndTime() {
        long result = serverStatistics.getEndTime();

        for (Statistics statistics : clientStatistics.values()) {
            if (statistics.getEndTime() > result) {
                result = statistics.getEndTime();
            }
        }

        return result;
    }

    private static long calculateInterval(long start, long end) {
        long result = (end - start) / 100;

        if (result < 1) {
            result = 1;
        }

        logger.debug("interval = " + result);

        return result;
    }

    private synchronized Statistics[] getClientStatistics() {
        return clientStatistics.values().toArray(new Statistics[0]);
    }
        
    
    private static void writePoolHistory(long start, long end,
            long interval, Formatter out, Statistics... allStatistics) {
        if (allStatistics.length == 0) {
            return;
        }

        out.format("time total_size participants average");
        
        // always write at lease one value past the "end" time
        for (long time = start; time <= (end + interval); time += interval) {
            int participants = 0;
            long total = 0;

            for (Statistics statistics : allStatistics) {
                long value = statistics.poolSizeAt(time);

                if (value >= 0) {
                    total = total + value;
                    participants++;
                }
            }

            double average = ((double) total) / ((double) participants);
            
            if (participants == 0) {
                average = 0;
            }

            out.format("%d %d %d %f\n", time - start, total, participants,
                    average);
        }

    }

    private synchronized double averageClientTraffic() {
        if (clientStatistics.size() == 0) {
            return 0;
        }

        double total = 0;

        for (Statistics statistics : clientStatistics.values()) {
            total = total + statistics.totalTraffic();
        }

        return total / clientStatistics.size();
    }

    /**
     * Write statistics to a file
     */
    private void write() {
        long start = getStartTime();
        long end = getEndTime();
        long interval = calculateInterval(start, end);

        File file = null;
        try {
            file = new File(poolName + ".stats.server");
            logger.debug("writing statistic file " + file);
            if (file.exists()) {
                file.renameTo(new File("old." + file.getName()));
            }

            Formatter out = new Formatter(file);

            out.format("#server stats:\n");
            serverStatistics.printCommStats(out, opcodeNames);

            out.format("#server total data transfer = %.2f\n", serverStatistics
                    .totalTraffic());

            out.format("#pool size\n");
            writePoolHistory(start, end, interval, out, serverStatistics);

            out.flush();
            out.close();

            // client stats

            file = new File(poolName + ".stats.clients");
            logger.debug("writing statistic file " + file);
            if (file.exists()) {
                file.renameTo(new File("old." + file.getName()));
            }

            out = new Formatter(file);

            out.format("#average #total data transfer = %.2f\n",
                    averageClientTraffic());

            out.format("#average pool size\n");
            writePoolHistory(start, end, interval, out, getClientStatistics());

            out.format("#raw data:\n");

            for (Statistics statistics : clientStatistics.values()) {
                out.format("offset = %d\n", statistics.getOffset());
                for (DataPoint point : statistics.getPoolSizeData()) {
                    out.format("%d %d\n", point.getTime() - start, point
                            .getValue());
                }
                out.format("##############\n");
            }

            out.flush();
            out.close();

            logger.debug("done writing statistic file " + file);
        } catch (IOException e) {
            logger.error("could not write statistics file: " + file, e);
        }

    }

    public synchronized void run() {
        logger.debug("starting statistics writer");

        while (true) {
            write();

            // randomize delay (average still the given timeout)
            long delay = (long) (timeout * 2 * Math.random());

            try {
                if (delay > 0) {
                    wait(delay);
                }
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    public synchronized void nudge() {
        notifyAll();
    }

}
