package ibis.ipl.registry;

import ibis.ipl.impl.IbisIdentifier;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Connection {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    private final VirtualSocket socket;

    private final DataOutputStream out;

    private final DataInputStream in;
    private final CountInputStream counter;

    static final byte REPLY_ERROR = 2;

    static final byte REPLY_OK = 1;

    public Connection(IbisIdentifier ibis, int timeout, boolean fillTimeout,
            VirtualSocketFactory factory) throws IOException {
        this(VirtualSocketAddress.fromBytes(ibis.getRegistryData(), 0),
                timeout, fillTimeout, factory);
    }

    public Connection(VirtualSocketAddress address, int timeout,
            boolean fillTimeout, VirtualSocketFactory factory)
            throws IOException {
        logger.debug("connecting to " + address + ", timeout = " + timeout
                + " , filltimeout = " + fillTimeout);
        
        final HashMap<String, Object> lightConnection 
        = new HashMap<String, Object>();
        //lightConnection.put("connect.module.allow", "ConnectModule(HubRouted)");    

        socket = factory.createClientSocket(address, timeout, fillTimeout,
                lightConnection);
        socket.setTcpNoDelay(true);

        out = new DataOutputStream(new BufferedOutputStream(socket
                .getOutputStream()));
        counter = new CountInputStream(new BufferedInputStream(socket
                .getInputStream()));
        in = new DataInputStream(counter);

        logger.debug("connection to " + address + " established");

    }

    /**
     * Accept incoming connection on given serverSocket.
     */
    public Connection(VirtualServerSocket serverSocket) throws IOException {
        logger.debug("waiting for incomming connection...");
        socket = serverSocket.accept();
        socket.setTcpNoDelay(true);

        counter = new CountInputStream(new BufferedInputStream(socket
                .getInputStream()));
        in = new DataInputStream(counter);
        out = new DataOutputStream(new BufferedOutputStream(socket
                .getOutputStream()));
        logger.debug("new connection from " + socket.getRemoteSocketAddress()
                + " accepted");
    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public int written() {
        return out.size();
    }

    public int read() {
        return counter.getCount();
    }

    public void getAndCheckReply() throws IOException {
        // flush output, just in case...
        out.flush();

        // get reply
        byte reply = in.readByte();
        if (reply == Connection.REPLY_ERROR) {
        	String message = in.readUTF();
            close();
            throw new RemoteException(message);
        } else if (reply != Connection.REPLY_OK) {
            close();
            throw new IOException("Unknown reply (" + reply + ")");
        }
    }

    public void sendOKReply() throws IOException {
        out.writeByte(Connection.REPLY_OK);
        out.flush();
    }

    public void closeWithError(String message) {
        if (message == null) {
            message = "";
        }
        try {
            out.writeByte(Connection.REPLY_ERROR);
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

        try {
            out.close();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            in.close();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            socket.close();
        } catch (IOException e) {
            // IGNORE
        }
    }

}
