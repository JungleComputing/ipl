package ibis.ipl.support.management;

import ibis.io.Conversion;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.support.Client;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagementClient implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(ManagementClient.class);

    private static final int CONNECTION_BACKLOG = 10;

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket serverSocket;

    private final Ibis ibis;

    private boolean ended;

    public ManagementClient(Properties properties, Ibis ibis)
            throws IOException {
        this.ibis = ibis;
        String clientID = properties.getProperty(Ibis.ID_PROPERTY);
        Client client = Client.getOrCreateClient(clientID, properties, 0);
        this.virtualSocketFactory = client.getFactory();

        serverSocket = virtualSocketFactory.createServerSocket(
                Protocol.VIRTUAL_PORT, CONNECTION_BACKLOG, null);

        ThreadPool.createNew(this, "Management Client");
    }

    private Object getIbisAttribute(String name) throws NoSuchPropertyException {
        if (name.equalsIgnoreCase("vivaldi")) {
            return ibis.getVivaldiCoordinates();
        }
        if (name.equalsIgnoreCase("connections")) {
            return ibis.connectedTo();
        }
        if (name.equalsIgnoreCase("outgoingMessageCount")) {
            return ibis.getOutgoingMessageCount();
        }
        if (name.equalsIgnoreCase("bytesWritten")) {
            return ibis.getBytesWritten();
        }
        if (name.equalsIgnoreCase("bytesSent")) {
            return ibis.getBytesSent();
        }
        if (name.equalsIgnoreCase("incomingMessageCount")) {
            return ibis.getIncomingMessageCount();
        }
        if (name.equalsIgnoreCase("bytesRead")) {
            return ibis.getBytesRead();
        }
        if (name.equals("sentBytesPerIbis")) {
            return ibis.getSentBytesPerIbis();
        }
        if (name.equals("receivedBytesPerIbis")) {
            return ibis.getReceivedBytesPerIbis();
        }
        if (name.equals("wonElections")) {
            return ibis.wonElections();
        }
        if (name.equals("senderConnectionTypes")) {
            return ibis.getSenderConnectionTypes();
        }
        if (name.equals("receiverConnectionTypes")) {
            return ibis.getReceiverConnectionTypes();
        }
        return ibis.getManagementProperty(name);
    }

    private void handleGetMonitorInfo(Connection connection) throws IOException {
        int length = connection.in().readInt();

        if (length < 0) {
            connection.closeWithError("End of stream on reading request");
            return;
        }

        AttributeDescription[] descriptions = new AttributeDescription[length];

        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = new AttributeDescription(connection.in()
                    .readUTF(), connection.in().readUTF());
        }

        Object[] result = new Object[descriptions.length];

        try {
            Class<?> factoryClass = Class
                    .forName("java.lang.management.ManagementFactory");

            Class<?> beanServerClass = Class
                    .forName("javax.management.MBeanServer");

            Class<?> objectNameClass = Class
                    .forName("javax.management.ObjectName");

            Constructor<?> objectNameClassConstructor = objectNameClass
                    .getConstructor(String.class);

            Method getAttributeMethod = beanServerClass.getMethod(
                    "getAttribute", objectNameClass, String.class);

            Object beanServer = factoryClass
                    .getMethod("getPlatformMBeanServer").invoke(null);

            for (int i = 0; i < descriptions.length; i++) {
                if (descriptions[i].getBeanName().equals("ibis")) {
                    result[i] = getIbisAttribute(descriptions[i].getAttribute());
                } else {
                    try {
                        Object objectName = objectNameClassConstructor
                                .newInstance(descriptions[i].getBeanName());

                        result[i] = getAttributeMethod.invoke(beanServer,
                                objectName, descriptions[i].getAttribute());
                    } catch (Throwable t) {
                        connection
                                .closeWithError("cannot get value for attribute \""
                                        + descriptions[i].getAttribute()
                                        + "\" of bean \""
                                        + descriptions[i].getBeanName() + "\"");
                    }
                }
            }

        } catch (Throwable t) {
            connection.closeWithError("Cannot load JMX: " + t);
            return;
        }

        connection.sendOKReply();

        byte[] bytes = Conversion.object2byte(result);
        connection.out().writeInt(bytes.length);
        connection.out().write(bytes);
        connection.out().flush();
        connection.close();
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
                case Protocol.OPCODE_GET_MONITOR_INFO:
                    handleGetMonitorInfo(connection);
                    break;
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
