/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.support;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.Conversion;
import ibis.io.IOProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

/**
 * Connection to the server and between clients using smartsockets.
 *
 * @author ndrost
 *
 */
public final class Connection {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    private final VirtualSocket socket;

    private final DataOutputStream out;

    private final DataInputStream in;
    private final CountInputStream counter;

    static final byte REPLY_ERROR = 2;

    static final byte REPLY_OK = 1;

    private static VirtualSocketAddress addressFromIdentifier(IbisIdentifier ibis, int virtualPort) throws IOException {
        VirtualSocketAddress registryAddress = VirtualSocketAddress.fromBytes(ibis.getRegistryData(), 0);

        if (registryAddress.port() == virtualPort) {
            return registryAddress;
        }

        // wrong port, create new address with correct port
        return new VirtualSocketAddress(registryAddress.machine(), virtualPort, registryAddress.hub(), registryAddress.cluster());
    }

    public Connection(IbisIdentifier ibis, int timeout, boolean fillTimeout, VirtualSocketFactory factory, int virtualPort) throws IOException {
        this(addressFromIdentifier(ibis, virtualPort), timeout, fillTimeout, factory);
    }

    public Connection(VirtualSocketAddress address, int timeout, boolean fillTimeout, VirtualSocketFactory factory) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("connecting to " + address + ", timeout = " + timeout + " , filltimeout = " + fillTimeout);
        }

        final HashMap<String, Object> lightConnection = new HashMap<>();
        // lightConnection.put("connect.module.allow",
        // "ConnectModule(HubRouted)");

        socket = factory.createClientSocket(address, timeout, fillTimeout, lightConnection);
        socket.setTcpNoDelay(true);

        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), IOProperties.BUFFER_SIZE));
        counter = new CountInputStream(new BufferedInputStream(socket.getInputStream(), IOProperties.BUFFER_SIZE));
        in = new DataInputStream(counter);

        if (logger.isDebugEnabled()) {
            logger.debug("connection to " + address + " established");
        }

    }

    /**
     * Accept incoming connection on given serverSocket.
     *
     * @param serverSocket the server socket
     * @throws IOException when an IO error occurs
     */
    public Connection(VirtualServerSocket serverSocket) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("waiting for incoming connection...");
        }
        socket = serverSocket.accept();
        socket.setTcpNoDelay(true);

        counter = new CountInputStream(new BufferedInputStream(socket.getInputStream(), IOProperties.BUFFER_SIZE));
        in = new DataInputStream(counter);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), IOProperties.BUFFER_SIZE));
        if (logger.isDebugEnabled()) {
            logger.debug("new connection from " + socket.getRemoteSocketAddress() + " accepted");
        }
    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        int size = in.readInt();

        if (size < 0) {
            throw new IOException("negative object size");
        }

        byte[] bytes = new byte[size];

        in.readFully(bytes);

        return Conversion.byte2object(bytes);
    }

    public void writeObject(Object object) throws IOException {
        byte[] bytes = Conversion.object2byte(object);

        out.writeInt(bytes.length);
        out.write(bytes);
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
