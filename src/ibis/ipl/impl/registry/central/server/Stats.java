package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.registry.central.Protocol;

import java.util.Formatter;

import org.apache.log4j.Logger;

final class Stats {
    private static Logger logger = Logger.getLogger(Stats.class);

    private double[] totalTimes;

    private long[] incomingRequestCounter;

    private long[] outgoingRequestCounter;

    private int opcodes;

    long start;

    Stats(int opcodes) {
        this.opcodes = opcodes;
        totalTimes = new double[opcodes];
        incomingRequestCounter = new long[opcodes];
        outgoingRequestCounter = new long[opcodes];

        start = System.currentTimeMillis();
    }

    synchronized void add(byte opcode, long time, boolean incoming) {
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

    synchronized boolean empty() {
        for (byte i = 0; i < opcodes; i++) {
            if (totalTimes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    synchronized String getStats(boolean clear) {

        StringBuilder message = new StringBuilder();
        Formatter formatter = new Formatter(message);

        formatter.format("registry server statistics at %.2f seconds:\n",
                (System.currentTimeMillis() - start) / 1000.0);
        formatter
                .format("TYPE          IN_COUNT OUT_COUNT  TOTAL_TIME   AVG_TIME\n");
        formatter
                .format("                                       (sec)       (ms)\n");
        for (byte i = 0; i < opcodes; i++) {
            double average = totalTimes[i]
                    / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }

            formatter.format("%-12s %9d %9d  %10.2f %10.2f\n", Protocol
                    .opcodeString(i), incomingRequestCounter[i],
                    outgoingRequestCounter[i], totalTimes[i] / 1000.0, average);
        }

        if (clear) {
            clear();
        }

        return message.toString();
    }

}