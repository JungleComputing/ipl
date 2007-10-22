package ibis.ipl.impl.registry.gossip;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.server.Client;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

class CommunicationHandler extends Thread {

    private static final int CONNECTION_BACKLOG = 50;

    private static final Logger logger =
            Logger.getLogger(CommunicationHandler.class);

    private final Pool pool;

    private final VirtualSocketFactory socketFactory;

    private final VirtualServerSocket serverSocket;

    private final ARRG arrg;

    CommunicationHandler(TypedProperties properties, Pool pool)
            throws IOException {
        this.pool = pool;

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
                        bootstrapList, serverAddress, pool.getName(),
                        socketFactory);

    }
    
    

    @Override
    public synchronized void start() {
        this.setDaemon(true);
        
        super.start();
        
        arrg.start();
    }



    private void handleArrgGossip(Connection connection) throws IOException {
        String poolName = connection.in().readUTF();

        if (!poolName.equals(pool.getName())) {
            connection.closeWithError("wrong pool name");
            return;
        }

        arrg.handleGossip(connection);
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (pool.isStopped()) {
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

    public VirtualSocketAddress getAddress() {
        return serverSocket.getLocalSocketAddress();
    }

}
