package ibis.server.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.server.Server;

public class RemoteHandler implements Runnable {

    private final Server server;

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private void sendReply(String... arguments) {
        String line = Protocol.SERVER_REPLY;

        // skip appending space for first argument
        if (arguments.length > 0) {
            line += arguments[0];
        }

        for (int i = 1; i < arguments.length; i++) {
            line += " " + arguments[i];
        }

        // System.err.println("sending reply: " + line);

        System.out.println(line);
        System.out.flush();
    }

    private String[] readCommand() throws IOException {
        String line = in.readLine();

        // System.err.println("got command: " + line);

        if (line == null) {
            // end of stream, stop server
            return null;
        }

        if (!line.startsWith(Protocol.CLIENT_COMMAND)) {
            throw new IOException("expected: " + Protocol.CLIENT_COMMAND
                    + ", got: " + line);
        }

        return line.substring(Protocol.CLIENT_COMMAND.length()).split(" ");
    }

    public RemoteHandler(Server server) {
        this.server = server;
    }

    private void handleGetLocalAddress() throws IOException {
        sendReply(server.getLocalAddress());
    }

    private void handleAddHub(String[] command) throws IOException {
        if (command.length < 2) {
            System.out.println("hub not given");
            return;
        }

        String hub = command[1];

        server.addHubs(hub);

        sendReply(new String[0]);
    }

    private void handleGetHubs() throws IOException {
        String[] hubs = server.getHubs();

        sendReply(hubs);
    }

    private void handleGetServiceNames() throws IOException {
        String[] services = server.getServiceNames();

        sendReply(services);
    }

    private void handleGetStatistics(String[] command) throws IOException {
        if (command.length < 2) {
            System.out.println("service name not given");
            return;
        }

        String serviceName = command[1];

        Map<String, String> statistics = server.getStats(serviceName);

        if (statistics == null) {
            System.out.println("Could not find service: " + serviceName);
            return;
        }

        ArrayList<String> reply = new ArrayList<String>();

        for (Map.Entry<String, String> entry : statistics.entrySet()) {
            reply.add(entry.getKey());
            if (entry.getValue() == null) {
                reply.add("null");
            } else {
                reply.add(entry.getValue());
            }
        }

        sendReply(reply.toArray(new String[0]));
    }

    private void handleEnd(String[] command) throws IOException {
        if (command.length < 2) {
            System.out.println("timeout not given");
            return;
        }

        String timeoutString = command[1];

        try {
            long timeout = Long.parseLong(timeoutString);

            server.end(timeout);

            sendReply(new String[0]);
        } catch (NumberFormatException e) {
            System.out.println("could not parse timeout");
            throw new IOException("error parsing long: " + e);
        }
    }

    public void run() {
        System.err.println("starting remote handler");

        while (true) {
            try {
                String[] command = readCommand();

                if (command == null) {
                    System.err.println("input stream closed, stopping server");
                    server.end(-1);
                    return;
                }

                if (command.length == 0) {
                    System.out.println("command not given");
                } else {
                    String opcode = command[0];

                    if (opcode.equals(Protocol.OPCODE_GET_LOCAL_ADDRESS)) {
                        handleGetLocalAddress();
                    } else if (opcode.equals(Protocol.OPCODE_ADD_HUB)) {
                        handleAddHub(command);
                    } else if (opcode.equals(Protocol.OPCODE_GET_HUBS)) {
                        handleGetHubs();
                    } else if (opcode.equals(Protocol.OPCODE_GET_SERVICE_NAMES)) {
                        handleGetServiceNames();
                    } else if (opcode.equals(Protocol.OPCODE_GET_STATISTICS)) {
                        handleGetStatistics(command);
                    } else if (opcode.equals(Protocol.OPCODE_END)) {
                        handleEnd(command);
                        return;
                    } else {
                        System.err.println("unknown command: " + command[1]);
                    }
                }
            } catch (Exception e) {
                System.err
                        .println("error on handling remote request (ignoring)");
                e.printStackTrace(System.err);
                System.out.println("error: " + e.getMessage());
            }
        }
    }
}
