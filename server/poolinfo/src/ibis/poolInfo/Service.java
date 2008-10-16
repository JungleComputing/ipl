package ibis.poolInfo;

import ibis.server.ServerProperties;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Service implements ibis.server.Service, Runnable {

    public static final int VIRTUAL_PORT = 301;

    public static final int RESULT_INVALID_SIZE = -1;

    public static final int RESULT_POOL_CLOSED = -2;

    public static final int RESULT_UNEQUAL_SIZE = -3;

    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    private final VirtualServerSocket serverSocket;

    private final Map<String, Pool> pools;

    private final boolean printEvents;

    public Service(TypedProperties properties, VirtualSocketFactory factory)
            throws IOException {
        pools = new HashMap<String, Pool>();

        serverSocket = factory.createServerSocket(VIRTUAL_PORT, 0, null);

        ThreadPool.createNew(this, "PoolInfoService");

        printEvents =
                properties.getBooleanProperty(ServerProperties.PRINT_EVENTS);

        logger
                .debug("Started PoolInfo service on virtual port "
                        + VIRTUAL_PORT);
    }

    public void end(long deadline) {
        //NOTHING
    }
    
    public String getServiceName() {
        return "poolinfo";
    }

    public Map<String, String> getStats() {
        return new HashMap<String, String>();
    }


    private synchronized Pool getPool(String poolName, int size) {
        Pool pool = pools.get(poolName);

        if (pool == null) {
            pool = new Pool(poolName, size, printEvents);
            pools.put(poolName, pool);
        }

        return pool;
    }

    /**
     * Called more than once, but doesn't matter
     */
    private synchronized void removePool(String poolName) {
        pools.remove(poolName);
    }

    public String toString() {
        return "PoolInfo service on virtual port " + VIRTUAL_PORT;
    }

    public void run() {
        while (true) {

            VirtualSocket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                logger.error("could not accept socket, stopping service", e);
                return;
            }

            // handle next connection in new thread;
            ThreadPool.createNew(this, "PoolInfoService");

            try {

                DataOutputStream out =
                        new DataOutputStream(new BufferedOutputStream(socket
                                .getOutputStream()));
                DataInputStream in =
                        new DataInputStream(new BufferedInputStream(socket
                                .getInputStream()));

                String poolName = in.readUTF();
                String address = in.readUTF();
                String cluster = in.readUTF();
                int size = in.readInt();

                Pool pool = getPool(poolName, size);

                // blocks until pool is complete
                int rank = pool.join(address, cluster, size);

                // remove pool from list of pools...
                removePool(poolName);

                out.writeInt(rank);
                if (rank >= 0) {
                    String[] addresses = pool.getAddresses();
                    for (int i = 0; i < addresses.length; i++) {
                        out.writeUTF(addresses[i]);
                    }
                    String[] clusters = pool.getClusters();
                    for (int i = 0; i < clusters.length; i++) {
                        out.writeUTF(clusters[i]);
                    }
                }
                out.flush();
                out.close();
                in.close();
                socket.close();
            } catch (Exception e) {
                logger.error("error on handling PoolInfo request", e);
            }
        }

    }


}
