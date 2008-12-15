package ibis.server;

import ibis.util.ThreadPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client to a server started with the "--remote" option. This client can parse
 * the address of the server from the standard out of the server process. Also
 * forwards the output to the given stream (with an optional prefix to each
 * line)
 * 
 * @author Niels Drost
 * 
 */
public class RemoteClient implements Runnable {

    private static final Logger logger = LoggerFactory
            .getLogger(RemoteClient.class);

    // standard out of server process. Turns into an input stream on this side
    private final BufferedReader stdout;

    // standard in of server process. We can write to it using an output stream
    private final OutputStream stdin;

    // stream to forward output from server to
    private final PrintStream output;

    private final String outputPrefix;

    private String address = null;

    private IOException exception = null;

    /**
     * Connect to the server with the given in and output stream
     * 
     * @param stdout
     *            Standard out of server process
     * @param stdin
     *            Standard in of server process
     * @param output
     *            Stream to forward output to
     * @param outputPrefix
     *            Prefix to add to all lines of output
     * 
     * @throws IOException
     */
    public RemoteClient(InputStream stdout, OutputStream stdin,
            PrintStream output, String outputPrefix) throws IOException {
        this.stdout = new BufferedReader(new InputStreamReader(stdout));
        this.stdin = stdin;
        this.output = output;

        this.outputPrefix = outputPrefix;

        ThreadPool.createNew(this, "remote client");
    }

    /**
     * Returns the address of the server as a string.
     * 
     * @param timeout
     *            time to wait until address is available
     * 
     * @return the address of the server.
     * 
     * @throws IOException
     *             if server fails to start, or address is not available within
     *             the specified time.
     */
    public synchronized String getAddress(long timeout) throws IOException {
        long deadline = System.currentTimeMillis() + timeout;
        while (address == null) {
            if (exception != null) {
                throw exception;
            }

            long waitTime = deadline - System.currentTimeMillis();

            if (waitTime > 0) {
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    // IGNORE
                }
            } else {
                throw new IOException("server did not produce address in time");
            }
        }

        return address;
    }

    /**
     * End Server by closing stream.
     */
    public void end() {
        try {
            stdin.close();
        } catch (IOException e) {
            // IGNORE
        }
    }

    private synchronized void setException(IOException exception) {
        this.exception = exception;
        notifyAll();
    }

    private synchronized void parseAddress(String line) {
        logger.debug("Parsing address from line: \"" + line + "\"");

        int prefixIndex = line.lastIndexOf(Server.ADDRESS_LINE_PREFIX);
        int postfixIndex = line.indexOf(Server.ADDRESS_LINE_POSTFIX);

        if (prefixIndex == -1 || postfixIndex == -1) {
            // address not in this line after all, print line to output
            logger.warn("Address prefix+postfix not found in line \"" + line + "\"");
            output.println(line);
            return;
        }
        
        if ((prefixIndex + Server.ADDRESS_LINE_PREFIX.length()) >= postfixIndex) {
            logger.warn("Invalid address in line \"" + line + "\"");
        }

        try {
            this.address = line.substring(prefixIndex
                    + Server.ADDRESS_LINE_PREFIX.length(), postfixIndex);
        } catch (IndexOutOfBoundsException e) {
            logger.warn("Invalid address in line \"" + line + "\"");
            return;
        }

        notifyAll();
    }

    /**
     * Forwards standard out of server to given output stream. Filters out line
     * containing server address.
     */
    public void run() {
        String address = null;
        while (true) {
            try {
                String line = stdout.readLine();

                if (line == null) {
                    setException(new IOException("server terminated"));
                    return;
                }

                if (address == null
                        && line.contains(Server.ADDRESS_LINE_PREFIX)) {
                    parseAddress(line);
                } else {
                    output.println(outputPrefix + line);
                }
            } catch (IOException e) {
                setException(e);
                return;
            }
        }

    }
}
