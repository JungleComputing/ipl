/* $Id: NameServerClient.java 5112 2007-02-27 16:09:05Z ceriel $ */

package ibis.impl.registry.smartsockets;

import ibis.impl.registry.NSProps;
import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.io.Conversion;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisConfigurationException;
import ibis.util.RunProcess;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

import smartsockets.direct.DirectSocketAddress;
import smartsockets.virtual.*;

public class NameServerClient extends ibis.impl.Registry
        implements Runnable, Protocol {

    private ElectionClient electionClient;

    private VirtualServerSocket serverSocket;

    private final Ibis ibisImpl;

    private final boolean needsUpcalls;

    private final IbisIdentifier id;

    private volatile boolean stop = false;

    private VirtualSocketAddress serverAddress;

    private String poolName;

    private VirtualSocketAddress myAddress;

    private boolean left = false;

    private static Logger logger = Logger.getLogger(NameServerClient.class);

    private static VirtualSocketFactory socketFactory;

    public NameServerClient(Ibis ibis, boolean ndsUpcalls, byte[] data)
            throws IOException, IbisConfigurationException {
        this.ibisImpl = ibis;
        this.needsUpcalls = ndsUpcalls;

        TypedProperties p = new TypedProperties(ibis.properties());

        // Try to use the same socket factory as the Ibis that created us...
        synchronized(NameServerClient.class) {
            if (socketFactory == null) {
                String name = "Factory for Ibis";
                socketFactory = VirtualSocketFactory.getSocketFactory(name);

                if (socketFactory == null) {
                    // We failed to find Ibis' socketfactory. Create a new one instead.
                    logger.warn("Failed to find VirtualSocketFactory: " + name);
                    logger.info("Creating new VirtualSocketFactory!");

                    try {
                        socketFactory =
                            VirtualSocketFactory.createSocketFactory(p, true);
                    } catch(Exception e) {
                        logger.error("Could not create VirtualSocketFactory",
                                e);
                        throw new IbisConfigurationException("Could not create "
                                + "VirtualSocketFactory", e);
                    }
                }
            }
        }

        serverSocket = socketFactory.createServerSocket(0, 50, true, null);
        myAddress = serverSocket.getLocalSocketAddress();

        serverAddress = getServerAddress(p);

        // Next, get the nameserver pool ....
        poolName = p.getProperty(NSProps.s_pool);
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "property ibis.registry.pool is not specified");
        }

        // Then connect to the nameserver
        VirtualSocket s = nsConnect(serverAddress, true, 60);

        logger.debug("Found nameServerInet " + serverAddress);

        DataOutputStream out = null;
        DataInputStream in = null;

        try {

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            logger.debug("NameServerClient: contacting nameserver");
            out.writeByte(IBIS_JOIN);
            out.writeUTF(poolName);
            myAddress.write(out);
            out.writeBoolean(ndsUpcalls);
            out.writeInt(data.length);
            out.write(data);
            Location l = getLocation(p);
            l.writeTo(out);
            out.flush();

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));

            int opcode = in.readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("NameServerClient: nameserver reply, opcode "
                        + opcode);
            }

            switch (opcode) {
            case IBIS_REFUSED:
                throw new ConnectionRefusedException("NameServerClient: "
                        + "registry did not accept new pool");
            case IBIS_ACCEPTED:
                // read the ports for the other name servers and start the
                // receiver thread...
                VirtualSocketAddress eAddr = new VirtualSocketAddress(in);
                electionClient = new ElectionClient(eAddr, this);
                id = new IbisIdentifier(in);
                if (ndsUpcalls) {
                    int poolSize = in.readInt();

                    if (logger.isDebugEnabled()) {
                        logger.debug("NameServerClient: accepted by nameserver"
                                    + ", poolsize " + poolSize);
                    }

                    // Read existing nodes (including this one).
                    IbisIdentifier[] ids = new IbisIdentifier[poolSize];
                    for (int i = 0; i < poolSize; i++) {
                        ids[i] = new IbisIdentifier(in);
                        logger.debug("NameServerClient: join of " + ids[i]);
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
            VirtualSocketFactory.close(s, out, in);
        }
    }

    public IbisIdentifier getIbisIdentifier() {
        return id;
    }


    private VirtualSocketAddress getClassicServerAddress(String server,
            TypedProperties p) {

        final String original = server;
        int port = p.getIntProperty(NSProps.s_port, 9826);

        int index = server.lastIndexOf(':');

        if (index >= 0) {
            port = Integer.parseInt(server.substring(index+1));
            server = server.substring(0, index);
        } else {
            // no port number present, so check if it's provided seperately
            String nameServerPortString = p.getProperty(NSProps.s_port);

            if (nameServerPortString != null) {
                try {
                    port = Integer.parseInt(nameServerPortString);
                    logger.debug("Using nameserver port: " + port);
                } catch (Exception e) {
                    System.err.println("illegal nameserver port: "
                            + nameServerPortString + ", using default");
                }
            }
        }

        logger.debug("server = " + server);

        if (server.equals("localhost")) {
            // We want to start the server on this machine.
            // TODO: Fix!!
            runNameServer(port, "bla");

            // return new VirtualSocketAddress(myAddress.machine(), port);
        }

        try {
            return VirtualSocketAddress.partialAddress(server, port);
        } catch (Exception e) {
            throw new IbisConfigurationException(
                "property ibis.registry.host contains illegal address: "
                    + original);
        }
    }

    private VirtualSocketAddress getServerAddress(TypedProperties p) {

        // This method tries to decypher the properties which contain the
        // address and port number of the nameserver. The possibilities are:
        //
        // 'Classic addressing':
        //
        //      'hostname' + 'port'
        //      'hostname:port'
        //      'ip' + 'port'
        //      'ip:port'
        //
        // 'Smart addressing':
        //
        //       'virtual address'
        //
        //       (which contains at least: IP-PORT1:PORT2, but may also be
        //        something like: IP1/IP2-P1/IP3-P2:PORT)

        String server = p.getProperty(NSProps.s_host);

        if (server == null) {
            throw new IbisConfigurationException(
                    "property ibis.registry.host is not specified");
        }

        // The easiest way to distinguish between the two forms is to check the
        // 'hostname' for a colon (':'). If it is NOT there is must be a classic
        // address. If it is there, we can check if it contains a dash ('-').
        // Again, if it is not there, it must be a classic address.

        if (server.indexOf(':') == -1 || server.indexOf('-') == -1) {
            return getClassicServerAddress(server, p);
        }

        // Now it becomes interesting. The server contains both a colon and a
        // dash, so it's likely to be a smart address, but it may still be a
        // classic one with a dash in the hostname, e.g., 'host-a.domain:2020'
        // The simplest way to continue now it just to try loading it as a
        // smart address, and trying again as a classic address if we fail.
        try {
            return new VirtualSocketAddress(server);
        } catch (Exception e) {
            return getClassicServerAddress(server, p);
        }
    }

    VirtualSocket nsConnect(VirtualSocketAddress dest,
            boolean verbose, int timeout) throws IOException {
        // TODO: fix timeout
        VirtualSocket s = null;
        int cnt = 0;
        while (s == null) {
            try {
                cnt++;
                s = socketFactory.createClientSocket(dest, 0, null);
            } catch (IOException e) {
                if (cnt >= timeout) {
                    if (verbose) {
                        logger.error("Nameserver client failed"
                                + " to connect to nameserver\n at "
                                + dest + ", gave up after " + timeout
                                + " seconds");
                    }
                    throw new ConnectionTimedOutException(e);
                }
                if (cnt == 10 && verbose) {
                    // Rather arbitrary, 10 seconds, print warning
                    logger.error("Nameserver client failed"
                            + " to connect to nameserver\n at " + dest
                            + ", will keep trying");
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

    void runNameServer(int prt, String srvr) {
        if (System.getProperty("os.name").matches(".*indows.*")) {
            // The code below does not work for windows2000, don't know why ...
            NameServer n = NameServer.createNameServer(true, false, false,
                    true);
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

            ArrayList<String> command = new ArrayList<String>();
            command.add(javadir + filesep + "bin" + filesep + "java");
            command.add("-classpath");
            command.add(javapath + pathsep);
            command.add("-Dibis.registry.port="+prt);
            command.add("ibis.impl.registry.smartsockets.NameServer");
            command.add("-single");
            command.add("-no-retry");
            command.add("-silent");
            command.add("-no-poolserver");

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

    /**
     * Initializes the <code>location</code> field.
     */
    private static Location getLocation(Properties tp) {
        String location = tp.getProperty("ibis.location");
        if (location == null) {
            return Location.defaultLocation();
        }
        return new Location(location);
    }


    public void maybeDead(ibis.ipl.IbisIdentifier ibisId) throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;

        try {
            logger.debug("Sending maybeDead(" + ibisId + ") to nameserver");
            s = socketFactory.createClientSocket(serverAddress, 60000, null);

            logger.debug("connection setup done");

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_ISALIVE);
            out.writeUTF(poolName);
            out.writeUTF(((IbisIdentifier)ibisId).myId);
            out.flush();

            logger.debug("NS client: isAlive sent");

        } catch (ConnectionTimedOutException e) {
            logger.warn("Could not contact nameserver!!", e);
            // Apparently, the nameserver left. Assume dead.
            return;
        } finally {
            VirtualSocketFactory.close(s, out, null);
        }
    }

    public void dead(ibis.ipl.IbisIdentifier corpse) throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;

        try {
            s = socketFactory.createClientSocket(serverAddress, -1, null);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_DEAD);
            out.writeUTF(poolName);
            out.writeUTF(((IbisIdentifier) corpse).myId);
            logger.debug("NS client: kill sent");
        } catch (ConnectionTimedOutException e) {
            return;
        } finally {
            VirtualSocketFactory.close(s, out, null);
        }
    }

    public void mustLeave(ibis.ipl.IbisIdentifier[] ibisses)
            throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;

        try {
            s = socketFactory.createClientSocket(serverAddress, 0, null);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_MUSTLEAVE);
            out.writeUTF(poolName);
            out.writeInt(ibisses.length);
            for (int i = 0; i < ibisses.length; i++) {
                out.writeUTF(((IbisIdentifier) ibisses[i]).myId);
            }
            logger.debug("NS client: mustLeave sent");
        } catch (ConnectionTimedOutException e) {
            return;
        } finally {
            VirtualSocketFactory.close(s, out, null);
        }
    }

    public long getSeqno(String name) throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            s = nsConnect(serverAddress, false, 60);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(SEQNO);
            out.writeUTF(name);
            out.flush();

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));

            return in.readLong();

        } finally {
            VirtualSocketFactory.close(s, out, in);
        }
    }

    public void leave() {
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        logger.info("NS client: leave");

        try {
            s = socketFactory.createClientSocket(serverAddress, 60000, null);
        } catch (IOException e) {
            if (logger.isInfoEnabled()) {
                logger.info("leave: connect got exception", e);
            }
            return;
        }

        try {
            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(IBIS_LEAVE);
            out.writeUTF(poolName);
            out.writeUTF(id.myId);
            out.flush();
            logger.info("NS client: leave sent");

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));

            in.readByte();
            logger.info("NS client: leave ack received");
        } catch (IOException e) {
            logger.info("leave got exception", e);
        } finally {
            VirtualSocketFactory.close(s, out, null);
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

            VirtualSocket s;
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
                in = new DataInputStream(
                        new BufferedInputStream(s.getInputStream()));
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
                        out.writeUTF(id.myId);
                    }
                    break;

                case (IBIS_JOIN):
                case (IBIS_LEAVE):
                case (IBIS_DEAD):
                case (IBIS_MUSTLEAVE):
                    count = in.readInt();
                    ids = new IbisIdentifier[count];
                    for (int i = 0; i < count; i++) {
                        ids[i] = new IbisIdentifier(in);
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
                VirtualSocketFactory.close(s, out, in);
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
