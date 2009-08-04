package ibis.ipl.impl.bt;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Provides a factory that can either produce smartsockets sockets or ordinary
 * sockets.
 */
class IbisSocketFactory {

    IbisSocketFactory(TypedProperties props) throws IbisConfigurationException, IOException {
        BtIbis.logger.info("Using plain TcpIbis");
    }

    void setIdent(IbisIdentifier id) {
    }

    IbisServerSocket createServerSocket(int port, int backlog, boolean retry,
            Properties properties) throws IOException {
            
    	IbisServerSocket ss = new IbisServerSocket();
    	ss.bind();
        return ss;
    }

    IbisSocket createClientSocket(IbisSocketAddress addr, int timeout,
            boolean fillTimeout, Map<String, String> properties)
            throws IOException {

    	IbisSocket s = new IbisSocket();

        s.connect(addr);
        return s;
    }

    void printStatistics(String s) {
    }
}
