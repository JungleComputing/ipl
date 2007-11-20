package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.Connection;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

final class ServerConnectionHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 50;

    static final int THREADS = 10;

    private static final Logger logger =
        Logger.getLogger(ServerConnectionHandler.class);

    private final Server server;

    private final VirtualSocketFactory socketFactory;

    private final VirtualServerSocket serverSocket;

    ServerConnectionHandler(Server server,
            VirtualSocketFactory connectionFactory) throws IOException {
        this.server = server;
        this.socketFactory = connectionFactory;

        serverSocket =
            socketFactory.createServerSocket(Server.VIRTUAL_PORT,
                CONNECTION_BACKLOG, null);

        ThreadPool.createNew(this, "registry server connection handler");
    }

    private void handleJoin(Connection connection) throws Exception {
        Member member;
        Pool pool;

        int length = connection.in().readInt();
        if (length < 0) {
            throw new IOException("unexpected end of data on join");
        }
        byte[] clientAddress = new byte[length];
        connection.in().readFully(clientAddress);

        String poolName = connection.in().readUTF();

        length = connection.in().readInt();
        if (length < 0) {
            throw new IOException("unexpected end of data on join");
        }
        byte[] implementationData = new byte[length];
        connection.in().readFully(implementationData);

        String ibisImplementationIdentifier = connection.in().readUTF();

        Location location = new Location(connection.in());

        long heartbeatInterval = connection.in().readLong();
        long eventPushInterval = connection.in().readLong();
        boolean gossip = connection.in().readBoolean();
        long gossipInterval = connection.in().readLong();
        boolean adaptGossipInterval = connection.in().readBoolean();
        boolean tree = connection.in().readBoolean();
        boolean closedWorld = connection.in().readBoolean();
        int poolSize = connection.in().readInt();

        pool =
            server.getAndCreatePool(poolName, heartbeatInterval,
                eventPushInterval, gossip, gossipInterval, adaptGossipInterval,
                tree, closedWorld, poolSize, ibisImplementationIdentifier);

        try {
            member =
                pool.join(implementationData, clientAddress, location,
                    ibisImplementationIdentifier);
        } catch (IOException e) {
            connection.closeWithError(e.getMessage());
            throw e;
        }

        connection.sendOKReply();
        member.getIbis().writeTo(connection.out());
        connection.out().writeInt(member.getCurrentTime());

        pool.writeBootstrapList(connection.out());

        connection.out().flush();
        pool.gotHeartbeat(member.getIbis());
    }

    private void handleLeave(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        try {
            pool.leave(identifier);
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            throw e;
        }

        connection.sendOKReply();

        if (pool.hasEnded()) {
            // wake up the server so it can check the pools (and remove this
            // one)
            server.nudge();
        }
        pool.gotHeartbeat(identifier);
    }

    private void handleElect(Connection connection) throws Exception {
        IbisIdentifier candidate = new IbisIdentifier(connection.in());
        String election = connection.in().readUTF();

        Pool pool = server.getPool(candidate.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + candidate.poolName() + " not found");
        }

        IbisIdentifier winner = pool.elect(election, candidate);

        connection.sendOKReply();

        winner.writeTo(connection.out());
        pool.gotHeartbeat(candidate);
    }

    private void handleGetSequenceNumber(Connection connection)
            throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String name = connection.in().readUTF();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        long number = pool.getSequenceNumber(name);

        connection.sendOKReply();

        connection.out().writeLong(number);
        pool.gotHeartbeat(identifier);
    }

    private void handleDead(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        IbisIdentifier corpse = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        try {
            pool.dead(corpse, new Exception("ibis declared dead by "
                    + identifier));
        } catch (Exception e) {
            connection.closeWithError(e.getMessage());
            throw e;
        }

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
    }

    private void handleMaybeDead(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        IbisIdentifier suspect = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        pool.maybeDead(suspect);

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
    }

    private void handleSignal(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        String signal = connection.in().readUTF();

        IbisIdentifier[] receivers =
            new IbisIdentifier[connection.in().readInt()];
        for (int i = 0; i < receivers.length; i++) {
            receivers[i] = new IbisIdentifier(connection.in());
        }

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        pool.signal(signal, receivers);

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
    }

    private void handleGetState(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        int joinTime = connection.in().readInt();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        connection.sendOKReply();
        pool.writeState(connection.out(), joinTime);
        connection.out().flush();
        pool.gotHeartbeat(identifier);
    }

    private void handleHeartbeat(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (server.isStopped()) {
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
        ThreadPool.createNew(this, "peer connection handler");

        if (connection == null) {
            return;
        }
        long start = System.currentTimeMillis();

        byte opcode = 0;
        try {
            byte magic = connection.in().readByte();

            if (magic != Protocol.SERVER_MAGIC_BYTE) {
                throw new IOException(
                        "Invalid header byte in accepting connection");
            }

            opcode = connection.in().readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("got request, opcode = "
                        + Protocol.opcodeString(opcode));
            }

            switch (opcode) {
            case Protocol.OPCODE_JOIN:
                handleJoin(connection);
                break;
            case Protocol.OPCODE_LEAVE:
                handleLeave(connection);
                break;
            case Protocol.OPCODE_ELECT:
                handleElect(connection);
                break;
            case Protocol.OPCODE_SEQUENCE_NR:
                handleGetSequenceNumber(connection);
                break;
            case Protocol.OPCODE_DEAD:
                handleDead(connection);
                break;
            case Protocol.OPCODE_MAYBE_DEAD:
                handleMaybeDead(connection);
                break;
            case Protocol.OPCODE_SIGNAL:
                handleSignal(connection);
                break;
            case Protocol.OPCODE_GET_STATE:
                handleGetState(connection);
                break;
            case Protocol.OPCODE_HEARTBEAT:
                handleHeartbeat(connection);
                break;
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (Exception e) {
            logger.error("error on handling connection", e);
        } finally {
            connection.close();
        }

        server.getStats().add(opcode, System.currentTimeMillis() - start,
            connection.read(), connection.written(), true);
        logger.debug("done handling request");
    }

    public void end() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            // IGNORE
        }
    }
}
