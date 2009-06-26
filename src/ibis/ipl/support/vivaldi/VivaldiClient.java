package ibis.ipl.support.vivaldi;

import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.support.Client;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VivaldiClient implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(VivaldiClient.class);

    // how long do we wait for a connection
    public static final int CONNECTION_TIMEOUT = 10 * 1000;

    // how long do we wait between two "pings"
    public static final int PING_INTERVAL = 10 * 1000;

    public static final int PING_COUNT = 4;

    private static final int CONNECTION_BACKLOG = 10;

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket serverSocket;

    private boolean ended;

    // private final Node node;

    private Coordinates coordinates;

    public VivaldiClient(Properties properties) throws IOException {
        // this.node = node;
        this.coordinates = new Coordinates();

        String clientID = properties.getProperty(Ibis.ID_PROPERTY);
        Client client = Client.getOrCreateClient(clientID, properties, 0);
        this.virtualSocketFactory = client.getFactory();

        serverSocket = virtualSocketFactory.createServerSocket(
                Protocol.VIRTUAL_PORT, CONNECTION_BACKLOG, null);

        ThreadPool.createNew(this, "Management Client");
    }

    public double ping(IbisIdentifier identifier, boolean updateCoordinates)
            throws IOException {
        double result = Double.MAX_VALUE;

        Connection connection = new Connection(identifier, CONNECTION_TIMEOUT,
                true, virtualSocketFactory, Protocol.VIRTUAL_PORT);

        InputStream in = connection.in();
        OutputStream out = connection.out();

        // get coordinates from peer
        byte[] coordinateBytes = new byte[Coordinates.SIZE];
        int remaining = Coordinates.SIZE;
        int offset = 0;
        while (remaining > 0) {
            int read = in.read(coordinateBytes, offset, remaining);
            if (read == -1) {
                throw new IOException("could not read Coordinates");
            }
            offset += read;
            remaining -= read;
        }
        Coordinates remoteCoordinates = new Coordinates(coordinateBytes);

        for (int i = 0; i < PING_COUNT; i++) {
            long start = System.nanoTime();
            out.write(i);
            out.flush();
            int reply = in.read();
            long end = System.nanoTime();
            if (reply != i) {
                throw new IOException("ping failed, wrong reply: " + reply);
            }

            long time = end - start;
            double rtt = (double) time / 1000000.0;

            if (rtt < result) {
                result = rtt;
            }
        }
        connection.close();

        if (updateCoordinates) {
            updateCoordinates(remoteCoordinates, result);
        }

        logger.debug("distance to " + identifier + " is " + result + " ms");

        return result;
    }

    public void handleConnection(Connection connection) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            DataOutputStream out = connection.out();
            DataInputStream in = connection.in();
            // send coordinates
            out.write(getCoordinates().toBytes());
            out.flush();

            for (int i = 0; i < PING_COUNT; i++) {
                int read = in.read();
                out.write(read);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("error on handling ping", e);
        }
        connection.close();
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    }

    public void start() {
        ThreadPool.createNew(this, "vivaldi");
        logger.info("Started Vivaldi service");
    }

    private synchronized void updateCoordinates(Coordinates remoteCoordinates,
            double rtt) {
        coordinates = coordinates.update(remoteCoordinates, rtt);
    }

    public synchronized Coordinates getCoordinates() {
        return coordinates;
    }

    private synchronized boolean ended() {
        return ended;
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

        try {
            virtualSocketFactory.end();
        } catch (Exception e) {
            // IGNORE
        }
    }

    public void run() {
        // Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        // while (!ended()) {
        // NodeInfo neighbour = node.clusterService().getRandomNeighbour();
        // if (neighbour != null) {
        // try {
        // ping(neighbour, true);
        // } catch (Exception e) {
        // logger.debug("error on pinging neighbour " + neighbour, e);
        //
        // }
        // }
        //
        // NodeInfo randomNode = node.gossipService().getRandomNode();
        // if (randomNode != null) {
        // try {
        // ping(randomNode, true);
        // } catch (Exception e) {
        // logger.debug("error on pinging random node " + randomNode,
        // e);
        //
        // }
        // }
        //
        // try {
        // Thread.sleep(PING_INTERVAL);
        // } catch (InterruptedException e) {
        // // IGNORE
        // }
        // }

    }

}
