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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

public class NameServer extends Thread implements Protocol {

    public static final int TCP_IBIS_NAME_SERVER_PORT_NR
            = TypedProperties.intProperty(NSProps.s_port, 9826);

    static final boolean DEBUG
            = TypedProperties.booleanProperty(NSProps.s_debug);

    static final boolean VERBOSE
            = TypedProperties.booleanProperty(NSProps.s_verbose);

    static int PINGER_TIMEOUT = TypedProperties.intProperty(NSProps.s_timeout,
            300) * 1000; // Property is in seconds, convert to milliseconds.

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

    private static boolean controlHubStarted = false;

    private static boolean poolServerStarted = false;

    private static ControlHub h = null;

    private NameServer(boolean singleRun, boolean poolserver,
            boolean controlhub) throws IOException {

        this.singleRun = singleRun;
        this.joined = false;

        myAddress = IPUtils.getAlternateLocalHostAddress();
        myAddress = InetAddress.getByName(myAddress.getHostName());

        String hubPort = System.getProperty("ibis.connect.hub_port");
        String poolPort = System.getProperty("ibis.pool.server.port");
        int port = TCP_IBIS_NAME_SERVER_PORT_NR;

        if (controlhub && !controlHubStarted) {
            if (hubPort == null) {
                hubPort = Integer.toString(port + 2);
                System.setProperty("ibis.connect.hub_port", hubPort);
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

        if (DEBUG) {
            System.err.println("NameServer: singleRun = " + singleRun);
        }

        // Create a server socket.
        serverSocket = NameServerClient.socketFactory.createServerSocket(port,
                null, false);

        pools = new Hashtable();

        if (VERBOSE) {
            System.err.println("NameServer: created server on " + serverSocket);
        }

    }

    private void forwardJoin(IbisInfo dest, IbisIdentifier id) {

        if (DEBUG) {
            System.err.println("NameServer: forwarding join of "
                    + id.toString() + " to " + dest.ibisNameServerAddress
                    + ", dest port: " + dest.ibisNameServerport);
        }
        try {

            Socket s = NameServerClient.socketFactory.createSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null,
                    -1 /* do not retry */);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_JOIN);
            out2.writeObject(id);
            NameServerClient.socketFactory.close(null, out2, s);

            if (DEBUG) {
                System.err.println("NameServer: forwarding join of "
                        + id.toString() + " to " + dest.identifier.toString()
                        + " DONE");
            }
        } catch (Exception e) {
            System.err.println("Could not forward join of " + id.toString()
                    + " to " + dest.identifier.toString() + "error = " + e);
        }

    }

    private boolean doPing(IbisInfo dest, String key) {
        try {
            Socket s = NameServerClient.socketFactory.createSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null,
                    -1 /* do not retry */);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_PING);
            out2.flush();
            DummyInputStream i = new DummyInputStream(s.getInputStream());
            DataInputStream in2 = new DataInputStream(
                    new BufferedInputStream(i));
            String k = in2.readUTF();
            NameServerClient.socketFactory.close(in2, out2, s);
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
            System.out.println(date + " pool " + key + " seems to be dead.");
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
            Socket s = NameServerClient.socketFactory.createSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null,
                    -1 /* do not retry */);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_DEAD);
            out2.writeObject(ids);
            NameServerClient.socketFactory.close(null, out2, s);
        } catch (Exception e) {
            System.err.println("Could not forward dead ibises to "
                    + dest.identifier.toString() + "error = " + e);
            //			e.printStackTrace();
        }
    }

    private void handleIbisJoin() throws IOException, ClassNotFoundException {
        String key = in.readUTF();
        IbisIdentifier id = (IbisIdentifier) in.readObject();
        InetAddress address = (InetAddress) in.readObject();
        int port = in.readInt();

        if (DEBUG) {
            System.err.print("NameServer: join to pool " + key);
            System.err.print(" requested by " + id.toString());
            System.err.println(", port " + port);
        }

        IbisInfo info = new IbisInfo(id, address, port);
        RunInfo p = (RunInfo) pools.get(key);

        if (p == null) {
            // new run
            poolPinger();
            p = new RunInfo();

            pools.put(key, p);
            joined = true;

            if (VERBOSE) {
                System.err.println("NameServer: new pool " + key + " created");
            }
        }

        if (p.pool.contains(info)) {
            out.writeByte(IBIS_REFUSED);

            if (DEBUG) {
                System.err.println("NameServer: join to pool " + key
                        + " of ibis " + id.toString() + " refused");
            }
            out.flush();
        } else {
            out.writeByte(IBIS_ACCEPTED);
            out.writeInt(p.portTypeNameServer.getPort());
            out.writeInt(p.receivePortNameServer.getPort());
            out.writeInt(p.electionServer.getPort());

            if (DEBUG) {
                System.err.println("NameServer: join to pool " + key
                        + " of ibis " + id.toString() + " accepted");
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

            System.out.println(date + " " + id.name() + " JOINS  pool " + key
                    + " (" + p.pool.size() + " nodes)");
        }
    }

    private void poolPinger(String key) {
        if (DEBUG) {
            System.err.print("NameServer: ping pool " + key);
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
        if (DEBUG) {
            System.err.println("NameServer: forwarding leave of "
                    + id.toString() + " to " + dest.identifier.toString());
        }

        try {
            Socket s = NameServerClient.socketFactory.createSocket(
                    dest.ibisNameServerAddress, dest.ibisNameServerport, null,
                    -1 /* do not retry */);

            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d));
            out2.writeByte(IBIS_LEAVE);
            out2.writeObject(id);
            NameServerClient.socketFactory.close(null, out2, s);
        } catch (Exception e) {
            System.err.println("Could not forward leave of " + id.toString()
                    + " to " + dest.identifier.toString() + "error = " + e);
            //			e.printStackTrace();
        }
    }

    private void killThreads(RunInfo p) {
        try {
            Socket s = NameServerClient.socketFactory.createSocket(myAddress,
                    p.portTypeNameServer.getPort(), null, -1 /* no retry */);
            DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
            DataOutputStream out1 = new DataOutputStream(
                    new BufferedOutputStream(d));
            out1.writeByte(PORTTYPE_EXIT);
            NameServerClient.socketFactory.close(null, out1, s);
        } catch (IOException e) {
            // Ignore.
        }

        try {
            Socket s2 = NameServerClient.socketFactory.createSocket(myAddress,
                    p.receivePortNameServer.getPort(), null, -1 /* no retry */);
            DummyOutputStream d2 = new DummyOutputStream(s2.getOutputStream());
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new BufferedOutputStream(d2));
            out2.writeByte(PORT_EXIT);
            NameServerClient.socketFactory.close(null, out2, s2);
        } catch (IOException e) {
            // ignore
        }

        try {
            Socket s3 = NameServerClient.socketFactory.createSocket(myAddress,
                    p.electionServer.getPort(), null, -1 /* do not retry */);
            DummyOutputStream d3 = new DummyOutputStream(s3.getOutputStream());
            ObjectOutputStream out3 = new ObjectOutputStream(
                    new BufferedOutputStream(d3));
            out3.writeByte(ELECTION_EXIT);
            NameServerClient.socketFactory.close(null, out3, s3);
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
        Socket s = NameServerClient.socketFactory.createSocket(myAddress,
                p.electionServer.getPort(), null, -1 /* do not retry */);
        DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
        ObjectOutputStream out2 = new ObjectOutputStream(
                new BufferedOutputStream(d));
        out2.writeByte(ELECTION_KILL);
        out2.writeObject(ids);
        NameServerClient.socketFactory.close(null, out2, s);
    }

    private void handleIbisLeave() throws IOException, ClassNotFoundException {
        String key = in.readUTF();
        IbisIdentifier id = (IbisIdentifier) in.readObject();

        RunInfo p = (RunInfo) pools.get(key);

        if (DEBUG) {
            System.err.println("NameServer: leave from pool " + key
                    + " requested by " + id.toString());
        }

        if (p == null) {
            // new run
            System.err.println("NameServer: unknown ibis " + id.toString()
                    + "/" + key + " tried to leave");
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
            if (DEBUG) {
                System.err.println("NameServer: leave from pool " + key
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

            System.out.println(date + " " + id.name() + " LEAVES pool " + key
                    + " (" + p.pool.size() + " nodes)");
            id.free();

            if (p.pool.size() == 0) {
                if (VERBOSE) {
                    System.err.println("NameServer: removing pool " + key);
                }

                pools.remove(key);
                killThreads(p);
            }
        } else {
            System.err.println("NameServer: unknown ibis " + id.toString()
                    + "/" + key + " tried to leave");
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
                if (DEBUG) {
                    System.err.println("NameServer: accepting incoming "
                            + "connections... ");
                }
                s = NameServerClient.socketFactory.accept(serverSocket);

                if (DEBUG) {
                    System.err.println("NameServer: incoming connection from "
                            + s.toString());
                }

            } catch (Exception e) {
                System.err.println("NameServer got an error " + e.getMessage());
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
                    System.err.println("NameServer got an illegal opcode: "
                            + opcode);
                }

                NameServerClient.socketFactory.close(in, out, s);
            } catch (Exception e1) {
                System.err.println("Got an exception in NameServer.run "
                        + e1.toString());
                e1.printStackTrace();
                if (s != null) {
                    NameServerClient.socketFactory.close(null, null, s);
                }
            }

            //			System.err.println("Pools are now: " + pools);
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new IbisRuntimeException("NameServer got an error", e);
        }

        if (h != null) {
            h.waitForCount(1);
        }

        if (VERBOSE) {
            System.err.println("NameServer: exit");
        }
    }

    static synchronized NameServer createNameServer(boolean singleRun,
            boolean retry, boolean poolserver, boolean controlhub) {
        if (nameServerCreated) {
            return null;
        }
        NameServer ns = null;
        while (true) {
            try {
                ns = new NameServer(singleRun, poolserver, controlhub);
                break;
            } catch (Throwable e) {
                if (retry) {
                    System.err.println("Nameserver: could not create server "
                            + "socket, retry in 1 second");
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) { /* do nothing */
                    }
                } else {
                    // System.err.println("Nameserver: could not create server "
                    //         + "socket");
                    return null;
                }
            }
        }
        nameServerCreated = true;
        return ns;
    }

    public static void main(String[] args) {
        boolean single = false;
        boolean control_hub = false;
        boolean pool_server = true;
        NameServer ns = null;

        for (int i = 0; i < args.length; i++) {
            if (false) { /* do nothing */
            } else if (args[i].equals("-single")) {
                single = true;
            } else if (args[i].equals("-controlhub")) {
                control_hub = true;
            } else if (args[i].equals("-no-controlhub")) {
                control_hub = false;
            } else if (args[i].equals("-poolserver")) {
                pool_server = true;
            } else if (args[i].equals("-no-poolserver")) {
                pool_server = false;
            } else {
                System.err.println("No such option: " + args[i]);
                System.exit(1);
            }
        }

        if (!single) {
            Properties p = System.getProperties();
            String singleS = p.getProperty(NSProps.s_single);

            single = (singleS != null && singleS.equals("true"));
        }

        ns = createNameServer(single, true, pool_server, control_hub);

        try {
            ns.run();
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("Nameserver got an exception: " + t);
            t.printStackTrace();
        }
    }
}
