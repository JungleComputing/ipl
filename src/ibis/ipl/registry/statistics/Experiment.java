package ibis.ipl.registry.statistics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gathers statistics for a single experiment from a directory of files
 */
public class Experiment {

    private static final Logger logger = LoggerFactory.getLogger(Experiment.class);

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
            logger.debug("no server file found");
        }

        clientStatistics = new ArrayList<Statistics>();

        if (!directory.isDirectory()) {
            throw new IOException(directory + " not a directory");
        }

        int loadErrors = 0;
        for (File file : directory.listFiles()) {
            if (file.getName().equals("server")
                    || file.getName().endsWith(".old")) {
                continue;
            }
            try {
                clientStatistics.add(new Statistics(file));
            } catch (IOException e) {
                logger.debug("cannot load statistics file: " + file
                        + " (trying .old version)", e);
                loadErrors++;
                try {
                    File oldFile = new File(file.getPath() + ".old");
                    clientStatistics.add(new Statistics(oldFile));
                } catch (IOException e2) {
                    logger.error("cannot load OLD statistics file: " + file, e2);
                }
            }
        }
        if (loadErrors > 0) {
            logger.warn(poolName + ": " + loadErrors + " files could not be read (used .old version instead)");
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

    String getName() {
        return poolName;
    }

    long duration() {
        logger.debug("duration = " + (endTime - startTime));

        return endTime - startTime;
    }

    double serverPoolSize(long time) {
        long realtime = time + startTime;

        if (serverStatistics != null) {
            double result = serverStatistics.poolSizeAt(realtime);

            logger.debug("SERVER statistics: value at " + time + " ("
                    + realtime + ") = " + result);

            if (result == -1) {
                return 0;
            }

            return result;
        }

        return 0;
    }

    double averagePoolSize(long time) {
        long realtime = time + startTime;

        double active = 0;
        double total = 0;

        for (Statistics statistics : clientStatistics) {
            double value = statistics.poolSizeAt(realtime);

            logger.debug("statistics: " + statistics + " value at " + time
                    + " (" + realtime + ") = " + value);

            if (value != -1) {
                active = active + 1;
                total = total + value;
            }
        }

        if (active == 0) {
            return 0;
        }
        
        logger.debug("total = " + total + ", active = " + active + ", value = " + (total / active));

        return total / active;
    }

    double averageClientTraffic() {
        if (clientStatistics.size() == 0) {
            return 0;
        }

        double total = 0;

        for (Statistics statistics : clientStatistics) {
            total = total + statistics.totalTraffic();
        }

        return total / clientStatistics.size();
    }
    
    double serverTraffic() {
        if (serverStatistics == null) {
            return 0;
        }
        
        return serverStatistics.totalTraffic();
    }

    public void serverCommStats(Formatter out) {
        if (serverStatistics == null) {
            return;
        }
        
        serverStatistics.printCommStats(out);
    }

}
