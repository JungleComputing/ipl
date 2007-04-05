package ibis.ipl.impl.registry.central;


import java.util.Formatter;

import org.apache.log4j.Logger;

final class Stats {
    private static Logger logger = Logger.getLogger(Stats.class);

    private double[] totalTimes;

    private long[] requestCounter;

    private int opcodes;

    long start;

    Stats(int opcodes) {
        this.opcodes = opcodes;
        totalTimes = new double[opcodes];
        requestCounter = new long[opcodes];

        start = System.currentTimeMillis();
    }

    synchronized void add(byte opcode, long time) {
        if (opcode >= opcodes) {
            logger.error("unknown opcode in handling stats: " + opcode);
        }

        totalTimes[opcode] = totalTimes[opcode] + time;
        requestCounter[opcode]++;
    }

    synchronized void clear() {
        for (int i = 0; i < opcodes; i++) {
            totalTimes[i] = 0;
            requestCounter[i] = 0;
        }
    }

    synchronized String getStats(boolean clear) {
        StringBuilder message = new StringBuilder();
        Formatter formatter = new Formatter(message);

        formatter.format("registry server statistics at %.2f seconds\n",
                (System.currentTimeMillis() - start) / 1000.0);
        formatter.format("TYPE          COUNT  TOTAL_TIME   AVG_TIME\n");
        formatter.format("                          (sec)       (ms)\n");
        for (byte i = 0; i < opcodes; i++) {
            double average = totalTimes[i] / requestCounter[i];
            if (requestCounter[i] == 0) {
                average = 0;
            }

            formatter.format("%-12s %6d  %10.2f %10.2f\n", Protocol
                    .opcodeString(i), requestCounter[i],
                    totalTimes[i] / 1000.0, average);
        }

        if (clear) {
            clear();
        }
        
        return message.toString();
    }

}