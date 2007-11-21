package ibis.ipl.impl.registry;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import ibis.ipl.impl.IbisIdentifier;

/**
 * Gathers statistics and writes them to disk
 */
public class StatisticsWriter extends Thread {

    private final String poolName;

    private final Long timeout;

    private final Map<IbisIdentifier, CommunicationStatistics> commStats;

    private final Map<IbisIdentifier, PoolStatistics> poolStats;

    private final CommunicationStatistics serverCommStats;

    private final PoolStatistics serverPoolStats;
    
    private final String[] opcodeNames;

    private boolean ended = false;

    public StatisticsWriter(String poolName, long timeout,
            CommunicationStatistics serverCommStats,
            PoolStatistics serverPoolStats, String[] opcodeNames) {
        this.poolName = poolName;
        this.timeout = timeout;
        this.serverCommStats = serverCommStats;
        this.serverPoolStats = serverPoolStats;
        this.opcodeNames = opcodeNames;

        commStats = new HashMap<IbisIdentifier, CommunicationStatistics>();
        poolStats = new HashMap<IbisIdentifier, PoolStatistics>();
    }

    public synchronized void addStatistics(
            CommunicationStatistics clientCommStatistics,
            PoolStatistics clientPoolStatistics, IbisIdentifier clientIdentifier,
            long timeOffset) {
        commStats.put(clientIdentifier, clientCommStatistics);
        poolStats.put(clientIdentifier, clientPoolStatistics);
    }

    /**
     * Write statistics to a file
     */
    private void write() {
        try {
            File file = new File(poolName + ".stats");
            if (file.exists()) {
                file.renameTo(new File(poolName + ".stats.old"));
            }
            
            PrintWriter out = new PrintWriter(poolName + ".stats");
            
            out.println(serverCommStats.toString(opcodeNames));
            out.println(serverPoolStats.toString());

            for(CommunicationStatistics stats: commStats.values()) {
                out.println(stats);
            }

            for(PoolStatistics stats: poolStats.values()) {
                out.println(stats);
            }
            
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public synchronized void end() {
        ended = true;
        notifyAll();

        write();
    }

    public synchronized void run() {
        while (!ended) {

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
        //delay two minutes, write on more time
        try {
                wait(120000);
        } catch (InterruptedException e) {
            // IGNORE
        }
        write();
    }

}
