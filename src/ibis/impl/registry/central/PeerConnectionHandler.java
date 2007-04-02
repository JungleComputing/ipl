package ibis.impl.registry.central;

import ibis.impl.registry.Connection;
import ibis.impl.registry.ConnectionFactory;
import ibis.impl.registry.Event;
import ibis.impl.registry.Protocol;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

final class PeerConnectionHandler implements Runnable {

    private static final Logger logger = Logger
            .getLogger(PeerConnectionHandler.class);

    private final ConnectionFactory connectionFactory;

    private final Registry registry;

    public PeerConnectionHandler(ConnectionFactory connectionFactory,
            Registry registry) {
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
        }

        int localTime = registry.currentEventTime();

        connection.sendOKReply();
        connection.out().writeInt(localTime);

        if (localTime > peerTime) {
            int sendEntries = localTime - peerTime;

            connection.out().writeInt(sendEntries);
            for (int i = 0; i < sendEntries; i++) {
                Event event = registry.getEvent(peerTime + i);

                event.writeTo(connection.out());
            }

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
    }

    public void run() {
        Connection connection;
        try {
            connection = connectionFactory.accept();
        } catch (IOException e) {
            if (!registry.isStopped()) {
                logger.error("error on accepting connection", e);
            }
            return;
        }
        // create new thread for next connection
        ThreadPool.createNew(this, "peer connection handler");
        
        try {
            switch (connection.getOpcode()) {
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
                logger.error("unknown opcode in request: "
                        + connection.getOpcode());
            }
            logger.debug("done handling request");
        } catch (IOException e) {
            logger.error("error on handling connection", e);
        }
        connection.close();
    }
}
