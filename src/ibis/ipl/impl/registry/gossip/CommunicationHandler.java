package ibis.ipl.impl.registry.gossip;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.Connection;
import ibis.ipl.impl.registry.statistics.Statistics;
import ibis.server.Client;
import ibis.server.ConfigurationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

class CommunicationHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 25;

    static final int MAX_THREADS = 25;

    private static final int LEAVE_CONNECTION_TIMEOUT = 1000;

    private static final int CONNECTION_TIMEOUT = 5000;

    private static final Logger logger =
        Logger.getLogger(CommunicationHandler.class);

    private final Registry registry;

    private final Statistics statistics;

    private final MemberSet pool;

    private final ElectionSet elections;

    private final VirtualSocketFactory socketFactory;

    private final VirtualServerSocket serverSocket;

    private final ARRG arrg;

    private final int nrOfLeavesSend;

    private int currentNrOfThreads = 0;

    private int maxNrOfThreads = 0;

    CommunicationHandler(TypedProperties properties, Registry registry,
            MemberSet members, ElectionSet elections, Statistics statistics)
            throws IbisConfigurationException, IOException {
        this.registry = registry;
        this.pool = members;
        this.elections = elections;
        this.statistics = statistics;

        nrOfLeavesSend =
            properties.getIntProperty(RegistryProperties.LEAVES_SEND);

        try {
            socketFactory = Client.getFactory(properties);
        } catch (ConfigurationException e) {
            throw new IbisConfigurationException(
                    "Could not create socket factory: " + e);
        } catch (Exception e) {
            throw new IOException("Could not create socket factory: " + e);
        }

        serverSocket =
            socketFactory.createServerSocket(0, CONNECTION_BACKLOG, null);

        VirtualSocketAddress serverAddress = null;
        try {
            serverAddress =
                Client.getServiceAddress(BootstrapService.VIRTUAL_PORT,

                properties);
        } catch (ConfigurationException e) {
            logger.warn("No valid bootstrap service address", e);
        }

        String[] bootstrapStringList =
            properties.getStringList(RegistryProperties.BOOTSTRAP_LIST);
        VirtualSocketAddress[] bootstrapList =
            new VirtualSocketAddress[bootstrapStringList.length];

        for (int i = 0; i < bootstrapList.length; i++) {
            bootstrapList[i] = new VirtualSocketAddress(bootstrapStringList[i]);
        }

        logger.debug("local address = " + serverSocket.getLocalSocketAddress());
        logger.debug("server address = " + serverAddress);

        arrg =
            new ARRG(serverSocket.getLocalSocketAddress(), false,
                    bootstrapList, serverAddress, registry.getPoolName(),
                    socketFactory, statistics);

    }

    public void start() {
        arrg.start();
        createThread();
    }

    public void sendSignals(String signal, IbisIdentifier[] ibises)
            throws IOException {
        String errorMessage = null;

        for (IbisIdentifier ibis : ibises) {
            try {
                long start = System.currentTimeMillis();
                Connection connection =
                    new Connection(ibis, CONNECTION_TIMEOUT, true,
                            socketFactory);

                connection.out().writeByte(Protocol.MAGIC_BYTE);
                connection.out().writeByte(Protocol.OPCODE_SIGNAL);
                registry.getIbisIdentifier().writeTo(connection.out());
                connection.out().writeUTF(signal);

                connection.getAndCheckReply();

                connection.close();
                if (statistics != null) {
                    statistics.add(Protocol.OPCODE_SIGNAL,
                        System.currentTimeMillis() - start, connection.read(),
                        connection.written(), false);
                }
            } catch (IOException e) {
                logger.error("could not send signal to " + ibis);
                if (errorMessage == null) {
                    errorMessage = "could not send signal to: " + ibis;
                } else {
                    errorMessage += ", " + ibis;
                }
            }
        }
        if (errorMessage != null) {
            throw new IOException(errorMessage);
        }
    }

    private void handleSignal(Connection connection) throws IOException {
        IbisIdentifier source = new IbisIdentifier(connection.in());
        String signal = connection.in().readUTF();

        connection.sendOKReply();

        registry.signal(signal, source);

        connection.close();
    }

    public void gossip() {
        VirtualSocketAddress address = arrg.getRandomMember();

        if (address == null
                || address.equals(serverSocket.getLocalSocketAddress())) {
            logger.debug("noone to gossip with, or (not) gossiping with self");
            return;
        }

        try {
            long start = System.currentTimeMillis();
            Connection connection =
                new Connection(address, CONNECTION_TIMEOUT, true, socketFactory);

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GOSSIP);
            registry.getIbisIdentifier().writeTo(connection.out());

            pool.writeGossipData(connection.out());
            elections.writeGossipData(connection.out());

            connection.getAndCheckReply();

            pool.readGossipData(connection.in());
            elections.readGossipData(connection.in());

            connection.close();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_GOSSIP,
                    System.currentTimeMillis() - start, connection.read(),
                    connection.written(), false);
            }
        } catch (IOException e) {
            logger.debug("could not gossip with " + address, e);
        }
    }

    private void handleGossip(Connection connection) throws IOException {
        IbisIdentifier peer = new IbisIdentifier(connection.in());

        if (peer.equals(registry.getIbisIdentifier())) {
            logger.error("eep! talking to ourselves");
            connection.closeWithError("talking to self");
        }

        if (!peer.poolName().equals(registry.getIbisIdentifier().poolName())) {
            connection.closeWithError("wrong pool");
        }

        pool.readGossipData(connection.in());
        elections.readGossipData(connection.in());

        connection.sendOKReply();

        pool.writeGossipData(connection.out());
        elections.writeGossipData(connection.out());

        connection.close();
    }

    /**
     * Sends leave message to everyone ARRG knows :)
     */
    public void broadcastLeave() {
        VirtualSocketAddress[] addresses =
            arrg.getRandomMembers(nrOfLeavesSend);

        Broadcaster broadcaster = new Broadcaster(this, addresses);

        // wait for all the broadcasts to be finished
        broadcaster.waitUntilDone();
    }

    void sendLeave(VirtualSocketAddress address) {
        if (address.equals(serverSocket.getLocalSocketAddress())) {
            // do not connect to self
            return;
        }

        try {
            long start = System.currentTimeMillis();
            Connection connection =
                new Connection(address, LEAVE_CONNECTION_TIMEOUT, true,
                        socketFactory);

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_LEAVE);
            registry.getIbisIdentifier().writeTo(connection.out());

            connection.getAndCheckReply();

            connection.close();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_LEAVE,
                    System.currentTimeMillis() - start, connection.read(),
                    connection.written(), false);
            }
        } catch (IOException e) {
            logger.debug(serverSocket.getLocalSocketAddress()
                    + " could not send leave to " + address);
        }
    }

//    private void handleLeave(Connection connection) throws IOException {
//        IbisIdentifier ibis = new IbisIdentifier(connection.in());
//
//        connection.sendOKReply();
//
//        pool.leave(ibis);
//
//        connection.close();
//    }

    public void ping(IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();
        Connection connection =
            new Connection(ibis, CONNECTION_TIMEOUT, true, socketFactory);

        connection.out().writeByte(Protocol.MAGIC_BYTE);
        connection.out().writeByte(Protocol.OPCODE_PING);

        connection.getAndCheckReply();
        IbisIdentifier result = new IbisIdentifier(connection.in());

        connection.close();

        if (!result.equals(ibis)) {
            throw new IOException("tried to ping " + ibis + ", reached "
                    + result + " instead");
        }
        if (statistics != null) {
            statistics.add(Protocol.OPCODE_PING, System.currentTimeMillis()
                    - start, connection.read(), connection.written(), false);
        }
    }

    private void handlePing(Connection connection) throws IOException {
        connection.sendOKReply();
        registry.getIbisIdentifier().writeTo(connection.out());
    }

    // ARRG gossip send and handled in ARRG class
    private void handleArrgGossip(Connection connection) throws IOException {
        String poolName = connection.in().readUTF();

        if (!poolName.equals(registry.getPoolName())) {
            connection.closeWithError("wrong pool name");
            return;
        }

        arrg.handleGossip(connection);
    }

    private synchronized void createThread() {
        while (currentNrOfThreads >= MAX_THREADS) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // create new thread for next connection
        ThreadPool.createNew(this, "connection handler");
        currentNrOfThreads++;

        if (currentNrOfThreads > maxNrOfThreads) {
            maxNrOfThreads = currentNrOfThreads;
        }
    }

    private synchronized void threadEnded() {
        currentNrOfThreads--;

        notifyAll();
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (registry.isStopped()) {
                threadEnded();
                return;
            }
            logger.error("Accept failed, waiting a second, will retry", e);

            // wait a bit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // IGNORE
            }
        }

        // create new thread for next connection
        createThread();

        if (connection == null) {
            threadEnded();
            return;
        }

        long start = System.currentTimeMillis();
        byte opcode = 0;
        try {
            byte magic = connection.in().readByte();

            if (magic != Protocol.MAGIC_BYTE) {
                throw new IOException(
                        "Invalid header byte in accepting connection");
            }

            opcode = connection.in().readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("got request, opcode = "
                        + Protocol.opcodeString(opcode));
            }

            switch (opcode) {
            case Protocol.OPCODE_ARRG_GOSSIP:
                handleArrgGossip(connection);
                break;
            case Protocol.OPCODE_SIGNAL:
                handleSignal(connection);
                break;
//            case Protocol.OPCODE_LEAVE:
//                handleLeave(connection);
//                break;
            case Protocol.OPCODE_GOSSIP:
                handleGossip(connection);
                break;
            case Protocol.OPCODE_PING:
                handlePing(connection);
                break;
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (IOException e) {
            logger.error("error on handling connection", e);
        } finally {
            connection.close();
        }

        logger.debug("done handling request");

        if (statistics != null) {
            statistics.add(opcode, System.currentTimeMillis() - start,
                connection.read(), connection.written(), true);
        }
        threadEnded();
    }

    VirtualSocketAddress getAddress() {
        return serverSocket.getLocalSocketAddress();
    }

}
