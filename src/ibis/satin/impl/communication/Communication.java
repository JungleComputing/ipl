/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.communication;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.Statistics;
import ibis.satin.impl.loadBalancing.Victim;

import java.io.IOException;
import java.net.InetAddress;

public final class Communication implements Config, Protocol {
    private Satin s;

    public PortType portType;

    public ReceivePort receivePort;

    private volatile boolean exitStageTwo = false;

    private volatile int barrierRequests = 0;

    private volatile boolean gotBarrierReply = false;

    private volatile int exitReplies = 0;

    public Ibis ibis;

    public Communication(Satin s) {
        this.s = s;

        IbisCapabilities ibisProperties = createIbisProperties();

        commLogger.debug("SATIN '" + "- " + "': init ibis");
        
        portType = createSatinPortType();
        
        try {
            ibis = IbisFactory.createIbis(ibisProperties,
                    null, s.ft.getRegistryEventHandler(), portType, createBarrierPortType(),
                    // lrmc port type
                    new PortType(
                        PortType.SERIALIZATION_DATA, PortType.COMMUNICATION_RELIABLE,
                        PortType.CONNECTION_MANY_TO_ONE, PortType.RECEIVE_AUTO_UPCALLS),
                    // so port type
                    new PortType(
                            PortType.CONNECTION_ONE_TO_MANY, PortType.CONNECTION_UPCALLS,
                            PortType.CONNECTION_DOWNCALLS, PortType.RECEIVE_EXPLICIT,
                            PortType.RECEIVE_AUTO_UPCALLS, PortType.SERIALIZATION_OBJECT));
        } catch (Exception e) {
            commLogger.fatal(
                    "SATIN '" + "- " + "': Could not start ibis: " + e, e);
            System.exit(1); // Could not start ibis
        }

        IbisIdentifier ident = ibis.ibisIdentifier();

        commLogger.debug("SATIN '" + "- " + "': init ibis DONE, "
                + "my location is '" + ident.getLocation()
                + "'");

        try {
            MessageHandler messageHandler = new MessageHandler(s);

            receivePort = ibis.createReceivePort(portType, "satin port",
                    messageHandler, s.ft.getReceivePortConnectHandler());
        } catch (Exception e) {
            commLogger.fatal("SATIN '" + ident + "': Could not start ibis: "
                    + e, e);
            System.exit(1); // Could not start ibis
        }

        if (CLOSED) {
            commLogger.info("SATIN '" + ident
                    + "': running with closed world, "
                    + ibis.getPoolSize() + " host(s)");
        } else {
            commLogger.info("SATIN '" + ident + "': running with open world");
        }
    }

    public IbisIdentifier electMaster() {
        Registry r = ibis.registry();
        IbisIdentifier ident = ibis.ibisIdentifier();
        IbisIdentifier masterIdent = null;

        String canonicalMasterHost = null;
        String localHostName = null;

        if (MASTER_HOST != null) {
            try {
                InetAddress a = InetAddress.getByName(MASTER_HOST);
                canonicalMasterHost = a.getCanonicalHostName();
            } catch (Exception e) {
                commLogger.warn("satin.masterhost is set to an unknown "
                        + "name: " + MASTER_HOST);
                commLogger.warn("continuing with default master election");
            }
            try {
                localHostName = InetAddress.getLocalHost()
                    .getCanonicalHostName();
            } catch (Exception e) {
                commLogger.warn("Could not get local hostname");
                canonicalMasterHost = null;
            }

            try {
                if (canonicalMasterHost == null
                        || !canonicalMasterHost.equals(localHostName)) {
                    masterIdent = r.getElectionResult("satin master");
                } else {
                    masterIdent = r.elect("satin master");
                }
            } catch (Exception e) {
                commLogger.fatal("SATIN '" + ident
                        + "': Could not do an election for the master: "
                        + e, e);
                System.exit(1); // Could not start ibis
            }
        } else {
            try {
                masterIdent = r.elect("satin master");
            } catch (Exception e) {
                commLogger.fatal("SATIN '" + ident
                        + "': Could not do an election for the master: " + e, e);
                System.exit(1); // Could not start ibis
            }
        }

        return masterIdent;
    }

    public void enableConnections() {
        receivePort.enableMessageUpcalls();
        receivePort.enableConnections();
    }

    public boolean inDifferentCluster(IbisIdentifier other) {
        return !s.ident.getLocation().getCluster().equals(
                other.getLocation().getCluster());
    }

    public IbisCapabilities createIbisProperties() {
        IbisCapabilities ibisProperties = new IbisCapabilities(
                CLOSED ? IbisCapabilities.WORLDMODEL_CLOSED : IbisCapabilities.WORLDMODEL_OPEN,
                IbisCapabilities.REGISTRY_UPCALLS);

        return ibisProperties;
    }

    public PortType createSatinPortType() {
        return new PortType(
                PortType.SERIALIZATION_OBJECT, PortType.COMMUNICATION_RELIABLE,
                PortType.CONNECTION_MANY_TO_ONE, PortType.CONNECTION_UPCALLS,
                PortType.CONNECTION_DOWNCALLS, PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_AUTO_UPCALLS);
    }

    // The barrier port type is different from the satin port type.
    // It does not do multicast, and does not need serialization.
    public PortType createBarrierPortType() throws IOException {
        return new PortType(
                PortType.SERIALIZATION_BYTE, PortType.COMMUNICATION_RELIABLE,
                PortType.CONNECTION_MANY_TO_ONE, PortType.RECEIVE_EXPLICIT);
    }

    public void bcastMessage(byte opcode) {
        Victim[] victims;
        synchronized (s) {
            victims = s.victims.victims();
        }

        for (int i = 0; i < victims.length; i++) {
            Victim v = victims[i];
            commLogger.debug("SATIN '" + s.ident + "': sending "
                    + opcodeToString(opcode) + " message to "
                    + v.getIdent());
            try {
                WriteMessage writeMessage = v.newMessage();
                writeMessage.writeByte(opcode);
                v.finish(writeMessage);
            } catch (IOException e) {
                synchronized (s) {
                    ftLogger.info("SATIN '" + s.ident
                            + "': could not send bcast message to "
                            + v.getIdent(), e);
                    try {
                        ibis.registry().maybeDead(v.getIdent());
                    } catch (IOException e2) {
                        ftLogger.warn("SATIN '" + s.ident
                                + "': got exception in maybeDead", e2);
                    }
                }
            }
        }
    }

    public static void disconnect(SendPort s, ReceivePortIdentifier ident) {
        try {
            s.disconnect(ident);
        } catch (IOException e) {
            // ignored
        }
    }

    public static ReceivePortIdentifier connect(SendPort s,
            IbisIdentifier ident, String name, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        ReceivePortIdentifier r = null;
        do {
            try {
                r = s.connect(ident, name, timeoutMillis);
            } catch (AlreadyConnectedException x) {
                commLogger.info("the port was already connected");
                ReceivePortIdentifier[] ports = s.connectedTo();
                for (int i = 0; i < ports.length; i++) {
                    if (ports[i].ibisIdentifier().equals(ident)
                            && ports[i].receivePortName().equals(name)) {
                        commLogger
                            .info("the port was already connected, found it");
                        return ports[i];
                            }
                }
                commLogger
                    .info("the port was already connected, but could not find it, retry!");
                // return null;
            } catch (IOException e) {
                commLogger.info(
                        "IOException in connect to " + ident + ": " + e, e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (r == null
                && System.currentTimeMillis() - startTime < timeoutMillis);

        if (r == null) {
            commLogger.info("could not connect port within given time");
        } else {
            commLogger.info("port connected");
        }
        return r;
    }

    /* Only allowed when not stealing. And with a closed world */
    private void barrier() {
        IbisIdentifier ident = ibis.ibisIdentifier();
        commLogger.debug("SATIN '" + ident + "': barrier start");

        int size;
        synchronized (s) {
            size = s.victims.size();
        }

        try {
            if (s.isMaster()) {
                synchronized (s) {
                    while (barrierRequests != size) {
                        try {
                            s.wait();
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    barrierRequests = 0;
                }

                for (int i = 0; i < size; i++) {
                    Victim v;
                    synchronized (s) {
                        v = s.victims.getVictim(i);
                    }
                    if (v == null) {
                        commLogger.fatal("a machine crashed with open world");
                        System.exit(1);
                    }

                    WriteMessage writeMessage = v.newMessage();
                    writeMessage.writeByte(Protocol.BARRIER_REPLY);
                    v.finish(writeMessage);
                }
            } else {
                Victim v;

                synchronized (s) {
                    v = s.victims.getVictim(s.getMasterIdent());
                }

                if (v == null) {
                    commLogger.fatal("could not get master victim.");
                    System.exit(1);
                }

                WriteMessage writeMessage = v.newMessage();
                writeMessage.writeByte(Protocol.BARRIER_REQUEST);
                writeMessage.finish();

                while (!gotBarrierReply/* && !exiting */) {
                    s.handleDelayedMessages();
                }
                /*
                 * Imediately reset gotBarrierReply, we know that a reply has
                 * arrived.
                 */
                gotBarrierReply = false;
            }
        } catch (IOException e) {
            commLogger.warn("SATIN '" + ident + "': error in barrier", e);
        }

        commLogger.debug("SATIN '" + ident + "': barrier DONE");
    }

    public void waitForExitReplies() {
        int size;
        synchronized (s) {
            size = s.victims.size();
        }

        // wait until everybody has send an ACK
        synchronized (s) {
            while (exitReplies != size) {
                try {
                    s.handleDelayedMessages();
                    s.wait(250);
                } catch (Exception e) {
                    // Ignore.
                }
                size = s.victims.size();
            }
        }
    }

    public void sendExitAck() {
        Victim v = null;

        synchronized (s) {
            v = s.victims.getVictim(s.getMasterIdent());
        }

        if (v == null)
            return; // node might have crashed

        try {
            WriteMessage writeMessage;
            commLogger.debug("SATIN '" + s.ident
                    + "': sending exit ACK message to " + s.getMasterIdent());

            writeMessage = v.newMessage();
            writeMessage.writeByte(Protocol.EXIT_REPLY);
            if (STATS) {
                s.stats.fillInStats();
                writeMessage.writeObject(s.stats);
            }
            v.finish(writeMessage);
        } catch (IOException e) {
            ftLogger.info("SATIN '" + s.ident
                    + "': could not send exit message to " + s.getMasterIdent(), e);
            try {
                ibis.registry().maybeDead(s.getMasterIdent());
            } catch (IOException e2) {
                ftLogger.warn("SATIN '" + s.ident
                        + "': got exception in maybeDead", e2);
            }
        }
    }

    public void waitForExitStageTwo() {
        synchronized (s) {
            while (!exitStageTwo) {
                try {
                    s.handleDelayedMessages();
                    s.wait(250);
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }
    }

    public void closeSendPorts() {
        // If not closed, free ports. Otherwise, ports will be freed in leave
        // calls.
        while (true) {
            try {
                Victim v;

                synchronized (s) {
                    if (s.victims.size() == 0) {
                        break;
                    }

                    v = s.victims.remove(0);

                    commLogger.debug("SATIN '" + s.ident
                            + "': closing sendport to " + v.getIdent());
                }

                if (v != null) {
                    v.close();
                }

            } catch (Throwable e) {
                commLogger.warn("SATIN '" + s.ident
                        + "': port.close() throws exception", e);
            }
        }
    }

    public void closeReceivePort() {
        try {
            receivePort.close();
        } catch (Throwable e) {
            commLogger.warn("SATIN '" + s.ident
                    + "': port.close() throws exception", e);
        }
    }

    public void end() {
        try {
            ibis.end();
        } catch (Throwable e) {
            commLogger.warn("SATIN '" + s.ident
                    + "': ibis.end() throws exception", e);
        }
    }

    public void waitForAllNodes() {
        commLogger.debug("SATIN '" + s.ident + "': pre barrier");

        int poolSize = ibis.getPoolSize();

        synchronized (s) {
            while (s.victims.size() != poolSize - 1) {
                try {
                    s.wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
            commLogger.debug("SATIN '" + s.ident
                    + "': barrier, everybody has joined");
        }

        barrier();

        commLogger.debug("SATIN '" + s.ident + "': post barrier");
    }

    public void handleExitReply(ReadMessage m) {

        SendPortIdentifier ident = m.origin();

        commLogger.debug("SATIN '" + s.ident + "': got exit ACK message from "
                + ident.ibisIdentifier());

        if (STATS) {
            try {
                Statistics stats = (Statistics) m.readObject();
                s.totalStats.add(stats);
            } catch (Exception e) {
                commLogger.warn("SATIN '" + s.ident
                        + "': Got Exception while reading stats: " + e, e);
                // System.exit(1);
            }
        }

        try {
            m.finish();
        } catch (Exception e) {
            /* ignore */
        }

        synchronized (s) {
            exitReplies++;
            s.notifyAll();
        }
    }

    public void handleExitMessage(IbisIdentifier ident) {
        commLogger.debug("SATIN '" + s.ident + "': got exit message from "
                + ident);

        synchronized (s) {
            s.exiting = true;
            s.notifyAll();
        }
    }

    public void handleExitStageTwoMessage(IbisIdentifier ident) {
        commLogger.debug("SATIN '" + s.ident + "': got exit2 message from "
                + ident);

        synchronized (s) {
            exitStageTwo = true;
            s.notifyAll();
        }
    }

    public void handleBarrierRequestMessage() {
        synchronized (s) {
            barrierRequests++;
            s.notifyAll();
        }
    }

    public void disableUpcallsForExit() {
        if (!CLOSED) {
            ibis.disableRegistryEvents();
        }

        s.ft.disableConnectionUpcalls();
    }

    public void handleBarrierReply(IbisIdentifier sender) {
        commLogger.debug("SATIN '" + s.ident
                + "': got barrier reply message from " + sender);

        synchronized (s) {
            if (ASSERTS && gotBarrierReply) {
                commLogger.fatal("Got barrier reply while I already got "
                        + "one.");
                System.exit(1); // Failed assertion
            }
            gotBarrierReply = true;
            s.notifyAll();
        }
    }

    public static String opcodeToString(int opcode) {
        switch (opcode) {
        case EXIT:
            return "EXIT";
        case EXIT_REPLY:
            return "EXIT_REPLY";
        case BARRIER_REPLY:
            return "BARRIER_REPLY";
        case STEAL_REQUEST:
            return "STEAL_REQUEST";
        case STEAL_REPLY_FAILED:
            return "STEAL_REPLY_FAILED";
        case STEAL_REPLY_SUCCESS:
            return "STEAL_REPLY_SUCCESS";
        case ASYNC_STEAL_REQUEST:
            return "ASYNC_STEAL_REQUEST";
        case ASYNC_STEAL_REPLY_FAILED:
            return "ASYNC_STEAL_REPLY_FAILED";
        case ASYNC_STEAL_REPLY_SUCCESS:
            return "ASYNC_STEAL_REPLY_SUCCESS";
        case JOB_RESULT_NORMAL:
            return "JOB_RESULT_NORMAL";
        case JOB_RESULT_EXCEPTION:
            return "JOB_RESULT_EXCEPTION";
        case ABORT:
            return "ABORT";
        case BLOCKING_STEAL_REQUEST:
            return "BLOCKING_STEAL_REQUEST";
        case CRASH:
            return "CRASH";
        case ABORT_AND_STORE:
            return "ABORT_AND_STORE";
        case RESULT_REQUEST:
            return "RESULT_REQUEST";
        case STEAL_AND_TABLE_REQUEST:
            return "STEAL_AND_TABLE_REQUEST";
        case ASYNC_STEAL_AND_TABLE_REQUEST:
            return "ASYNC_STEAL_AND_TABLE_REQUEST";
        case STEAL_REPLY_FAILED_TABLE:
            return "STEAL_REPLY_FAILED_TABLE";
        case STEAL_REPLY_SUCCESS_TABLE:
            return "STEAL_REPLY_SUCCESS_TABLE";
        case ASYNC_STEAL_REPLY_FAILED_TABLE:
            return "ASYNC_STEAL_REPLY_FAILED_TABLE";
        case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
            return "ASYNC_STEAL_REPLY_SUCCESS_TABLE";
        case RESULT_PUSH:
            return "RESULT_PUSH";
        case SO_INVOCATION:
            return "SO_INVOCATION";
        case SO_REQUEST:
            return "SO_REQUEST";
        case SO_TRANSFER:
            return "SO_TRANSFER";
        case EXIT_STAGE2:
            return "EXIT_STAGE2";
        case BARRIER_REQUEST:
            return "BARRIER_REQUEST";
        }

        throw new Error("unknown opcode in opcodeToString");
    }
}
