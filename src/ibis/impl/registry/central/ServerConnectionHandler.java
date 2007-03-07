package ibis.impl.registry.central;

import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.util.ThreadPool;

import java.io.IOException;
import org.apache.log4j.Logger;

public class ServerConnectionHandler implements Runnable {

    public static final int THREADS = 10;

    private static final Logger logger = Logger
            .getLogger(ServerConnectionHandler.class);

    private final Server server;

    private final ConnectionFactory connectionFactory;

    private final Stats stats;

    ServerConnectionHandler(Server server, ConnectionFactory connectionFactory) {
        this.server = server;
        this.connectionFactory = connectionFactory;
        stats = new Stats(Protocol.NR_OF_OPCODES);

        ThreadPool.createNew(this, "registry server connection handler");
    }

    private void handleJoin(Connection connection) throws IOException {
        IbisIdentifier result;
        Pool pool;

        byte[] clientAddress = new byte[connection.in().readInt()];
        connection.in().read(clientAddress);
        
        String poolName = connection.in().readUTF();

        byte[] implementationData = new byte[connection.in().readInt()];
        connection.in().read(implementationData);

        Location location = new Location(connection.in());
        
        boolean gossip = connection.in().readBoolean();
        boolean keepNodeState = connection.in().readBoolean();

        pool = server.getAndCreatePool(poolName, gossip, keepNodeState);

        try {
            result = pool.join(implementationData, clientAddress, location);
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            return;
        }

        connection.sendOKReply();
        result.writeTo(connection.out());

        Member[] peers = pool
                .getRandomMembers(Protocol.BOOTSTRAP_LIST_SIZE);

        connection.out().writeInt(peers.length);
        for (Member member : peers) {
            member.ibis().writeTo(connection.out());
        }

        connection.out().flush();
    }

    private void handleLeave(Connection connection) throws IOException {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.getPool());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        try {
            pool.leave(identifier);
        } catch (Exception e) {
            logger.error("error on leave", e);
            connection.closeWithError(e.toString());
            return;
        }
        
        if (pool.ended()) {
            //wake up the server so it can check the pools (and remove this one)
            server.nudge();
        }

        connection.sendOKReply();
    }

    private void handleElect(Connection connection) throws IOException {
        IbisIdentifier candidate = new IbisIdentifier(connection.in());
        String election = connection.in().readUTF();

        Pool pool = server.getPool(candidate.getPool());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        IbisIdentifier winner = pool.elect(election, candidate);

        connection.sendOKReply();

        winner.writeTo(connection.out());
    }

    private void handleGetSequenceNumber(Connection connection)
            throws IOException {
        String poolName = connection.in().readUTF();

        Pool pool = server.getPool(poolName);

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        long number = pool.getSequenceNumber();

        connection.sendOKReply();

        connection.out().writeLong(number);
    }

    private void handleDead(Connection connection) throws IOException {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.getPool());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        try {
            pool.dead(identifier);
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            return;
        }

        connection.sendOKReply();

    }

    private void handleMaybeDead(Connection connection) throws IOException {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.getPool());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        pool.maybeDead(identifier);

        connection.sendOKReply();

    }

    private void handleMustLeave(Connection connection) throws IOException {
        String poolName = connection.in().readUTF();

        IbisIdentifier[] identifiers = new IbisIdentifier[connection.in()
                .readInt()];
        for (int i = 0; i < identifiers.length; i++) {
            identifiers[i] = new IbisIdentifier(connection.in());

        }

        Pool pool = server.getPool(poolName);

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        pool.mustLeave(identifiers);

        connection.sendOKReply();

    }

    void printStats(boolean clear) {
        stats.print("registry server statistics", clear);
    }

    public void run() {
        Connection connection;
        try {
            connection = connectionFactory.accept();
        } catch (IOException e) {
            if (!server.isStopped()) {
                logger.error("could not accept socket", e);
            }
            return;
        }

        // new thread for next connection
        ThreadPool.createNew(this, "registry server connection handler");

        long start = System.currentTimeMillis();

        try {

            if (logger.isDebugEnabled()) {
                logger.debug("got request, opcode = "
                        + Protocol.opcodeString(connection.getOpcode()));
            }

            switch (connection.getOpcode()) {
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
            case Protocol.OPCODE_MUST_LEAVE:
                handleMustLeave(connection);
                break;
            default:
                logger.error("unknown opcode: " + connection.getOpcode());
            }
        } catch (IOException e) {
            logger.error("error on handling connection", e);
        }
        connection.close();
        
        stats.add(connection.getOpcode(), System.currentTimeMillis() - start);
        logger.debug("done handling request");
    }
}
