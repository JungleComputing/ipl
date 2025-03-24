/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            if (logger.isDebugEnabled()) {
                logger.debug("no server file found");
            }
        }

        clientStatistics = new ArrayList<>();

        if (!directory.isDirectory()) {
            throw new IOException(directory + " not a directory");
        }

        int loadErrors = 0;
        for (File file : directory.listFiles()) {
            if (file.getName().equals("server") || file.getName().endsWith(".old")) {
                continue;
            }
            try {
                clientStatistics.add(new Statistics(file));
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("cannot load statistics file: " + file + " (trying .old version)", e);
                }
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
        if (logger.isDebugEnabled()) {
            logger.debug("duration = " + (endTime - startTime));
        }

        return endTime - startTime;
    }

    double serverPoolSize(long time) {
        long realtime = time + startTime;

        if (serverStatistics != null) {
            double result = serverStatistics.poolSizeAt(realtime);

            if (logger.isDebugEnabled()) {
                logger.debug("SERVER statistics: value at " + time + " (" + realtime + ") = " + result);
            }

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

            if (logger.isDebugEnabled()) {
                logger.debug("statistics: " + statistics + " value at " + time + " (" + realtime + ") = " + value);
            }

            if (value != -1) {
                active = active + 1;
                total = total + value;
            }
        }

        if (active == 0) {
            return 0;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("total = " + total + ", active = " + active + ", value = " + (total / active));
        }

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
