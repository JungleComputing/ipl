/* $Id: NameServerClient.java 5112 2007-02-27 16:09:05Z ceriel $ */

package ibis.impl.registry.smartsockets;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.impl.registry.RegistryProperties;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisConfigurationException;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

import smartsockets.direct.DirectSocketAddress;
import smartsockets.virtual.*;

public class NameServerClient extends ibis.impl.Registry implements Runnable,
        Protocol {

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

    private final NameServer server;

    public NameServerClient(Ibis ibis, boolean ndsUpcalls, byte[] data)
            throws IOException, IbisConfigurationException {
        this.ibisImpl = ibis;
        this.needsUpcalls = ndsUpcalls;

        TypedProperties typedProperties = new TypedProperties(ibis.properties());

        try {
            socketFactory = VirtualSocketFactory.getOrCreateSocketFactory(
                    "ibis", typedProperties, true);
        } catch (Exception e) {
            logger.error("Could not create VirtualSocketFactory", e);
            throw new IbisConfigurationException("Could not create "
                    + "VirtualSocketFactory", e);
        }

        serverSocket = socketFactory.createServerSocket(0, 50, true, null);
        myAddress = serverSocket.getLocalSocketAddress();

        String serverString = typedProperties
                .getProperty(RegistryProperties.SERVER_ADDRESS);

        if (serverString == null) {
            throw new IOException("Server address unspecified");
        }

        serverAddress = createAddressFromString(serverString);

        // start a server...
        if (serverSocket.getLocalSocketAddress().machine().sameMachine(
                serverAddress.machine())) {
            server = runNameServer(serverAddress.port(), typedProperties);
        } else {
            server = null;
        }

        // Next, get the nameserver pool ....
        poolName = typedProperties.getProperty(RegistryProperties.POOL);
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

            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

            logger.debug("NameServerClient: contacting nameserver");
            out.writeByte(IBIS_JOIN);
            out.writeUTF(poolName);
            myAddress.write(out);
            out.writeBoolean(ndsUpcalls);
            out.writeInt(data.length);
            out.write(data);
            Location l = getLocation(typedProperties);
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

    private static VirtualSocketAddress createAddressFromString(
            String serverString) throws IOException {

        VirtualSocketAddress serverAddress = null;

        // first, try to create a complete virtual socket address
        try {
            serverAddress = new VirtualSocketAddress(serverString);
        } catch (IllegalArgumentException e) {
            logger.debug("could not create server address", e);
        }

        // maybe it is a socketaddressset without a virtual port?
        if (serverAddress == null) {
            try {
                DirectSocketAddress directAddress = DirectSocketAddress
                        .getByAddress(serverString);
                int[] ports = directAddress.getPorts(false);
                if (ports.length == 0) {
                    throw new IOException(
                            "cannot determine port from server address: "
                                    + serverString);
                }
                int port = ports[0];
                for (int p : ports) {
                    if (p != port) {
                        throw new IOException(
                                "cannot determine port from server address: "
                                        + serverString);
                    }
                }
                serverAddress = new VirtualSocketAddress(directAddress, port);
            } catch (IllegalArgumentException e) {
                logger.debug("could not create server address", e);
            }
        }

        // maybe it is only a hostname?
        if (serverAddress == null) {
            try {
                DirectSocketAddress directAddress = DirectSocketAddress
                        .getByAddress(serverString,
                                RegistryProperties.DEFAULT_SERVER_PORT);
                serverAddress = new VirtualSocketAddress(directAddress,
                        RegistryProperties.DEFAULT_SERVER_PORT);
            } catch (Exception e) {
                logger.debug("could not create server address", e);
            }
        }

        if (serverAddress == null) {
            throw new IOException("Invalid server address: " + serverString);
        }

        return serverAddress;
    }

    public IbisIdentifier getIbisIdentifier() {
        return id;
    }

    VirtualSocket nsConnect(VirtualSocketAddress dest, boolean verbose,
            int timeout) throws IOException {
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
                                + " to connect to nameserver\n at " + dest
                                + ", gave up after " + timeout + " seconds");
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

    NameServer runNameServer(int port, Properties properties) {
        try {
            properties.setProperty(RegistryProperties.SERVER_PORT, Integer
                    .toString(port));

            properties.setProperty(RegistryProperties.SERVER_SINGLE, "true");
            NameServer server = new NameServer(properties);
            server.setDaemon(true);
            server.start();
            logger.warn("Automagically created "
                    + server.toString());
            return server;
        } catch (Throwable t) {
            // IGNORE
            return null;
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

    private void addMessage(byte type, IbisIdentifier[] list) {
        synchronized (messages) {
            messages.add(new Message(type, list));
            if (messages.size() == 1) {
                messages.notifyAll();
            }
        }
    }

    private void upcaller() {
        for (;;) {
            Message m;
            synchronized (messages) {
                while (messages.size() == 0) {
                    try {
                        messages.wait(60000);
                    } catch (Exception e) {
                        // ignored
                    }
                    if (messages.size() == 0 && stop) {
                        return;
                    }
                }
                m = (Message) messages.removeFirst();
            }
            switch (m.type) {
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
                logger
                        .warn("Internal error, unknown opcode in message, ignored");
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

            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

            out.writeByte(IBIS_ISALIVE);
            out.writeUTF(poolName);
            out.writeUTF(((IbisIdentifier) ibisId).myId);
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

            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

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

    public void mustLeave(ibis.ipl.IbisIdentifier[] ibisses) throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;

        try {
            s = socketFactory.createClientSocket(serverAddress, 0, null);

            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

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

            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

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
            out = new DataOutputStream(new BufferedOutputStream(s
                    .getOutputStream()));

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

        if (!needsUpcalls) {
            synchronized (this) {
                stop = true;
            }
        } else {
            synchronized (this) {
                while (!left) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
        }

        if (server != null) {
            try {
                server.join();
            } catch (InterruptedException e) {
                // ignored
            }
        }

        logger.info("NS client: leave DONE");
    }

    public void run() {
        logger.info("NameServerClient: thread started");

        while (!stop) {

            VirtualSocket s;

            try {
                s = serverSocket.accept();

                logger.debug("NameServerClient: incoming connection " + "from "
                        + s.toString());

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
                    synchronized (this) {
                        stop = true;
                        left = true;
                        notifyAll();
                    }
                }
                throw new RuntimeException("NameServerClient: got an error", e);
            }

            byte opcode = 0;
            DataInputStream in = null;
            DataOutputStream out = null;

            try {
                in = new DataInputStream(new BufferedInputStream(s
                        .getInputStream()));
                int count;
                IbisIdentifier[] ids;

                opcode = in.readByte();
                logger.debug("NameServerClient: opcode " + opcode);

                switch (opcode) {
                case (IBIS_PING): {
                    out = new DataOutputStream(new BufferedOutputStream(s
                            .getOutputStream()));
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
                        synchronized (this) {
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
                if (!stop) {
                    logger.error("Got an exception in "
                            + "NameServerClient.run " + "(opcode = " + opcode
                            + ")", e1);
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
