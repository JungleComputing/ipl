package ibis.ipl.impl.registry.central;

import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

final class PeerConnectionHandler implements Runnable {

    private static final Logger logger = Logger
            .getLogger(PeerConnectionHandler.class);

    private final ConnectionFactory connectionFactory;

    private final Registry registry;

    PeerConnectionHandler(ConnectionFactory connectionFactory, Registry registry) {
        this.connectionFactory = connectionFactory;
        this.registry = registry;

        ThreadPool.createNew(this, "peer connection handler");
    }

    private void handleGossip(Connection connection) throws IOException {
        logger.debug("got a gossip request");

        String poolName = connection.in().readUTF();
        int peerTime = connection.in().readInt();

        if (!poolName.equals(registry.getPoolName())) {
            logger.error("wrong pool: " + poolName + " instead of "
                    + registry.getPoolName());
            connection.closeWithError("wrong pool: " + poolName
                    + " instead of " + registry.getPoolName());
            return;
        }

        int localTime = registry.currentEventTime();

        connection.sendOKReply();
        connection.out().writeInt(localTime);
        connection.out().flush();

        if (localTime > peerTime) {
            int sendEntries = localTime - peerTime;

            connection.out().writeInt(sendEntries);
            for (int i = 0; i < sendEntries; i++) {
                Event event = registry.getEvent(peerTime + i);

                event.writeTo(connection.out());
            }
            connection.out().flush();

        } else if (localTime < peerTime) {
            Event[] newEvents = new Event[connection.in().readInt()];
            for (int i = 0; i < newEvents.length; i++) {
                newEvents[i] = new Event(connection.in());
            }

            connection.close();

            registry.handleNewEvents(newEvents);
        }
        connection.close();
    }

    private void handlePush(Connection connection) throws IOException {
        logger.debug("got a push from the server");

        String poolName = connection.in().readUTF();

        if (!poolName.equals(registry.getPoolName())) {
            logger.error("wrong pool: " + poolName + " instead of "
                    + registry.getPoolName());
            connection.closeWithError("wrong pool: " + poolName
                    + " instead of " + registry.getPoolName());
        }

        if (!registry.statefulServer()) {
            connection.out().writeInt(registry.currentEventTime());
            connection.out().flush();
        }

        int events = connection.in().readInt();

        if (events < 0) {
            connection.closeWithError("negative event size");
            return;
        }

        Event[] newEvents = new Event[connection.in().readInt()];
        for (int i = 0; i < newEvents.length; i++) {
            newEvents[i] = new Event(connection.in());
        }

        connection.sendOKReply();

        connection.close();

        registry.handleNewEvents(newEvents);
    }

    private void handlePing(Connection connection) throws IOException {
        logger.debug("got a ping request");
        connection.sendOKReply();
        registry.getIbisIdentifier().writeTo(connection.out());
        connection.out().flush();
        connection.close();
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = connectionFactory.accept();
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (!registry.isStopped()) {
                logger.error("error on accepting connection", e);
            }
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

        try {
            byte magic = connection.in().readByte();

            if (magic != Protocol.CLIENT_MAGIC_BYTE) {
                throw new IOException(
                        "Invalid header byte in accepting connection");
            }

            byte opcode = connection.in().readByte();

            logger.debug("received request: " + Protocol.opcodeString(opcode));

            switch (opcode) {
            case Protocol.OPCODE_GOSSIP:
                handleGossip(connection);
                break;
            case Protocol.OPCODE_PING:
                handlePing(connection);
                break;
            case Protocol.OPCODE_PUSH:
                handlePush(connection);
                break;
            default:
                logger.error("unknown opcode in request: " + opcode + "("
                        + Protocol.opcodeString(opcode) + ")");
            }
            logger.debug("done handling request");
        } catch (IOException e) {
            logger.error("error on handling request", e);
        }
    }
}
