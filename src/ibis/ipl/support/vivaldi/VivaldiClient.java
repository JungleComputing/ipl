package ibis.ipl.support.vivaldi;

import ibis.ipl.impl.Ibis;
import ibis.ipl.support.Client;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VivaldiClient implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(VivaldiClient.class);

    private static final int CONNECTION_BACKLOG = 10;

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket serverSocket;

    private boolean ended;

    public VivaldiClient(Properties properties) throws IOException {
        String clientID = properties.getProperty(Ibis.ID_PROPERTY);
        Client client = Client.getOrCreateClient(clientID, properties, 0);
        this.virtualSocketFactory = client.getFactory();

        serverSocket = virtualSocketFactory.createServerSocket(
                Protocol.VIRTUAL_PORT, CONNECTION_BACKLOG, null);

        ThreadPool.createNew(this, "Management Client");
    }

    private synchronized boolean ended() {
        return ended;
    }

    public void run() {
        Connection connection = null;

        while (!ended()) {
            try {
                logger.debug("accepting connection");
                connection = new Connection(serverSocket);
                logger.debug("connection accepted");
            } catch (IOException e) {
                if (ended) {
                    return;
                } else {
                    logger.error("Accept failed, waiting a second, will retry",
                            e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // IGNORE
                    }
                }
            }

            try {
                byte magic = connection.in().readByte();

                if (magic != Protocol.MAGIC_BYTE) {
                    throw new IOException(
                            "Invalid header byte in accepting connection");
                }

                byte opcode = connection.in().readByte();

                if (opcode < Protocol.NR_OF_OPCODES) {
                    logger.debug("received request: "
                            + Protocol.OPCODE_NAMES[opcode]);
                }

                switch (opcode) {
                // case Protocol.OPCODE_GET_MONITOR_INFO:
                // handleGetMonitorInfo(connection);
                // break;
                default:
                    logger.error("unknown opcode in request: " + opcode);
                }
                logger.debug("done handling request");
            } catch (Throwable e) {
                logger.error("error on handling request", e);
            } finally {
                connection.close();
            }
        }
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

}
