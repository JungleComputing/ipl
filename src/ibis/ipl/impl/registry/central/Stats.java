package ibis.ipl.impl.registry.central;

import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public final class Stats {
    private static Logger logger = Logger.getLogger(Stats.class);

    private double[] totalTimes;

    private long[] incomingRequestCounter;

    private long[] outgoingRequestCounter;

    private int opcodes;

    long start;

    public Stats(int opcodes) {
        this.opcodes = opcodes;
        totalTimes = new double[opcodes];
        incomingRequestCounter = new long[opcodes];
        outgoingRequestCounter = new long[opcodes];

        start = System.currentTimeMillis();
    }

    public synchronized void add(byte opcode, long time, boolean incoming) {
        if (opcode >= opcodes) {
            logger.error("unknown opcode in handling stats: " + opcode);
        }

        totalTimes[opcode] = totalTimes[opcode] + time;
        if (incoming) {
            incomingRequestCounter[opcode]++;
        } else {
            outgoingRequestCounter[opcode]++;
        }
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

    public synchronized Map<String, String> getMap() {
        Map<String, String> result = new TreeMap<String, String>();

        for (byte i = 0; i < opcodes; i++) {
            String name = Protocol.opcodeString(i);
            result.put(name + " in count", "" + incomingRequestCounter[i]);
            result.put(name + " out count", "" + outgoingRequestCounter[i]);
            result.put(name + " time spend", "" + totalTimes[i]);

            double average =
                    totalTimes[i]
                            / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }
            result.put(name + " average time spend", "" + average);
        }

        return result;
    }

    public synchronized String toString() {

        StringBuilder message = new StringBuilder();
        Formatter formatter = new Formatter(message);

        formatter.format("registry statistics at %.2f seconds:\n", (System
                .currentTimeMillis() - start) / 1000.0);
        formatter
                .format("TYPE          IN_COUNT OUT_COUNT  TOTAL_TIME   AVG_TIME\n");
        formatter
                .format("                                       (sec)       (ms)\n");
        for (byte i = 0; i < opcodes; i++) {
            double average =
                    totalTimes[i]
                            / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }

            formatter.format("%-12s %9d %9d  %10.2f %10.2f\n", Protocol
                    .opcodeString(i), incomingRequestCounter[i],
                    outgoingRequestCounter[i], totalTimes[i] / 1000.0, average);
        }

        return message.toString();
    }

}