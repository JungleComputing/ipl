package ibis.ipl.impl.registry;


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

import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

public class Connection {

    // private static final int MAX_TRIES = 10;
    
    private static final int INITIAL_MAX_WAIT = 1000;
    
    private static final Logger logger = Logger.getLogger(Connection.class);

    private final VirtualSocket virtualSocket;

    private final Socket plainSocket;

    private final DataOutputStream out;

    private final DataInputStream in;

    private final byte opcode;

    Connection(VirtualSocketAddress address, VirtualSocketFactory factory,
            byte opcode, long timeout, boolean printWarning) throws IOException {
        long hardDeadline = System.currentTimeMillis() + timeout;
        plainSocket = null;
        VirtualSocket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        this.opcode = opcode;

        // int maxWait = (int) (timeout / Math.pow(2, (MAX_TRIES - 1)));
        int maxWait = INITIAL_MAX_WAIT;

        int tries = 0;
        boolean success = false;
        while (!success) {
            int currentTimeout = (int) (Math.random() * maxWait);
            long deadline = System.currentTimeMillis() + currentTimeout;
            if (deadline > hardDeadline) {
                currentTimeout = (int) (hardDeadline - System
                        .currentTimeMillis());
                deadline = hardDeadline;
                logger.debug("last attempt, timeout = " + currentTimeout);
            }

            try {
                socket = factory.createClientSocket(address, currentTimeout,
                        new HashMap<String, Object>());

                out = new DataOutputStream(new BufferedOutputStream(socket
                        .getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(socket
                        .getInputStream()));

                // write opcode
                out.writeByte(opcode);
                out.flush();

                success = true;
            } catch (IOException e) {
                // failure: wait some time before trying again...
                if (printWarning && tries == 0) {
                    logger.warn("Registry: failed to connect to " + address
                            + ", will keep trying");
                }

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

        this.virtualSocket = socket;
        this.out = out;
        this.in = in;

    }

    public Connection(InetSocketAddress address, byte opcode, int timeout,
            boolean printWarning) throws IOException {
        virtualSocket = null;
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        this.opcode = opcode;

        long hardDeadline = System.currentTimeMillis() + timeout;

        // int maxWait = (int) (timeout / Math.pow(2, (MAX_TRIES - 1)));
        int maxWait = INITIAL_MAX_WAIT;


        int tries = 0;
        boolean success = false;
        while (!success) {
            int currentTimeout = (int) (Math.random() * maxWait);
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

                out = new DataOutputStream(new BufferedOutputStream(socket
                        .getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(socket
                        .getInputStream()));

                // write opcode
                out.writeByte(opcode);
                out.flush();

                success = true;
            } catch (IOException e) {
                // failure: wait some time before trying again...
                if (printWarning && tries == 0) {
                    logger.warn("Registry: failed to connect to " + address
                            + ", will keep trying");
                }

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

        in = new DataInputStream(new BufferedInputStream(virtualSocket
                .getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(virtualSocket
                .getOutputStream()));

        opcode = in.readByte();
    }

    public Connection(ServerSocket plainServerSocket) throws IOException {
        virtualSocket = null;
        logger.debug("waiting for incomming connection...");
        plainSocket = plainServerSocket.accept();

        in = new DataInputStream(new BufferedInputStream(plainSocket
                .getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(plainSocket
                .getOutputStream()));

        opcode = in.readByte();

    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public byte getOpcode() {
        return opcode;
    }

    public void getAndCheckReply() throws IOException {
        // flush output, just in case...
        out.flush();

        // get reply
        byte reply = in.readByte();
        if (reply == Protocol.REPLY_ERROR) {
            close();
            throw new IOException(Protocol.opcodeString(opcode) + " ERROR: "
                    + in.readUTF());
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
            // IGNORE
        }

        if (virtualSocket != null) {
            try {
                virtualSocket.close();
            } catch (IOException e) {
                // IGNORE
            }
        }

        if (plainSocket != null) {
            try {
                plainSocket.close();
            } catch (IOException e) {
                // IGNORE
            }
        }
    }

}
