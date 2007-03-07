/* $Id$ */

package ibis.impl.registry.tcp;

import ibis.io.Conversion;
import ibis.impl.registry.RegistryProperties;
import ibis.impl.registry.Server;
import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.util.IPUtils;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class NameServer extends Server implements Protocol, Runnable {

    public static final int CONNECTION_BACKLOG = 256;

    static final Logger logger = Logger.getLogger(NameServer.class);

    /**
     * The <code>Sequencer</code> class provides a global numbering. This can
     * be used, for instance, for global ordering of messages. A sender must
     * then first obtain a sequence number from the sequencer, and tag the
     * message with it. The receiver must then handle the messages in the "tag"
     * order.
     * <p>
     * A Sequencer associates a numbering scheme with a name, so the user can
     * associate different sequences with different names.
     */
    private static class Sequencer {
        private HashMap<String, LongObject> counters;

        private static class LongObject {
            long val;

            LongObject(long v) {
                val = v;
            }

            public String toString() {
                return "" + val;
            }
        }

        Sequencer() {
            counters = new HashMap<String, LongObject>();
        }

        /**
         * Returns the next sequence number associated with the specified name.
         * 
         * @param name
         *            the name of the sequence.
         * @return the next sequence number
         */
        public synchronized long getSeqno(String name) {
            LongObject i = counters.get(name);
            if (i == null) {
                i = new LongObject(ibis.ipl.ReadMessage.INITIAL_SEQNO);
                counters.put(name, i);
            }
            return i.val++;
        }

        public String toString() {
            return "" + counters;
        }
    }

    static InetAddress myAddress;
    static {
        myAddress = IPUtils.getAlternateLocalHostAddress();
        try {
            myAddress = InetAddress.getByName(myAddress.getHostName());
        } catch (Exception e) {
            logger.warn("Got exception in initializer", e);
        }
    }

    static class IbisInfo {
        int ibisNameServerport;

        InetAddress ibisNameServerAddress;

        boolean needsUpcalls;

        boolean completelyJoined = false;

        IbisIdentifier id;

        IbisInfo(InetAddress ibisNameServerAddress, int ibisNameServerport,
                boolean needsUpcalls, RunInfo p, byte[] data,
                Location location, String poolId) throws IOException {
            this.ibisNameServerAddress = ibisNameServerAddress;
            this.ibisNameServerport = ibisNameServerport;
            this.needsUpcalls = needsUpcalls;
            synchronized (p) {
                id = new IbisIdentifier(Integer.toString(p.joinCount++), data,
                        null, location, poolId);
            }
        }

        public boolean equals(Object other) {
            if (other instanceof IbisInfo) {
                return id.equals(((IbisInfo) other).id);
            }
            return false;
        }

        public int hashCode() {
            return id.hashCode();
        }

        public String toString() {
            return "ibisInfo(" + id + " at " + ibisNameServerAddress + ":"
                    + ibisNameServerport + ")";
        }
    }

    static class PingerEntry {
        String poolId;

        String id;

        PingerEntry(String poolId, String id) {
            this.poolId = poolId;
            this.id = id;
        }

        boolean largerOrEqual(PingerEntry e) {
            if (poolId == null) {
                // poolId == null means: ping everything.
                return true;
            }
            if (e.poolId == null) {
                return false;
            }
            if (!poolId.equals(e.poolId)) {
                // unrelated.
                return false;
            }

            if (id == null) {
                // Same poolId, so this one pings whole pool.
                return true;
            }

            return false;
        }
    }

    static class DeadNotifier implements Runnable {
        ArrayList<IbisIdentifier[]> corpses = new ArrayList<IbisIdentifier[]>();

        final RunInfo runInfo;

        boolean done = false;

        int count = 0;

        final int port;

        final InetAddress addr;

        final byte message;

        final int connectTimeout;

        DeadNotifier(RunInfo r, InetAddress a, int p, byte m, int connectTimeout) {
            runInfo = r;
            port = p;
            addr = a;
            message = m;
            this.connectTimeout = connectTimeout;
        }

        synchronized void addCorpses(IbisIdentifier[] ids) {
            if (ids.length == 0) {
                return;
            }
            corpses.add(ids);
            count += ids.length;
            notifyAll();
        }

        synchronized void quit() {
            done = true;
            notifyAll();
        }

        public void run() {
            for (;;) {
                IbisIdentifier[] deadOnes = null;
                synchronized (this) {
                    while (!done && count == 0) {
                        try {
                            this.wait();
                        } catch (Exception e) {
                            // ignored
                        }
                    }
                    if (count > 0) {
                        deadOnes = new IbisIdentifier[count];
                        int i = 0;
                        while (corpses.size() > 0) {
                            IbisIdentifier[] el = corpses.remove(0);
                            for (int j = 0; j < el.length; j++) {
                                deadOnes[i] = el[j];
                                i++;
                            }
                        }
                        count = 0;
                    }
                }
                if (deadOnes != null) {
                    try {
                        send(deadOnes);
                    } catch (Exception e) {
                        // ignored
                    }
                }
                synchronized (this) {
                    if (count != 0) {
                        continue;
                    }
                    if (done) {
                        return;
                    }
                }
            }
        }

        private void send(IbisIdentifier[] ids) throws IOException {
            Socket s = null;
            DataOutputStream out2 = null;

            try {
                s = createClientSocket(new InetSocketAddress(addr, port),
                        connectTimeout);
                out2 = new DataOutputStream(new BufferedOutputStream(s
                        .getOutputStream()));
                out2.writeByte(message);
                out2.writeInt(ids.length);
                for (int i = 0; i < ids.length; i++) {
                    ids[i].writeTo(out2);
                }
            } finally {
                closeConnection(null, out2, s);
            }
        }
    }

    static Socket createClientSocket(InetSocketAddress remote, int timeout)
            throws IOException {
        Socket s = new Socket();
        s.bind(null);
        s.connect(remote, timeout);
        s.setTcpNoDelay(true);
        return s;
    }

    static class RunInfo {
        ArrayList<IbisInfo> unfinishedJoins; // a list of IbisInfos

        ArrayList<IbisInfo> arrayPool; // IbisInfos in fixed order.

        Hashtable<String, IbisInfo> pool;

        ArrayList<IbisInfo> leavers;

        int forwarders;

        int pingers;

        int failed;

        ElectionServer electionServer;

        DeadNotifier electionKiller;

        long pingLimit;

        int joinCount = 0;

        RunInfo(int connectTimeout, int pingerTimeout) throws IOException {
            unfinishedJoins = new ArrayList<IbisInfo>();
            arrayPool = new ArrayList<IbisInfo>();
            pool = new Hashtable<String, IbisInfo>();
            leavers = new ArrayList<IbisInfo>();
            electionServer = new ElectionServer();
            electionKiller = new DeadNotifier(this, myAddress, electionServer
                    .getPort(), ELECTION_KILL, connectTimeout);
            ThreadPool.createNew(electionKiller, "ElectionKiller");
            pingLimit = System.currentTimeMillis() + pingerTimeout;
        }

        public String toString() {
            String res = "runinfo:\n" + "  pool = \n";
            IbisInfo[] elts = instances();

            for (int i = 0; i < elts.length; i++) {
                res += "    " + elts[i] + "\n";
            }

            return res;
        }

        public IbisInfo[] instances() {
            return pool.values().toArray(new IbisInfo[pool.size()]);
        }

        public void remove(IbisInfo iinf) {
            pool.remove(iinf.id.myId);
            if (!iinf.completelyJoined) {
                int index = unfinishedJoins.indexOf(iinf);
                if (index == -1) {
                    logger
                            .error("Internal error: "
                                    + iinf.id.myId
                                    + " not completelyJoined but not in unfinishedJoins!");
                } else {
                    unfinishedJoins.remove(index);
                }
            }
        }
    }

    private final int port;

    private final int pingerTimeout;

    private final int connectTimeout;

    private final int joinerInterval;

    private final int checkerInterval;

    private final int maxThreads;

    private final Hashtable<String, RunInfo> pools;

    private final ArrayList<PingerEntry> pingerEntries;

    private final ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private final boolean singleRun;

    private boolean joined;

    private final Sequencer seq;

    static class CloseJob {
        DataInputStream in;

        DataOutputStream out;

        ByteArrayOutputStream baos;

        Socket s;

        long startTime;

        int opcode;

        long myStartTime = System.currentTimeMillis();

        CloseJob(DataInputStream in, DataOutputStream out,
                ByteArrayOutputStream baos, Socket s, int opcode, long start) {
            this.in = in;
            this.out = out;
            this.baos = baos;
            this.s = s;
            this.opcode = opcode;
            this.startTime = start;
        }

        void close() {
            try {
                out.flush();
                baos.writeTo(s.getOutputStream());
                closeConnection(in, out, s);
            } catch (Exception e) {
                logger.error("Exception in close", e);
            }
            if (logger.isInfoEnabled() && opcode >= 0) {
                String job = "unknown opcode " + opcode;
                switch (opcode) {
                case (IBIS_ISALIVE):
                    job = "ISALIVE";
                    break;
                case (IBIS_DEAD):
                    job = "DEAD";
                    break;
                case (IBIS_JOIN):
                    job = "JOIN";
                    break;
                case (IBIS_MUSTLEAVE):
                    job = "MUSTLEAVE";
                    break;
                case (IBIS_LEAVE):
                    job = "LEAVE";
                    break;
                case (IBIS_CHECK):
                    job = "CHECK";
                    break;
                case (IBIS_CHECKALL):
                    job = "CHECKALL";
                    break;
                default:
                    job = "unknown opcode " + opcode;
                    break;
                }

                long now = System.currentTimeMillis();

                logger.info("Request " + job + " took " + (now - startTime)
                        + " ms. -> " + "job took " + (myStartTime - startTime)
                        + " closing took " + (now - myStartTime));
            }
        }
    }

    static ArrayList<CloseJob> closeJobs = new ArrayList<CloseJob>();

    static int numClosers;

    private static class Closer implements Runnable {
        int maxThreads;

        Closer(int maxThreads) {
            this.maxThreads = maxThreads;
            numClosers++;
        }

        static void addJob(CloseJob cl) {
            synchronized (closeJobs) {
                closeJobs.add(cl);
                closeJobs.notify();
            }
        }

        public void run() {
            for (;;) {
                CloseJob cl;
                synchronized (closeJobs) {
                    while (closeJobs.size() == 0) {
                        if (numClosers > 1) {
                            numClosers--;
                            return;
                        }
                        try {
                            closeJobs.wait();
                        } catch (Exception e) {
                            // ignored
                        }
                    }
                    cl = closeJobs.remove(0);
                    if (numClosers < maxThreads && closeJobs.size() > 0) {
                        ThreadPool.createNew(new Closer(maxThreads), "Closer");
                    }

                }
                cl.close();
            }
        }
    }

    public NameServer(Properties properties) throws IOException {
        pingerEntries = new ArrayList<PingerEntry>();

        TypedProperties typedProperties = new TypedProperties(properties);

        port = typedProperties.getIntProperty(RegistryProperties.SERVER_PORT);

        pingerTimeout = typedProperties
                .getIntProperty(RegistryProperties.CLASSIC_PINGER_TIMEOUT);

        connectTimeout = typedProperties
                .getIntProperty(RegistryProperties.CLASSIC_CONNECT_TIMEOUT);

        joinerInterval = typedProperties
                .getIntProperty(RegistryProperties.CLASSIC_JOINER_INTERVAL);

        checkerInterval = typedProperties
                .getIntProperty(RegistryProperties.CLASSIC_CHECKER_INTERVAL);

        maxThreads = typedProperties
                .getIntProperty(RegistryProperties.CLASSIC_MAX_THREADS);

        singleRun = typedProperties
                .booleanProperty(RegistryProperties.SERVER_SINGLE);
        this.joined = false;

        seq = new Sequencer();

        logger.info("Creating nameserver on " + myAddress);

        if (checkerInterval != 0) {
            final PoolChecker ck = new PoolChecker(null, myAddress
                    .getHostName(), port, checkerInterval);
            Thread p = new Thread("PoolChecker Upcaller") {
                public void run() {
                    ck.run();
                }
            };
            p.setDaemon(true);
            p.start();
        }

        logger.info("NameServer: singleRun = " + singleRun);

        // Create a server socket.

        serverSocket = new ServerSocket(port, CONNECTION_BACKLOG);

        pools = new Hashtable<String, RunInfo>();

        Thread p = new Thread("NameServer Upcaller") {
            public void run() {
                upcaller();
            }
        };

        p.setDaemon(true);
        p.start();

        p = new Thread("Pinger Handler") {
            public void run() {
                pingRunner();
            }
        };

        p.setDaemon(true);
        p.start();

        logger.info("NameServer: created server on " + serverSocket);

    }

    // Should be called within synchronized on inf.
    private void sendLeavers(RunInfo inf) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("sendLeavers ... size = " + inf.leavers.size()
                    + ", forwarders = " + inf.forwarders);
        }

        IbisInfo[] leavers = null;

        if (inf.leavers.size() != 0) {
            leavers = inf.leavers.toArray(new IbisInfo[inf.leavers.size()]);
            inf.leavers.clear();

            // Obtain elements to send to first. The forward() method
            // may wait (and loose the lock).
            IbisInfo[] elts = inf.instances();
            for (int i = 0; i < elts.length; i++) {
                if (elts[i].needsUpcalls) {
                    forward(IBIS_LEAVE, inf, elts[i], leavers, 0);
                }
            }

            for (int i = 0; i < leavers.length; i++) {
                if (leavers[i].needsUpcalls) {
                    forward(IBIS_LEAVE, inf, leavers[i], leavers, 0);
                }
            }

            IbisIdentifier[] ids = new IbisIdentifier[leavers.length];

            for (int i = 0; i < leavers.length; i++) {
                ids[i] = leavers[i].id;
            }

            // Let the election server know about it.
            inf.electionKiller.addCorpses(ids);
        }

        // After sendLeavers finishes, forwarders should be 0, even if
        // this instance did not send anything! It may be called from
        // several threads, so even if this instance does not send anything,
        // there may still be work in progress. We wait for it to finish here.
        while (inf.forwarders != 0) {
            try {
                inf.wait();
            } catch (Exception ex) {
                // ignored
            }
        }
    }

    void pingRunner() {
        for (;;) {
            PingerEntry e = null;
            synchronized (pingerEntries) {
                while (pingerEntries.size() == 0) {
                    try {
                        pingerEntries.wait();
                    } catch (Exception ex) {
                        // ignored
                    }
                }
                e = pingerEntries.get(0);
            }
            if (e.poolId == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Doing full check");
                }
                poolPinger();
            } else if (e.id == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Doing check of pool " + e.poolId);
                }
                poolPinger(e.poolId);
            } else {
                RunInfo p = pools.get(e.poolId);
                if (p != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Doing check of ibis " + e.id);
                    }
                    checkPool(p, e.id, false, e.poolId);
                }
            }
            synchronized (pingerEntries) {
                // note: other threads may only replace/remove indices != 0.
                pingerEntries.remove(0);
            }
        }
    }

    void addPingerEntry(String poolId, String id) {

        PingerEntry added = new PingerEntry(poolId, id);
        boolean replaced = false;

        synchronized (pingerEntries) {
            // First check if this request is already present. If so,
            // just return. If a "larger" request is already present,
            // also just return.
            // Vice versa, if the request to be added is "larger" than
            // any request in the list, remove/replace the "smaller" requests.
            for (int i = 0; i < pingerEntries.size(); i++) {
                PingerEntry e = pingerEntries.get(i);
                if (e.largerOrEqual(added)) {
                    return;
                }
                if (added.largerOrEqual(e)) {
                    if (i == 0) {
                        continue;
                    }
                    if (!replaced) {
                        pingerEntries.set(i, added);
                        replaced = true;
                    } else {
                        pingerEntries.remove(i);
                        i--;
                    }
                }
            }
            if (!replaced) {
                pingerEntries.add(added);
                pingerEntries.notifyAll();
            }
        }
    }

    void upcaller() {
        for (;;) {
            try {
                Thread.sleep(joinerInterval);
            } catch (InterruptedException e) {
                // ignore
            }
            for (Enumeration<String> e = pools.keys(); e.hasMoreElements();) {
                String poolId = e.nextElement();
                RunInfo inf = pools.get(poolId);
                boolean joinFailed = false;

                synchronized (inf) {
                    try {
                        sendLeavers(inf);
                    } catch (IOException ex) {
                        logger.error("Got exception: " + ex);
                    }

                    if (inf.unfinishedJoins.size() > 0) {
                        inf.failed = 0;
                        IbisInfo[] message = inf.unfinishedJoins
                                .toArray(new IbisInfo[inf.unfinishedJoins
                                        .size()]);

                        inf.unfinishedJoins.clear();

                        // Obtain elements to send to first. The forward()
                        // method may wait (and loose the lock).
                        IbisInfo[] elts = inf.instances();
                        for (int i = 0; i < elts.length; i++) {
                            if (elts[i].completelyJoined
                                    && elts[i].needsUpcalls) {
                                forward(IBIS_JOIN, inf, elts[i], message, 0);
                            }
                        }

                        for (int i = 0; i < message.length; i++) {
                            IbisInfo ibisInf = message[i];
                            if (ibisInf.needsUpcalls && i + 1 < message.length) {
                                forward(IBIS_JOIN, inf, ibisInf, message, i + 1);
                            }
                            ibisInf.completelyJoined = true;
                        }

                        while (inf.forwarders != 0) {
                            try {
                                inf.wait();
                            } catch (Exception ex) {
                                // ignored
                            }
                        }
                        if (inf.failed != 0) {
                            joinFailed = true;
                        }
                    }
                    if (joinFailed) {
                        addPingerEntry(poolId, null);
                    }
                }
            }
        }
    }

    private class Forwarder implements Runnable {
        RunInfo inf;

        IbisInfo dest;

        IbisInfo info[];

        int offset;

        byte message;

        Forwarder(byte message, RunInfo inf, IbisInfo dest, IbisInfo[] info,
                int offset) {
            this.inf = inf;
            this.dest = dest;
            this.info = info;
            this.offset = offset;
            this.message = message;
        }

        private String type(int msg) {
            switch (msg) {
            case IBIS_DEAD:
                return "dead";
            case IBIS_LEAVE:
                return "leave";
            case IBIS_JOIN:
                return "join";
            case IBIS_MUSTLEAVE:
                return "mustLeave";
            default:
                return "unknown";
            }
        }

        public void run() {
            Socket s = null;
            DataOutputStream out2 = null;
            boolean failed = true;

            // QUICK HACK -- JASON
            for (int h = 0; h < 3; h++) {
                try {
                    s = createClientSocket(
                            new InetSocketAddress(dest.ibisNameServerAddress,
                                    dest.ibisNameServerport), connectTimeout);
                    out2 = new DataOutputStream(new BufferedOutputStream(s
                            .getOutputStream()));
                    out2.writeByte(message);
                    out2.writeInt(info.length - offset);
                    for (int i = offset; i < info.length; i++) {
                        info[i].id.writeTo(out2);

                        if (logger.isDebugEnabled()) {
                            logger.debug("NameServer: forwarding "
                                    + type(message) + " of " + info[i].id.myId
                                    + " to " + dest + " DONE");
                        }
                    }

                    failed = false;
                    closeConnection(null, out2, s);
                    break;
                } catch (Exception e) {
                    closeConnection(null, out2, s);
                    logger.error("Could not forward " + type(message) + " to "
                            + dest, e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException x) {
                        // ignore
                    }
                }
            }

            synchronized (inf) {
                inf.forwarders--;
                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: forwarders decr: "
                            + inf.forwarders);
                }
                if (failed) {
                    inf.failed++;
                    if (logger.isDebugEnabled()) {
                        logger.debug("NameServer: failed: " + inf.failed);
                    }
                }
                if (inf.forwarders == 0 || inf.forwarders == maxThreads - 1) {
                    inf.notifyAll();
                }
            }
        }
    }

    private void forward(byte message, RunInfo inf, IbisInfo dest,
            IbisInfo[] info, int offset) {

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: forwarding to " + dest);
        }

        if (offset >= info.length) {
            return;
        }

        Forwarder forwarder = new Forwarder(message, inf, dest, info, offset);

        synchronized (inf) {
            while (inf.forwarders >= maxThreads) {
                try {
                    inf.wait();
                } catch (Exception e) {
                    // Ignored
                }
            }
            inf.forwarders++;
            if (logger.isDebugEnabled()) {
                logger.debug("NameServer: forwarders = " + inf.forwarders);
            }
            ThreadPool.createNew(forwarder, "Forwarder thread");
        }
    }

    private class PingThread implements Runnable {
        RunInfo run;

        IbisInfo dest;

        String poolId;

        Vector<IbisInfo> deadIbises;

        PingThread(RunInfo run, IbisInfo dest, String poolId,
                Vector<IbisInfo> deadIbises) {
            this.run = run;
            this.dest = dest;
            this.poolId = poolId;
            this.deadIbises = deadIbises;
        }

        public void run() {
            doPing();
            synchronized (run) {
                run.pingers--;
                run.notifyAll();
            }
        }

        private void doPing() {
            Socket s = null;
            DataOutputStream out2 = null;
            DataInputStream in2 = null;

            try {
                s = createClientSocket(new InetSocketAddress(
                        dest.ibisNameServerAddress, dest.ibisNameServerport),
                        connectTimeout);
                out2 = new DataOutputStream(new BufferedOutputStream(s
                        .getOutputStream()));
                out2.writeByte(IBIS_PING);
                out2.flush();
                in2 = new DataInputStream(new BufferedInputStream(s
                        .getInputStream()));
                String k = in2.readUTF();
                String n = in2.readUTF();
                if (!k.equals(poolId) || !n.equals(dest.id.myId)) {
                    deadIbises.add(dest);
                }
            } catch (Exception e) {
                deadIbises.add(dest);
            } finally {
                closeConnection(in2, out2, s);
            }
        }
    }

    /**
     * Checks or kills a pool.
     * 
     * @param p
     *            the pool.
     * @param id
     *            the id of the ibis instance that must be checked/killed, or
     *            <code>null</code>, in which case the whole pool is
     *            checked/killed.
     * @param kill
     *            <code>true</code> when the victim must be killed,
     *            <code>false</code> otherwise.
     * @param poolId
     *            the poolId of the pool.
     */
    private void checkPool(RunInfo p, String id, boolean kill, String poolId) {

        Vector<IbisInfo> deadIbises = new Vector<IbisInfo>();

        synchronized (p) {
            // Obtain elements to send to first.
            IbisInfo[] elts = p.instances();
            for (int i = 0; i < elts.length; i++) {
                IbisInfo temp = elts[i];
                if (id == null || temp.id.myId.equals(id)) {
                    if (!kill) {
                        PingThread pt = new PingThread(p, temp, poolId,
                                deadIbises);
                        while (p.pingers >= maxThreads) {
                            try {
                                p.wait();
                            } catch (Exception ex) {
                                // ignored
                            }
                        }
                        p.pingers++;
                        ThreadPool.createNew(pt, "Ping thread");
                    } else {
                        deadIbises.add(temp);
                    }
                }
            }

            while (p.pingers > 0) {
                try {
                    p.wait();
                } catch (Exception e) {
                    // ignored
                }
            }

            for (int j = 0; j < deadIbises.size(); j++) {
                IbisInfo temp = deadIbises.get(j);
                logger.info("NameServer: ibis " + temp.id + " seems dead");

                p.remove(temp);
            }

            if (deadIbises.size() != 0) {
                // Put the dead ones in an array.
                IbisIdentifier[] ids = new IbisIdentifier[deadIbises.size()];
                IbisInfo[] ibisIds = new IbisInfo[ids.length];
                for (int j = 0; j < ids.length; j++) {
                    IbisInfo temp2 = deadIbises.get(j);
                    ids[j] = temp2.id;
                    ibisIds[j] = temp2;
                }

                // Pass the dead ones on to the election server
                p.electionKiller.addCorpses(ids);

                // ... and to all other ibis instances in this pool.
                elts = p.instances();
                for (int i = 0; i < elts.length; i++) {
                    IbisInfo ibisInf = elts[i];
                    if (ibisInf.needsUpcalls) {
                        forward(IBIS_DEAD, p, ibisInf, ibisIds, 0);
                    }
                }

                if (kill) {
                    for (int i = 0; i < ibisIds.length; i++) {
                        if (ibisIds[i].needsUpcalls) {
                            forward(IBIS_DEAD, p, ibisIds[i], ibisIds, 0);
                        }
                    }
                }

                while (p.forwarders != 0) {
                    try {
                        p.wait();
                    } catch (Exception ex) {
                        // ignored
                    }
                }
            }

            p.pingLimit = System.currentTimeMillis() + pingerTimeout;

            if (p.pool.size() == 0) {
                pools.remove(poolId);
                logger.warn("pool " + poolId + " seems to be dead.");
                killThreads(p);
            }
        }
    }

    private void handleIbisIsalive(boolean kill) throws IOException {
        String poolId = in.readUTF();
        String id = in.readUTF();

        if (!kill) {
            addPingerEntry(poolId, id);
            return;
        }

        RunInfo p = pools.get(poolId);
        if (p != null) {
            checkPool(p, id, kill, poolId);
        }
    }

    private void handleCheck() throws IOException {
        String poolId = in.readUTF();
        logger.info("Got check for pool " + poolId);
        addPingerEntry(poolId, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleCheckAll() throws IOException {
        logger.info("Got checkAll");
        addPingerEntry(null, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisJoin(long startTime) throws IOException {
        String poolId = in.readUTF();
        int len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully(buf, 0, len);
        InetAddress address = null;
        try {
            address = (InetAddress) Conversion.byte2object(buf);
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not read InetAddress");
        }
        int port = in.readInt();

        // System.out.println("After readInetAddress: " +
        // (System.currentTimeMillis() - startTime));

        boolean needsUpcalls = in.readBoolean();
        len = in.readInt();
        byte[] data = new byte[len];
        in.readFully(data, 0, len);
        Location location = new Location(in);

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + poolId
                    + " requested by " + address + ":" + port);
        }

        RunInfo p = pools.get(poolId);

        if (p == null) {
            // new run
            //
            if (singleRun && joined) {
                out.writeByte(IBIS_REFUSED);

                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: join to pool " + poolId
                            + " refused");
                }
                out.flush();
                return;
            }
            initiatePoolPinger();
            p = new RunInfo(connectTimeout, pingerTimeout);

            pools.put(poolId, p);
            joined = true;

            logger.info("NameServer: new pool " + poolId + " created");
        }

        // System.out.println("before poolPinger: " +
        // (System.currentTimeMillis() - startTime));
        initiatePoolPinger(poolId);
        // System.out.println("after poolPinger: " +
        // (System.currentTimeMillis() - startTime));
        // Handle delayed leave messages before adding new members
        // to a pool, otherwise new members get leave messages from nodes
        // that they have never seen.

        out.writeByte(IBIS_ACCEPTED);
        out.writeInt(p.electionServer.getPort());

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + poolId + " accepted");
        }
        // System.out.println("before synchronized block: " +
        // (System.currentTimeMillis() - startTime));

        IbisInfo info = new IbisInfo(address, port, needsUpcalls, p, data,
                location, poolId);

        synchronized (p) {
            sendLeavers(p);
            p.pool.put(info.id.myId, info);
            p.unfinishedJoins.add(info);
            p.arrayPool.add(info);
        }

        info.id.writeTo(out);
        // System.out.println("after synchronized block: " +
        // (System.currentTimeMillis() - startTime));

        // first send all existing nodes (including the new one) to the
        // new one.
        if (needsUpcalls) {
            out.writeInt(p.pool.size());

            if (logger.isDebugEnabled()) {
                logger.debug("Sending " + p.pool.size() + " nodes");
            }

            int i = 0;
            while (i < p.arrayPool.size()) {
                IbisInfo temp = p.pool.get(p.arrayPool.get(i).id.myId);

                if (temp != null) {
                    temp.id.writeTo(out);
                    i++;
                } else {
                    p.arrayPool.remove(i);
                }
            }
        }
        out.flush();

        logger.info("" + address + ":" + port + " JOINS  pool " + poolId + " ("
                + p.pool.size() + " nodes)");
    }

    private void poolPinger(String poolId) {

        RunInfo p = pools.get(poolId);

        if (p == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: ping pool " + poolId);
        }

        checkPool(p, null, false, poolId);
    }

    private void initiatePoolPinger(String poolId) {
        RunInfo p = pools.get(poolId);

        if (p == null) {
            return;
        }

        long t = System.currentTimeMillis();

        // If the pool has not reached its ping-limit yet, return.
        if (t < p.pingLimit) {
            if (logger.isDebugEnabled()) {
                logger
                        .debug("NameServer: ping timeout not reached yet for pool "
                                + poolId);
            }
            return;
        }
        addPingerEntry(poolId, null);
    }

    private void initiatePoolPinger() {
        for (Enumeration<String> e = pools.keys(); e.hasMoreElements();) {
            String poolId = e.nextElement();
            initiatePoolPinger(poolId);
        }
    }

    /**
     * Checks all pools to see if they still are alive. If a pool is dead
     * (connect to all members fails), the pool is killed.
     */
    private void poolPinger() {
        for (Enumeration<String> e = pools.keys(); e.hasMoreElements();) {
            String poolId = e.nextElement();
            poolPinger(poolId);
        }
    }

    private void killThreads(RunInfo p) {
        Socket s3 = null;
        DataOutputStream out3 = null;

        p.electionKiller.quit();

        try {
            s3 = createClientSocket(new InetSocketAddress(myAddress,
                    p.electionServer.getPort()), 0);
            out3 = new DataOutputStream(new BufferedOutputStream(s3
                    .getOutputStream()));
            out3.writeByte(ELECTION_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            closeConnection(null, out3, s3);
        }
    }

    private void handleIbisLeave() throws IOException {
        String poolId = in.readUTF();
        String id = in.readUTF();

        RunInfo p = pools.get(poolId);

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: leave from pool " + poolId
                    + " requested by " + id);
        }

        if (p == null) {
            logger.error("NameServer: unknown ibis " + id + "/" + poolId
                    + " tried to leave");
        } else {
            IbisInfo iinf = p.pool.get(id);

            if (iinf != null) {
                // found it.
                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: leave from pool " + poolId
                            + " of ibis " + id + " accepted");
                }

                // Also forward the leave to the requester.
                // It is used as an acknowledgement, and
                // the leaver is only allowed to exit when it
                // has received its own leave message.
                synchronized (p) {
                    p.leavers.add(iinf);
                    p.remove(iinf);
                }

                logger.info(id + " LEAVES pool " + poolId + " ("
                        + p.pool.size() + " nodes)");

                if (p.pool.size() == 0) {
                    logger.info("NameServer: removing pool " + poolId);

                    // Send leavers before removing this run
                    synchronized (p) {
                        sendLeavers(p);
                    }

                    pools.remove(poolId);
                    killThreads(p);
                }
            } else {
                logger.error("NameServer: unknown ibis " + id + "/" + poolId
                        + " tried to leave");
            }
        }

        logger.info("NameServer: confirming LEAVE " + id);
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisMustLeave() throws IOException {
        String poolId = in.readUTF();
        RunInfo p = pools.get(poolId);
        int count = in.readInt();
        String[] ids = new String[count];
        IbisInfo[] iinf = new IbisInfo[count];

        for (int i = 0; i < count; i++) {
            ids[i] = in.readUTF();
        }

        if (p == null) {
            logger.error("NameServer: unknown pool " + poolId);
            return;
        }

        int found = 0;

        synchronized (p) {
            for (int i = 0; i < count; i++) {
                IbisInfo info = p.pool.get(ids[i]);
                if (info != null) {
                    found++;
                    iinf[i] = info;
                }
            }

            // Obtain elements to send to first. The forward() method
            // may wait (and loose the lock).
            IbisInfo[] elts = p.instances();
            for (int i = 0; i < elts.length; i++) {
                IbisInfo ipp = elts[i];
                if (ipp.needsUpcalls) {
                    forward(IBIS_MUSTLEAVE, p, ipp, iinf, 0);
                }
            }

            while (p.forwarders != 0) {
                try {
                    p.wait();
                } catch (Exception ex) {
                    // ignored
                }
            }
        }

        out.writeByte(0);
        out.flush();
    }

    boolean stop = false;

    public class RequestHandler extends Thread {
        LinkedList<Socket> jobs = new LinkedList<Socket>();

        int maxSize;

        public RequestHandler(int maxSize) {
            this.maxSize = maxSize;
        }

        public synchronized void addJob(Socket s) {
            while (jobs.size() > maxSize) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
            }
            if (jobs.size() == 0) {
                notifyAll();
            }
            jobs.addLast(s);
        }

        public void run() {
            for (;;) {
                Socket s;
                synchronized (this) {
                    while (!stop && jobs.size() == 0) {
                        try {
                            this.wait();
                        } catch (Exception e) {
                        }
                    }
                    if (jobs.size() == 0) {
                        return;
                    }
                    if (jobs.size() >= maxSize) {
                        notifyAll();
                    }
                    s = jobs.remove(0);
                }

                handleRequest(s);
            }
        }
    }

    public void run() {
        Socket s;

        RequestHandler reqHandler = new RequestHandler(256);
        reqHandler.start();
        ThreadPool.createNew(new Closer(maxThreads), "Closer");

        while (!stop) {
            try {
                logger.info("NameServer: accepting incoming connections... ");

                s = serverSocket.accept();

                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: incoming connection from "
                            + s.toString());
                }
            } catch (Throwable e) {
                logger.error("NameServer got an error", e);

                try {
                    Thread.sleep(1000);
                } catch (Exception x) {
                    // ignore
                }
                continue;
            }
            // reqHandler.addJob(s);
            handleRequest(s);
            // No separate reqHandler thread, because "stop" is not dealt
            // with correctly, then. Instead, TODO, make this whole loop
            // multithreaded.
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new RuntimeException("NameServer got an error", e);
        }

        logger.info("NameServer: exit");
    }

    private void handleSeqno() throws IOException {
        String name = in.readUTF();

        long l = seq.getSeqno(name);
        out.writeLong(l);
        out.flush();
    }

    public void handleRequest(Socket s) {
        int opcode = -1;
        out = null;
        in = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();

        try {

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(baos));

            opcode = in.readByte();

            logger.debug("NameServer got opcode: " + opcode);

            switch (opcode) {
            case SEQNO:
                handleSeqno();
                break;
            case (IBIS_ISALIVE):
            case (IBIS_DEAD):
                logger
                        .debug("NameServer handling opcode IBIS_ISALIVE/IBIS_DEAD");
                handleIbisIsalive(opcode == IBIS_DEAD);
                break;
            case (IBIS_JOIN):
                handleIbisJoin(startTime);
                break;
            case (IBIS_MUSTLEAVE):
                handleIbisMustLeave();
                break;
            case (IBIS_LEAVE):
                handleIbisLeave();
                if (singleRun && pools.size() == 0) {
                    synchronized (this) {
                        if (joined) {
                            stop = true;
                        }
                    }
                    // ignore invalid leave req.
                }
                break;
            case (IBIS_CHECK):
                handleCheck();
                break;
            case (IBIS_CHECKALL):
                handleCheckAll();
                break;
            default:
                logger.error("NameServer got an illegal opcode: " + opcode);
            }

        } catch (Exception e1) {
            logger.error("Got an exception in NameServer.run", e1);
        } finally {
            Closer.addJob(new CloseJob(in, out, baos, s, opcode, startTime));
        }
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Closes a socket and streams that are associated with it. These streams
     * are given as separate parameters, because they may be streams that are
     * built on top of the actual socket streams.
     * 
     * @param in
     *            the inputstream ot be closed
     * @param out
     *            the outputstream to be closed
     * @param s
     *            the socket to be closed
     */
    static void closeConnection(InputStream in, OutputStream out, Socket s) {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (s != null) {
            try {
                s.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public String getLocalAddress() {
        return serverSocket.toString();
    }

}
