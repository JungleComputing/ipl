/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.controlHub.ControlHub;
import ibis.impl.nameServer.NSProps;
import ibis.ipl.IbisRuntimeException;
import ibis.io.Conversion;
import ibis.util.IPUtils;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;

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
import java.util.Calendar;
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
        = TypedProperties.intProperty(NSProps.s_timeout, 60) * 1000;
        // Property is in seconds, convert to milliseconds.

    static int JOINER_INTERVAL
        = TypedProperties.intProperty(NSProps.s_joiner_interval, 1) * 1000;

    InetAddress myAddress;

    static class IbisInfo {
        String name;
        byte[] serializedId;
        int ibisNameServerport;
        InetAddress ibisNameServerAddress;

        IbisInfo(String name, byte[] serializedId,
                InetAddress ibisNameServerAddress, int ibisNameServerport) {
            this.name = name;
            this.serializedId = serializedId;
            this.ibisNameServerAddress = ibisNameServerAddress;
            this.ibisNameServerport = ibisNameServerport;
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

    static class RunInfo {
        ArrayList pool; // a list of IbisInfos

        ArrayList leavers;

        int joinsSent = 0;

        Vector toBeDeleted; // a list of ibis names

        PortTypeNameServer portTypeNameServer;

        ReceivePortNameServer receivePortNameServer;

        ElectionServer electionServer;

        long pingLimit;

        RunInfo(boolean silent) throws IOException {
            pool = new ArrayList();
            leavers = new ArrayList();
            toBeDeleted = new Vector();
            portTypeNameServer = new PortTypeNameServer(silent);
            receivePortNameServer = new ReceivePortNameServer(silent);
            electionServer = new ElectionServer(silent);
            pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;
        }

        public String toString() {
            String res = "runinfo:\n" + "  pool = \n";

            for (int i = 0; i < pool.size(); i++) {
                res += "    " + pool.get(i) + "\n";
            }

            res += "  toBeDeleted = \n";

            for (int i = 0; i < toBeDeleted.size(); i++) {
                res += "    " + ((IbisInfo) (toBeDeleted.get(i))).name + "\n";
            }

            return res;
        }
    }

    private static boolean nameServerCreated = false;

    private Hashtable pools;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private boolean singleRun;

    private boolean joined;

    private boolean silent;

    private static boolean controlHubStarted = false;

    private static boolean poolServerStarted = false;

    private static ControlHub h = null;

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

        if (controlhub && !controlHubStarted) {
            if (hubPort == null) {
                hubPort = Integer.toString(port + 2);
                System.setProperty("ibis.connect.hub.port", hubPort);
            }
            try {
                h = new ControlHub();
                h.setDaemon(true);
                h.start();
                Thread.sleep(2000); // Give it some time to start up
            } catch (Throwable e) {
                throw new IOException("Could not start control hub" + e);
            }
            controlHubStarted = true;
        }

        if (poolserver && !poolServerStarted) {
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
            poolServerStarted = true;
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

        if (! silent && logger.isInfoEnabled()) {
            logger.info("NameServer: created server on " + serverSocket);
        }
    }

    private void sendLeavers(RunInfo inf) {
        if (logger.isDebugEnabled()) {
            logger.debug("sendLeavers ... size = " + inf.leavers.size());
        }
        synchronized(inf) {
            if (inf.leavers.size() != 0) {
                IbisInfo[] iinf = new IbisInfo[0];
                iinf = (IbisInfo[]) inf.leavers.toArray(iinf);
                for (int i = 0; i < inf.pool.size(); i++) {
                    forwardLeave((IbisInfo) inf.pool.get(i), iinf);
                }
                for (int i = 0; i < iinf.length; i++) {
                    forwardLeave(iinf[i], iinf);
                }
                inf.leavers.clear();
            }
        }
    }

    private void upcaller() {
        for (;;) {
            try {
                Thread.sleep(JOINER_INTERVAL);
            } catch(InterruptedException e) {
                // ignore
            }
            for (Enumeration e = pools.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                RunInfo inf = (RunInfo) pools.get(key);
                synchronized(inf) {
                    sendLeavers(inf);
                    if (inf.joinsSent < inf.pool.size()) {
                        IbisInfo[] message = new IbisInfo[inf.pool.size() - inf.joinsSent];
                        for (int i = 0; i < message.length; i++) {
                            message[i] = (IbisInfo) inf.pool.get(i + inf.joinsSent);
                        }
                        for (int i = 0; i < inf.joinsSent; i++) {
                            forwardJoin((IbisInfo) inf.pool.get(i), message, 0);
                        }

                        for (int i = inf.joinsSent; i < inf.pool.size(); i++) {
                            forwardJoin((IbisInfo) inf.pool.get(i), message, i - inf.joinsSent + 1);
                        }
                        inf.joinsSent = inf.pool.size();
                    }
                }
            }
        }
    }

    private void forwardJoin(IbisInfo dest, IbisInfo[] info, int offset) {

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: forwarding joins to " + dest.name);
        }

        if (offset >= info.length) {
            return;
        }

        Socket s = null;
        DataOutputStream out2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);
            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(IBIS_JOIN);
            out2.writeInt(info.length - offset);
            for (int i = offset; i < info.length; i++) {
                out2.writeInt(info[i].serializedId.length);
                out2.write(info[i].serializedId);

                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: forwarding join of "
                            + info[i].name + " to " + dest.name + " DONE");
                }
            }
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward join to " + dest.name, e);
            }
        } finally {
            closeConnection(null, out2, s);
        }
    }

    private boolean doPing(IbisInfo dest, String key) {
        Socket s = null;
        DataOutputStream out2 = null;
        DataInputStream in2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);
            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(IBIS_PING);
            out2.flush();
            in2 = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            String k = in2.readUTF();
            if (!k.equals(key)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            closeConnection(in2, out2, s);
        }
        return true;
    }

    private void checkPool(RunInfo p, String victim, String key) {

        Vector deadIbises = new Vector();

        synchronized(p) {
            for (int i = 0; i < p.pool.size(); i++) {
                IbisInfo temp = (IbisInfo) p.pool.get(i);
                if (victim == null || ! temp.name.equals(victim)) {
                    if (doPing(temp, key)) {
                        continue;
                    }
                }
                deadIbises.add(temp);
                if (p.joinsSent > i) {
                    p.joinsSent--;
                }
                p.pool.remove(i);
            }

            if (deadIbises.size() != 0) {
                // Put the dead ones in an array.
                String[] ids = new String[deadIbises.size()];
                byte[][] ibisIds = new byte[ids.length][];
                for (int j = 0; j < ids.length; j++) {
                    IbisInfo temp2 = (IbisInfo) deadIbises.get(j);
                    ids[j] = temp2.name;
                    ibisIds[j] = temp2.serializedId;
                }

                // Pass the dead ones on to the election server ...
                try {
                    electionKill(p, ids);
                } catch (IOException e) {
                    // ignored
                }

                // ... and to all other ibis instances in this pool.
                for (int i = 0; i < p.pool.size(); i++) {
                    forwardDead((IbisInfo) p.pool.get(i), ibisIds);
                }
            }

            p.pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;

            if (p.pool.size() == 0) {
                pools.remove(key);
                String date = Calendar.getInstance().getTime().toString();
                if (! silent) {
                    logger.warn(date + " pool " + key + " seems to be dead.");
                }
                killThreads(p);
            }
        }
    }

    private void handleIbisIsalive(boolean kill) throws IOException {
        String key = in.readUTF();
        String name = in.readUTF();

        RunInfo p = (RunInfo) pools.get(key);
        if (p != null) {
            checkPool(p, kill ? name : null, key);
        }
    }

    private void forwardDead(IbisInfo dest, byte[][] ids) {
        Socket s = null;
        DataOutputStream out2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(IBIS_DEAD);
            out2.writeInt(ids.length);
            for (int i = 0; i < ids.length; i++) {
                byte[] buf = ids[i];
                out2.writeInt(buf.length);
                out2.write(buf);
            }
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward dead ibises to "
                        + dest.name, e);
            }
        } finally {
            closeConnection(null, out2, s);
        }
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

        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: join to pool " + key + " requested by "
                    + name +", port " + port);
        }

        IbisInfo info = new IbisInfo(name, serializedId, address, port);
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
            poolPinger();
            p = new RunInfo(silent);

            pools.put(key, p);
            joined = true;

            if (! silent && logger.isInfoEnabled()) {
                logger.info("NameServer: new pool " + key + " created");
            }
        }

        if (p.pool.contains(info)) {
            out.writeByte(IBIS_REFUSED);

            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                        + name + " refused");
            }
            out.flush();
        } else {

            // Handle delayed leave messages before adding new members
            // to a pool, otherwise new members get leave messages from nodes
            // that they have never seen.
            sendLeavers(p);

            out.writeByte(IBIS_ACCEPTED);
            out.writeInt(p.portTypeNameServer.getPort());
            out.writeInt(p.receivePortNameServer.getPort());
            out.writeInt(p.electionServer.getPort());

            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                    + name + " accepted");
            }

            synchronized(p) {
                p.pool.add(info);
            }

            // first send all existing nodes (including the new one) to the
            // new one.
            out.writeInt(p.pool.size());

            for (int i = 0; i < p.pool.size(); i++) {
                IbisInfo temp = (IbisInfo) p.pool.get(i);
                out.writeInt(temp.serializedId.length);
                out.write(temp.serializedId);
            }

            //send all nodes about to leave to the new one
            out.writeInt(p.toBeDeleted.size());

            for (int i = 0; i < p.toBeDeleted.size(); i++) {
                IbisInfo temp = (IbisInfo) p.toBeDeleted.get(i);
                out.writeInt(temp.serializedId.length);
                out.write(temp.serializedId);
            }
            out.flush();

            String date = Calendar.getInstance().getTime().toString();

            if (! silent && logger.isInfoEnabled()) {
                logger.info(date + " " + name + " JOINS  pool " + key
                        + " (" + p.pool.size() + " nodes)");
            }
        }
    }

    private void poolPinger(String key) {
        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: ping pool " + key);
        }

        RunInfo p = (RunInfo) pools.get(key);

        if (p == null) {
            return;
        }

        long t = System.currentTimeMillis();

        // If the pool has not reached its ping-limit yet, return.
        if (t < p.pingLimit) {
            return;
        }

        checkPool(p, null, key);
    }

    /**
     * Checks all pools to see if they still are alive. If a pool is dead
     * (connect to all members fails), the pool is killed.
     */
    private void poolPinger() {
        for (Enumeration e = pools.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            poolPinger(key);
        }
    }

    private void forwardLeave(IbisInfo dest, IbisInfo[] info) {
        if (! silent && logger.isDebugEnabled()) {
            logger.debug("NameServer: forwarding leaves to " + dest.name);
        }

        Socket s = null;
        DataOutputStream out2 = null;

        try {
            s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            out2 = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out2.writeByte(IBIS_LEAVE);
            out2.writeInt(info.length);
            for (int i = 0; i < info.length; i++) {
                if (! silent && logger.isDebugEnabled()) {
                    logger.debug("NameServer: forwarding leave of "
                            + info[i].name + " to " + dest.name);
                }
                out2.writeInt(info[i].serializedId.length);
                out2.write(info[i].serializedId);
            }
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward leave to " + dest.name, e);
            }
        } finally {
            closeConnection(null, out2, s);
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
                    p.portTypeNameServer.getPort(), null, -1);
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
                    p.receivePortNameServer.getPort(), null, -1);
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
                    p.electionServer.getPort(), null, -1);
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
            return;
        }

        IbisInfo iinf = null;
        int index = -1;

        for (int i = 0; i < p.pool.size(); i++) {
            IbisInfo info = (IbisInfo) p.pool.get(i);
            if (info.name.equals(name)) {
                iinf = info;
                index = i;
                break;
            }
        }

        if (iinf != null) {
            // found it.
            if (! silent && logger.isDebugEnabled()) {
                logger.debug("NameServer: leave from pool " + key
                        + " of ibis " + name + " accepted");
            }

            // Let the election server know about it.
            electionKill(p, new String[] { name });

            // Also forward the leave to the requester.
            // It is used as an acknowledgement, and
            // the leaver is only allowed to exit when it
            // has received its own leave message.
            synchronized(p) {
                p.leavers.add(iinf);
                p.toBeDeleted.remove(iinf);
                p.pool.remove(index);
                if (p.joinsSent > index) {
                    p.joinsSent--;
                }
            }

            String date = Calendar.getInstance().getTime().toString();

            if (! silent && logger.isInfoEnabled()) {
                logger.info(date + " " + name + " LEAVES pool " + key
                        + " (" + p.pool.size() + " nodes)");
            }

            if (p.pool.size() == 0) {
                if (! silent && logger.isInfoEnabled()) {
                    logger.info("NameServer: removing pool " + key);
                }

                // Send leavers before removing this run
                sendLeavers(p);

                pools.remove(key);
                killThreads(p);
            }
        } else {
            if (! silent) {
                logger.error("NameServer: unknown ibis " + name
                    + "/" + key + " tried to leave");
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
                case (IBIS_LEAVE):
                    handleIbisLeave();
                    if (singleRun && pools.size() == 0) {
                        if (joined) {
                            stop = true;
                        }
                        // ignore invalid leave req.
                    }
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

        if (h != null) {
            h.waitForCount(1);
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
        if (nameServerCreated) {
            return null;
        }
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
                    return null;
                }
            }
        }
        nameServerCreated = true;
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
