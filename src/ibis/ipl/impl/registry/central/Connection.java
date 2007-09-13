package ibis.ipl.impl.registry.central;

import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.log4j.Logger;

public final class Connection {

    private static final int INITIAL_MAX_WAIT = 1000;

    private static final Logger logger = Logger.getLogger(Connection.class);

    private final VirtualSocket virtualSocket;

    private final Socket plainSocket;

    private final DataOutputStream out;

    private final DataInputStream in;

    Connection(VirtualSocketAddress address, VirtualSocketFactory factory,
            int timeout, boolean fillTimeout) throws IOException {
        logger.debug("connecting to " + address + ", timeout = " + timeout
                + " , filltimeout = " + fillTimeout);
        plainSocket = null;
        VirtualSocket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        socket = factory.createClientSocket(address, timeout, fillTimeout,
                new HashMap<String, Object>());
        logger.debug("connection created, sending opcode");
        socket.setTcpNoDelay(true);

        out = new DataOutputStream(new BufferedOutputStream(socket
                .getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket
                .getInputStream()));

        this.virtualSocket = socket;
        this.out = out;
        this.in = in;

        logger.debug("connection to " + address + " established");

    }

    Connection(InetSocketAddress address, int timeout) throws IOException {
        virtualSocket = null;
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        long hardDeadline = System.currentTimeMillis() + timeout;

        // int maxWait = (int) (timeout / Math.pow(2, (MAX_TRIES - 1)));
        int maxWait = INITIAL_MAX_WAIT;

        int tries = 0;
        boolean success = false;
        while (!success) {
            // int currentTimeout = (int) (Math.random() * maxWait);
            int currentTimeout = (int) (maxWait / 2 + Math.random()
                    * (maxWait / 2));
            long deadline = System.currentTimeMillis() + currentTimeout;
            if (deadline > hardDeadline) {
                currentTimeout = (int) (hardDeadline - System
                        .currentTimeMillis());
                logger.debug("last attempt, timeout = " + currentTimeout);
                deadline = hardDeadline;
            }

            try {
                socket = new Socket();
                socket.connect(address, timeout);
                socket.setTcpNoDelay(true);

                out = new DataOutputStream(new BufferedOutputStream(socket
                        .getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(socket
                        .getInputStream()));

                success = true;
            } catch (IOException e) {

                long currentTime = System.currentTimeMillis();

                if (currentTime >= hardDeadline) {
                    throw e;
                }

                long sleepTime = deadline - currentTime;

                logger.debug("failed to connect to " + address, e);

                logger.debug("maxWait = " + maxWait);

                logger.debug("failure, waiting " + sleepTime + " milliseconds");

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e2) {
                        // IGNORE
                    }
                }

                tries++;
                maxWait = maxWait * 2;
            }

        }

        this.plainSocket = socket;
        this.out = out;
        this.in = in;

    }

    Connection(VirtualServerSocket serverSocket) throws IOException {
        plainSocket = null;
        logger.debug("waiting for incomming connection...");
        virtualSocket = serverSocket.accept();
        virtualSocket.setTcpNoDelay(true);

        in = new DataInputStream(new BufferedInputStream(virtualSocket
                .getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(virtualSocket
                .getOutputStream()));
        logger.debug("new connection from "
                + virtualSocket.getRemoteSocketAddress() + " accepted");
    }

    Connection(ServerSocket plainServerSocket) throws IOException {
        virtualSocket = null;
        logger.debug("waiting for incomming connection...");
        plainSocket = plainServerSocket.accept();
        plainSocket.setTcpNoDelay(true);

        in = new DataInputStream(new BufferedInputStream(plainSocket
                .getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(plainSocket
                .getOutputStream()));

    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public void getAndCheckReply() throws IOException {
        // flush output, just in case...
        out.flush();

        // get reply
        byte reply = in.readByte();
        if (reply == Protocol.REPLY_ERROR) {
            close();
            throw new IOException("ERROR: " + in.readUTF());
        } else if (reply != Protocol.REPLY_OK) {
            close();
            throw new IOException("Unknown reply (" + reply + ")");
        }

    }

    public void sendOKReply() throws IOException {
        out.writeByte(Protocol.REPLY_OK);
    }

    public void closeWithError(String message) {
        if (message == null) {
            message = "";
        }
        try {
            out.writeByte(Protocol.REPLY_ERROR);
            out.writeUTF(message);

            close();
        } catch (IOException e) {
            // IGNORE
        }
    }

    public void close() {
        try {
            out.flush();
        } catch (IOException e) {
            logger.error("Got exception in flush", e);
            // IGNORE
        }

        if (virtualSocket != null) {
            try {
                virtualSocket.close();
            } catch (IOException e) {
                logger.error("Got exception in close", e);
                // IGNORE
            }
        }

        if (plainSocket != null) {
            try {
                plainSocket.close();
            } catch (IOException e) {
                logger.error("Got exception in close", e);
                // IGNORE
            }
        }
    }

}
