package ibis.impl.tcp;

import ibis.impl.IbisIdentifier;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;

import smartsockets.hub.servicelink.ServiceLink;
import smartsockets.virtual.InitializationException;
import smartsockets.virtual.VirtualSocketFactory;

/**
 * Provides a factory that can either produce smartsockets sockets or
 * ordinary sockets.
 */
class IbisSocketFactory {

    private final VirtualSocketFactory factory;

    IbisSocketFactory(TypedProperties props) throws IOException {
        boolean useSmartsockets = props.booleanProperty(
                "ibis.impl.tcp.smartsockets", false);
        if (useSmartsockets) {
            TcpIbis.logger.info("Using smartsockets TcpIbis");
            try {
                factory = VirtualSocketFactory.getDefaultSocketFactory();
            } catch(InitializationException e) {
                throw new IOException("Failed to create socket factory");
            }
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
                    sl.registerProperty("ibis", id.toString());
                }
            } catch(Throwable e) {
                // ignored
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
            InetSocketAddress local = new InetSocketAddress(
                    IPUtils.getLocalHostAddress(), port);
            server.bind(local, backlog);
            return new IbisServerSocket(server);
        }
    }

    IbisSocket createClientSocket(IbisSocketAddress addr, int timeout,
            Map<String, Object> properties) throws IOException {
        if (factory != null) {
            return new IbisSocket(factory.createClientSocket(
                        addr.virtualAddress, timeout, properties));
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
