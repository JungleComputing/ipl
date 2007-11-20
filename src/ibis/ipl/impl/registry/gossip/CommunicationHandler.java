package ibis.ipl.impl.registry.gossip;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.Connection;
import ibis.server.Client;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

class CommunicationHandler extends Thread {

    private static final int CONNECTION_BACKLOG = 50;

    private static final int CONNECTION_TIMEOUT = 5000;

    private static final Logger logger =
            Logger.getLogger(CommunicationHandler.class);

    private final Registry registry;
    
    private final Pool pool;
    
    private final ElectionSet elections;

    private final VirtualSocketFactory socketFactory;

    private final VirtualServerSocket serverSocket;

    private final ARRG arrg;

    CommunicationHandler(TypedProperties properties, Registry registry,
            Pool members, ElectionSet elections) throws IOException {
        this.registry = registry;
        this.pool = members;
        this.elections = elections;

        try {
            socketFactory = Client.getFactory(properties);
        } catch (InitializationException e) {
            throw new IOException("Could not create socket factory: " + e);
        }

        serverSocket =
                socketFactory.createServerSocket(0, CONNECTION_BACKLOG, null);

        VirtualSocketAddress serverAddress = null;
        try {
            serverAddress =
                    Client.getServiceAddress(BootstrapService.VIRTUAL_PORT,

                    properties);
        } catch (IOException e) {
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
                        socketFactory);

    }

    @Override
    public synchronized void start() {
        this.setDaemon(true);

        super.start();

        arrg.start();
    }

    public void sendSignals(String signal, IbisIdentifier[] ibises)
            throws IOException {
        String errorMessage = null;

        for (IbisIdentifier ibis : ibises) {
            try {
                Connection connection =
                        new Connection(ibis, CONNECTION_TIMEOUT, true,
                                socketFactory);

                connection.out().writeByte(Protocol.MAGIC_BYTE);
                connection.out().writeByte(Protocol.OPCODE_SIGNAL);
                registry.getIbisIdentifier().writeTo(connection.out());
                connection.out().writeUTF(signal);

                connection.getAndCheckReply();

                connection.close();

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
        IbisIdentifier target = new IbisIdentifier(connection.in());
        String signal = connection.in().readUTF();

        if (!target.equals(registry.getIbisIdentifier())) {
            connection
                    .closeWithError("signal target not equal to local identifier");
            return;
        }

        connection.sendOKReply();

        registry.signal(signal);

        connection.close();
    }

    public void gossip() {
        VirtualSocketAddress address = arrg.getRandomMember();
        
        if (address == null || address.equals(serverSocket.getLocalSocketAddress())) {
            logger.debug("noone to gossip with, or (not) gossiping with self");
            return;
        }
        
        try {
            Connection connection =
                    new Connection(address, CONNECTION_TIMEOUT, true,
                            socketFactory);

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GOSSIP);
            registry.getIbisIdentifier().writeTo(connection.out());
            
            pool.writeGossipData(connection.out());
            elections.writeGossipData(connection.out());
            
            connection.getAndCheckReply();
            
            pool.readGossipData(connection.in());
            elections.readGossipData(connection.in());
            
            connection.close();
        } catch (IOException e) {
            logger.error("could not gossip with " + address);
        }
    }
    
    private void handleGossip(Connection connection) throws IOException {
        IbisIdentifier peer = new IbisIdentifier(connection.in());
        
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
        VirtualSocketAddress[] addresses = arrg.getMembers();

        for (VirtualSocketAddress address : addresses) {
            try {
                Connection connection =
                        new Connection(address, CONNECTION_TIMEOUT, true,
                                socketFactory);

                connection.out().writeByte(Protocol.MAGIC_BYTE);
                connection.out().writeByte(Protocol.OPCODE_LEAVE);
                registry.getIbisIdentifier().writeTo(connection.out());

                connection.getAndCheckReply();

                connection.close();

            } catch (IOException e) {
                logger.error("could not send leave to " + address);
            }
        }
    }
    
    private void handleLeave(Connection connection) throws IOException {
        IbisIdentifier ibis = new IbisIdentifier(connection.in());

        connection.sendOKReply();

        pool.leave(ibis);

        connection.close();
    }
    
    public void ping(IbisIdentifier ibis) throws IOException {
        Connection connection =
            new Connection(ibis, CONNECTION_TIMEOUT, true,
                    socketFactory);

        connection.out().writeByte(Protocol.MAGIC_BYTE);
        connection.out().writeByte(Protocol.OPCODE_PING);

        connection.getAndCheckReply();
        IbisIdentifier result = new IbisIdentifier(connection.in());
        
        connection.close();
        
        if (!result.equals(ibis)) {
            throw new IOException("tried to ping " + ibis + ", reached " + result + " instead");
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

        arrg.handleGossip(connection, false);
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (registry.isStopped()) {
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
            case Protocol.OPCODE_LEAVE:
                handleLeave(connection);
                break;
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

    }

    VirtualSocketAddress getAddress() {
        return serverSocket.getLocalSocketAddress();
    }

}
