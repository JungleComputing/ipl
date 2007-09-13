package ibis.ipl.impl.registry.old;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

final class ServerConnectionHandler implements Runnable {

    static final int THREADS = 10;

    private static final Logger logger =
            Logger.getLogger(ServerConnectionHandler.class);

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
        connection.in().readFully(clientAddress);

        String poolName = connection.in().readUTF();

        byte[] implementationData = new byte[connection.in().readInt()];
        connection.in().readFully(implementationData);

        Location location = new Location(connection.in());

        boolean gossip = connection.in().readBoolean();
        boolean keepNodeState = connection.in().readBoolean();
        long pingInterval = connection.in().readLong();

        pool =
                server.getAndCreatePool(poolName, gossip, keepNodeState,
                        pingInterval);

        try {
            result = pool.join(implementationData, clientAddress, location);
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            return;
        }

        connection.sendOKReply();
        result.writeTo(connection.out());

        pool.writeBootstrap(connection.out());

        connection.out().flush();
    }

    private void handleLeave(Connection connection) throws IOException {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

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

        connection.sendOKReply();

        if (pool.ended()) {
            // wake up the server so it can check the pools (and remove this
            // one)
            server.nudge();
        }
    }

    private void handleElect(Connection connection) throws IOException {
        IbisIdentifier candidate = new IbisIdentifier(connection.in());
        String election = connection.in().readUTF();

        Pool pool = server.getPool(candidate.poolName());

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

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        try {
            pool.dead(identifier, new Exception("ibis declared dead by application"));
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            return;
        }

        connection.sendOKReply();

    }

    private void handleMaybeDead(Connection connection) throws IOException {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        pool.maybeDead(identifier);

        connection.sendOKReply();

    }

    private void handleSignal(Connection connection) throws IOException {
        String poolName = connection.in().readUTF();

        String signal = connection.in().readUTF();

        IbisIdentifier[] identifiers =
                new IbisIdentifier[connection.in().readInt()];
        for (int i = 0; i < identifiers.length; i++) {
            identifiers[i] = new IbisIdentifier(connection.in());

        }

        Pool pool = server.getPool(poolName);

        if (pool == null) {
            connection.closeWithError("pool not found");
            return;
        }

        pool.signal(signal, identifiers);

        connection.sendOKReply();

    }

    Stats getStats() {
        return stats;
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = connectionFactory.accept();
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
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (IOException e) {
            logger.error("error on handling connection", e);
            return;
        } finally {
            connection.close();
        }

        stats.add(opcode, System.currentTimeMillis() - start);
        logger.debug("done handling request");
    }
}
