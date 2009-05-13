package ibis.ipl.registry.gossip;

import ibis.ipl.registry.Connection;
import ibis.ipl.registry.statistics.Statistics;
import ibis.ipl.server.ServerProperties;
import ibis.ipl.server.Service;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapService implements Service, Runnable {

    public static final int VIRTUAL_PORT = 303;

    private static final int CONNECTION_BACKLOG = 50;

    static final int MAX_THREADS = 50;

    private static final Logger logger = LoggerFactory
            .getLogger(BootstrapService.class);

    private final VirtualServerSocket serverSocket;

    private final VirtualSocketFactory socketFactory;

    private final Map<String, ARRG> arrgs;

    // private final boolean printEvents;

    // private final boolean printEvents;

    private final boolean printErrors;

    private final boolean keepStatistics;

    private boolean ended = false;

    private int currentNrOfThreads = 0;

    private int maxNrOfThreads = 0;

    public BootstrapService(TypedProperties properties,
            VirtualSocketFactory socketFactory) throws IOException {
        this.socketFactory = socketFactory;

        printErrors = properties
                .getBooleanProperty(ServerProperties.PRINT_ERRORS);

        //printEvents = properties.getBooleanProperty(ServerProperties.PRINT_EVENTS);

        keepStatistics = properties
                .getBooleanProperty(RegistryProperties.STATISTICS);

        arrgs = new HashMap<String, ARRG>();

        serverSocket = socketFactory.createServerSocket(VIRTUAL_PORT,
            CONNECTION_BACKLOG, null);

        createThread();
    }

    public String getServiceName() {
        return "bootstrap";
    }

    public synchronized void end(long deadline) {
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
            Statistics statistics = null;
            if (keepStatistics) {
                statistics = new Statistics(Protocol.OPCODE_NAMES);
                statistics.setID("server", poolName);
                statistics.startWriting(60000);
            }
            result = new ARRG(serverSocket.getLocalSocketAddress(), true,
                    new VirtualSocketAddress[0], null, poolName, socketFactory,
                    null);
            result.start();
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

    private synchronized void createThread() {
        while (currentNrOfThreads >= MAX_THREADS) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // create new thread for next connection
        ThreadPool.createNew(this, "bootstrap service connection handler");
        currentNrOfThreads++;

        logger.trace("now " + currentNrOfThreads + " threads");

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
                String poolName = connection.in().readUTF();

                ARRG arrg = getOrCreateARRG(poolName);

                arrg.handleGossip(connection);

                connection.close();

                Statistics statistics = arrg.getStatistics();
                if (statistics != null) {
                    statistics.add(opcode, System.currentTimeMillis() - start,
                        connection.read(), connection.written(), true);
                }
                break;
            default:
                logger.error("unknown opcode: " + opcode);
                connection.close();
            }
        } catch (IOException e) {
            if (printErrors) {
                System.out.println("error on handling connection");
                e.printStackTrace(System.out);
            }
            connection.close();
        }

        logger.debug("done handling request");

        // delete any dead ARRG's
        cleanup();
        threadEnded();
    }

    public String toString() {
        return "Bootstrap service on virtual port " + VIRTUAL_PORT;
    }

    public Map<String, String> getStats() {
        // no statistics
        return new HashMap<String, String>();
    }

}
