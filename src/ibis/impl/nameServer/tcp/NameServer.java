/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.controlHub.ControlHub;
import ibis.impl.nameServer.NSProps;
import ibis.ipl.IbisRuntimeException;
import ibis.io.Conversion;
import ibis.util.IPUtils;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;
import ibis.util.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.log4j.Logger;

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

    static final int MAXTHREADS = 32;

    InetAddress myAddress;

    static class IbisInfo {
        String name;
        byte[] serializedId;
        int ibisNameServerport;
        InetAddress ibisNameServerAddress;
        boolean needsUpcalls;
        boolean completelyJoined = false;

        IbisInfo(String name, byte[] serializedId,
                InetAddress ibisNameServerAddress, int ibisNameServerport,
                boolean needsUpcalls) {
            this.name = name;
            this.serializedId = serializedId;
            this.ibisNameServerAddress = ibisNameServerAddress;
            this.ibisNameServerport = ibisNameServerport;
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
            return "ibisInfo(" + name + "at " + ibisNameServerAddress
                    + ":" + ibisNameServerport + ")";
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

        ElectionServer electionServer;

        long pingLimit;

        RunInfo(boolean silent) throws IOException {
            unfinishedJoins = new ArrayList();
            arrayPool = new ArrayList();
            pool = new Hashtable();
            leavers = new ArrayList();
            toBeDeleted = new Vector();
            portTypeNameServer = new PortTypeNameServer(silent,
                    NameServerClient.socketFactory);
            receivePortNameServer = new ReceivePortNameServer(silent,
                    NameServerClient.socketFactory);
            electionServer = new ElectionServer(silent,
                    NameServerClient.socketFactory);
            pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;
        }

        public String toString() {
            String res = "runinfo:\n" + "  pool = \n";

            for (Enumeration e = pool.elements(); e.hasMoreElements();) {
                res += "    " + e.nextElement() + "\n";
            }

            res += "  toBeDeleted = \n";

            for (int i = 0; i < toBeDeleted.size(); i++) {
                res += "    " + ((IbisInfo) (toBeDeleted.get(i))).name + "\n";
            }

            return res;
        }

        public void remove(IbisInfo iinf) {
            pool.remove(iinf.name);
            if (! iinf.completelyJoined) {
                int index = unfinishedJoins.indexOf(iinf);
                if (index == -1) {
                    logger.error("Internal error: " + iinf.name + " not completelyJoined but not in unfinishedJoins!");
                } else {
                    unfinishedJoins.remove(index);
                }
            }
        }
    }

    private Hashtable pools;

    private ArrayList pingerEntries = new ArrayList();

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private boolean singleRun;

    private boolean joined;

    boolean silent;

    private ControlHub controlHub = null;

    static Logger logger = 
            ibis.util.GetLogger.getLogger(NameServer.class.getName());

    private NameServer(boolean singleRun, boolean poolserver,
            boolean controlhub, boolean silent) throws IOException {

        this.singleRun = singleRun;
        this.joined = false;
        this.silent = silent;

        myAddress = IPUtils.getAlternateLocalHostAddress();
        myAddress = InetAddress.getByName(myAddress.getHostName());

        String hubPort = System.getProperty("ibis.connect.hub.port");
        String poolPort = System.getProperty("ibis.pool.server.port");
        int port = TCP_IBIS_NAME_SERVER_PORT_NR;

        if (controlhub) {
            if (hubPort == null) {
                hubPort = Integer.toString(port + 2);
                System.setProperty("ibis.connect.hub.port", hubPort);
            }
            try {
                controlHub = new ControlHub();
                controlHub.setDaemon(true);
                controlHub.start();
                Thread.sleep(2000); // Give it some time to start up
            } catch (Throwable e) {
                throw new IOException("Could not start control hub" + e);
            }
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

        if (CHECKER_INTERVAL != 0) {
            final KeyChecker ck
                    = new KeyChecker(null, myAddress.getHostName(), port,
                        CHECKER_INTERVAL);
            Thread p = new Thread("KeyChecker Upcaller") {
                public void run() {
                    ck.run();
                }
            };
            p.setDaemon(true);
            p.start();
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: singleRun = " + singleRun);
        }

        // Create a server socket.
        serverSocket = NameServerClient.socketFactory.createServerSocket(port,
                null, 50, false, null);

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
            logger.info("NameServer: created server on " + serverSocket);
        }
    }

    // Should be called within synchronized on inf.
    private void sendLeavers(RunInfo inf) {
        if (logger.isDebugEnabled() && inf.leavers.size() > 0) {
            logger.debug("sendLeavers ... size = " + inf.leavers.size());
        }

        IbisInfo[] leavers = null;

        if (inf.leavers.size() != 0) {
            IbisInfo[] iinf = new IbisInfo[0];
            leavers = (IbisInfo[]) inf.leavers.toArray(iinf);
            inf.leavers.clear();

            for (Enumeration e = inf.pool.elements(); e.hasMoreElements();) {
                IbisInfo ibisInf = (IbisInfo) e.nextElement();
                if (ibisInf.needsUpcalls) {
                    forward(IBIS_LEAVE, inf, ibisInf, leavers, 0);
                }
            }

            for (int i = 0; i < leavers.length; i++) {
                if (leavers[i].needsUpcalls) {
                    forward(IBIS_LEAVE, inf, leavers[i], leavers, 0);
                }
            }
            while (inf.forwarders != 0) {
                try {
                    inf.wait();
                } catch(Exception ex) {
                    // ignored
                }
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Doing full check");
                }
                poolPinger(true);
            } else if (e.name == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Doing check of pool " + e.key);
                }
                poolPinger(e.key, true);
            } else {
                RunInfo p = (RunInfo) pools.get(e.key);
                if (p != null) {
                    if (logger.isDebugEnabled()) {
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
                    sendLeavers(inf);
                    inf.failed = 0;
                    inf.forwarders = 0;

                    if (inf.unfinishedJoins.size() > 0) {
                        IbisInfo[] message = (IbisInfo[])
                                inf.unfinishedJoins.toArray(new IbisInfo[0]);

                        inf.unfinishedJoins.clear();

                        for (Enumeration e2 = inf.pool.elements(); e2.hasMoreElements();) {
                            IbisInfo ibisInf = (IbisInfo) e2.nextElement();
                            if (ibisInf.completelyJoined && ibisInf.needsUpcalls) {
                                forward(IBIS_JOIN, inf, ibisInf, message, 0);
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
            Socket s = null;
            DataOutputStream out2 = null;
            boolean failed = true;

            // QUICK HACK -- JASON
            for (int h=0;h<3;h++) { 
                try {
                    s = NameServerClient.socketFactory.createClientSocket(
                            dest.ibisNameServerAddress, dest.ibisNameServerport, null, CONNECT_TIMEOUT);
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
                    closeConnection(null, out2, s);
                    break;
                } catch (Exception e) {
                    closeConnection(null, out2, s);
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
                if (failed) {
                    inf.failed++;
                }
                inf.notifyAll();
            }
        }
    }

    private void forward(byte message, RunInfo inf, IbisInfo dest,
            IbisInfo[] info, int offset) {

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: forwarding to " + dest);
        }

        if (offset >= info.length) {
            return;
        }

        Forwarder forwarder = new Forwarder(message, inf, dest, info, offset);

        synchronized(inf) {
            while (inf.forwarders > MAXTHREADS) {
                try {
                    inf.wait();
                } catch(Exception e) {
                    // Ignored
                }
            }
            inf.forwarders++;
        }
        ThreadPool.createNew(forwarder, "Forwarder thread");
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
            Socket s = null;
            DataOutputStream out2 = null;
            DataInputStream in2 = null;

            try {
                s = NameServerClient.socketFactory.createClientSocket(
                        dest.ibisNameServerAddress, dest.ibisNameServerport, null, CONNECT_TIMEOUT);
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
                closeConnection(in2, out2, s);
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

        synchronized(p) {
            for (Enumeration e = p.pool.elements(); e.hasMoreElements();) {
                IbisInfo temp = (IbisInfo) e.nextElement();
                if (victim == null || temp.name.equals(victim)) {
                    if (! kill) {
                        p.pingers++;
                        PingThread pt = new PingThread(p, temp, key, deadIbises);
                        while (p.pingers > MAXTHREADS) {
                            try {
                                p.wait();
                            } catch(Exception ex) {
                                // ignored
                            }
                        }
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

                // Pass the dead ones on to the election server ...
                try {
                    electionKill(p, ids);
                } catch (IOException e) {
                    // ignored
                }

                // Pass the dead ones on to the receiveport nameserver ...
                try {
                    receiveportKill(p, ids);
                } catch (IOException e) {
                    // ignored
                }

                // ... and to all other ibis instances in this pool.
                synchronized(p) {
                    for (Enumeration e = p.pool.elements(); e.hasMoreElements();) {
                        IbisInfo ibisInf = (IbisInfo) e.nextElement();
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
                        } catch(Exception ex) {
                            // ignored
                        }
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
        logger.info("Got check for pool " + key);
        addPingerEntry(key, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleCheckAll() throws IOException {
        logger.info("Got checkAll");
        addPingerEntry(null, null);
        out.writeByte(0);
        out.flush();
    }

    private void handleIbisJoin() throws IOException {
        String key = in.readUTF();
        String name = in.readUTF();
        int len = in.readInt();
        byte[] serializedId = new byte[len];
        in.readFully(serializedId, 0, len);
        len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully(buf, 0, len);
        InetAddress address = null;
        try {
            address = (InetAddress) Conversion.byte2object(buf);
        } catch(ClassNotFoundException e) {
            throw new IOException("Could not read InetAddress");
        }
        int port = in.readInt();

        boolean needsUpcalls = in.readBoolean();

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + key + " requested by "
                    + name +", port " + port);
        }

        IbisInfo info = new IbisInfo(name, serializedId, address, port,
                needsUpcalls);

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
            poolPinger(key, false);
            // Handle delayed leave messages before adding new members
            // to a pool, otherwise new members get leave messages from nodes
            // that they have never seen.

            out.writeByte(IBIS_ACCEPTED);
            out.writeInt(p.portTypeNameServer.getPort());
            out.writeInt(p.receivePortNameServer.getPort());
            out.writeInt(p.electionServer.getPort());

            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                    + name + " accepted");
            }

            synchronized(p) {
                sendLeavers(p);
                p.pool.put(info.name, info);
                p.unfinishedJoins.add(info);
                p.arrayPool.add(info);
            }

            // first send all existing nodes (including the new one) to the
            // new one.
            if (needsUpcalls) {
                out.writeInt(p.pool.size());

                logger.debug("Sending " + p.pool.size() + " nodes to " + name);

                int i = 0;
                while (i < p.arrayPool.size()) {
                    IbisInfo temp = (IbisInfo) p.pool.get(((IbisInfo) p.arrayPool.get(i)).name);

                    if (temp != null) {
                        out.writeInt(temp.serializedId.length);
                        logger.debug("Sending " + temp.name + " to " + name);
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
        Socket s = null;
        Socket s2 = null;
        Socket s3 = null;
        DataOutputStream out1 = null;
        DataOutputStream out2 = null;
        DataOutputStream out3 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.portTypeNameServer.getPort(), null, CONNECT_TIMEOUT);
            out1 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out1.writeByte(PORTTYPE_EXIT);
        } catch (IOException e) {
            // Ignore.
        } finally {
            closeConnection(null, out1, s);
            s = null;
        }

        try {
            s2 = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.receivePortNameServer.getPort(), null, CONNECT_TIMEOUT);
            out2 = new DataOutputStream(new BufferedOutputStream(s2.getOutputStream()));
            out2.writeByte(PORT_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            closeConnection(null, out2, s2);
        }

        try {
            s3 = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.electionServer.getPort(), null);
            out3 = new DataOutputStream(new BufferedOutputStream(s3.getOutputStream()));
            out3.writeByte(ELECTION_EXIT);
        } catch (IOException e) {
            // ignore
        } finally {
            closeConnection(null, out3, s3);
        }
    }

    /**
     * Notifies the election server of the specified pool that the
     * specified ibis instances are dead.
     * @param p   the specified pool
     * @param ids the dead ibis instances
     * @exception IOException is thrown in case of trouble.
     */
    private void electionKill(RunInfo p, String[] ids)
            throws IOException {
        Socket s = null;
        DataOutputStream out2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.electionServer.getPort(), null, CONNECT_TIMEOUT);
            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(ELECTION_KILL);
            out2.writeInt(ids.length);
            for (int i = 0; i < ids.length; i++) {
                out2.writeUTF(ids[i]);
            }
        } finally {
            closeConnection(null, out2, s);
        }
    }

    /**
     * Notifies the receiveport nameserver of the specified pool that the
     * specified ibis instances are dead.
     * @param p   the specified pool
     * @param ids the dead ibis instances
     * @exception IOException is thrown in case of trouble.
     */
    private void receiveportKill(RunInfo p, String[] ids)
            throws IOException {
        Socket s = null;
        DataOutputStream out2 = null;
        DataInputStream in2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.receivePortNameServer.getPort(), null, CONNECT_TIMEOUT);
            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(PORT_KILL);
            out2.writeInt(ids.length);
            for (int i = 0; i < ids.length; i++) {
                out2.writeUTF(ids[i]);
            }
            out2.flush();
            in2 = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            in2.readInt();
        } finally {
            closeConnection(in2, out2, s);
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

                // Let the election server know about it.
                electionKill(p, new String[] { name });

                // Let the receiveport nameserver know about it.
                receiveportKill(p, new String[] { name });

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

            for (Enumeration e = p.pool.elements(); e.hasMoreElements();) {
                IbisInfo ipp = (IbisInfo) e.nextElement();
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

    public void run() {
        int opcode;
        Socket s;
        boolean stop = false;

        while (!stop) {
            try {
                if (! silent && logger.isInfoEnabled()) {
                    logger.info("NameServer: accepting incoming connections... ");
                }
                s = serverSocket.accept();

                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: incoming connection from "
                            + s.toString());
                }
            } catch (Exception e) {
                if (! silent) {
                    logger.error("NameServer got an error", e);
                }
                continue;
            }

            out = null;
            in = null;

            try {
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

                opcode = in.readByte();

                switch (opcode) {
                case (IBIS_ISALIVE):
                case (IBIS_DEAD):
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
                    logger.error("Got an exception in NameServer.run", e1);
                }
            } finally {
                closeConnection(in, out, s);
            }
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new IbisRuntimeException("NameServer got an error", e);
        }

        if (controlHub != null) {
            controlHub.waitForCount(1);
        }

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: exit");
        }
    }
    
    public int port() {
        return serverSocket.getLocalPort();
    }

    public static synchronized NameServer createNameServer(boolean singleRun,
            boolean retry, boolean poolserver, boolean controlhub,
            boolean silent) {
        NameServer ns = null;
        while (true) {
            try {
                ns = new NameServer(singleRun, poolserver, controlhub, silent);
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
                out.flush();
            } catch (Exception e) {
                // ignore
            }
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

    public static void main(String[] args) {
        boolean single = false;
        boolean silent = false;
        boolean control_hub = false;
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
            } else if (args[i].equals("-controlhub")) {
                control_hub = true;
            } else if (args[i].equals("-no-controlhub")) {
                control_hub = false;
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

        ns = createNameServer(single, retry, pool_server, control_hub, silent);

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
