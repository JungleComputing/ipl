package ibis.ipl.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.support.Connection;
import ibis.ipl.support.management.AttributeDescription;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerConnectionHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 50;

    static final int MAX_THREADS = 10;

    private static final Logger logger = LoggerFactory
            .getLogger(ServerConnectionHandler.class);

    private final Server server;

    private final VirtualServerSocket serverSocket;

    private int currentNrOfThreads = 0;

    private boolean ended = false;

    public ServerConnectionHandler(Server server,
            VirtualSocketFactory socketFactory) throws IOException {
        this.server = server;

        serverSocket = socketFactory.createServerSocket(ServerConnectionProtocol.VIRTUAL_PORT,
                CONNECTION_BACKLOG, null);
        
        createThread();
    }

    private synchronized void createThread() {
        while (currentNrOfThreads >= MAX_THREADS && !ended) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        if (ended) {
            return;
        }

        // create new thread for next connection
        ThreadPool.createNew(this, "server connection handler");
        currentNrOfThreads++;

        logger.debug("Now " + currentNrOfThreads + " connections");
    }

    private synchronized void threadEnded() {
        currentNrOfThreads--;

        notifyAll();
    }

    private synchronized boolean hasEnded() {
        return ended;
    }

    private void handleGetAddress(Connection connection) throws IOException {
        connection.sendOKReply();
        connection.out().writeUTF(server.getAddress());
    }

    private void handleGetServiceNames(Connection connection)
            throws IOException {
        String[] result = server.getServiceNames();

        connection.sendOKReply();

        connection.out().writeInt(result.length);
        for (String name : result) {
            connection.out().writeUTF(name);
        }
    }

    private void handleGetHubs(Connection connection) throws IOException {
        String[] result = server.getHubs();

        connection.sendOKReply();

        connection.out().writeInt(result.length);
        for (String hub : result) {
            connection.out().writeUTF(hub);
        }
    }

    private void handleAddHubs(Connection connection) throws IOException {
        int nrOfNewHubs = connection.in().readInt();

        if (nrOfNewHubs < 0) {
            throw new IOException("negative hub list size");
        }

        String[] hubs = new String[nrOfNewHubs];
        for (int i = 0; i < hubs.length; i++) {
            hubs[i] = connection.in().readUTF();
        }

        server.addHubs(hubs);

        connection.sendOKReply();
    }

    private void handleEnd(Connection connection) throws IOException {
        long timeout = connection.in().readLong();

        // send reply and close first. End will cause our
        // SocketFactory to dissappear
        connection.sendOKReply();
        connection.close();

        server.end(timeout);
    }

    private void handleRegistryGetPools(Connection connection)
            throws IOException {
        String[] result = server.getRegistryService().getPools();

        connection.sendOKReply();

        connection.out().writeInt(result.length);
        for (String element : result) {
            connection.out().writeUTF(element);
        }
    }

    private void handleRegistryGetPoolSizes(Connection connection)
            throws IOException {
        Map<String, Integer> result = server.getRegistryService().getPoolSizes();

        connection.sendOKReply();

        connection.out().writeInt(result.size());
        for (Map.Entry<String, Integer> entry: result.entrySet()) {
            connection.out().writeUTF(entry.getKey());
            connection.out().writeInt(entry.getValue());
        }
    }

    private void handleRegistryGetLocations(Connection connection)
            throws IOException {
        String poolName = connection.in().readUTF();
        
        String[] result = server.getRegistryService().getLocations(poolName);

        connection.sendOKReply();

        connection.out().writeInt(result.length);
        for (String element : result) {
            connection.out().writeUTF(element);
        }
    }

    private void handleRegistryGetMembers(Connection connection)
            throws IOException {
        String pool = connection.in().readUTF();

        IbisIdentifier[] result = server.getRegistryService().getMembers(pool);

        connection.sendOKReply();

        connection.out().writeInt(result.length);
        for (IbisIdentifier element : result) {
            byte[] bytes = element.toBytes();
            connection.out().write(bytes);
        }
    }

    private void handleManagementGetAttributes(Connection connection)
            throws IOException, ClassNotFoundException {

        IbisIdentifier ibis = (IbisIdentifier) connection.readObject();
        AttributeDescription[] attributes = (AttributeDescription[]) connection
                .readObject();

        Object[] result = server.getManagementService().getAttributes(ibis,
                attributes);

        connection.sendOKReply();

        connection.writeObject(result);
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (hasEnded()) {
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

        createThread();

        if (connection == null) {
            threadEnded();
            return;
        }

        byte opcode = 0;
        try {
            byte magic = connection.in().readByte();

            if (magic != ServerConnectionProtocol.MAGIC_BYTE) {
                throw new IOException(
                        "Invalid header byte in accepting connection");
            }

            opcode = connection.in().readByte();

            logger.debug("got request, opcode = " + opcode);

            // public static final byte OPCODE_GET_ADDRESS = 0;
            // public static final byte OPCODE_GET_SERVICE_NAMES = 1;
            // public static final byte OPCODE_GET_HUBS = 2;
            // public static final byte OPCODE_ADD_HUBS = 3;
            // public static final byte OPCODE_END = 4;
            // public static final byte OPCODE_REGISTRY_GET_MEMBERS = 5;
            // public static final byte OPCODE_MANAGEMENT_GET_ATTRIBUTES = 6;

            switch (opcode) {
            case ServerConnectionProtocol.OPCODE_GET_ADDRESS:
                handleGetAddress(connection);
                break;
            case ServerConnectionProtocol.OPCODE_GET_SERVICE_NAMES:
                handleGetServiceNames(connection);
                break;
            case ServerConnectionProtocol.OPCODE_GET_HUBS:
                handleGetHubs(connection);
                break;
            case ServerConnectionProtocol.OPCODE_ADD_HUBS:
                handleAddHubs(connection);
                break;
            case ServerConnectionProtocol.OPCODE_END:
                handleEnd(connection);
                break;
            case ServerConnectionProtocol.OPCODE_REGISTRY_GET_POOLS:
                handleRegistryGetPools(connection);
                break;
            case ServerConnectionProtocol.OPCODE_REGISTRY_GET_POOL_SIZES:
                handleRegistryGetPoolSizes(connection);
                break;
            case ServerConnectionProtocol.OPCODE_REGISTRY_GET_LOCATIONS:
                handleRegistryGetLocations(connection);
                break;
            case ServerConnectionProtocol.OPCODE_REGISTRY_GET_MEMBERS:
                handleRegistryGetMembers(connection);
                break;
            case ServerConnectionProtocol.OPCODE_MANAGEMENT_GET_ATTRIBUTES:
                handleManagementGetAttributes(connection);
                break;
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (Exception e) {
            // send error to client
            connection.closeWithError("Server: " + e.getMessage());
            logger.error("error on handling connection", e);
        } finally {
            connection.close();
        }

        threadEnded();
    }

    public void end() {
        synchronized (this) {
            ended = true;
            notifyAll();
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            // IGNORE
        }
    }

}
