package ibis.ipl.impl.registry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public final class CommunicationStatistics {
    private static final Logger logger = Logger
            .getLogger(CommunicationStatistics.class);

    private final long start;

    private final int opcodes;

    private final double[] totalTimes;

    private final long[] incomingRequestCounter;

    private final long[] outgoingRequestCounter;

    private final long[] bytesIn;

    private final long[] bytesOut;

    public CommunicationStatistics(int opcodes) {
        this.opcodes = opcodes;
        
        start = System.currentTimeMillis();

        totalTimes = new double[opcodes];
        incomingRequestCounter = new long[opcodes];
        outgoingRequestCounter = new long[opcodes];
        bytesIn = new long[opcodes];
        bytesOut = new long[opcodes];

    }

    public CommunicationStatistics(DataInputStream in) throws IOException {
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

    public synchronized Map<String, String> getMap(String[] opcodeNames) {
        Map<String, String> result = new TreeMap<String, String>();

        long totalTraffic = 0;
        for (byte i = 0; i < opcodes; i++) {
            String name = opcodeNames[i];
            result.put(name + " in count", "" + incomingRequestCounter[i]);
            result.put(name + " out count", "" + outgoingRequestCounter[i]);
            result.put(name + " bytes in", "" + bytesIn[i]);
            result.put(name + " bytes out", "" + bytesOut[i]);
            result.put(name + " time spend", "" + totalTimes[i]);

            totalTraffic += bytesIn[i] + bytesOut[i];

            double average = totalTimes[i]
                    / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }
            result.put(name + " average time spend", "" + average);
        }
        result.put("total traffic ", "" + totalTraffic);

        return result;
    }

    public synchronized String toString(String[] opcodeNames) {
        long totalTraffic = 0;

        StringBuilder message = new StringBuilder();
        Formatter formatter = new Formatter(message);

        formatter.format("registry statistics at %.2f seconds:\n", (System
                .currentTimeMillis() - start) / 1000.0);
        formatter
                .format("TYPE          IN_COUNT OUT_COUNT BYTES_IN BYTES_OUT TOTAL_TIME   AVG_TIME\n");
        formatter
                .format("                                                        (sec)       (ms)\n");
        for (byte i = 0; i < opcodes; i++) {
            totalTraffic += bytesIn[i] + bytesOut[i];

            double average = totalTimes[i]
                    / (incomingRequestCounter[i] + outgoingRequestCounter[i]);
            if (incomingRequestCounter[i] == 0
                    && outgoingRequestCounter[i] == 0) {
                average = 0;
            }

            formatter.format("%-12s %9d %9d %8d %9d %10.2f %10.2f\n", opcodeNames[i]
                    , incomingRequestCounter[i],
                    outgoingRequestCounter[i], bytesIn[i], bytesOut[i],
                    totalTimes[i] / 1000.0, average);
        }
        formatter.format("total traffic (MB): %.2f",
                ((double) totalTraffic / 1024.0 / 1024.0));

        return message.toString();
    }

}