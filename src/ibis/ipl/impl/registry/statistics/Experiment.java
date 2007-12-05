package ibis.ipl.impl.registry.statistics;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.ipl.impl.IbisIdentifier;

/**
 * Gathers statistics for a single experiment from a directory of files
 */
public class Experiment extends Thread {

    private static final Logger logger = Logger.getLogger(Experiment.class);

    private final String poolName;

    private final List<Statistics> clientStatistics;

    private final Statistics serverStatistics;

    private final long startTime;

    private final long endTime;

    Experiment(File directory) throws IOException {

        poolName = directory.getName();

        File serverFile = new File(directory, "server");

        if (serverFile.exists()) {
            serverStatistics = new Statistics(serverFile);
        } else {
            serverStatistics = null;
        }

        clientStatistics = new ArrayList<Statistics>();

        for (File file : directory.listFiles()) {
            if (!file.getName().equals("server")) {
                clientStatistics.add(new Statistics(file));
            }
        }

        startTime = getStartTime();
        endTime = getEndTime();
    }

    private long getStartTime() {
        long result = Long.MAX_VALUE;

        if (serverStatistics != null) {
            result = serverStatistics.getStartTime();
        }

        for (Statistics statistics : clientStatistics) {
            if (statistics.getStartTime() < result) {
                result = statistics.getStartTime();
            }
        }

        return result;
    }

    private long getEndTime() {
        long result = 0;

        if (serverStatistics != null) {
            result = serverStatistics.getEndTime();
        }

        for (Statistics statistics : clientStatistics) {
            if (statistics.getEndTime() > result) {
                result = statistics.getEndTime();
            }
        }

        return result;
    }
    
    long duration() {
        return endTime - startTime;
    }

    private static void writePoolHistory(long start, long end, long interval,
            Formatter out, Statistics... allStatistics) {
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

    private double averageClientTraffic() {
        if (clientStatistics.size() == 0) {
            return 0;
        }

        double total = 0;

        for (Statistics statistics : clientStatistics) {
            total = total + statistics.totalTraffic();
        }

        return total / clientStatistics.size();
    }

    
}
