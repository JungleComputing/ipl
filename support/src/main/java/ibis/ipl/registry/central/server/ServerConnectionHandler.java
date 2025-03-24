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
package ibis.ipl.registry.central.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.Conversion;
import ibis.ipl.Credentials;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.registry.ControlPolicy;
import ibis.ipl.registry.central.Member;
import ibis.ipl.registry.central.Protocol;
import ibis.ipl.server.ServerProperties;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

final class ServerConnectionHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 50;

    static final int MAX_THREADS = 50;

    private static final Logger logger = LoggerFactory.getLogger(ServerConnectionHandler.class);

    private final CentralRegistryService server;

    private final VirtualSocketFactory socketFactory;

    private final VirtualServerSocket serverSocket;

    private int currentNrOfThreads = 0;

    private int maxNrOfThreads = 0;

    private ControlPolicy policy;

    ServerConnectionHandler(CentralRegistryService server, VirtualSocketFactory connectionFactory, ControlPolicy policy) throws IOException {
        this.server = server;
        this.socketFactory = connectionFactory;

        serverSocket = socketFactory.createServerSocket(Protocol.VIRTUAL_PORT, CONNECTION_BACKLOG, null);
        this.policy = policy;

        createThread();
    }

    private Pool handleJoin(Connection connection) throws Exception {
        Member member;
        Pool pool;

        // long start = System.currentTimeMillis();

        String version = ServerProperties.implementationVersion;

        String peerVersion = connection.in().readUTF();

        if (peerVersion == null || !peerVersion.equals(version)) {
            throw new IOException("Wrong ipl server version in join: " + peerVersion + ", should be " + version);
        }

        int length = connection.in().readInt();
        if (length < 0) {
            throw new IOException("unexpected end of data on join");
        }
        byte[] clientAddress = new byte[length];
        connection.in().readFully(clientAddress);

        String poolName = connection.in().readUTF();

        length = connection.in().readInt();
        if (length < 0) {
            throw new IOException("unexpected end of data on join");
        }
        byte[] implementationData = new byte[length];
        connection.in().readFully(implementationData);

        String implementationVersion = connection.in().readUTF();

        Location location = new Location(connection.in());

        boolean peerBootstrap = connection.in().readBoolean();
        long heartbeatInterval = connection.in().readLong();
        long eventPushInterval = connection.in().readLong();
        boolean gossip = connection.in().readBoolean();
        long gossipInterval = connection.in().readLong();
        boolean adaptGossipInterval = connection.in().readBoolean();
        boolean tree = connection.in().readBoolean();
        boolean closedWorld = connection.in().readBoolean();
        int poolSize = connection.in().readInt();
        boolean keepStatistics = connection.in().readBoolean();
        long statisticsInterval = connection.in().readLong();
        boolean purgeHistory = connection.in().readBoolean();

        length = connection.in().readInt();
        if (length < 0) {
            throw new IOException("unexpected end of data on join");
        }

        byte[] credentialBytes = new byte[length];

        connection.in().readFully(credentialBytes);

        Credentials credentials = (Credentials) Conversion.byte2object(credentialBytes);

        length = connection.in().readInt();
        byte[] applicationTag = null;
        if (length >= 0) {
            applicationTag = new byte[length];
            connection.in().readFully(applicationTag);
        }

        if (policy != null) {
            try {
                policy.onJoin(credentials);
            } catch (Exception e) {
                connection.closeWithError(e.getMessage());
                throw e;
            }
        }

        // long dataRead = System.currentTimeMillis();

        pool = server.getOrCreatePool(poolName, peerBootstrap, heartbeatInterval, eventPushInterval, gossip, gossipInterval, adaptGossipInterval,
                tree, closedWorld, poolSize, keepStatistics, statisticsInterval, purgeHistory, implementationVersion);

        // long poolRetrieved = System.currentTimeMillis();

        try {
            member = pool.join(implementationData, clientAddress, location, implementationVersion, applicationTag);
        } catch (IOException e) {
            connection.closeWithError(e.getMessage());
            throw e;
        }

        // long joinDone = System.currentTimeMillis();

        connection.sendOKReply();
        // write info on new member (including identifier, join time and
        // current minimum time of pool
        member.getIbis().writeTo(connection.out());
        connection.out().writeInt(member.getEvent().getTime());
        connection.out().writeInt(member.getCurrentTime());

        pool.writeBootstrapList(connection.out());

        connection.out().flush();

        // long dataWritten = System.currentTimeMillis();

        pool.gotHeartbeat(member.getIbis());

        // logger.debug("dataRead = " + (start - dataRead) + ", poolRetrieved =
        // "
        // + (poolRetrieved - dataRead) + ", joinDone = " + (joinDone -
        // poolRetrieved) + ", datawritten = " + (dataWritten - joinDone));

        return pool;
    }

    private Pool handleLeave(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            return null;
        }

        try {
            pool.leave(identifier);
        } catch (Exception e) {
            connection.closeWithError(e.toString());
            throw e;
        }

        connection.sendOKReply();

        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleElect(Connection connection) throws Exception {
        IbisIdentifier candidate = new IbisIdentifier(connection.in());
        String election = connection.in().readUTF();

        Pool pool = server.getPool(candidate.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + candidate.poolName() + " not found");
        }

        IbisIdentifier winner = pool.elect(election, candidate);

        connection.sendOKReply();

        winner.writeTo(connection.out());
        pool.gotHeartbeat(candidate);
        return pool;

    }

    private Pool handleGetSequenceNumber(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String name = connection.in().readUTF();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        long number = pool.getSequenceNumber(name);

        connection.sendOKReply();

        connection.out().writeLong(number);
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleAddTokens(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String name = connection.in().readUTF();
        int count = connection.in().readInt();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        pool.addTokens(name, count);

        connection.sendOKReply();

        pool.gotHeartbeat(identifier);
        return pool;
    }

    private Pool handleGetToken(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String name = connection.in().readUTF();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        String result = pool.getToken(name);

        connection.sendOKReply();
        connection.out().writeInt(result == null ? 0 : 1);

        pool.gotHeartbeat(identifier);
        return pool;
    }

    private Pool handleDead(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        IbisIdentifier corpse = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        try {
            pool.dead(corpse, new Exception("ibis declared dead by " + identifier));
        } catch (Exception e) {
            connection.closeWithError(e.getMessage());
            throw e;
        }

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleMaybeDead(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        IbisIdentifier suspect = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        pool.maybeDead(suspect);

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleSignal(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        String signal = connection.in().readUTF();

        IbisIdentifier[] receivers = new IbisIdentifier[connection.in().readInt()];
        for (int i = 0; i < receivers.length; i++) {
            receivers[i] = new IbisIdentifier(connection.in());
        }

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        pool.signal(signal, identifier, receivers);

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleGetState(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        int joinTime = connection.in().readInt();

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        connection.sendOKReply();
        pool.writeState(connection.out(), joinTime);
        connection.out().flush();
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleHeartbeat(Connection connection) throws Exception {
        IbisIdentifier identifier = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(identifier.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + identifier.poolName() + " not found");
        }

        connection.sendOKReply();
        pool.gotHeartbeat(identifier);
        return pool;

    }

    private Pool handleTerminate(Connection connection) throws Exception {
        IbisIdentifier source = new IbisIdentifier(connection.in());

        Pool pool = server.getPool(source.poolName());

        if (pool == null) {
            connection.closeWithError("pool not found");
            throw new Exception("pool " + source.poolName() + " not found");
        }

        pool.terminate(source);

        connection.sendOKReply();
        pool.gotHeartbeat(source);
        return pool;

    }

    private synchronized void createThread() {
        while (currentNrOfThreads >= MAX_THREADS) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // create new thread for next connection
        ThreadPool.createNew(this, "server connection handler");
        currentNrOfThreads++;

        if (currentNrOfThreads > maxNrOfThreads) {
            maxNrOfThreads = currentNrOfThreads;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Now " + currentNrOfThreads + " connections");
        }
    }

    private synchronized void threadEnded() {
        currentNrOfThreads--;

        notifyAll();
    }

    @Override
    public void run() {
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("accepting connection");
            }
            connection = new Connection(serverSocket);
            if (logger.isDebugEnabled()) {
                logger.debug("connection accepted");
            }
        } catch (IOException e) {
            if (server.isStopped()) {
                threadEnded();
                return;
            }
            logger.error("Accept failed, waiting a second, will retry", e);

            // wait a bit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // IGNORE
            }
        }

        createThread();

        if (connection == null) {
            threadEnded();
            return;
        }
        long start = System.currentTimeMillis();

        byte opcode = 0;
        Pool pool = null;
        try {
            byte magic = connection.in().readByte();

            if (magic != Protocol.MAGIC_BYTE) {
                throw new IOException("Invalid header byte in accepting connection");
            }

            opcode = connection.in().readByte();

            if (logger.isDebugEnabled() && opcode < Protocol.NR_OF_OPCODES) {
                logger.debug("got request, opcode = " + Protocol.OPCODE_NAMES[opcode]);
            }

            switch (opcode) {
            case Protocol.OPCODE_JOIN:
                pool = handleJoin(connection);
                break;
            case Protocol.OPCODE_LEAVE:
                pool = handleLeave(connection);
                break;
            case Protocol.OPCODE_ELECT:
                pool = handleElect(connection);
                break;
            case Protocol.OPCODE_SEQUENCE_NR:
                pool = handleGetSequenceNumber(connection);
                break;
            case Protocol.OPCODE_DEAD:
                pool = handleDead(connection);
                break;
            case Protocol.OPCODE_MAYBE_DEAD:
                pool = handleMaybeDead(connection);
                break;
            case Protocol.OPCODE_SIGNAL:
                pool = handleSignal(connection);
                break;
            case Protocol.OPCODE_GET_STATE:
                pool = handleGetState(connection);
                break;
            case Protocol.OPCODE_HEARTBEAT:
                pool = handleHeartbeat(connection);
                break;
            case Protocol.OPCODE_TERMINATE:
                pool = handleTerminate(connection);
                break;
            case Protocol.OPCODE_ADD_TOKENS:
                pool = handleAddTokens(connection);
                break;
            case Protocol.OPCODE_GET_TOKEN:
                pool = handleGetToken(connection);
                break;
            default:
                logger.error("unknown opcode: " + opcode);
            }
        } catch (Exception e) {
            // send error to client
            connection.closeWithError("Server: " + e.getMessage());
            logger.error("error on handling connection", e);
        } finally {
            connection.close();
        }

        if (pool != null) {
            if (pool.getStatistics() != null) {
                pool.getStatistics().add(opcode, System.currentTimeMillis() - start, connection.read(), connection.written(), true);
                if (logger.isDebugEnabled()) {
                    logger.debug("done handling request");
                }
            }
            if (pool.hasEnded()) {
                // save statistics
                pool.saveStatistics();
            }
        }
        threadEnded();
    }

    public void end() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            // IGNORE
        }
        if (logger.isInfoEnabled()) {
            synchronized (this) {
                logger.debug("max simultanious connections was: " + maxNrOfThreads);
            }
        }
    }
}
