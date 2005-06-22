import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import ibis.ipl.*;
import ibis.util.PoolInfo;

public class ParTest {

    public static final int DEFAULT_PROBE_SIZE = 1024 * 100; // in KB

    public static final int DEFAULT_SLICE_SIZE = DEFAULT_PROBE_SIZE*1024;//127 * 1024; // in bytes

    public static final int DEFAULT_BUF_SIZE = 128 * 1024; // in bytes

    static final boolean COMPARE = false;

    private int probeSize, sliceSize, bufSize, rounds;

    private int connectionCounter;

    private Ibis ibis;

    private PoolInfo pool;

    ParTest(int probeSize, int sliceSize, int bufSize, int rounds)
            throws IOException, IbisException {
        this.probeSize = probeSize;
        this.sliceSize = sliceSize;
        this.bufSize = bufSize;
        this.rounds = rounds;

        connectionCounter = 0;
        System.out.println("Creating Ibis...");
        ibis = Ibis.createIbis(properties(), null);
        System.out.println("Creating PoolInfo...");
        pool = PoolInfo.createPoolInfo();
    }

    void end() {
        try {
            ibis.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StaticProperties properties() {
        StaticProperties prop = new StaticProperties();
        prop.add("communication",
                "OneToOne, FifoOrdered, Reliable, ExplicitReceipt");
        prop.add("serialization", "Ibis");
        prop.add("worldmodel", "closed");
 //       prop.add("name", "tcp");

        return prop;
    }

    void run() throws Exception {
        System.out.println("Running...");

        byte[] probe = new byte[probeSize * 1024];

        if (COMPARE) {
            for (int i = 0; i < probe.length; i++) {
                probe[i] = (byte) (i % 100);
            }
        }

        String me = pool.clusterName();
        String peer = pool.clusterName(1 - pool.rank());
        if (me.equals(peer)) {
            me = "aap";
        }
        String src = pool.rank() == 0 ? me : peer;
        String dst = pool.rank() == 0 ? peer : me;

        Connection control = new Connection(me, peer, 1, false);

        for (int noStreams = 16; noStreams <= 64; noStreams *= 2) {
            Connection c = new Connection(me, peer, noStreams, true);
            for (int i = 1; i <= rounds; i++) {

                long time = System.currentTimeMillis();
                int offset = 0;
                int msgCount = 0;

                try {
                    if (me.equals(src)) {
                        System.err.println("I am the sender");
                        WriteMessage msg = c.sport.newMessage();
                        while (offset < probe.length) {
                            System.out.println("Sending msg " + (msgCount++)
                             + "...");
                            int length = Math.min(sliceSize, probe.length
                                    - offset);
                            System.out.println("Sending " + offset + ":" +
                             length);
                            msg.writeArray(probe, offset, length);
                            offset += length;
                        }
                        System.out.println("Sending msg " + (msgCount++)
                                + "PRE FINISH");
                        msg.finish();
                        System.out.println("Sending msg " + (msgCount++)
                                + "DONE");

                        ReadMessage ack = c.rport.receive();
                        ack.readByte();
                        ack.finish();
                    } else {
                        System.err.println("I am the receiver");
                        ReadMessage msg = c.rport.receive();
                        while (offset < probe.length) {
                            System.out.println("Receiving msg " +
                             (msgCount++) + "...");
                            int length = Math.min(sliceSize, probe.length
                                    - offset);
                            System.out.println("Receiving " + offset + ":" +
                             length);
                            msg.readArray(probe, offset, length);
                            offset += length;
                        }
                        System.out.println("Receiving msg " +
                                (msgCount++) + "PRE FINISH");
                        msg.finish();
                        System.out.println("Receiving msg " +
                                (msgCount++) + "DONE");
                        if (COMPARE) {
                            for (int x = 0; x < probe.length; x++) {
                                if (probe[x] != (byte) (x % 100)) {
                                    throw new Error("Read corrupt data!");
                                }
                            }
                        }

                        WriteMessage ack = c.sport.newMessage();
                        ack.writeByte((byte) 0);
                        ack.finish();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                time = System.currentTimeMillis() - time;

                double transferTime = time / 1000.0;
                double throughput = (probeSize / 1024.0 * 8) / transferTime;

                System.out.println("round " + i + ", " + noStreams
                        + " streams: " + throughput + " Mbit/sec");
            }
            c.close();
        }

        control.close();
    }

    private static int parseInt(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            System.err.println("we need an integer " + name);
            usage();
        }
        return 0; // NEVER REACHED
    }

    private static void usage() {
        System.err.println("usage:");
        System.err
                .println("  java ParTest [-probe <size> (in KB)] [-slice <size> (in bytes)]");
        System.err.println("  [-buf <size> (in bytes)] [-rounds <no. rounds>]");
        System.exit(1);
    }

    public static void main(String[] args) {
        int probeSize = ParTest.DEFAULT_PROBE_SIZE;
        int sliceSize = ParTest.DEFAULT_SLICE_SIZE;
        int bufSize = ParTest.DEFAULT_BUF_SIZE;
        int rounds = 5;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-probe")) {
                probeSize = parseInt("probe", args[++i]);
            } else if (args[i].equals("-slice")) {
                sliceSize = parseInt("slice", args[++i]);
            } else if (args[i].equals("-buf")) {
                bufSize = parseInt("buf", args[++i]);
            } else if (args[i].equals("-rounds")) {
                bufSize = parseInt("rounds", args[++i]);
            } else if (args[i].equals("-time")) {
                System.err.println("unknown option: " + args[i]);
                usage();
            }
        }

        if (sliceSize == 0) {
            sliceSize = probeSize * 1024; // use 1 writemessage per measurement
        }

        System.out.println("Probe size: " + probeSize);
        System.out.println("Slice size: " + sliceSize);
        System.out.println("Buf size:   " + bufSize);

        ParTest test = null;
        try {
            test = new ParTest(probeSize, sliceSize, bufSize, rounds);
            test.run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (test != null)
                test.end();
        }
    }

    class Connection {

        SendPort sport;

        ReceivePort rport;

        Connection(String me, String peer, int noStreams, boolean tweak)
                throws IOException, IbisException {
            System.out.print("Creating connection " + me + "<->" + peer + " ("
                    + noStreams + " streams)...");
            System.out.flush();

            PortType portType = ibis.createPortType("test" + noStreams, properties());

            String myName = "" + pool.rank() + "_" + connectionCounter;
            String peerName = "" + (pool.rank() == 0 ? 1 : 0) + "_" 
                    + connectionCounter;
            connectionCounter++;

            Map props = new HashMap();
            if (tweak) {
                props.put("ibis.connect.NumWays", "" + noStreams);
                props.put("ibis.connect.BlockSize", "" + 128*1024);
                props.put("ibis.connect.InputBufferSize", "" + bufSize);
                props.put("ibis.connect.OutputBufferSize", "" + bufSize);
            } else {
                props.put("ibis.connect.NumWays", "1");
            }

            rport = portType.createReceivePort(myName);
            rport.setProperties(props);
            rport.enableConnections();

            sport = portType.createSendPort();
            sport.setProperties(props);
            ReceivePortIdentifier rportId = ibis.registry().lookupReceivePort(
                    peerName);
            sport.connect(rportId);

            System.out.println("done");
            System.out.flush();
        }

        void close() throws IOException {
            sport.close();
            rport.close();
        }
    }
}
