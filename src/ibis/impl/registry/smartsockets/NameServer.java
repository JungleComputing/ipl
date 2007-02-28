/* $Id: NameServer.java 5112 2007-02-27 16:09:05Z ceriel $ */

package ibis.impl.registry.smartsockets;

import ibis.ipl.IbisFactory;
import ibis.io.Conversion;
import ibis.impl.registry.NSProps;
import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.util.PoolInfoServer;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

public class NameServer extends Thread implements Protocol {

    static {
        Logger ibisLogger = Logger.getLogger("ibis");
        if (!ibisLogger.getAllAppenders().hasMoreElements()) {
            // No appenders defined, print to standard out by default
            PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p %m%n");
            WriterAppender appender = new WriterAppender(layout, System.err);
            ibisLogger.addAppender(appender);
            ibisLogger.setLevel(Level.WARN);
        }
    }

    public static final TypedProperties
            attribs = new TypedProperties(IbisFactory.getDefaultProperties());

    public static final int TCP_IBIS_NAME_SERVER_PORT_NR
            = attribs.getIntProperty(NSProps.s_port, 9826);

    static final int PINGER_TIMEOUT
        = attribs.getIntProperty(NSProps.s_pinger_timeout, 60) * 1000;
        // Property is in seconds, convert to milliseconds.

    static final int CONNECT_TIMEOUT
        = attribs.getIntProperty(NSProps.s_connect_timeout, 10) * 1000;
        // Property is in seconds, convert to milliseconds.

    static final int JOINER_INTERVAL
        = attribs.getIntProperty(NSProps.s_joiner_interval, 5) * 1000;

    // In seconds, as PoolChecker expects.
    static final int CHECKER_INTERVAL
        = attribs.getIntProperty(NSProps.s_poolchecker_interval, 0);

    static final int MAXTHREADS =
        attribs.getIntProperty(NSProps.s_max_threads, 8);

    static final Logger logger = Logger.getLogger(NameServer.class);

    /**
     * The <code>Sequencer</code> class provides a global numbering.
     * This can be used, for instance, for global ordering of messages.
     * A sender must then first obtain a sequence number from the sequencer,
     * and tag the message with it. The receiver must then handle the messages
     * in the "tag" order.
     * <p>
     * A Sequencer associates a numbering scheme with a name, so the user can
     * associate different sequences with different names.
     */
    private static class Sequencer {
        private HashMap counters;

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
            counters = new HashMap();
        }

        /**
         * Returns the next sequence number associated with the specified name.
         * @param name the name of the sequence.
         * @return the next sequence number
         */
        public synchronized long getSeqno(String name) {
            LongObject i = (LongObject) counters.get(name);
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

    static class IbisInfo {
        VirtualSocketAddress address;
        boolean needsUpcalls;
        boolean completelyJoined = false;
        IbisIdentifier id;

        IbisInfo(VirtualSocketAddress address, boolean needsUpcalls, RunInfo p,
                byte[] data, Location location, String poolId)
                throws IOException {
            this.address = address;
            this.needsUpcalls = needsUpcalls;
            synchronized(p) {
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
            return "ibisInfo(" + id + " at " + address + ")";
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
            if (! poolId.equals(e.poolId)) {
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
        ArrayList corpses = new ArrayList();
        final RunInfo runInfo;
        boolean done = false;
        int count = 0;
        final VirtualSocketAddress serverAddr;
        final byte message;

        DeadNotifier(RunInfo r, VirtualSocketAddress s, byte m) {
            runInfo = r;
            serverAddr = s;
            message = m;
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
                synchronized(this) {
                    while (! done && count == 0) {
                        try {
                            this.wait();
                        } catch(Exception e) {
                            // ignored
                        }
                    }
                    if (count > 0) {
                        deadOnes = new IbisIdentifier[count];
                        int i = 0;
                        while (corpses.size() > 0) {
                            IbisIdentifier[] el
                                    = (IbisIdentifier[]) corpses.remove(0);
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
                    } catch(Exception e) {
                        // ignored
                    }
                }
                synchronized(this) {
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
            VirtualSocket s = null;
            DataOutputStream out2 = null;

            try {
                s = socketFactory.createClientSocket(serverAddr,
                        CONNECT_TIMEOUT, null);
                out2 = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream()));
                out2.writeByte(message);
                out2.writeInt(ids.length);
                for (int i = 0; i < ids.length; i++) {
                    ids[i].writeTo(out2);
                }
            } finally {
                VirtualSocketFactory.close(s, out2, null);
            }
        }
    }

    static class RunInfo {
        ArrayList unfinishedJoins; // a list of IbisInfos

        ArrayList arrayPool;    // IbisInfos in fixed order.

        Hashtable pool;

        ArrayList leavers;

        int forwarders;

        int pingers;

        int failed;

        ElectionServer electionServer;

        DeadNotifier electionKiller;

        long pingLimit;

        boolean silent;

        int joinCount = 0;

        RunInfo(boolean silent) throws IOException {
            unfinishedJoins = new ArrayList();
            arrayPool = new ArrayList();
            pool = new Hashtable();
            leavers = new ArrayList();
            electionServer = new ElectionServer(silent, socketFactory);
            electionKiller = new DeadNotifier(this, electionServer.getAddress(),
                    ELECTION_KILL);
            ThreadPool.createNew(electionKiller, "ElectionKiller");
            pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;
            this.silent = silent;
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
            return (IbisInfo[]) pool.values().toArray(new IbisInfo[0]);
        }

        public void remove(IbisInfo iinf) {
            pool.remove(iinf.id.myId);
            if (! iinf.completelyJoined) {
                int index = unfinishedJoins.indexOf(iinf);
                if (index == -1) {
                    if (! silent) {
                        logger.error("Internal error: " + iinf.id.myId + " not completelyJoined but not in unfinishedJoins!");
                    }
                } else {
                    unfinishedJoins.remove(index);
                }
            }
        }
    }

    private Hashtable pools;

    private VirtualServerSocket serverSocket;

    private ArrayList pingerEntries = new ArrayList();

    private DataInputStream in;

    private DataOutputStream out;

    private boolean singleRun;

    private boolean joined;

    boolean silent;

    private static VirtualSocketFactory socketFactory;

    private Sequencer seq;

    static class CloseJob {
        DataInputStream in;
        DataOutputStream out;
        ByteArrayOutputStream baos;
        VirtualSocket s;
        long startTime;
        int opcode;

        long myStartTime = System.currentTimeMillis();
        
        CloseJob(DataInputStream in, DataOutputStream out,
                ByteArrayOutputStream baos, VirtualSocket s,
                int opcode, long start) {
            this.in = in;
            this.out = out;
            this.baos = baos;
            this.s = s;
            this.opcode = opcode;
            this.startTime = start;
        }

        void close(boolean silent) {
            try {
                out.flush();
                baos.writeTo(s.getOutputStream());
                VirtualSocketFactory.close(s, out, in);
            } catch(Exception e) {
                if (! silent) {
                    logger.error("Exception in close", e);
                }
            }
            if (logger.isInfoEnabled() && opcode >= 0) {
                String job = "unknown opcode " + opcode;
                switch(opcode) {
                case (IBIS_ISALIVE):
                    job = "ISALIVE"; break;
                case (IBIS_DEAD):
                    job = "DEAD"; break;
                case (IBIS_JOIN):
                    job = "JOIN"; break;
                case (IBIS_MUSTLEAVE):
                    job = "MUSTLEAVE"; break;
                case (IBIS_LEAVE):
                    job = "LEAVE"; break;
                case (IBIS_CHECK):
                    job = "CHECK"; break;
                case (IBIS_CHECKALL):
                    job = "CHECKALL"; break;
                default:
                    job = "unknown opcode " + opcode; break;
                }
                
                long now = System.currentTimeMillis();
                
                logger.info("Request " + job + " took "
                        + (now - startTime) + " ms. -> " 
                        + "job took " + (myStartTime - startTime) 
                        + " closing took " + (now-myStartTime));
            }
        }
    }

    static ArrayList closeJobs = new ArrayList();
    static int numClosers;

    private static class Closer implements Runnable {
        boolean silent;
        Closer(boolean silent) {
            this.silent = silent;
            numClosers++;
        }

        static void addJob(CloseJob cl) {
            synchronized(closeJobs) {
                closeJobs.add(cl);
                closeJobs.notify();
            }
        }

        public void run() {
            for (;;) {
                CloseJob cl;
                synchronized(closeJobs) {
                    while (closeJobs.size() == 0) {
                        if (numClosers > 1) {
                            numClosers--;
                            return;
                        }
                        try {
                            closeJobs.wait();
                        } catch(Exception e) {
                            // ignored
                        }
                    }
                    cl = (CloseJob) closeJobs.remove(0);
                    if (numClosers < MAXTHREADS && closeJobs.size() > 0) {
                        ThreadPool.createNew(new Closer(silent), "Closer");
                    }

                }
                cl.close(silent);
            }
        }
    }

    private NameServer(boolean singleRun, boolean poolserver, boolean silent)
            throws IOException {

        this.singleRun = singleRun;
        this.joined = false;
        this.silent = silent;

        synchronized(NameServer.class) {
            if (socketFactory == null) {
                HashMap properties = new HashMap();        
                properties.put("smartsockets.direct.port",
                        "" + TCP_IBIS_NAME_SERVER_PORT_NR);
            
                // Bit of a hack to improve the visualization
                properties.put("smartsockets.register.property", "nameserver");

                try {
                    socketFactory = VirtualSocketFactory
                        .createSocketFactory(properties, true);
                } catch(Throwable e) {
                    throw new IOException("could not create socket factory");
                }
            }
        }

        seq = new Sequencer();

        String poolPort = attribs.getProperty("ibis.pool.server.port");
        int port = TCP_IBIS_NAME_SERVER_PORT_NR;

        if (! silent && logger.isInfoEnabled()) {
            logger.info("Creating nameserver on port " + port);
        }

        if (poolserver) {
            if (poolPort == null) {
                poolPort = Integer.toString(port + 1);
                System.setProperty("ibis.pool.server.port", poolPort);
            }
            try {
                PoolInfoServer p = new PoolInfoServer(singleRun);
                p.setDaemon(true);
                p.start();
            } catch (Throwable e) {
                // May have been started by PoolInfoClient already.
                // throw new IOException("Could not start poolInfoServer" + e);
            }
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: singleRun = " + singleRun);
        }

        // Create a server socket.
        serverSocket = socketFactory.createServerSocket(port, 256, false, null);

        if (CHECKER_INTERVAL != 0) {
            final PoolChecker ck = new PoolChecker(socketFactory, null,
                    serverSocket.getLocalSocketAddress(), CHECKER_INTERVAL);

            // TODO: Use threadpool?
            Thread p = new Thread("PoolChecker Upcaller") {
                public void run() {
                    ck.run();
                }
            };
            p.setDaemon(true);
            p.start();
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: created server on "
                    + serverSocket.getLocalSocketAddress());
        }

        pools = new Hashtable();

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

        if (! silent && logger.isInfoEnabled()) {
            Runtime.getRuntime().addShutdownHook(
                new Thread("Nameserver ShutdownHook") {
                    public void run() {
                        logger.info("Shutdown hook triggered");
                    }
                });
        }
    }

    // Should be called within synchronized on inf.
    private void sendLeavers(RunInfo inf) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("sendLeavers ... size = " + inf.leavers.size()
                    + ", forwarders = " + inf.forwarders);
        }

        IbisInfo[] leavers = null;

        if (inf.leavers.size() != 0) {
            IbisInfo[] iinf = new IbisInfo[0];
            leavers = (IbisInfo[]) inf.leavers.toArray(iinf);
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
            } catch(Exception ex) {
                // ignored
            }
        }
    }

    void pingRunner() {
        for (;;) {
            PingerEntry e = null;
            synchronized(pingerEntries) {
                while (pingerEntries.size() == 0) {
                    try {
                        pingerEntries.wait();
                    } catch(Exception ex) {
                        // ignored
                    }
                }
                e = (PingerEntry) pingerEntries.get(0);
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
                RunInfo p = (RunInfo) pools.get(e.poolId);
                if (p != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Doing check of ibis " + e.id);
                    }
                    checkPool(p, e.id, false, e.poolId);
                }
            }
            synchronized(pingerEntries) {
                // note: other threads may only replace/remove indices != 0.
                pingerEntries.remove(0);
            }
        }
    }

    void addPingerEntry(String poolId, String id) {

        PingerEntry added = new PingerEntry(poolId, id);
        boolean replaced = false;

        synchronized(pingerEntries) {
            // First check if this request is already present. If so,
            // just return. If a "larger" request is already present,
            // also just return.
            // Vice versa, if the request to be added is "larger" than
            // any request in the list, remove/replace the "smaller" requests.
            for (int i = 0; i < pingerEntries.size(); i++) {
                PingerEntry e = (PingerEntry) pingerEntries.get(i);
                if (e.largerOrEqual(added)) {
                    return;
                }
                if (added.largerOrEqual(e)) {
                    if (i == 0) {
                        continue;
                    }
                    if (! replaced) {
                        pingerEntries.set(i, added);
                        replaced = true;
                    } else {
                        pingerEntries.remove(i);
                        i--;
                    }
                }
            }
            if (! replaced) {
                pingerEntries.add(added);
                pingerEntries.notifyAll();
            }
        }
    }

    void upcaller() {
        for (;;) {
            try {
                Thread.sleep(JOINER_INTERVAL);
            } catch(InterruptedException e) {
                // ignore
            }
            for (Enumeration e = pools.keys(); e.hasMoreElements();) {
                String poolId = (String) e.nextElement();
                RunInfo inf = (RunInfo) pools.get(poolId);
                boolean joinFailed = false;

                synchronized(inf) {
                    try {
                        sendLeavers(inf);
                    } catch(IOException ex) {
                        if (! silent) {
                            logger.error("Got exception: " + ex);
                        }
                    }

                    if (inf.unfinishedJoins.size() > 0) {
                        inf.failed = 0;
                        IbisInfo[] message = (IbisInfo[])
                                inf.unfinishedJoins.toArray(new IbisInfo[0]);

                        inf.unfinishedJoins.clear();

                        // Obtain elements to send to first. The forward()
                        // method may wait (and loose the lock).
                        IbisInfo[] elts = inf.instances();
                        for (int i = 0; i < elts.length; i++) {
                            if (elts[i].completelyJoined && elts[i].needsUpcalls) {
                                forward(IBIS_JOIN, inf, elts[i], message, 0);
                            }
                        }

                        for (int i = 0; i < message.length; i++) {
                            IbisInfo ibisInf = message[i];
                            if (ibisInf.needsUpcalls && i+1 < message.length) {
                                forward(IBIS_JOIN, inf, ibisInf, message, i+1);
                            }
                            ibisInf.completelyJoined = true;
                        }

                        while (inf.forwarders != 0) {
                            try {
                                inf.wait();
                            } catch(Exception ex) {
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
            switch(msg) {
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
            VirtualSocket s = null;
            DataOutputStream out2 = null;
            boolean failed = true;

            // QUICK HACK -- JASON
            for (int h=0;h<3;h++) { 
                try {
                    s = socketFactory.createClientSocket(
                            dest.address, CONNECT_TIMEOUT, null);
                    out2 = new DataOutputStream(
                            new BufferedOutputStream(s.getOutputStream()));
                    out2.writeByte(message);
                    out2.writeInt(info.length - offset);
                    for (int i = offset; i < info.length; i++) {
                        info[i].id.writeTo(out2);
                        
                        if (logger.isDebugEnabled()) {
                            logger.debug("NameServer: forwarding "
                                    + type(message) + " of "
                                    + info[i].id.myId + " to " + dest + " DONE");
                        }
                    }

                    failed = false;
                    VirtualSocketFactory.close(s, out2, null);
                    break;
                } catch (Exception e) {
                    VirtualSocketFactory.close(s, out2, null);
                    if (! silent) {
                        logger.error("Could not forward "
                                + type(message) + " to " + dest, e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException x) {
                        // ignore
                    }
                }
            }

            synchronized(inf) {
                inf.forwarders--;
                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: forwarders decr: " + inf.forwarders);
                }
                if (failed) {
                    inf.failed++;
                    if (logger.isDebugEnabled()) {
                        logger.debug("NameServer: failed: " + inf.failed);
                    }
                }
                if (inf.forwarders == 0 || inf.forwarders == MAXTHREADS-1) {
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
            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: forwarding skipped");
            }
            return;
        }

        Forwarder forwarder = new Forwarder(message, inf, dest, info, offset);

        synchronized(inf) {
            while (inf.forwarders >= MAXTHREADS) {
                try {
                    inf.wait();
                } catch(Exception e) {
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
        Vector deadIbises;

        PingThread(RunInfo run, IbisInfo dest, String poolId, Vector deadIbises) {
            this.run = run;
            this.dest = dest;
            this.poolId = poolId;
            this.deadIbises = deadIbises;
        }

        public void run() {
            doPing();
            synchronized(run) {
                run.pingers--;
                run.notifyAll();
            }
        }

        private void doPing() {
            VirtualSocket s = null;
            DataOutputStream out2 = null;
            DataInputStream in2 = null;

            try {
                s = socketFactory.createClientSocket(
                        dest.address, CONNECT_TIMEOUT, null);
                out2 = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream()));
                out2.writeByte(IBIS_PING);
                out2.flush();
                in2 = new DataInputStream(
                        new BufferedInputStream(s.getInputStream()));
                String k = in2.readUTF();
                String n = in2.readUTF();
                if (!k.equals(poolId) || ! n.equals(dest.id.myId)) {
                    deadIbises.add(dest);
                }
            } catch (Exception e) {
                deadIbises.add(dest);
            } finally {
                VirtualSocketFactory.close(s, out2, in2);
            }
        }
    }

    /**
     * Checks or kills a pool.
     * @param p the pool.
     * @param id the id of the ibis instance that must be checked/killed,
     *     or <code>null</code>, in which case the whole pool is checked/killed.
     * @param kill <code>true</code> when the victim must be killed,
     *     <code>false</code> otherwise.
     * @param poolId the poolId of the pool.
     */
    private void checkPool(RunInfo p, String id, boolean kill, String poolId) {

        Vector deadIbises = new Vector();

        if (! silent && logger.isInfoEnabled()) {
            logger.info("Testing pool " + poolId + " for dead ibises");
        }

        synchronized(p) {
            // Obtain elements to send to first.
            IbisInfo[] elts = p.instances();
            for (int i = 0; i < elts.length; i++) {
                IbisInfo temp = elts[i];
                if (id == null || temp.id.myId.equals(id)) {
                    if (! kill) {
                        PingThread pt = new PingThread(p, temp, poolId, deadIbises);
                        while (p.pingers >= MAXTHREADS) {
                            try {
                                p.wait();
                            } catch(Exception ex) {
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
                } catch(Exception e) {
                    // ignored
                }
            }

            for (int j = 0; j < deadIbises.size(); j++) {
                IbisInfo temp = (IbisInfo) deadIbises.get(j);
                if (! kill && ! silent && logger.isInfoEnabled()) {
                    logger.info("NameServer: ibis " + temp.id + " seems dead");
                }

                p.remove(temp);
            }

            if (deadIbises.size() != 0) {
                // Put the dead ones in an array.
                IbisIdentifier[] ids = new IbisIdentifier[deadIbises.size()];
                IbisInfo[] ibisIds = new IbisInfo[ids.length];
                for (int j = 0; j < ids.length; j++) {
                    IbisInfo temp2 = (IbisInfo) deadIbises.get(j);
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
                        if (!silent) {
                            logger.warn("nameserver waitting for pool " + poolId
                                    + " pingers to return (" + p.forwarders
                                    + ")");
                        }
                        p.wait(1000);
                    } catch(Exception ex) {
                        // ignored
                    }
                }
            }

            p.pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;

            if (p.pool.size() == 0) {
                pools.remove(poolId);
                if (! silent) {
                    logger.warn("pool " + poolId + " seems to be dead.");
                }
                killThreads(p);
            }
        }
    }

    private void handleIbisIsalive(boolean kill) throws IOException {
        String poolId = in.readUTF();
        String id = in.readUTF();

        logger.debug("Got handleIbisIsalive(" + kill + ") " + poolId + "/" + id);

        if (! kill) {
            addPingerEntry(poolId, id);
            return;
        }

        RunInfo p = (RunInfo) pools.get(poolId);
        if (p != null) {
            checkPool(p, id, kill, poolId);
        }
    }

    private void handleCheck() throws IOException {
        String poolId = in.readUTF();
        if (! silent && logger.isInfoEnabled()) {
            logger.info("Got check for pool " + poolId);
        }
        addPingerEntry(poolId, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleCheckAll() throws IOException {
        if (! silent && logger.isInfoEnabled()) {
            logger.info("Got checkAll");
        }
        addPingerEntry(null, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisJoin(long startTime) throws IOException {
        String poolId = in.readUTF();
        VirtualSocketAddress address = new VirtualSocketAddress(in);
    
        // System.out.println("After readVirtualAddress: " +
        //         (System.currentTimeMillis() - startTime));

        boolean needsUpcalls = in.readBoolean();
        int len = in.readInt();
        byte[] data = new byte[len];
        in.readFully(data, 0, len);
        Location location = new Location(in);

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + poolId
                    + " requested, " + address);
        }


        RunInfo p = (RunInfo) pools.get(poolId);

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
            p = new RunInfo(silent);

            pools.put(poolId, p);
            joined = true;

            if (! silent && logger.isInfoEnabled()) {
                logger.info("NameServer: new pool " + poolId + " created");
            }
        }

        // System.out.println("before poolPinger: " +
        //         (System.currentTimeMillis() - startTime));
        initiatePoolPinger(poolId);
        // System.out.println("after poolPinger: " +
        //         (System.currentTimeMillis() - startTime));
        // Handle delayed leave messages before adding new members
        // to a pool, otherwise new members get leave messages from nodes
        // that they have never seen.

        out.writeByte(IBIS_ACCEPTED);
        p.electionServer.getAddress().write(out);

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + poolId + " accepted");
        }
        // System.out.println("before synchronized block: " +
        //         (System.currentTimeMillis() - startTime));

        IbisInfo info = new IbisInfo(address, needsUpcalls, p, data, location,
                poolId);

        synchronized(p) {
            sendLeavers(p);
            p.pool.put(info.id.myId, info);
            p.unfinishedJoins.add(info);
            p.arrayPool.add(info);
        }

        info.id.writeTo(out);
        // System.out.println("after synchronized block: " +
        //         (System.currentTimeMillis() - startTime));

        // first send all existing nodes (including the new one) to the
        // new one.
        if (needsUpcalls) {
            out.writeInt(p.pool.size());

            if (logger.isDebugEnabled()) {
                logger.debug("Sending " + p.pool.size() + " nodes");
            }

            int i = 0;
            while (i < p.arrayPool.size()) {
                IbisInfo temp = (IbisInfo) p.pool.get(((IbisInfo) p.arrayPool.get(i)).id.myId);

                if (temp != null) {
                    temp.id.writeTo(out);
                    i++;
                } else {
                    p.arrayPool.remove(i);
                }
            }
        }
        out.flush();

        if (! silent && logger.isInfoEnabled()) {
            logger.info("Ibis " + info.id + " at " + address + " JOINS  pool "
                    + poolId + " (" + p.pool.size() + " nodes)");
        }
        // System.out.println("after write answer: " +
        //         (System.currentTimeMillis() - startTime));
    }

    private void poolPinger(String poolId) {

        RunInfo p = (RunInfo) pools.get(poolId);

        if (p == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: ping pool " + poolId);
        }

        checkPool(p, null, false, poolId);
    }

    private void initiatePoolPinger(String poolId) {
        RunInfo p = (RunInfo) pools.get(poolId);

        if (p == null) {
            return;
        }

        long t = System.currentTimeMillis();

        // If the pool has not reached its ping-limit yet, return.
        if (t < p.pingLimit) {
            if (logger.isDebugEnabled()) {
                logger.debug("NameServer: ping timeout not reached yet for pool " + poolId);
            }
            return;
        }
        addPingerEntry(poolId, null);
    }

    private void initiatePoolPinger() {
        for (Enumeration e = pools.keys(); e.hasMoreElements();) {
            String poolId = (String) e.nextElement();
            initiatePoolPinger(poolId);
        }
    }

    /**
     * Checks all pools to see if they still are alive. If a pool is dead
     * (connect to all members fails), the pool is killed.
     */
    private void poolPinger() {
        for (Enumeration e = pools.keys(); e.hasMoreElements();) {
            String poolId = (String) e.nextElement();
            poolPinger(poolId);
        }
    }


    private void killThreads(RunInfo p) {
        VirtualSocket s3 = null;
        DataOutputStream out3 = null;

        p.electionKiller.quit();
        
        try {
            s3 = socketFactory.createClientSocket(
                    p.electionServer.getAddress(), CONNECT_TIMEOUT, null);
            out3 = new DataOutputStream(
                    new BufferedOutputStream(s3.getOutputStream()));
            out3.writeByte(ELECTION_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            VirtualSocketFactory.close(s3, out3, null);
        }
    }

    private void handleIbisLeave() throws IOException {
        String poolId = in.readUTF();
        String id = in.readUTF();

        RunInfo p = (RunInfo) pools.get(poolId);

        if (logger.isDebugEnabled()) {
            logger.debug("NameServer: leave from pool " + poolId
                    + " requested by " + id);
        }

        if (p == null) {
            // new run
            if (! silent) {
                logger.error("NameServer: unknown ibis " + id
                        + "/" + poolId + " tried to leave");
            }
        } else {
            IbisInfo iinf = (IbisInfo) p.pool.get(id);

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
                synchronized(p) {
                    p.leavers.add(iinf);
                    p.remove(iinf);
                }

                if (! silent && logger.isInfoEnabled()) {
                    logger.info(id + " LEAVES pool " + poolId
                            + " (" + p.pool.size() + " nodes)");
                }

                if (p.pool.size() == 0) {
                    if (! silent && logger.isInfoEnabled()) {
                        logger.info("NameServer: removing pool " + poolId);
                    }

                    // Send leavers before removing this run
                    synchronized(p) {
                        sendLeavers(p);
                    }

                    pools.remove(poolId);
                    killThreads(p);
                }
            } else {
                if (! silent) {
                    logger.error("NameServer: unknown ibis " + id
                        + "/" + poolId + " tried to leave");
                }
            }
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: confirming LEAVE " + id);
        }
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisMustLeave() throws IOException {
        String poolId = in.readUTF();
        RunInfo p = (RunInfo) pools.get(poolId);
        int count = in.readInt();
        String[] ids = new String[count];
        IbisInfo[] iinf = new IbisInfo[count];

        for (int i = 0; i < count; i++) {
            ids[i] = in.readUTF();
        }

        if (p == null) {
            if (! silent) {
                logger.error("NameServer: unknown pool " + poolId);
            }
            return;
        }

        int found = 0;

        synchronized(p) {
            for (int i = 0; i < count; i++) {
                IbisInfo info = (IbisInfo) p.pool.get(ids[i]);
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
                } catch(Exception ex) {
                    // ignored
                }
            }
        }

        out.writeByte(0);
        out.flush();
    }

    boolean stop = false;

    public class RequestHandler extends Thread {
        LinkedList<VirtualSocket> jobs = new LinkedList<VirtualSocket>();
        int maxSize;

        public RequestHandler(int maxSize) {
            this.maxSize = maxSize;
        }

        public synchronized void addJob(VirtualSocket s) {
            while (jobs.size() > maxSize) {
                try {
                    wait();
                } catch(Exception e) {
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
                VirtualSocket s;
                synchronized(this) {
                    while (! stop && jobs.size() == 0) {
                        try {
                            this.wait();
                        } catch(Exception e) {
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
        VirtualSocket s;

        RequestHandler reqHandler = new RequestHandler(256);
        reqHandler.start();
        ThreadPool.createNew(new Closer(silent), "Closer");

        while (!stop) {
            try {
                if (! silent && logger.isInfoEnabled()) {
                    logger.info("NameServer: accepting incoming connections... ");
                }

                s = serverSocket.accept();

                if (logger.isDebugEnabled()) {
                    logger.debug("NameServer: incoming connection from "
                            + s.toString());
                }
            } catch (Throwable e) {
                if (! silent) {
                    logger.error("NameServer got an error", e);
                }
                
                try {
                    Thread.sleep(1000);
                } catch (Exception x) {
                    //ignore
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

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: exit");
        }
    }

    private void handleSeqno() throws IOException {
        String name = in.readUTF();

        long l = seq.getSeqno(name);
        out.writeLong(l);
        out.flush();
    }

    public void handleRequest(VirtualSocket s) {
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
                logger.debug("NameServer handling opcode IBIS_ISALIVE/IBIS_DEAD");
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
                    synchronized(this) {
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
                if (! silent) {
                    logger.error("NameServer got an illegal opcode: " + opcode);
                }
            }

        } catch (Exception e1) {
            if (! silent) {
                logger.error("Got an exception in NameServer.run", e1);
            }
        } finally {
            Closer.addJob(new CloseJob(in, out, baos, s, opcode, startTime));
        }
    }
    
    public VirtualSocketAddress getAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    public static synchronized NameServer createNameServer(boolean singleRun,
            boolean retry, boolean poolserver, boolean silent) {
        NameServer ns = null;
        while (true) {
            try {
                ns = new NameServer(singleRun, poolserver, silent);
                break;
            } catch (Throwable e) {
                if (retry) {
                    e.printStackTrace();
                    if (! silent) {
                        logger.warn("Nameserver: could not create server "
                                + "socket, retry in 1 second, cause = " + e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) { /* do nothing */
                    }
                } else {
                    if (! silent) {
                        logger.warn("Nameserver: could not create server "
                                + "socket, cause = " + e, e);
                    }
                    return null;
                }
            }
        }
        return ns;
    }

    public static void main(String[] args) {
        boolean single = false;
        boolean silent = false;
        boolean pool_server = true;
        boolean retry = true;
        NameServer ns = null;

        for (int i = 0; i < args.length; i++) {
            if (false) { /* do nothing */
            } else if (args[i].equals("-single")) {
                single = true;
            } else if (args[i].equals("-silent")) {
                silent = true;
            } else if (args[i].equals("-retry")) {
                retry = true;
            } else if (args[i].equals("-no-retry")) {
                retry = false;
            } else if (args[i].equals("-poolserver")) {
                pool_server = true;
            } else if (args[i].equals("-no-poolserver")) {
                pool_server = false;
            } else if (args[i].equals("-verbose") || args[i].equals("-v")) {
                if (logger.getEffectiveLevel().isGreaterOrEqual(org.apache.log4j.Level.INFO)) {
                    logger.setLevel(org.apache.log4j.Level.INFO);
                }
            } else {
                if (! silent) {
                    logger.fatal("No such option: " + args[i]);
                }
                System.exit(1);
            }
        }

        if (!single) {
            single = attribs.booleanProperty(NSProps.s_single);
        }

        ns = createNameServer(single, retry, pool_server, silent);

        try {
            if (ns == null) {
                if (! silent) {
                    logger.error("No nameserver created");
                }
            } else {
                ns.run();
            }
            System.exit(0);
        } catch (Throwable t) {
            if (! silent) {
                logger.error("Nameserver got an exception", t);
            }
        }
    }
}
