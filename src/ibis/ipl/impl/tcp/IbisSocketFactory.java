package ibis.ipl.impl.tcp;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.support.Client;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a factory that can either produce smartsockets sockets or ordinary
 * sockets.
 */
class IbisSocketFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(IbisSocketFactory.class);

    private final VirtualSocketFactory factory;

    IbisSocketFactory(TypedProperties properties)
            throws IbisConfigurationException, IOException {
        boolean useSmartsockets = properties.getBooleanProperty(
                "ibis.ipl.impl.tcp.smartsockets", true);
        if (useSmartsockets) {
            TcpIbis.logger.info("Using smartsockets TcpIbis");
            String clientID = properties.getProperty(Ibis.ID_PROPERTY);
            Client client = Client.getOrCreateClient(clientID, properties, 0);
            factory = client.getFactory();
        } else {
            TcpIbis.logger.info("Using plain TcpIbis");
            factory = null;
        }
    }

    void setIdent(IbisIdentifier id) {
        if (factory != null) {
            try {
                ServiceLink sl = factory.getServiceLink();
                if (sl != null) {
                    sl.registerProperty("smartsockets.viz", "I^" + id.name()
                            + "," + id.location().toString());
                    // sl.registerProperty("ibis", id.toString());
                } else {
                    logger
                            .warn("could not set smartsockets viz property: could not get smartsockets service link");
                }
            } catch (Throwable e) {
                logger.warn("could not set smartsockets viz property");
            }
        }
    }

    IbisServerSocket createServerSocket(int port, int backlog, boolean retry,
            Properties properties) throws IOException {
        if (factory != null) {
            return new IbisServerSocket(factory.createServerSocket(port,
                    backlog, retry, properties));
        } else {
            ServerSocket server = new ServerSocket();
            InetSocketAddress local = new InetSocketAddress(IPUtils
                    .getLocalHostAddress(), port);
            server.bind(local, backlog);
            return new IbisServerSocket(server);
        }
    }

    IbisSocket createClientSocket(IbisSocketAddress addr, int timeout,
            boolean fillTimeout, Map<String, String> properties)
            throws IOException {

        if (factory != null) {
            HashMap<String, Object> h = new HashMap<String, Object>();
            if (properties != null) {
                h.putAll(properties);
            }
            return new IbisSocket(factory.createClientSocket(
                    addr.virtualAddress, timeout, fillTimeout, h));
        }
        Socket s = new Socket();

        s.connect(addr.address, timeout);
        return new IbisSocket(s);
    }

    void printStatistics(String s) {
        if (factory != null) {
            factory.printStatistics(s);
        }
    }
}
