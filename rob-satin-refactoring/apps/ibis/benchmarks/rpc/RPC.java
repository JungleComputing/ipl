/* $Id$ */

import java.util.Properties;
import java.util.Hashtable;
import java.io.IOException;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.ResizeHandler;
import ibis.ipl.IbisException;

import ibis.util.TypedProperties;
import ibis.util.Timer;
import ibis.util.PoolInfo;
import java.util.HashMap;

class RszHandler implements ResizeHandler {

    java.util.Vector idents = new java.util.Vector();

    public void joined(IbisIdentifier id) {
        synchronized (this) {
            idents.add(id);
            notifyAll();
        }
        System.err.println(this + " See join of " + id + "; n := "
                + idents.size());
    }

    public void left(IbisIdentifier id) {
        synchronized (this) {
            idents.remove(id);
            notifyAll();
        }
        // System.err.println(this + " See leave of " + id + "; n := " + idents.size());
    }

    public void died(IbisIdentifier corpse) {
        left(corpse);
    }

    public void mustLeave(IbisIdentifier[] ids) {
        // We don't do this.
    }

    void sync(int n) {
        synchronized (this) {
            while (idents.size() < n) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}

class RPC implements Upcall, Runnable, ReceivePortConnectUpcall,
        SendPortConnectUpcall {

    private final static int BUFSIZ = TypedProperties.intProperty(
            "socketbuffersize", 0);

    private final static boolean VARIANCE_TIMER = TypedProperties
            .booleanProperty("variance-timer", false);

    private Ibis myIbis;

    private Registry registry;

    private RszHandler rszHandler;

    private PortType requestPortType;

    private PortType replyPortType;

    private SendPort sport;

    private ReceivePort rport;

    private byte[] byte_buffer;

    private short[] short_buffer;

    private int[] int_buffer;

    private long[] long_buffer;

    private float[] float_buffer;

    private double[] double_buffer;

    private VBFField[] VBFField_buffer;

    private Object[] object_buffer;

    private Object single_object = null;

    private boolean upcall = true;

    private boolean connectUpcalls = false;

    private boolean one_way = false;

    private final boolean consume = true;

    private int count = 1000;

    private int services = 0;

    private int size = 0;

    private WriteMessage writeMessage = null;

    private boolean finish_upcall_msg = false;

    private boolean gc_on_rcve = false;

    private int ncpus = -1;

    private int rank = -1;

    private int clients = -1;

    private int servers = -1;

    private boolean bcast = false;

    private boolean bcast_all = false;

    private boolean i_am_client = false;

    private boolean busy = false;

    private int warmup = -1;

    private int first_warmup = 0;

    private int server_spin = 0;

    private int client_spin = 0;

    private java.util.Random rand = new java.util.Random();

    private boolean requireOneToMany = false;

    private boolean sequenced = false;

    private final boolean USE_RESIZEHANDLER = false;

    private final boolean EMPTY_REPLY = true; // false; // true;

    private final boolean closePortsAtExit = false; // true;

    private final int DATA_BYTES = 0;

    private final int DATA_SHORTS = DATA_BYTES + 1;

    private final int DATA_INTS = DATA_SHORTS + 1;

    private final int DATA_LONGS = DATA_INTS + 1;

    private final int DATA_FLOATS = DATA_LONGS + 1;

    private final int DATA_DOUBLES = DATA_FLOATS + 1;

    private final int DATA_OBJ_1 = DATA_DOUBLES + 1;

    private final int DATA_OBJ_2 = DATA_OBJ_1 + 1;

    private final int DATA_INNER = DATA_OBJ_2 + 1;

    private final int DATA_HASH = DATA_INNER + 1;

    private final int DATA_VBFField = DATA_HASH + 1;

    private final int DATATYPES = DATA_VBFField + 1;

    private final long data_size[] = { 1, 2, 4, 8, 4, 8, 4 * 4, 4 * 4, 3 * 4,
            0, 4 + 3 * 8, 8 };

    private int data_type = DATA_BYTES;

    // private Timer t_send = Timer.createTimer();
    // private Timer t_s_finish = Timer.createTimer();
    // private Timer t_get_msg = Timer.createTimer();
    // private Timer t_rcve = Timer.createTimer();
    // private Timer t_r_finish = Timer.createTimer();
    // private Timer t_upcall = Timer.createTimer();
    // private Timer t_client = Timer.createTimer();

    private void send_one(boolean is_server, int size) throws IOException {
        writeMessage = sport.newMessage();

        if (!EMPTY_REPLY || !is_server) {
            if (single_object != null) {
                // System.err.println("Send single object " + single_object);
                writeMessage.writeObject(single_object);
            } else if (size > 0) {
                switch (data_type) {
                case DATA_BYTES:
                    // System.err.println(rank + ": writeArray[" + byte_buffer.length + "] writeMessage " + writeMessage);
                    writeMessage.writeArray(byte_buffer);
                    break;
                case DATA_SHORTS:
                    writeMessage.writeArray(short_buffer);
                    break;
                case DATA_INTS:
                    writeMessage.writeArray(int_buffer);
                    break;
                case DATA_LONGS:
                    writeMessage.writeArray(long_buffer);
                    break;
                case DATA_FLOATS:
                    writeMessage.writeArray(float_buffer);
                    break;
                case DATA_DOUBLES:
                    writeMessage.writeArray(double_buffer);
                    break;
                case DATA_VBFField:
                    writeMessage.writeArray(VBFField_buffer);
                    break;
                case DATA_OBJ_1:
                case DATA_OBJ_2:
                case DATA_HASH:
                case DATA_INNER:
                    writeMessage.writeObject(object_buffer);
                    break;
                }
            }
        }

        // t_s_finish.start();
        writeMessage.finish();
        // t_s_finish.stop();
        // System.err.println("^" + size + " data_type " + data_type + " data " + (byte_buffer == null ? "<nope>" : Integer.toString(byte_buffer.length)));
        // System.err.print("^");
    }

    private void send_one(boolean is_server) throws IOException {
        send_one(is_server, size);
    }

    private void rcve_one(boolean read_data, int size, ReadMessage m)
            throws IOException, ClassNotFoundException {
        // t_rcve.start();
        if (!EMPTY_REPLY || read_data) {
            if (single_object != null) {
                single_object = m.readObject();
                // System.err.println("Receive single_object " + single_object);
            } else if (size > 0) {
                switch (data_type) {
                case DATA_BYTES:
                    m.readArray(byte_buffer);
                    break;
                case DATA_SHORTS:
                    m.readArray(short_buffer);
                    break;
                case DATA_INTS:
                    m.readArray(int_buffer);
                    break;
                case DATA_LONGS:
                    m.readArray(long_buffer);
                    break;
                case DATA_FLOATS:
                    m.readArray(float_buffer);
                    break;
                case DATA_DOUBLES:
                    m.readArray(double_buffer);
                    break;
                case DATA_VBFField:
                    m.readArray(VBFField_buffer);
                    break;
                case DATA_OBJ_1:
                case DATA_OBJ_2:
                case DATA_HASH:
                case DATA_INNER:
                    object_buffer = (Object[]) m.readObject();
                    break;
                }
            }
        }
        // t_rcve.stop();
        // System.err.print("v");
        // System.err.print(rank + " ");
    }

    private void rcve_one(boolean read_data, int partners, int size)
            throws IOException, ClassNotFoundException {
        ReadMessage m = null;
        for (int i = 0; i < partners; i++) {
            // t_get_msg.start();
            // System.err.println("Do a downcall receive from partner " + i);
            m = rport.receive();
            // t_get_msg.stop();
            rcve_one(read_data, size, m);
            m.finish();
            // System.err.println("Done a downcall receive from partner " + i);
        }
    }

    private void rcve_one(boolean read_data, int partners) throws IOException,
            ClassNotFoundException {
        rcve_one(read_data, partners, size);
    }

    private void send(int partners, int count) throws IOException,
            ClassNotFoundException {

        if (count == 0) {
            return;
        }

        if (one_way) {
            for (int i = 0; i < count; i++) {
                for (int k = 0; k < client_spin; k++) {
                    int r = rand.nextInt();
                }
                send_one(false /* Not is_server */);
                // System.err.print(".");
                // System.err.print(i + " ");
            }
            rcve_one(false /* Not read_data */, partners, 0);
        } else {

            ibis.util.Timer t = null;
            ibis.util.Timer sum = null;
            if (VARIANCE_TIMER) {
                t = ibis.util.Timer.createTimer("ibis.util.nativeCode.Rdtsc");
                sum = ibis.util.Timer.createTimer("ibis.util.nativeCode.Rdtsc");
            }

            for (int i = 0; i < count; i++) {
                for (int k = 0; k < client_spin; k++) {
                    int r = rand.nextInt();
                }
                if (VARIANCE_TIMER) {
                    t.reset();
                    t.start();
                }
                send_one(false /* Not is_server */);
                rcve_one(false /* Not read_data */, partners);
                if (VARIANCE_TIMER) {
                    t.stop();
                    double tNow = t.totalTimeVal();
                    double tAv = sum.averageTimeVal();
                    if (i > 0 && tNow > 10 * tAv) {
                        System.err.println("Now see " + tNow + "; av " + tAv);
                    } else {
                        sum.add(t);
                    }
                }
                // System.err.print(".");
            }
            if (VARIANCE_TIMER) {
                System.err.println("av " + sum.averageTimeVal());
            }
        }
    }

    private void serve(int partners, int count) throws IOException,
            ClassNotFoundException {

        if (count == 0) {
            return;
        }

        if (one_way) {
            for (int i = 0; i < count; i++) {
                rcve_one(true /* read_data */, partners);
                for (int k = 0; k < server_spin; k++) {
                    int r = rand.nextInt();
                }
            }
            send_one(true /* is_server */, 0);
        } else {
            for (int i = 0; i < count; i++) {
                // System.err.println("Server wants to receive message " + i + " out of " + count);
                rcve_one(true /* read_data */, partners);
                // System.err.print(":");
                for (int k = 0; k < server_spin; k++) {
                    int r = rand.nextInt();
                }
                if (clients == 1 || (i + 1) % clients == 0) {
                    // System.err.println("Server want to send reply " + i + " out of " + count);
                    send_one(true /* is_server */);
                }
                if (gc_on_rcve) {
                    object_buffer = null;
                    System.gc();
                }
            }
        }
    }

    public void upcall(ReadMessage m) throws IOException {
        try {
            // System.err.println(rank + ": upcall");
            // t_upcall.start();
            if (consume) {
                rcve_one(true /* read_data */, size, m);
            }
            // System.err.println(rank + ": received msg");
            if (!one_way) {
                if (clients == 1 || (services + 1) % clients == 0) {
                    send_one(true /* is_server */);
                    // System.err.println(rank + ": sent ack");
                }
            }
            if (gc_on_rcve) {
                object_buffer = null;
                System.gc();
            }

            if (finish_upcall_msg) {
                // t_r_finish.start();
                m.finish();
                // t_r_finish.stop();
            }
            // System.err.print("+");
            // System.err.print(services + " ");

            // Can we lift the synchronized from the increment?
            // After all, the implementation will surely do
            // synchronized()
            // ???????????????
            // t_upcall.stop();
            services++;

            if (services == first_warmup * clients) {
                System.err.println("services " + services + " first_warmup "
                        + first_warmup + " count " + count + " clients "
                        + clients);
                synchronized (this) {
                    first_warmup = 0;
                    notify();
                }
            }

            if (services == count * clients) {
                synchronized (this) {
                    notify();
                }
            }

            for (int k = 0; k < server_spin; k++) {
                int r = rand.nextInt();
            }

            /*
             } catch (IOException e) {
             System.err.println("upcall catches exception " + e);
             e.printStackTrace();
             */

        } catch (ClassNotFoundException ec) {
            System.err.println("upcall catches exception " + ec);
            ec.printStackTrace(System.err);
            System.exit(177);
        }
    }

    private void client() throws IOException, ClassNotFoundException {
        if (warmup > 0) {
            System.err.println("Do warmup:      " + warmup + " calls");
            // warmup
        }
        send(servers, warmup);

        System.err.println("Do measurement: " + count + " calls");

        if (myIbis instanceof ibis.impl.messagePassing.Ibis) {
            ibis.impl.messagePassing.Ibis.resetStats();
        }
        System.gc();
        // t_client.start();
        long time = System.currentTimeMillis();

        // test
        send(servers, count);

        time = System.currentTimeMillis() - time;
        // t_client.stop();

        double speed = (time * 1000.0) / (double) count;

        System.err
                .println("Latency: " + count + " calls took "
                        + ((double) time / 1000.0) + " s, time/call = " + speed
                        + " us");
        if (size > 0) {
            final double MB = 1048576.0;
            double thrp = (size * data_size[data_type] * count) / MB
                    / (time / 1000.0);

            if (!EMPTY_REPLY && !one_way) {
                thrp *= 2;
            }
            System.err.println("Throughput " + thrp + " MB/s");
        }
    }

    private class BusyThread extends Thread {

        boolean started;

        public void run() {
            synchronized (this) {
                started = true;
                notifyAll();
            }
            while (true) {
                // spin ahead
            }
        }

    }

    private void startBusyThread() {
        if (busy) {
            BusyThread bt = new BusyThread();
            bt.setDaemon(true);
            bt.start();
            synchronized (bt) {
                while (!bt.started) {
                    try {
                        bt.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    private void server() throws IOException, ClassNotFoundException {
        System.err.println(rank + ": Start service, warmup " + warmup
                + " msgs " + count);

        startBusyThread();

        if (upcall) {

            synchronized (this) {
                while (services < warmup * clients) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                System.err.println(rank + ": Server: seen " + services
                        + " msgs for warmup");
                services -= warmup;
            }
            if (one_way) {
                send_one(true /* is_server */, 0);
            }

            // myIbis.disableResizeUpcalls();
            if (myIbis instanceof ibis.impl.messagePassing.Ibis) {
                ibis.impl.messagePassing.Ibis.resetStats();
            }
            System.gc();
            // myIbis.enableResizeUpcalls();

            // test
            synchronized (this) {
                while (services < count * clients) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            System.err.println(rank + ": Server: seen " + services + " msgs");
            if (one_way) {
                send_one(true /* is_server */, 0);
            }

        } else {
            // warmup
            serve(clients, warmup);
            if (myIbis instanceof ibis.impl.messagePassing.Ibis) {
                ibis.impl.messagePassing.Ibis.resetStats();
            }

            // test
            serve(clients, count);
        }

        // I am afraid Net is so fast closing the connection via
        // the ConnectionListener, that we better keep the channel open
        // for a while in the hopes that the client can process our
        // last answer
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void runClient() throws IOException, ClassNotFoundException {
        System.err.println(rank + ": I am " + rank + " -- client; clients "
                + clients + " servers " + servers
                + "; client port name \"client port " + rank + "\"");

        if (connectUpcalls) {
            sport = requestPortType.createSendPort("latency-client",
                    (SendPortConnectUpcall) this);
        } else {
            sport = requestPortType.createSendPort("latency-client");
        }
        if (BUFSIZ != 0) {
            HashMap dp = new HashMap();
            dp.put("InputBufferSize", new Integer(BUFSIZ));
            dp.put("OutputBufferSize", new Integer(BUFSIZ));
            sport.setProperties(dp);
        }

        myIbis.enableResizeUpcalls();

        if (connectUpcalls) {
            rport = replyPortType.createReceivePort("client port " + rank,
                    (ReceivePortConnectUpcall) this);
        } else {
            rport = replyPortType.createReceivePort("client port " + rank);
        }
        // System.err.println(rank + ": t = " + ((ibis.impl.net.NetIbis)myIbis).now() + "  created \"client port " + rank + "\"");

        if (BUFSIZ != 0) {
            HashMap dp = new HashMap();
            dp.put("InputBufferSize", new Integer(BUFSIZ));
            dp.put("OutputBufferSize", new Integer(BUFSIZ));
            rport.setProperties(dp);
        }
        rport.enableConnections();

        for (int i = 0; i < servers; i++) {
            // System.err.println(rank + ": t = " + ((ibis.impl.net.NetIbis)myIbis).now() + " lookup \"server port " + i + "\"");
            ReceivePortIdentifier rp = registry
                    .lookupReceivePort("server port " + i);
            sport.connect(rp);
            // System.err.println(rank + ": t = " + ((ibis.impl.net.NetIbis)myIbis).now() + " connected to \"server port " + i + "\"");
        }

        System.err.println(rank + ": client: connected");

        // Do a poor-man's barrier to allow the connections to proceed.
        for (int i = 0; i < servers; i++) {
            System.err.println(rank + ": Poor-man's barrier " + i
                    + " receive start...");
            ReadMessage r = rport.receive();
            r.finish();
            System.err.println(rank + ": Poor-man's barrier " + i
                    + " receive finished");
        }

        System.err.println("Go ahead now!");

        client();

        report();

        /* free the send ports first */
        if (closePortsAtExit) {
            sport.close();
            System.err.println("Freed my send port");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Try again
            }
            rport.close();
            System.err.println("Freed my rcve port");
        }
    }

    private void runServer() throws IOException, ClassNotFoundException {
        System.err
                .println(rank + ": I am " + rank + " -- server; clients "
                        + clients + " servers " + servers
                        + "; server port name \"server port "
                        + (rank - clients) + "\"");

        if (connectUpcalls) {
            sport = replyPortType.createSendPort("latency-server",
                    (SendPortConnectUpcall) this);
        } else {
            sport = replyPortType.createSendPort("latency-server");
        }

        if (BUFSIZ != 0) {
            HashMap dp = new HashMap();
            dp.put("InputBufferSize", new Integer(BUFSIZ));
            dp.put("OutputBufferSize", new Integer(BUFSIZ));
            sport.setProperties(dp);
        }

        myIbis.enableResizeUpcalls();

        if (upcall) {
            if (connectUpcalls) {
                rport = requestPortType.createReceivePort("server port "
                        + (rank - clients), this,
                        (ReceivePortConnectUpcall) this);
            } else {
                rport = requestPortType.createReceivePort("server port "
                        + (rank - clients), (Upcall) this);
            }
        } else {
            if (connectUpcalls) {
                rport = requestPortType.createReceivePort("server port "
                        + (rank - clients), (ReceivePortConnectUpcall) this);
            } else {
                rport = requestPortType.createReceivePort("server port "
                        + (rank - clients));
            }
        }
        // System.err.println(rank + ": created \"server port " + (rank - clients) + "\"");
        if (BUFSIZ != 0) {
            HashMap dp = new HashMap();
            dp.put("InputBufferSize", new Integer(BUFSIZ));
            dp.put("OutputBufferSize", new Integer(BUFSIZ));
            rport.setProperties(dp);
        }

        rport.enableConnections();

        for (int i = 0; i < clients; i++) {
            ReceivePortIdentifier rp = registry
                    .lookupReceivePort("client port " + i);
            // System.err.println(rank + ": t = " + ((ibis.impl.net.NetIbis)myIbis).now() + " connect to \"client port " + i + "\"");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Give up
            }
            sport.connect(rp);
            // System.err.println(rank + ": t = " + ((ibis.impl.net.NetIbis)myIbis).now() + " Server: connected to \"client port " + i + "\"");
        }

        // Do a poor-man's barrier to allow the connections to proceed.
        WriteMessage w = sport.newMessage();
        w.finish();
        System.err.println(rank + ": Poor-man's barrier send finished");

        System.err.println("Go ahead now!");

        if (upcall) {
            rport.enableUpcalls();
        }
        server();

        report();

        /* free the send ports first */
        if (closePortsAtExit) {
            sport.close();
            System.err.println("Freed my send port");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Try again
            }
            rport.close();
            System.err.println("Freed my rcve port");
        }
    }

    private void parseArgs(String[] args) {
        int options = 0;

        for (int i = 0; i < args.length; i++) {
            // System.err.println("Now inspect option[" + i + "] = " + args[i]);
            if (false) {

            } else if (args[i].equals("-no-upcall")
                    || args[i].equals("-downcall")) {
                upcall = false;
            } else if (args[i].equals("-upcall")) {
                upcall = true;

            } else if (args[i].equals("-one-way")) {
                one_way = true;

                /* Consume is final:
                 } else if (args[i].equals("-no-consume")) {
                 consume = false;
                 } else if (args[i].equals("-consume")) {
                 consume = true;
                 * Consume is final */

            } else if (args[i].equals("-connect-upcall")) {
                connectUpcalls = true;

            } else if (args[i].equals("-no-connect-upcall")) {
                connectUpcalls = false;

            } else if (args[i].equals("-gc")) {
                gc_on_rcve = true;

            } else if (args[i].equals("-servers")) {
                servers = Integer.parseInt(args[++i]);

            } else if (args[i].equals("-clients")) {
                clients = Integer.parseInt(args[++i]);

            } else if (args[i].equals("-bcast")) {
                bcast = true;

            } else if (args[i].equals("-bcast-all")) {
                bcast_all = true;

            } else if (args[i].equals("-warmup")) {
                warmup = Integer.parseInt(args[++i]);

            } else if (args[i].equals("-client-spin")) {
                client_spin = Integer.parseInt(args[++i]);

            } else if (args[i].equals("-server-spin")) {
                server_spin = Integer.parseInt(args[++i]);

            } else if (args[i].equals("-finish")
                    || args[i].equals("-upcall-finish")) {
                finish_upcall_msg = true;
            } else if (args[i].equals("-no-finish")
                    || args[i].equals("-no-upcall-finish")) {
                finish_upcall_msg = false;

            } else if (args[i].equals("-busy")) {
                busy = true;
            } else if (args[i].equals("-sequenced")) {
                sequenced = true;

            } else if (args[i].equals("-n2n")) {
                requireOneToMany = true;

            } else if (args[i].equals("-byte")) {
                data_type = DATA_BYTES;
            } else if (args[i].equals("-short")) {
                data_type = DATA_SHORTS;
            } else if (args[i].equals("-int")) {
                data_type = DATA_INTS;
            } else if (args[i].equals("-long")) {
                data_type = DATA_LONGS;
            } else if (args[i].equals("-float")) {
                data_type = DATA_FLOATS;
            } else if (args[i].equals("-double")) {
                data_type = DATA_DOUBLES;
            } else if (args[i].equals("-obj1")) {
                data_type = DATA_OBJ_1;
            } else if (args[i].equals("-obj2")) {
                data_type = DATA_OBJ_2;
            } else if (args[i].equals("-inner")) {
                data_type = DATA_INNER;
            } else if (args[i].equals("-tree")) {
                data_type = DATA_OBJ_2;
            } else if (args[i].equals("-hash")) {
                data_type = DATA_HASH;

            } else if (args[i].equals("-VBFField")) {
                data_type = DATA_VBFField;

            } else if (options == 0) {
                count = Integer.parseInt(args[i]);
                options++;

            } else if (options == 1) {
                size = Integer.parseInt(args[i]);
                options++;

            } else {
                System.err.println("Unknown option: " + args[i] + " -- abort");
                System.exit(33);
            }
        }

        // System.err.println("finish_upcall_msg = " + finish_upcall_msg);

        if (options < 1) {
            System.err.println("Usage: Latency <count>");
            System.exit(33);
        }

        if (warmup == -1) {
            warmup = count;
        }
        first_warmup = warmup;
    }

    private void parseDataType() {
        if (size == 1
                && (data_type == DATA_OBJ_1 || data_type == DATA_OBJ_2 || data_type == DATA_INNER)) {
            if (data_type == DATA_OBJ_1) {
                single_object = new Data1();
            } else if (data_type == DATA_OBJ_2) {
                single_object = new Data2(1);
            } else if (data_type == DATA_INNER) {
                single_object = new WithInner();
            }
        } else if (size > 0) {

            if (data_type != DATA_OBJ_1 && data_type != DATA_OBJ_2
                    && data_type != DATA_INNER && data_type != DATA_HASH) {
                size /= data_size[data_type];
            }

            switch (data_type) {
            case DATA_BYTES:
                byte_buffer = new byte[size];
                break;
            case DATA_SHORTS:
                short_buffer = new short[size];
                break;
            case DATA_INTS:
                int_buffer = new int[size];
                break;
            case DATA_LONGS:
                long_buffer = new long[size];
                break;
            case DATA_FLOATS:
                float_buffer = new float[size];
                break;
            case DATA_DOUBLES:
                double_buffer = new double[size];
                System.err.println("Allocated double buffer size " + size);
                break;
            case DATA_VBFField:
                VBFField_buffer = new VBFField[size];
                for (int i = 0; i < size; i++) {
                    VBFField_buffer[i] = new VBFField();
                }
                break;
            case DATA_OBJ_1:
                object_buffer = new Data1[size];
                for (int i = 0; i < size; i++) {
                    object_buffer[i] = new Data1();
                }
                break;
            case DATA_OBJ_2:
                single_object = new Data2(size);
                break;
            case DATA_HASH:
                Hashtable h = new Hashtable();
                for (int i = 0; i < size; i++) {
                    h.put(new Integer(i), new Object());
                }
                single_object = h;
                break;
            case DATA_INNER:
                object_buffer = new WithInner[size];
                for (int i = 0; i < size; i++) {
                    object_buffer[i] = new WithInner();
                }
                break;
            }
        }

        if (data_type == DATA_OBJ_2) {
            // System.err.println(single_object);
        }
    }

    private void parseIbisName() throws IOException, IbisException {
        /* Parse commandline. */

        PoolInfo info = PoolInfo.createPoolInfo();
        ncpus = info.size();
        rank = info.rank();

        if (ncpus == 0) {
            System.err.println("PoolInfo.size() should be > 0");
            System.exit(41);
        } else if (bcast) {
            if (clients != -1 || servers != -1) {
                System.err
                        .println("Cannot both specify -bcast[-all] and -cliets/-servers");
                System.exit(33);
            }
            clients = 1;
            servers = ncpus - 1;
        } else if (bcast_all) {
            if (clients != -1 || servers != -1) {
                System.err
                        .println("Cannot both specify -bcast[-all] and -cliets/-servers");
                System.exit(33);
            }
            clients = 1;
            servers = ncpus;
        }
        if (clients == -1) {
            if (ncpus == 1) {
                clients = 1;
            } else {
                clients = ncpus - 1;
            }
        }
        if (servers == -1) {
            servers = 1;
        }

        requireOneToMany = requireOneToMany || bcast || bcast_all
                || clients > 1 || servers > 1;
    }

    private void createIbis() throws IOException, IbisException {

        if (USE_RESIZEHANDLER || rank == -1) {
            rszHandler = new RszHandler();
        }

        java.util.Random random = new java.util.Random();
        String hostName = "localhost";
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            hostName = addr.getHostName();
        } catch (java.net.UnknownHostException e) {
            // let it be the default
        }
        String basicProps = "OneToOne Reliable AutoUpcalls ExplicitReceipt";
        String requestProps = "";
        String replyProps = "";
        if (requireOneToMany) {
            requestProps += " OneToMany";
            replyProps += " ManyToOne";
            if (rank == 0) {
                System.err.println("Require a multicast/multireceive PortType");
            }
        }
        if (sequenced) {
            requestProps += " Numbered";
        }
        StaticProperties s = new StaticProperties();
        s.add("communication", basicProps + requestProps + replyProps);
        s.add("serialization", "object");
        myIbis = Ibis.createIbis(s, rszHandler);

        // myIbis.init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {

                try {
                    myIbis.end();
                } catch (IOException e) {
                    // Too late to do anything about it
                }
                System.err.println("Ended Ibis");
            }
        });

        s = new StaticProperties();
        s.add("communication", basicProps + requestProps);
        requestPortType = myIbis.createPortType("request port type", s);
        s = new StaticProperties();
        s.add("communication", basicProps + replyProps);
        replyPortType = myIbis.createPortType("reply port type", s);

        if (rank == -1 && rszHandler != null) {
            rszHandler.sync(clients + servers);
            rank = rszHandler.idents.indexOf(myIbis.identifier());
        }
    }

    private void registerIbis() throws IOException, IbisException {
        registry = myIbis.registry();
    }

    public void run() {

        try {

            if (rank != -1) {

            } else if (rszHandler != null) {
                rszHandler.sync(clients + servers);
                rank = rszHandler.idents.indexOf(myIbis.identifier());

            } else {
                System.err.println("Going to find out my rank, my ID is "
                        + myIbis.identifier().toString() + " name = "
                        + myIbis.identifier() + "; I am "
                        + (i_am_client ? "client" : "server"));

                IbisIdentifier master = registry.elect("RPC");
                // System.err.println("Election master id=" + master + " name=" + master);
                // System.err.println("Election contender = " + myIbis + " id=" + myIbis.identifier() + " name=" + myIbis.identifier());
                if (master.equals(myIbis.identifier())) {
                    // System.err.println("YES--------- I'm the winner, master = " + master + "; me " + myIbis.identifier());
                    rank = 0;
                } else {
                    rank = 1;
                }
            }

            if (rank == 0) {
                System.err.println("RPC: ****** Upcall server is now default");
            }

            if (!i_am_client) {
                runServer();
            } else {
                runClient();
            }
            System.err.println(rank + ": call it quits...; I am "
                    + (i_am_client ? "" : "not ") + "a client\n");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Try again
            }

        } catch (IOException e) {
            System.out.println(rank + ": Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();

        } catch (ClassNotFoundException e) {
            System.out.println(rank + ": Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }

    private void report() {
        // System.err.println("Timer: t_send     " + t_send.totalTime() + " / " + t_send.nrTimes() + " = " + t_send.averageTimeVal());
        // System.err.println("Timer: t_s_finish " + t_s_finish.totalTime() + " / " + t_s_finish.nrTimes() + " = " + t_s_finish.averageTimeVal());
        // System.err.println("Timer: t_get_msg  " + t_get_msg.totalTime() + " / " + t_get_msg.nrTimes() + " = " + t_get_msg.averageTimeVal());
        // System.err.println("Timer: t_rcve     " + t_rcve.totalTime() + " / " + t_rcve.nrTimes() + " = " + t_rcve.averageTimeVal());
        // System.err.println("Timer: t_r_finish " + t_r_finish.totalTime() + " / " + t_r_finish.nrTimes() + " = " + t_r_finish.averageTimeVal());
        // System.err.println("Timer: t_upcall   " + t_upcall.totalTime() + " / " + t_upcall.nrTimes() + " = " + t_upcall.averageTimeVal());
        // System.err.println("Timer: t_client   " + t_client.totalTime() + " / " + t_client.nrTimes() + " = " + t_client.averageTimeVal());
    }

    RPC(String[] args, RPC client) {
        try {

            parseArgs(args);
            parseIbisName();
            parseDataType();

            if (client == null) {
                createIbis();
                // Timer timer = Timer.createTimer();
                // timer.start();
                registerIbis();
                if (servers == ncpus && rank < clients) {
                    System.err.println("Kick-force server run; my rank " + rank
                            + " ncpus " + ncpus + " clients " + clients
                            + " servers " + servers);
                    RPC dl = new RPC(args, this);
                    Thread server = new Thread(dl);
                    server.setName("RPC server");
                    server.start();
                    i_am_client = true;
                } else {
                    i_am_client = rank < clients;
                }
                System.err.println(rank + ": Regular run; rank " + rank
                        + " ncpus " + ncpus + " clients " + clients
                        + " servers " + servers);
                run();
                // timer.stop();
                // System.err.println("Nano timering -> " + timer.totalTimeVal());
                System.exit(0);
            } else {
                i_am_client = false;
                rank = ncpus;
                clients = 1;
                System.err.println(rank + ": Forced server run; rank " + rank
                        + " ncpus " + ncpus + " clients " + clients
                        + " servers " + servers);
                myIbis = client.myIbis;
                this.rszHandler = client.rszHandler;
                this.requestPortType = client.requestPortType;
                this.replyPortType = client.replyPortType;
                registerIbis();
            }

        } catch (IOException e) {
            System.err.println("Top-level exception " + e);

        } catch (IbisException e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }

    RPC(String[] args) {
        this(args, null);
    }

    /**
     * Interface ReceivePortConnectUpcall
     */
    public boolean gotConnection(ReceivePort rp, SendPortIdentifier sp) {
        System.err.println("Got connection with send port " + sp);
        return true;
    }

    public void lostConnection(ReceivePort rp, SendPortIdentifier sp,
            Exception e) {
        System.err.println("Lost connection with send port " + sp + " threw "
                + e);
    }

    /**
     * Interface SendPortConnectUpcall
     */
    public void lostConnection(SendPort sp, ReceivePortIdentifier rp,
            Exception e) {
        System.err.println("Lost connection with receive port " + rp
                + " threw " + e);
    }

    public static void main(String[] args) {
        new RPC(args);
    }
}