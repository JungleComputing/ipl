package ibis.ipl.impl.registry.gossip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.server.ServerProperties;
import ibis.server.Service;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

public class BootstrapService implements Service, Runnable {

    public static final int VIRTUAL_PORT = 303;

    private static final int CONNECTION_BACKLOG = 50;

    private static final Logger logger =
            Logger.getLogger(BootstrapService.class);

    private final VirtualServerSocket serverSocket;

    private final VirtualSocketFactory socketFactory;

    private final Map<String, ARRG> arrgs;

//    private final boolean printStats;

    private final boolean printErrors;

    private final boolean printEvents;
    
    private boolean ended = false;

    public BootstrapService(TypedProperties properties,
            VirtualSocketFactory socketFactory) throws IOException {
        this.socketFactory = socketFactory;

//        printStats =
//                properties
//                        .getBooleanProperty(ServerProperties.PRINT_STATS);

        printErrors =
                properties
                        .getBooleanProperty(ServerProperties.PRINT_ERRORS);

        printEvents =
            properties
                    .getBooleanProperty(ServerProperties.PRINT_EVENTS);
        
        arrgs = new HashMap<String, ARRG>();

        serverSocket =
                socketFactory.createServerSocket(VIRTUAL_PORT,
                        CONNECTION_BACKLOG, null);

        ThreadPool.createNew((Runnable) this,
                "bootstrap service connection handler");

    }

    public synchronized void end(boolean waitUntilIdle) {
        ended = true;

        try {
            serverSocket.close();
        } catch (IOException e) {
            // IGNORE
        }

        for (ARRG arrg : arrgs.values()) {
            arrg.end();
        }

        notifyAll();
    }

    synchronized boolean hasEnded() {
        return ended;
    }

    private synchronized ARRG getOrCreateARRG(String poolName) {
        ARRG result = arrgs.get(poolName);

        if (result == null) {
            result =
                    new ARRG(serverSocket.getLocalSocketAddress(), true,
                            new VirtualSocketAddress[0], null, poolName,
                            socketFactory);
            arrgs.put(poolName, result);

            System.out.println("Bootstrap service for new pool: " + poolName);
        }

        return result;
    }

    private synchronized void cleanup() {
        // copy values so we can remove them from the map
        for (ARRG arrg : arrgs.values().toArray(new ARRG[0])) {
            if (arrg.isDead()) {
                // this pool is dead, remove it...
                arrgs.remove(arrg.getPoolName());
            }
        }
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (hasEnded()) {
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
        ThreadPool.createNew(this, "bootstrap service connection handler");

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
                String poolName = connection.in().readUTF();

                ARRG arrg = getOrCreateARRG(poolName);

                arrg.handleGossip(connection, printEvents);
                
                break;
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (IOException e) {
            if (printErrors) {
                System.out.println("error on handling connection");
                e.printStackTrace(System.out);
            }
        } finally {
            connection.close();
        }

        logger.debug("done handling request");

        // delete any dead ARRG's
        cleanup();
    }

    public String toString() {
        return "Bootstrap service on virtual port " + VIRTUAL_PORT;
    }

}
