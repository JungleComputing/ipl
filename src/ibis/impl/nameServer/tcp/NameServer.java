/* $Id$ */

package ibis.impl.nameServer.tcp;

//import ibis.connect.controlhub.Hub;
import ibis.impl.nameServer.NSProps;
import ibis.util.PoolInfoServer;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

public class NameServer extends Thread implements Protocol {

    public static final int TCP_IBIS_NAME_SERVER_PORT_NR
            = TypedProperties.intProperty(NSProps.s_port, 9826);

    static final int PINGER_TIMEOUT
        = TypedProperties.intProperty(NSProps.s_pinger_timeout, 60) * 1000;
        // Property is in seconds, convert to milliseconds.

    static final int CONNECT_TIMEOUT
        = TypedProperties.intProperty(NSProps.s_connect_timeout, 10) * 1000;
        // Property is in seconds, convert to milliseconds.

    static final int JOINER_INTERVAL
        = TypedProperties.intProperty(NSProps.s_joiner_interval, 5) * 1000;

    // In seconds, as KeyChecker expects.
    static final int CHECKER_INTERVAL
        = TypedProperties.intProperty(NSProps.s_keychecker_interval, 0);

    static final int MAXTHREADS =
        TypedProperties.intProperty(NSProps.s_max_threads, 8);

   // VirtualSocketAddress myAddress;

    static class IbisInfo {
        String name;
        byte[] serializedId;  
        
        VirtualSocketAddress address;         
        
        boolean needsUpcalls;
        boolean completelyJoined = false;

        IbisInfo(String name, byte[] serializedId, VirtualSocketAddress address, 
                boolean needsUpcalls) {
            this.name = name;
            this.serializedId = serializedId;
            this.address = address;
            this.needsUpcalls = needsUpcalls;
        }

        public boolean equals(Object other) {
            if (other instanceof IbisInfo) {
                return name.equals(((IbisInfo) other).name);
            }
            return false;
        }

        public int hashCode() {
            return name.hashCode();
        }

        public String toString() {
            return "ibisInfo(" + name + "at " + address + ")";
        }
    }

    static class PingerEntry {
        String key;
        String name;

        PingerEntry(String key, String name) {
            this.key = key;
            this.name = name;
        }

        boolean largerOrEqual(PingerEntry e) {
            if (key == null) {
                // key == null means: ping everything.
                return true;
            }
            if (e.key == null) {
                return false;
            }
            if (! key.equals(e.key)) {
                // unrelated.
                return false;
            }

            if (name == null) {
                // Same key, so this one pings whole pool.
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

        DeadNotifier(RunInfo p, VirtualSocketAddress s, byte m) {
            runInfo = p;
            serverAddr = s;
            message = m;
        }

        synchronized void addCorpses(String[] ids) {
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
                String[] deadOnes = null;
                synchronized(this) {
                    while (! done && count == 0) {
                        try {
                            this.wait();
                        } catch(Exception e) {
                            // ignored
                        }
                    }
                    if (count > 0) {
                        deadOnes = new String[count];
                        int i = 0;
                        while (corpses.size() > 0) {
                            String[] el = (String[]) corpses.remove(0);
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

        private void send(String[] ids) throws IOException {
            VirtualSocket s = null;
            DataOutputStream out2 = null;

            try {
                s = socketFactory.createClientSocket(serverAddr,
                        CONNECT_TIMEOUT, null);
                out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                out2.writeByte(message);
                out2.writeInt(ids.length);
                for (int i = 0; i < ids.length; i++) {
                    out2.writeUTF(ids[i]);
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

        Vector toBeDeleted; // a list of ibis names

        PortTypeNameServer portTypeNameServer;

        ReceivePortNameServer receivePortNameServer;

        DeadNotifier receivePortKiller;

        ElectionServer electionServer;

        DeadNotifier electionKiller;

        long pingLimit;

        boolean silent;

        RunInfo(boolean silent) throws IOException {
            unfinishedJoins = new ArrayList();
            arrayPool = new ArrayList();
            pool = new Hashtable();
            leavers = new ArrayList();
            toBeDeleted = new Vector();
            portTypeNameServer = new PortTypeNameServer(silent,
                    socketFactory);
            receivePortNameServer = new ReceivePortNameServer(silent,
                    socketFactory);
            receivePortKiller = new DeadNotifier(this,
                    receivePortNameServer.getAddress(), PORT_KILL);
            ThreadPool.createNew(receivePortKiller, "ReceivePortKiller");
            electionServer = new ElectionServer(silent,
                    socketFactory);
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

            res += "  toBeDeleted = \n";

            for (int i = 0; i < toBeDeleted.size(); i++) {
                res += "    " + ((IbisInfo) (toBeDeleted.get(i))).name + "\n";
            }

            return res;
        }

        public IbisInfo[] instances() {
            return (IbisInfo[]) pool.values().toArray(new IbisInfo[0]);
        }

        public void remove(IbisInfo iinf) {
            pool.remove(iinf.name);
            if (! iinf.completelyJoined) {
                int index = unfinishedJoins.indexOf(iinf);
                if (index == -1) {
                    if (! silent) {
                        logger.error("Internal error: " + iinf.name + " not completelyJoined but not in unfinishedJoins!");
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

  //  private Hub h = null;

    private static VirtualSocketFactory socketFactory; 

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
    
    static { 
        HashMap properties = new HashMap();        
        properties.put("modules.direct.port", "" + TCP_IBIS_NAME_SERVER_PORT_NR);
        
        // Bit of a hack to improve the visualization
        properties.put("smartsockets.register.property", "nameserver");      
        
        socketFactory = VirtualSocketFactory.createSocketFactory(properties, true);  
    }
    
    static Logger logger = 
            ibis.util.GetLogger.getLogger(NameServer.class.getName());

    private NameServer(boolean singleRun, boolean poolserver,
            boolean starthub, boolean silent) throws IOException {

        this.singleRun = singleRun;
        this.joined = false;
        this.silent = silent;

       // myAddress = IPUtils.getAlternateLocalHostAddress();
      //  myAddress = InetAddress.getByName(myAddress.getHostName());

        String hubPort = System.getProperty("ibis.connect.hub.port");
        String poolPort = System.getProperty("ibis.pool.server.port");
        
        int port = TCP_IBIS_NAME_SERVER_PORT_NR;


        //if (! silent && logger.isInfoEnabled()) {
            logger.info("Creating nameserver on port " + port);
        //}
/*
        if (controlhub) {
            if (hubPort == null) {
                hubPort = Integer.toString(port + 2);
                System.setProperty("ibis.connect.hub.port", hubPort);
*/
        // TODO check if this is merged correclty !
        
            /*
        if (starthub) { 
            
            int p = 0; 
            
            if (hubPort != null) {
                p = Integer.parseInt(hubPort);
            } else { 
                p = port + 2;
            }

            try {
                h = Hub.createHub(p, true);
                Thread.sleep(2000); // Give it some time to start up
            } catch (Throwable e) {
                throw new IOException("Could not start control hub" + e);
            }
            
            String host = h.getAddress().toString();            
            System.setProperty("ibis.connect.virtual.hub.host", host);
            
            if (logger.isInfoEnabled()) {
                logger.info("NameServer: created hub on " + host);
            }
        }
             */
                        
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
        serverSocket = socketFactory.createServerSocket(port,
                256, false, null);

        if (CHECKER_INTERVAL != 0) {
            final KeyChecker ck = new KeyChecker(socketFactory, null, 
                            serverSocket.getLocalSocketAddress(),
                            CHECKER_INTERVAL);
            
            // TODO: Use threadpool ? 
            Thread p = new Thread("KeyChecker Upcaller") {
                public void run() {
                    ck.run();
                }
            };
            p.setDaemon(true);
            p.start();
        }

        System.err.println("NameServer created on: " 
                + serverSocket.getLocalSocketAddress());

        
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

/*        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: created server on " + serverSocket);
        }
  */      
        
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
        if (! silent && logger.isDebugEnabled()) {
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

            String[] names = new String[leavers.length];

            for (int i = 0; i < leavers.length; i++) {
                names[i] = leavers[i].name;
            }

            // Let the election server know about it.
            inf.electionKiller.addCorpses(names);

            // Let the receiveport nameserver know about it.
            inf.receivePortKiller.addCorpses(names);
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
            if (e.key == null) {
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("Doing full check");
                }
                poolPinger(true);
            } else if (e.name == null) {
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("Doing check of pool " + e.key);
                }
                poolPinger(e.key, true);
            } else {
                RunInfo p = (RunInfo) pools.get(e.key);
                if (p != null) {
                    if (! silent && logger.isDebugEnabled()) {
                        logger.debug("Doing check of ibis " + e.name);
                    }
                    checkPool(p, e.name, false, e.key);
                }
            }
            synchronized(pingerEntries) {
                // note: other threads may only replace/remove indices != 0.
                pingerEntries.remove(0);
            }
        }
    }

    void addPingerEntry(String key, String name) {

        PingerEntry added = new PingerEntry(key, name);
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
                String key = (String) e.nextElement();
                RunInfo inf = (RunInfo) pools.get(key);
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
                        poolPinger(key, true);
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
                    out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                    out2.writeByte(message);
                    out2.writeInt(info.length - offset);
                    for (int i = offset; i < info.length; i++) {
                        out2.writeInt(info[i].serializedId.length);
                        out2.write(info[i].serializedId);
                        
                        if (! silent && logger.isDebugEnabled()) {
                            logger.debug("NameServer: forwarding "
                                    + type(message) + " of "
                                    + info[i].name + " to " + dest + " DONE");
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
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: forwarders decr: " + inf.forwarders);
                }
                if (failed) {
                    inf.failed++;
                    if (! silent && logger.isDebugEnabled()) {
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

        if (! silent && logger.isDebugEnabled()) {
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
            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: forwarders = " + inf.forwarders);
            }
            ThreadPool.createNew(forwarder, "Forwarder thread");
        }
    }

    private class PingThread implements Runnable {
        RunInfo run;
        IbisInfo dest;
        String key;
        Vector deadIbises;

        PingThread(RunInfo run, IbisInfo dest, String key, Vector deadIbises) {
            this.run = run;
            this.dest = dest;
            this.key = key;
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
                out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                out2.writeByte(IBIS_PING);
                out2.flush();
                in2 = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                String k = in2.readUTF();
                String name = in2.readUTF();
                if (!k.equals(key) || ! name.equals(dest.name)) {
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
     * @param victim the name of the ibis instance that must be checked/killed,
     *     or <code>null</code>, in which case the whole pool is checked/killed.
     * @param kill <code>true</code> when the victim must be killed,
     *     <code>false</code> otherwise.
     * @param key the key of the pool.
     */
    private void checkPool(RunInfo p, String victim, boolean kill, String key) {

        Vector deadIbises = new Vector();

        if (! silent && logger.isInfoEnabled()) {
            logger.info("Testing pool " + key + " for dead ibises");
        }
        
        synchronized(p) {
            // Obtain elements to send to first.
            IbisInfo[] elts = p.instances();
            for (int i = 0; i < elts.length; i++) {
                IbisInfo temp = elts[i];
/*
                if (victim == null || (! kill &&  temp.name.equals(victim))) {
                    p.pingers++;
                    
                    logger.info("Creating pinger for ibis " + key + " / " 
                            + temp.name);
                    
                    PingThread pt = new PingThread(p, temp, key, deadIbises);
                    
                    while (p.pingers > MAXTHREADS) {
                        try {
                            p.wait();
                        } catch(Exception ex) {
                            // ignored
*/
                if (victim == null || temp.name.equals(victim)) {
                    if (! kill) {
                        PingThread pt = new PingThread(p, temp, key, deadIbises);
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
/*
                    ThreadPool.createNew(pt, "Ping thread");
                } else if (kill &&  temp.name.equals(victim)) {
                    logger.info("Ibis " + key + " / " + temp.name 
                            + " declared dead by user");
                    
                    toDie = temp;
                    deadIbises.add(temp);
                    */
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
                    logger.info("NameServer: ibis " + temp.name + " seems dead");
                }

                p.remove(temp);
            }

            if (deadIbises.size() != 0) {
                // Put the dead ones in an array.
                String[] ids = new String[deadIbises.size()];
                IbisInfo[] ibisIds = new IbisInfo[ids.length];
                for (int j = 0; j < ids.length; j++) {
                    IbisInfo temp2 = (IbisInfo) deadIbises.get(j);
                    ids[j] = temp2.name;
                    ibisIds[j] = temp2;
                }

                // Pass the dead ones on to the election server and
                // receiveport nameserver
                p.electionKiller.addCorpses(ids);
                p.receivePortKiller.addCorpses(ids);

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
                            logger.warn("nameserver waitting for pool " + key + 
                                   " pingers to return (" + p.forwarders + ")");
                        }
                        p.wait(1000);
                    } catch(Exception ex) {
                        // ignored
                    }
                }
            }

            p.pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;

            if (p.pool.size() == 0) {
                pools.remove(key);
                if (! silent) {
                    logger.warn("pool " + key + " seems to be dead.");
                }
                killThreads(p);
            }
        }
    }

    private void handleIbisIsalive(boolean kill) throws IOException {
        
        String key = in.readUTF();
        String name = in.readUTF();

        logger.debug("Got handleIbisIsalive(" + kill + ") " + key + "/" + name);
        
        if (! kill) {
            addPingerEntry(key, name);
            return;
        }

        RunInfo p = (RunInfo) pools.get(key);
        if (p != null) {
            checkPool(p, name, kill, key);
        }
    }

    private void handleCheck() throws IOException {
        String key = in.readUTF();
        if (! silent && logger.isInfoEnabled()) {
            logger.info("Got check for pool " + key);
        }
        addPingerEntry(key, null);
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
    
    private void writeVirtualSocketAddress(DataOutputStream out, VirtualSocketAddress a) throws IOException {         
        //byte [] buf = Conversion.object2byte(a);
        //out.writeInt(buf.length);
        //out.write(buf);
        out.writeUTF(a.toString());
    }
    
    private VirtualSocketAddress readVirtualSocketAddress(DataInputStream in) throws IOException {
        /*int len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully(buf, 0, len);

        try {
            return (VirtualSocketAddress) Conversion.byte2object(buf);
        } catch(ClassNotFoundException e) {
            throw new IOException("Could not read InetAddress");
        }*/
        
        return new VirtualSocketAddress(in.readUTF());
    }
        
    private void handleIbisJoin(long startTime) throws IOException {
        String key = in.readUTF();
        String name = in.readUTF();
        
        int len = in.readInt();
        byte[] serializedId = new byte[len];
        in.readFully(serializedId, 0, len);
        // System.out.println("Join: serialized id length = " + len);
    
        VirtualSocketAddress address = readVirtualSocketAddress(in);
        // System.out.println("After readVirtualSocketAddress: " +
        //         (System.currentTimeMillis() - startTime));

        boolean needsUpcalls = in.readBoolean();

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + key + " requested by "
                    + name +", address " + address);
        }

        IbisInfo info = new IbisInfo(name, serializedId, address, needsUpcalls);

        RunInfo p = (RunInfo) pools.get(key);

        if (p == null) {
            // new run
            //
            if (singleRun && joined) {
                out.writeByte(IBIS_REFUSED);

                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: join to pool " + key + " of ibis "
                            + name + " refused");
                }
                out.flush();
                return;
            }
            poolPinger(false);
            p = new RunInfo(silent);

            pools.put(key, p);
            joined = true;

            if (! silent && logger.isInfoEnabled()) {
                logger.info("NameServer: new pool " + key + " created");
            }
        }

        if (p.pool.containsKey(info.name)) {
            out.writeByte(IBIS_REFUSED);

            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                        + name + " refused");
            }
            out.flush();
        } else {
            // System.out.println("before poolPinger: " +
            //         (System.currentTimeMillis() - startTime));
            poolPinger(key, false);
            // System.out.println("after poolPinger: " +
            //         (System.currentTimeMillis() - startTime));
            // Handle delayed leave messages before adding new members
            // to a pool, otherwise new members get leave messages from nodes
            // that they have never seen.

            out.writeByte(IBIS_ACCEPTED);
            writeVirtualSocketAddress(out, p.portTypeNameServer.getAddress());
            writeVirtualSocketAddress(out, p.receivePortNameServer.getAddress());
            writeVirtualSocketAddress(out, p.electionServer.getAddress());

            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                    + name + " accepted");
            }
            // System.out.println("before synchronized block: " +
            //         (System.currentTimeMillis() - startTime));

            synchronized(p) {
                sendLeavers(p);
                p.pool.put(info.name, info);
                p.unfinishedJoins.add(info);
                p.arrayPool.add(info);
            }
            // System.out.println("after synchronized block: " +
            //         (System.currentTimeMillis() - startTime));

            // first send all existing nodes (including the new one) to the
            // new one.
            if (needsUpcalls) {
                out.writeInt(p.pool.size());

                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("Sending " + p.pool.size() + " nodes to " + name);
                }

                int i = 0;
                while (i < p.arrayPool.size()) {
                    IbisInfo temp = (IbisInfo) p.pool.get(((IbisInfo) p.arrayPool.get(i)).name);

                    if (temp != null) {
                        out.writeInt(temp.serializedId.length);
                        if (! silent && logger.isDebugEnabled()) {
                            logger.debug("Sending " + temp.name + " to " + name);
                        }
                        out.write(temp.serializedId);
                        i++;
                    } else {
                        p.arrayPool.remove(i);
                    }
                }

                //send all nodes about to leave to the new one
                out.writeInt(p.toBeDeleted.size());

                for (i = 0; i < p.toBeDeleted.size(); i++) {
                    IbisInfo temp = (IbisInfo) p.toBeDeleted.get(i);
                    out.writeInt(temp.serializedId.length);
                    out.write(temp.serializedId);
                }
            }
            out.flush();

            if (! silent && logger.isInfoEnabled()) {
                logger.info("" + name + " JOINS  pool " + key
                        + " (" + p.pool.size() + " nodes)");
            }
            // System.out.println("after write answer: " +
            //         (System.currentTimeMillis() - startTime));
        }
    }

    private void poolPinger(String key, boolean force) {

        RunInfo p = (RunInfo) pools.get(key);

        if (p == null) {
            return;
        }

        if (! force) {
            long t = System.currentTimeMillis();

            // If the pool has not reached its ping-limit yet, return.
            if (t < p.pingLimit) {
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: ping timeout not reached yet for pool " + key);
                }
                return;
            }
        }

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: ping pool " + key);
        }

        checkPool(p, null, false, key);
    }

    /**
     * Checks all pools to see if they still are alive. If a pool is dead
     * (connect to all members fails), the pool is killed.
     */
    private void poolPinger(boolean force) {
        for (Enumeration e = pools.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            poolPinger(key, force);
        }
    }

    private void killThreads(RunInfo p) {
        VirtualSocket s = null;
        VirtualSocket s2 = null;
        VirtualSocket s3 = null;
        DataOutputStream out1 = null;
        DataOutputStream out2 = null;
        DataOutputStream out3 = null;

        p.electionKiller.quit();
        p.receivePortKiller.quit();
        
        try {
            s = socketFactory.createClientSocket(
                    p.portTypeNameServer.getAddress(), CONNECT_TIMEOUT, null);
            out1 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out1.writeByte(PORTTYPE_EXIT);
        } catch (IOException e) {
            // Ignore.
        } finally {
            VirtualSocketFactory.close(s, out1, null);
            s = null;
        }
        
        try {
            s2 = socketFactory.createClientSocket(
                    p.receivePortNameServer.getAddress(), CONNECT_TIMEOUT, null);
            out2 = new DataOutputStream(new BufferedOutputStream(s2.getOutputStream()));
            out2.writeByte(PORT_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            VirtualSocketFactory.close(s2, out2, null);
        }
        
        try {
            s3 = socketFactory.createClientSocket(
                    p.electionServer.getAddress(), CONNECT_TIMEOUT, null);
            out3 = new DataOutputStream(new BufferedOutputStream(s3.getOutputStream()));
            out3.writeByte(ELECTION_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            VirtualSocketFactory.close(s3, out3, null);
        }
    }

    private void handleIbisLeave() throws IOException {
        String key = in.readUTF();
        String name = in.readUTF();

        RunInfo p = (RunInfo) pools.get(key);

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: leave from pool " + key
                    + " requested by " + name);
        }

        if (p == null) {
            // new run
            if (! silent) {
                logger.error("NameServer: unknown ibis " + name
                        + "/" + key + " tried to leave");
            }
        } else {
            IbisInfo iinf = (IbisInfo) p.pool.get(name);

            if (iinf != null) {
                // found it.
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: leave from pool " + key
                            + " of ibis " + name + " accepted");
                }

                // Also forward the leave to the requester.
                // It is used as an acknowledgement, and
                // the leaver is only allowed to exit when it
                // has received its own leave message.
                synchronized(p) {
                    p.leavers.add(iinf);
                    p.toBeDeleted.remove(iinf);
                    p.remove(iinf);
                }

                if (! silent && logger.isInfoEnabled()) {
                    logger.info(name + " LEAVES pool " + key
                            + " (" + p.pool.size() + " nodes)");
                }

                if (p.pool.size() == 0) {
                    if (! silent && logger.isInfoEnabled()) {
                        logger.info("NameServer: removing pool " + key);
                    }

                    // Send leavers before removing this run
                    synchronized(p) {
                        sendLeavers(p);
                    }

                    pools.remove(key);
                    killThreads(p);
                }
            } else {
                if (! silent) {
                    logger.error("NameServer: unknown ibis " + name
                        + "/" + key + " tried to leave");
                }
            }
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: confirming LEAVE " + name);
        }
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisMustLeave() throws IOException {
        String key = in.readUTF();
        RunInfo p = (RunInfo) pools.get(key);
        int count = in.readInt();
        String[] names = new String[count];
        IbisInfo[] iinf = new IbisInfo[count];

        for (int i = 0; i < count; i++) {
            names[i] = in.readUTF();
        }

        if (p == null) {
            if (! silent) {
                logger.error("NameServer: unknown pool " + key);
            }
            return;
        }
        // TODO ...
        //

        int found = 0;

        synchronized(p) {
            for (int i = 0; i < count; i++) {
                IbisInfo info = (IbisInfo) p.pool.get(names[i]);
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
        LinkedList jobs = new LinkedList();
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
                    s = (VirtualSocket) jobs.remove(0);
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

                if (! silent && logger.isInfoEnabled()) {
                    logger.debug("NameServer: incoming connection from "
                            + s.toString());
                }
            } catch (Exception e) {
                if (! silent) {
                    logger.error("NameServer got an error", e);
                }
                continue;
            }
            /*
<<<<<<< .mine

            out = null;
            in = null;

            try {
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

                opcode = in.readByte();

                logger.debug("NameServer got opcode: " + opcode);
                
                switch (opcode) {
                case (IBIS_ISALIVE):
                case (IBIS_DEAD):
                    logger.debug("NameServer handling opcode IBIS_ISALIVE/IBIS_DEAD");
                    handleIbisIsalive(opcode == IBIS_DEAD);
                    break;
                case (IBIS_JOIN):
                    handleIbisJoin();
                    break;
                case (IBIS_MUSTLEAVE):
                    handleIbisMustLeave();
                    break;
                case (IBIS_LEAVE):
                    handleIbisLeave();
                    if (singleRun && pools.size() == 0) {
                        if (joined) {
                            stop = true;
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
                    logger.error("Got an exception in NameServer.run <Rob doesn't want a stacktrace!>");
                }
            } finally {
                VirtualSocketFactory.close(s, out, in);
            }
=======*/
            reqHandler.addJob(s);
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new RuntimeException("NameServer got an error", e);
        }

/* TODO -- fix!!
  
        if (h != null) {
            h.waitForCount(1);
        }
*/
        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: exit");
        }
    }

    public void handleRequest(VirtualSocket s) {
        int opcode = -1;
        out = null;
        in = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();

        try {

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(baos));

            opcode = in.readByte();

            logger.debug("NameServer got opcode: " + opcode);
            
            switch (opcode) {
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
                    if (joined) {
                        stop = true;
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
            boolean retry, boolean poolserver, boolean starthub, boolean silent) {
        NameServer ns = null;
        while (true) {
            try {
                ns = new NameServer(singleRun, poolserver, starthub, silent);
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
        boolean starthub = true; 
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
            } else if (args[i].equals("-no-hub")) {
                starthub = false;
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
            Properties p = System.getProperties();
            String singleS = p.getProperty(NSProps.s_single);

            single = (singleS != null && singleS.equals("true"));
        }

        ns = createNameServer(single, retry, pool_server, starthub, silent);

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
