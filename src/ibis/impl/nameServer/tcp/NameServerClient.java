/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.nameServer.NSProps;
import ibis.io.Conversion;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.impl.IbisIdentifier;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;
import ibis.util.RunProcess;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

public class NameServerClient extends ibis.impl.Registry
        implements Runnable, Protocol {

    private ElectionClient electionClient;

    private ServerSocket serverSocket;

    private Ibis ibisImpl;

    private boolean needsUpcalls;

    private IbisIdentifier id;

    private volatile boolean stop = false;

    private InetAddress serverAddress;

    private String server;

    private int port;

    private String poolName;

    private InetAddress myAddress;

    private int localPort;

    private boolean left = false;

    static IbisSocketFactory socketFactory = IbisSocketFactory.getFactory();

    private static Logger logger
            = ibis.util.GetLogger.getLogger(NameServerClient.class.getName());

    static Socket nsConnect(InetAddress dest, int port, InetAddress me,
            boolean verbose, int timeout) throws IOException {
        Socket s = null;
        int cnt = 0;
        while (s == null) {
            try {
                cnt++;
                s = socketFactory.createClientSocket(dest, port, me, 0, -1, null);
            } catch (IOException e) {
                if (cnt == timeout) {
                    if (verbose) {
                        logger.error("Nameserver client " + me + " failed"
                                + " to connect to nameserver\n at "
                                + dest + ":" + port);
                        logger.error("Gave up after " + timeout
                                + " seconds");
                    }
                    throw new ConnectionTimedOutException(e);
                }
                if (cnt == 10 && verbose) {
                    // Rather arbitrary, 10 seconds, print warning
                    logger.error("Nameserver client " + me + " failed"
                            + " to connect to nameserver\n at " + dest
                            + ":" + port + ", will keep trying");
                } 
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    // don't care
                }
            }
        }
        return s;
    }

    public NameServerClient() {
        /* do nothing */
    }

    void runNameServer(int prt, String srvr) {
        if (System.getProperty("os.name").matches(".*indows.*")) {
            // The code below does not work for windows2000, don't know why ...
            NameServer n = NameServer.createNameServer(true, false, false,
                    false, true);
            if (n != null) {
                n.setDaemon(true);
                n.start();
            }
        } else {
            // Start the nameserver in a separate jvm, so that it can keep
            // on running if this particular ibis instance dies.
            String javadir = System.getProperty("java.home");
            String javapath = System.getProperty("java.class.path");
            String filesep = System.getProperty("file.separator");
            String pathsep = System.getProperty("path.separator");

            if (javadir.endsWith("jre")) {
                javadir = javadir.substring(0, javadir.length()-4);
            }

            String conn = System.getProperty("ibis.connect.control_links");
            String hubhost = null;
            String hubport = null;
            if (conn != null && conn.equals("RoutedMessages")) {
                hubhost = System.getProperty("ibis.connect.hub.host");
                if (hubhost == null) {
                    hubhost = srvr;
                }
                hubport = System.getProperty("ibis.connect.hub.port");
            }

            ArrayList<String> command = new ArrayList<String>();
            command.add(javadir + filesep + "bin" + filesep + "java");
            command.add("-classpath");
            command.add(javapath + pathsep);
            command.add("-Dibis.name_server.port="+prt);
            if (hubhost != null && ! srvr.equals(hubhost)) {
                command.add("-Dibis.connect.hub.host=" + hubhost);
            }
            if (hubport != null) {
                command.add("-Dibis.connect.hub.port=" + hubport);
            }
            command.add("ibis.impl.nameServer.tcp.NameServer");
            command.add("-single");
            command.add("-no-retry");
            command.add("-silent");
            command.add("-no-poolserver");
            if (hubhost != null && hubhost.equals(srvr)) {
                command.add("-controlhub");
            }

            final String[] cmd = (String[]) command.toArray(new String[0]);

            Thread p = new Thread("NameServer starter") {
                public void run() {
                    RunProcess r = new RunProcess(cmd, new String[0]);
                    byte[] err = r.getStderr();
                    byte[] out = r.getStdout();
                    if (out.length != 0) {
                        System.out.write(out, 0, out.length);
                        System.out.println("");
                    }
                    if (err.length != 0) {
                        System.err.write(err, 0, err.length);
                        System.err.println("");
                    }
                }
            };

            p.setDaemon(true);
            p.start();
        }
    }

    private static class Message {
        byte type;
        IbisIdentifier[] list;
        Message(byte type, IbisIdentifier[] list) {
            this.type = type;
            this.list = list;
        }
    }

    private LinkedList<Message> messages = new LinkedList<Message>();
    private boolean stopUpcaller = false;

    private void addMessage(byte type, IbisIdentifier[] list) {
        synchronized(messages) {
            messages.add(new Message(type, list));
            if (messages.size() == 1) {
                messages.notifyAll();
            }
        }
    }

    private void upcaller() {
        for (;;) {
            Message m;
            synchronized(messages) {
                while (messages.size() == 0) {
                    try {
                        messages.wait(60000);
                    } catch(Exception e) {
                        // ignored
                    }
                    if (messages.size() == 0 && stop) {
                        return;
                    }
                }
                m = (Message) messages.removeFirst();
            }
            switch(m.type) {
            case IBIS_JOIN:
                ibisImpl.joined(m.list);
                break;
            case IBIS_LEAVE:
                ibisImpl.left(m.list);
                break;
            case IBIS_DEAD:
                ibisImpl.died(m.list);
                break;
            case IBIS_MUSTLEAVE:
                ibisImpl.mustLeave(m.list);
                break;
            default:
                logger.warn("Internal error, unknown opcode in message, ignored");
                break;
            }
        }
    }

    public IbisIdentifier init(Ibis ibis, boolean ndsUpcalls, byte[] data)
            throws IOException, IbisConfigurationException {
        this.ibisImpl = ibis;
        this.needsUpcalls = ndsUpcalls;

        Properties p = System.getProperties();

        myAddress = IPUtils.getAlternateLocalHostAddress();

        server = p.getProperty(NSProps.s_host);
        if (server == null) {
            throw new IbisConfigurationException(
                    "property ibis.name_server.host is not specified");
        }

        if (server.equals("localhost")) {
            server = myAddress.getHostName();
        }

        poolName = p.getProperty(NSProps.s_key);
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "property ibis.name_server.key is not specified");
        }

        String nameServerPortString = p.getProperty(NSProps.s_port);
        port = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
        if (nameServerPortString != null) {
            try {
                port = Integer.parseInt(nameServerPortString);
                logger.debug("Using nameserver port: " + port);
            } catch (Exception e) {
                logger.error("illegal nameserver port: "
                        + nameServerPortString + ", using default");
            }
        }

        serverAddress = InetAddress.getByName(server);
        // serverAddress.getHostName();

        if (myAddress.equals(serverAddress)) {
            // Try and start a nameserver ...
            runNameServer(port, server);
        }

        logger.debug("Found nameServerInet " + serverAddress);

        serverSocket = socketFactory.createServerSocket(0, myAddress, 50, true, null);

        localPort = serverSocket.getLocalPort();

        Socket s = nsConnect(serverAddress, port, myAddress, true, 60);

        DataOutputStream out = null;
        DataInputStream in = null;
        int len;
        byte[] buf;

        try {

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            logger.debug("NameServerClient: contacting nameserver");
            out.writeByte(IBIS_JOIN);
            out.writeUTF(poolName);
            buf = Conversion.object2byte(myAddress);
            out.writeInt(buf.length);
            out.write(buf);
            out.writeInt(localPort);
            out.writeBoolean(ndsUpcalls);
            out.writeInt(data.length);
            out.write(data);
            out.writeUTF(IbisIdentifier.getCluster());
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            int opcode = in.readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("NameServerClient: nameserver reply, opcode "
                        + opcode);
            }

            switch (opcode) {
            case IBIS_REFUSED:
                throw new ConnectionRefusedException("NameServerClient: "
                        + id + " is not unique!");
            case IBIS_ACCEPTED:
                // read the ports for the other name servers and start the
                // receiver thread...
                int temp = in.readInt(); /* Port for the ElectionServer */
                electionClient = new ElectionClient(myAddress, serverAddress,
                        temp);
                try {
                    len = in.readInt();
                    buf = new byte[len];
                    in.readFully(buf, 0, len);
                    id = (IbisIdentifier) Conversion.byte2object(buf);
                } catch(ClassNotFoundException e) {
                    throw new IOException("Receive IbisIdent of unknown class "
                            + e);
                }
                if (ndsUpcalls) {
                    int poolSize = in.readInt();

                    if (logger.isDebugEnabled()) {
                        logger.debug("NameServerClient: accepted by nameserver, "
                                    + "poolsize " + poolSize);
                    }

                    // Read existing nodes (including this one).
                    IbisIdentifier[] ids = new IbisIdentifier[poolSize];
                    for (int i = 0; i < poolSize; i++) {
                        try {
                            len = in.readInt();
                            buf = new byte[len];
                            in.readFully(buf, 0, len);
                            ids[i] = (IbisIdentifier) Conversion.byte2object(buf);
                        } catch (ClassNotFoundException e) {
                            throw new IOException("Receive IbisIdent of unknown class "
                                    + e);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("NameServerClient: join of " + ids[i]);
                        }
                    }
                    addMessage(IBIS_JOIN, ids);
                }

                Thread t = new Thread(this, "NameServerClient accept thread");
                t.setDaemon(true);
                t.start();
                t = new Thread("upcaller") {
                    public void run() {
                        upcaller();
                    }
                };
                t.setDaemon(true);
                t.start();
                break;
            default:
                throw new StreamCorruptedException(
                        "NameServerClient: got illegal opcode " + opcode);
            }
        } finally {
            NameServer.closeConnection(in, out, s);
        }
        return id;
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibisId) throws IOException {
        Socket s = null;
        DataOutputStream out = null;

        try {
            logger.debug("Sending maybeDead(" + ibisId + ") to nameserver");
            s = nsConnect(serverAddress, port, myAddress, false, 2);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_ISALIVE);
            out.writeUTF(poolName);
            out.writeInt(((IbisIdentifier)ibisId).getId());
            out.flush();
            logger.debug("NS client: isAlive sent");

        } catch (ConnectionTimedOutException e) {
            // Apparently, the nameserver left. Assume dead.
            return;
        } finally {
            NameServer.closeConnection(null, out, s);
        }
    }

    public void dead(ibis.ipl.IbisIdentifier corpse) throws IOException {
        Socket s = null;
        DataOutputStream out = null;

        try {
            s = nsConnect(serverAddress, port, myAddress, false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_DEAD);
            out.writeUTF(poolName);
            out.writeInt(((IbisIdentifier) corpse).getId());
            logger.debug("NS client: kill sent");
        } catch (ConnectionTimedOutException e) {
            return;
        } finally {
            NameServer.closeConnection(null, out, s);
        }
    }

    public void mustLeave(ibis.ipl.IbisIdentifier[] ibisses)
            throws IOException {
        Socket s = null;
        DataOutputStream out = null;

        try {
            s = nsConnect(serverAddress, port, myAddress, false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_MUSTLEAVE);
            out.writeUTF(poolName);
            out.writeInt(ibisses.length);
            for (int i = 0; i < ibisses.length; i++) {
                out.writeInt(((IbisIdentifier) ibisses[i]).getId());
            }
            logger.debug("NS client: mustLeave sent");
        } catch (ConnectionTimedOutException e) {
            return;
        } finally {
            NameServer.closeConnection(null, out, s);
        }
    }

    public long getSeqno(String name) throws IOException {
        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            s = nsConnect(serverAddress, port, myAddress, false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(SEQNO);
            out.writeUTF(name);
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            return in.readLong();

        } finally {
            NameServer.closeConnection(in, out, s);
        }
    }

    public void leave() {
        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        logger.info("NS client: leave");

        try {
            s = nsConnect(serverAddress, port, myAddress, false, 10);
        } catch (IOException e) {
            if (logger.isInfoEnabled()) {
                logger.info("leave: connect got exception", e);
            }
            return;
        }

        try {
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_LEAVE);
            out.writeUTF(poolName);
            out.writeInt(id.getId());
            out.flush();
            logger.info("NS client: leave sent");

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            in.readByte();
            logger.info("NS client: leave ack received");
        } catch (IOException e) {
            logger.info("leave got exception", e);
        } finally {
            NameServer.closeConnection(in, out, s);
        }

        if (! needsUpcalls) {
            synchronized (this) {
                stop = true;
            }
        } else {
            synchronized(this) {
                while (! left) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
            }
        }

        logger.info("NS client: leave DONE");
    }

    public void run() {
        logger.info("NameServerClient: thread started");

        while (! stop) {

            Socket s;
            IbisIdentifier ibisId;

            try {
                s = serverSocket.accept();

                logger.debug("NameServerClient: incoming connection "
                        + "from " + s.toString());

            } catch (Throwable e) {
                if (stop) {
                    logger.info("NameServerClient: thread dying");
                    try {
                        serverSocket.close();
                    } catch (IOException e1) {
                        /* do nothing */
                    }
                    break;
                }
                if (needsUpcalls) {
                    synchronized(this) {
                        stop = true;
                        left = true;
                        notifyAll();
                    }
                }
                throw new RuntimeException(
                        "NameServerClient: got an error", e);
            }

            byte opcode = 0;
            DataInputStream in = null;
            DataOutputStream out = null;

            try {
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                int count;
                IbisIdentifier[] ids;

                opcode = in.readByte();
                logger.debug("NameServerClient: opcode " + opcode);

                switch (opcode) {
                case (IBIS_PING):
                    {
                        out = new DataOutputStream(
                                new BufferedOutputStream(s.getOutputStream()));
                        out.writeUTF(poolName);
                        out.writeInt(id.getId());
                    }
                    break;

                case (IBIS_JOIN):
                case (IBIS_LEAVE):
                case (IBIS_DEAD):
                case (IBIS_MUSTLEAVE):
                    count = in.readInt();
                    ids = new IbisIdentifier[count];
                    for (int i = 0; i < count; i++) {
                        int sz = in.readInt();
                        byte[] buf = new byte[sz];
                        in.readFully(buf, 0, sz);
                        ids[i] = (IbisIdentifier) Conversion.byte2object(buf);
                        if (opcode == IBIS_LEAVE && ids[i].equals(this.id)) {
                            // received an ack from the nameserver that I left.
                            logger.info("NameServerClient: thread dying");
                            stop = true;
                        }
                    }
                    if (stop) {
                        synchronized(this) {
                            left = true;
                            notifyAll();
                        }
                    } else {
                        addMessage(opcode, ids);                       
                    }
                    break;

                default:
                    logger.error("NameServerClient: got an illegal "
                            + "opcode " + opcode);
                }
            } catch (Exception e1) {
                if (! stop) {
                    logger.error("Got an exception in "
                            + "NameServerClient.run "
                            + "(opcode = " + opcode + ")", e1);
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }
        }
        logger.debug("NameServerClient: thread stopped");
    }

    public ibis.ipl.IbisIdentifier elect(String election) throws IOException {
        return electionClient.elect(election, id);
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election)
            throws IOException {
        return electionClient.elect(election, null);
    }
}
