package ibis.ipl.server;

import ibis.ipl.management.ManagementService;
import ibis.smartsockets.direct.DirectSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection to a server in another JVM (possibly on another machine).
 * Connection can be established either by connection stdin/stdout to a server
 * started with the "--remote" option, or by passing the address of the server.
 * 
 * @author Niels Drost
 * 
 */
public class ServerConnection implements ServerInterface {

    private static final Logger logger = LoggerFactory
            .getLogger(ServerConnection.class);

    private final ServerPipe pipe;

    private final String address;

    /**
     * Connect to the server with the given in and output stream. Will parse the
     * address of the server from the standard out of the server process. Also
     * forwards the output to the given stream (with an optional prefix to each
     * line). When this connection is terminated, the (remote) server terminates
     * as well.
     * 
     * @param stdout
     *            Standard out of server process
     * @param stdin
     *            Standard in of server process
     * @param output
     *            Stream to forward output to
     * @param outputPrefix
     *            Prefix to add to all lines of output
     * @param timeout
     *            Number of milliseconds to wait for the server to become
     *            available
     * 
     * @throws IOException
     */
    public ServerConnection(InputStream stdout, OutputStream stdin,
            PrintStream output, String outputPrefix, long timeout)
            throws IOException {

        pipe = new ServerPipe(stdout, stdin, output, outputPrefix);
        address = pipe.getAddress(timeout);
    }

    /**
     * Connections to the server at the given address
     * 
     * @param address
     */
    public ServerConnection(String address) {
        pipe = null;
        this.address = address;
    }

    @Override
    public void addHubs(DirectSocketAddress... hubAddresses) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addHubs(String... hubAddresses) {
        // TODO Auto-generated method stub

    }

    @Override
    public void end(long timeout) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getHubs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ManagementService getManagementService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RegistryServiceInterface getRegistryService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getServiceNames() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Closes connection to the server. Will also terminate server if
     * stdin/stdout connection is used
     */
    public void closeConnection() {
        if (pipe != null) {
            pipe.end();
        }
    }

}
