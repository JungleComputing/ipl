package ibis.server.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class RemoteClient {

    private static final Logger logger = Logger.getLogger(RemoteClient.class);

    private final BufferedReader in;

    private final PrintStream out;

    private String serverAddress = null;

    /**
     * Connect to the server with the given in and output stream
     * 
     * @throws IOException
     */
    public RemoteClient(InputStream in, OutputStream out) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new PrintStream(out);

    }

    private void sendCommand(String... command) {
        String line = Protocol.CLIENT_COMMAND;

        //skip appending space for first argument
        if (command.length > 0) {
            line += command[0];
        }
        
        for (int i = 1; i < command.length; i++) {
            line += " " + command[i];
        }

        logger.debug("sending command: \"" + line + "\""); 
        
        out.println(line);
        out.flush();
    }

    private String[] readReply() throws IOException {
        String line = in.readLine();
        
        logger.debug("read reply: \"" + line + "\""); 

        if (line == null) {
            throw new IOException("end of stream while reading reply");
        }

        if (!line.startsWith(Protocol.SERVER_REPLY)) {
            throw new IOException("expected: " + Protocol.SERVER_REPLY
                    + ", got: " + line);
        }

        return line.substring(Protocol.SERVER_REPLY.length()).split(" ");
    }

    public synchronized void addHubs(String... hubs) throws IOException {
        for (String hub : hubs) {
            addHub(hub);
        }
    }

    /**
     * Tell the server about a hub
     */
    public synchronized void addHub(String hub) throws IOException {
        sendCommand(Protocol.OPCODE_ADD_HUB, hub);

        // no data send in reply, just an ack
        readReply();
    }

    /**
     * Returns the addresses of all hubs known to this server
     */
    public synchronized String[] getHubs() throws IOException {
        sendCommand(Protocol.OPCODE_GET_HUBS);

        String[] result = readReply();

        return result;
    }

    /**
     * Returns the local address of the server as a string
     */
    public synchronized String getLocalAddress() throws IOException {
        if (serverAddress == null) {

            sendCommand(Protocol.OPCODE_GET_LOCAL_ADDRESS);

            String[] reply = readReply();

            if (reply.length != 1) {
                String message = "expecting single string reply, got: ";
                for (String element : reply) {
                    message += " \"" + element + "\"";
                }
                throw new IOException(message);
            }

            serverAddress = reply[0];

        }

        return serverAddress;
    }

    /**
     * Returns the names of all services currently in this server
     * 
     * @throws IOException
     *             in case of trouble
     */
    public synchronized String[] getServiceNames() throws IOException {
        sendCommand(Protocol.OPCODE_GET_SERVICE_NAMES);

        String[] result = readReply();

        return result;
    }

    /**
     * Function to retrieve statistics for a given service
     * 
     * @param serviceName
     *            Name of service to get statistics of
     * 
     * @return statistics for given service, or null if service exist.
     * @throws IOException
     *             in case of trouble.
     */
    public synchronized Map<String, String> getStats(String serviceName)
            throws IOException {
        sendCommand(Protocol.OPCODE_GET_STATISTICS, serviceName);

        String[] reply = readReply();

        Map<String, String> result = new HashMap<String, String>();
        // while there are _two_ string remaining
        for (int i = 0; i + 1 < reply.length; i += 2) {
            String key = reply[i];
            String value = reply[i + 1];
            if (value.equals("null")) {
                value = null;
            }

            result.put(key, value);
        }

        return result;
    }

    public synchronized void end(long timeout) throws IOException {
        sendCommand(Protocol.OPCODE_END, Long.toString(timeout));

        try {
            readReply();
        } catch (IOException e) {
            logger.debug("error on ending server", e);
        }

        in.close();
        out.close();
    }
}
