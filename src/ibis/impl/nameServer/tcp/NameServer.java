/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.controlHub.ControlHub;
import ibis.impl.nameServer.NSProps;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.util.IPUtils;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

public class NameServer extends Thread implements Protocol {

    public static final int TCP_IBIS_NAME_SERVER_PORT_NR
            = TypedProperties.intProperty(NSProps.s_port, 9826);

    static int PINGER_TIMEOUT = TypedProperties.intProperty(NSProps.s_timeout,
            60) * 1000; // Property is in seconds, convert to milliseconds.

    InetAddress myAddress;

    static class IbisInfo {
        IbisIdentifier identifier;

        int ibisNameServerport;

        InetAddress ibisNameServerAddress;

        IbisInfo(IbisIdentifier identifier, InetAddress ibisNameServerAddress,
                int ibisNameServerport) {
            this.identifier = identifier;
            this.ibisNameServerAddress = ibisNameServerAddress;
            this.ibisNameServerport = ibisNameServerport;
        }

        public boolean equals(Object other) {
            if (other instanceof IbisInfo) {
                return identifier.equals(((IbisInfo) other).identifier);
            }
            return false;
        }

        public int hashCode() {
            return identifier.hashCode();
        }

        public String toString() {
            return "ibisInfo(" + identifier + "at " + ibisNameServerAddress
                    + ":" + ibisNameServerport + ")";
        }
    }

    static class RunInfo {
        Vector pool; // a list of IbisInfos

        Vector toBeDeleted; // a list of ibis identifiers

        PortTypeNameServer portTypeNameServer;

        ReceivePortNameServer receivePortNameServer;

        ElectionServer electionServer;

        long pingLimit;

        RunInfo() throws IOException {
            pool = new Vector();
            toBeDeleted = new Vector();
            portTypeNameServer = new PortTypeNameServer();
            receivePortNameServer = new ReceivePortNameServer();
            electionServer = new ElectionServer();
            pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;
        }

        public String toString() {
            String res = "runinfo:\n" + "  pool = \n";

            for (int i = 0; i < pool.size(); i++) {
                res += "    " + pool.get(i) + "\n";
            }

            res += "  toBeDeleted = \n";

            for (int i = 0; i < toBeDeleted.size(); i++) {
                res += "    " + toBeDeleted.get(i) + "\n";
            }

            return res;
        }
    }

    private static boolean nameServerCreated = false;

    private Hashtable pools;

    private ServerSocket serverSocket;

    private ObjectInputStream in;

    private ObjectOutputStream out;

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

        if (! silent) {
            logger.info("NameServer: singleRun = " + singleRun);
        }

        // Create a server socket.
        serverSocket = NameServerClient.socketFactory.createServerSocket(port,
                null, 50, false, null);

        pools = new Hashtable();

        if (! silent) {
            logger.info("NameServer: created server on " + serverSocket);
        }
    }

    private void forwardJoin(IbisInfo dest, IbisIdentifier id) {

        if (! silent) {
            logger.debug("NameServer: forwarding join of "
                    + id.toString() + " to " + dest.ibisNameServerAddress
                    + ", dest port: " + dest.ibisNameServerport);
        }
        try {

            Socket s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_JOIN);
            out2.writeObject(id);
            closeConnection(null, out2, s);

            if (! silent) {
                logger.debug("NameServer: forwarding join of "
                        + id.toString() + " to " + dest.identifier.toString()
                        + " DONE");
            }
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward join of " + id.toString()
                        + " to " + dest.identifier.toString(), e);
            }
        }

    }

    private boolean doPing(IbisInfo dest, String key) {
        try {
            Socket s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_PING);
            out2.flush();
            DummyInputStream i = new DummyInputStream(s.getInputStream());
            DataInputStream in2 = new DataInputStream(
                    new BufferedInputStream(i));
            String k = in2.readUTF();
            closeConnection(in2, out2, s);
            if (!k.equals(key)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void checkPool(RunInfo p, IbisIdentifier victim, String key) {

        Vector deadIbises = new Vector();
        if (victim != null) {
            deadIbises.add(victim);
        }

        for (int i = 0; i < p.pool.size(); i++) {
            IbisInfo temp = (IbisInfo) p.pool.get(i);
            if (victim != null && temp.identifier.equals(victim)) {
                continue;
            }
            if (doPing(temp, key)) {
                continue;
            }
            deadIbises.add(temp);
        }

        if (deadIbises.size() != 0) {

            // Remove the dead ones from the pool.
            p.pool.removeAll(deadIbises);

            // Put the dead ones in an array.
            IbisIdentifier[] ids = new IbisIdentifier[deadIbises.size()];
            for (int j = 0; j < ids.length; j++) {
                IbisInfo temp2 = (IbisInfo) deadIbises.get(j);
                ids[j] = temp2.identifier;
                temp2.identifier.free();
            }

            // Pass the dead ones on to the election server ...
            try {
                electionKill(p, ids);
            } catch (IOException e) {
                // ignored
            }

            // ... and to all other ibis instances in this pool.
            for (int i = 0; i < p.pool.size(); i++) {
                forwardDead((IbisInfo) p.pool.get(i), ids);
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

    private void handleIbisIsalive(boolean kill) throws IOException,
            ClassNotFoundException {
        String key = in.readUTF();
        IbisIdentifier id = (IbisIdentifier) in.readObject();

        RunInfo p = (RunInfo) pools.get(key);
        if (p != null) {
            checkPool(p, kill ? id : null, key);

        }
    }

    private void forwardDead(IbisInfo dest, IbisIdentifier[] ids) {
        try {
            Socket s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_DEAD);
            out2.writeObject(ids);
            closeConnection(null, out2, s);
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward dead ibises to "
                        + dest.identifier.toString(), e);
            }
        }
    }

    private void handleIbisJoin() throws IOException, ClassNotFoundException {
        String key = in.readUTF();
        IbisIdentifier id = (IbisIdentifier) in.readObject();
        InetAddress address = (InetAddress) in.readObject();
        int port = in.readInt();

        if (! silent) {
            logger.debug("NameServer: join to pool " + key + " requested by "
                    + id.toString() +", port " + port);
        }

        IbisInfo info = new IbisInfo(id, address, port);
        RunInfo p = (RunInfo) pools.get(key);

        if (p == null) {
            // new run
            poolPinger();
            p = new RunInfo();

            pools.put(key, p);
            joined = true;

            if (! silent) {
                logger.info("NameServer: new pool " + key + " created");
            }
        }

        if (p.pool.contains(info)) {
            out.writeByte(IBIS_REFUSED);

            if (! silent) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                        + id.toString() + " refused");
            }
            out.flush();
        } else {
            out.writeByte(IBIS_ACCEPTED);
            out.writeInt(p.portTypeNameServer.getPort());
            out.writeInt(p.receivePortNameServer.getPort());
            out.writeInt(p.electionServer.getPort());

            if (! silent) {
                logger.debug("NameServer: join to pool " + key + " of ibis "
                    + id.toString() + " accepted");
            }

            // first send all existing nodes to the new one.
            out.writeInt(p.pool.size());

            for (int i = 0; i < p.pool.size(); i++) {
                IbisInfo temp = (IbisInfo) p.pool.get(i);
                out.writeObject(temp.identifier);
            }

            //send all nodes about to leave to the new one
            out.writeInt(p.toBeDeleted.size());

            for (int i = 0; i < p.toBeDeleted.size(); i++) {
                out.writeObject(p.toBeDeleted.get(i));
            }
            out.flush();

            for (int i = 0; i < p.pool.size(); i++) {
                IbisInfo temp = (IbisInfo) p.pool.get(i);
                forwardJoin(temp, id);
            }

            p.pool.add(info);

            String date = Calendar.getInstance().getTime().toString();

            if (! silent) {
                logger.info(date + " " + id + " JOINS  pool " + key
                        + " (" + p.pool.size() + " nodes)");
            }
        }
    }

    private void poolPinger(String key) {
        if (! silent) {
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

    private void forwardLeave(IbisInfo dest, IbisIdentifier id) {
        if (! silent) {
            logger.debug("NameServer: forwarding leave of "
                    + id.toString() + " to " + dest.identifier.toString());
        }

        try {
            Socket s = NameServerClient.socketFactory.createClientSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_LEAVE);
            out2.writeObject(id);
            closeConnection(null, out2, s);
        } catch (Exception e) {
            if (! silent) {
                logger.error("Could not forward leave of " + id.toString()
                        + " to " + dest.identifier.toString(), e);
            }
        }
    }

    private void killThreads(RunInfo p) {
        try {
            Socket s = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.portTypeNameServer.getPort(), null, -1);
            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            DataOutputStream out1 = new DataOutputStream(
                    new BufferedOutputStream(d));
            out1.writeByte(PORTTYPE_EXIT);
            closeConnection(null, out1, s);
        } catch (IOException e) {
            // Ignore.
        }

        try {
            Socket s2 = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.receivePortNameServer.getPort(), null, -1);
            DummyOutputStream d2 = new DummyOutputStream(s2.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d2));
            out2.writeByte(PORT_EXIT);
            closeConnection(null, out2, s2);
        } catch (IOException e) {
            // ignore
        }

        try {
            Socket s3 = NameServerClient.socketFactory.createClientSocket(myAddress,
                    p.electionServer.getPort(), null);
            DummyOutputStream d3 = new DummyOutputStream(s3.getOutputStream());
            ObjectOutputStream out3 = new ObjectOutputStream(
                    new BufferedOutputStream(d3));
            out3.writeByte(ELECTION_EXIT);
            closeConnection(null, out3, s3);
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Notifies the election server of the specified pool that the
     * specified ibis instances are dead.
     * @param p   the specified pool
     * @param ids the dead ibis instances
     * @exception IOException is thrown in case of trouble.
     */
    private void electionKill(RunInfo p, IbisIdentifier[] ids)
            throws IOException {
        Socket s = NameServerClient.socketFactory.createClientSocket(myAddress,
                p.electionServer.getPort(), null, -1);
        DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
        ObjectOutputStream out2 = new ObjectOutputStream(
                new BufferedOutputStream(d));
        out2.writeByte(ELECTION_KILL);
        out2.writeObject(ids);
        closeConnection(null, out2, s);
    }

    private void handleIbisLeave() throws IOException, ClassNotFoundException {
        String key = in.readUTF();
        IbisIdentifier id = (IbisIdentifier) in.readObject();

        RunInfo p = (RunInfo) pools.get(key);

        if (! silent) {
            logger.debug("NameServer: leave from pool " + key
                    + " requested by " + id.toString());
        }

        if (p == null) {
            // new run
            if (! silent) {
                logger.error("NameServer: unknown ibis " + id.toString()
                        + "/" + key + " tried to leave");
            }
            return;
        }
        int index = -1;

        for (int i = 0; i < p.pool.size(); i++) {
            IbisInfo info = (IbisInfo) p.pool.get(i);
            if (info.identifier.equals(id)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // found it.
            if (! silent) {
                logger.debug("NameServer: leave from pool " + key
                        + " of ibis " + id.toString() + " accepted");
            }

            // Let the election server know about it.
            electionKill(p, new IbisIdentifier[] { id });

            // Also forward the leave to the requester.
            // It is used as an acknowledgement, and
            // the leaver is only allowed to exit when it
            // has received its own leave message.
            for (int i = 0; i < p.pool.size(); i++) {
                forwardLeave((IbisInfo) p.pool.get(i), id);
            }
            p.pool.remove(index);
            p.toBeDeleted.remove(id);

            String date = Calendar.getInstance().getTime().toString();

            if (! silent) {
                logger.info(date + " " + id + " LEAVES pool " + key
                        + " (" + p.pool.size() + " nodes)");
            }
            id.free();

            if (p.pool.size() == 0) {
                if (! silent) {
                    logger.info("NameServer: removing pool " + key);
                }

                pools.remove(key);
                killThreads(p);
            }
        } else {
            if (! silent) {
                logger.error("NameServer: unknown ibis " + id.toString()
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
                if (! silent) {
                    logger.info("NameServer: accepting incoming connections... ");
                }
                s = serverSocket.accept();

                if (! silent) {
                    logger.debug("NameServer: incoming connection from "
                            + s.toString());
                }

            } catch (Exception e) {
                if (! silent) {
                    logger.error("NameServer got an error", e);
                }
                continue;
            }

            try {
                DummyOutputStream dos
                        = new DummyOutputStream(s.getOutputStream());
                out = new ObjectOutputStream(new BufferedOutputStream(dos));

                DummyInputStream di = new DummyInputStream(s.getInputStream());
                in = new ObjectInputStream(new BufferedInputStream(di));

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

                closeConnection(in, out, s);
            } catch (Exception e1) {
                if (! silent) {
                    logger.error("Got an exception in NameServer.run", e1);
                }
                if (s != null) {
                    closeConnection(null, null, s);
                }
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

        if (! silent) {
            logger.info("NameServer: exit");
        }
    }
    
    public int port() {
        return serverSocket.getLocalPort();
    }

    public static synchronized NameServer createNameServer(boolean singleRun,
            boolean retry, boolean poolserver, boolean controlhub, boolean silent) {
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
                    if (! silent) {
                        logger.warn("Nameserver: could not create server "
                                + "socket, retry in 1 second");
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
                logger.setLevel(org.apache.log4j.Level.INFO);
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
                logger.error("No nameserver created");
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
