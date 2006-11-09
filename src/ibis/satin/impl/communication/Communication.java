/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.communication;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
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

    public Communication(Satin s, StaticProperties requestedProperties) {
        this.s = s;

        StaticProperties ibisProperties = createIbisProperties(requestedProperties);

        commLogger.debug("SATIN '" + "- " + "': init ibis");

        try {
            ibis = Ibis.createIbis(ibisProperties, s.ft.getResizeHandler());
        } catch (IbisException e) {
            commLogger.fatal(
                "SATIN '" + "- " + "': Could not start ibis: " + e, e);
            System.exit(1); // Could not start ibis
        }

        IbisIdentifier ident = ibis.identifier();

        commLogger.debug("SATIN '" + "- " + "': init ibis DONE, "
            + "my cluster is '" + ident.cluster() + "'");

        electMaster();

        try {
            portType = createSatinPortType(requestedProperties);

            MessageHandler messageHandler = new MessageHandler(s);

            if (LOCALPORTS) {
                receivePort = portType.createLocalReceivePort("satin port",
                        messageHandler, s.ft.getReceivePortConnectHandler());
            } else {
                receivePort = portType.createReceivePort(
                        "satin port on " + ident.name(),
                        messageHandler, s.ft.getReceivePortConnectHandler());
            }
            receivePort.enableUpcalls();
            receivePort.enableConnections();

        } catch (Exception e) {
            commLogger.fatal("SATIN '" + ident + "': Could not start ibis: "
                + e, e);
            System.exit(1); // Could not start ibis            
        }

        if (CLOSED) {
            commLogger.info("SATIN '" + ident
                + "': running with closed world, "
                + ibis.totalNrOfIbisesInPool() + " host(s)");
        } else {
            commLogger.info("SATIN '" + ident + "': running with open world");
        }
    }

    public void electMaster() {
        Registry r = ibis.registry();
        IbisIdentifier ident = ibis.identifier();

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
        }

        try {
            if (canonicalMasterHost == null
                || canonicalMasterHost.equals(localHostName)) {
                s.masterIdent = r.elect("satin master");
            } else {
                s.masterIdent = r.getElectionResult("satin master");
            }
        } catch (Exception e) {
            commLogger.fatal("SATIN '" + ident
                + "': Could not do an election for the master: " + e, e);
            System.exit(1); // Could not start ibis
        }

        if (s.masterIdent.equals(ident)) {
            /* I an the master. */
            commLogger
                .info("SATIN '" + ident + "': init ibis: I am the master");
            s.setMaster(true);
        } else {
            commLogger.info("SATIN '" + ident + "': init ibis I am slave");
        }
    }

    public boolean inDifferentCluster(IbisIdentifier other) {
        return !s.ident.cluster().equals(other.cluster());
    }

    public StaticProperties createIbisProperties(
        StaticProperties requestedProperties) {
        StaticProperties ibisProperties = new StaticProperties(
            requestedProperties);

        ibisProperties.add("serialization", "byte, object");

        if (CLOSED) {
            ibisProperties.add("worldmodel", "closed");
        } else {
            ibisProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, OneToMany, ManyToOne, ExplicitReceipt, Reliable";
        commprops += ", ConnectionUpcalls, ConnectionDowncalls";
        commprops += ", AutoUpcalls";

        if (LOCALPORTS) {
            commprops += ", LocalReceivePorts";
        }

        ibisProperties.add("communication", commprops);
        return ibisProperties;
    }

    public PortType createSatinPortType(StaticProperties reqprops)
        throws IOException, IbisException {
        StaticProperties satinPortProperties = new StaticProperties(reqprops);

        if (CLOSED) {
            satinPortProperties.add("worldmodel", "closed");
        } else {
            satinPortProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, ManyToOne, ExplicitReceipt, Reliable";
        commprops += ", ConnectionUpcalls, ConnectionDowncalls";
        commprops += ", AutoUpcalls";
        satinPortProperties.add("communication", commprops);

        satinPortProperties.add("serialization", "object");

        return ibis.createPortType("satin porttype", satinPortProperties);
    }

    // The barrier port type is different from the satin port type.
    // It does not do multicast, and does not need serialization.
    public PortType createBarrierPortType(StaticProperties reqprops)
        throws IOException, IbisException {
        StaticProperties s = new StaticProperties(reqprops);

        s.add("serialization", "byte");
        if (CLOSED) {
            s.add("worldmodel", "closed");
        } else {
            s.add("worldmodel", "open");
        }

        s.add("communication", "OneToOne, ManyToOne, Reliable, "
            + "ExplicitReceipt");

        return this.ibis.createPortType("satin barrier porttype", s);
    }

    public void bcastMessage(byte opcode) {
        int size = 0;
        synchronized (s) {
            size = s.victims.size();
        }

        for (int i = 0; i < size; i++) {
            Victim v = null;
            try {
                WriteMessage writeMessage;
                synchronized (s) {
                    v = s.victims.getVictim(i);
                }
                commLogger.debug("SATIN '" + s.ident + "': sending "
                    + opcodeToString(opcode) + " message to " + v.getIdent());

                writeMessage = v.newMessage();
                writeMessage.writeByte(opcode);
                writeMessage.finish();
            } catch (IOException e) {
                synchronized (s) {
                    ftLogger.info("SATIN '" + s.ident
                        + "': could not send bcast message to " + v.getIdent(),
                        e);
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

    public static void connect(SendPort s, ReceivePortIdentifier ident) {
        boolean success = false;
        do {
            try {
                s.connect(ident);
                success = true;
            } catch (AlreadyConnectedException x) {
                return;
            } catch (IOException e) {
                commLogger.info(
                    "IOException in connect to " + ident + ": " + e, e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (!success);
    }

    public static void disconnect(SendPort s, ReceivePortIdentifier ident) {
        try {
            s.disconnect(ident);
        } catch (IOException e) {
            // ignored
        }
    }

    public static boolean connect(SendPort s, ReceivePortIdentifier ident,
        long timeoutMillis) {
        boolean success = false;
        long startTime = System.currentTimeMillis();
        do {
            try {
                s.connect(ident, timeoutMillis);
                success = true;
            } catch (AlreadyConnectedException x) {
                return true;
            } catch (IOException e) {
                commLogger.info(
                    "IOException in connect to " + ident + ": " + e, e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (!success
            && System.currentTimeMillis() - startTime < timeoutMillis);
        return success;
    }

    public static ReceivePortIdentifier connect(SendPort s,
            IbisIdentifier ident, String name, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        ReceivePortIdentifier r = null;
        do {
            try {
                r = s.connect(ident, name, timeoutMillis);
            } catch (AlreadyConnectedException x) {
                ReceivePortIdentifier[] ports = s.connectedTo();
                for (int i = 0; i < ports.length; i++) {
                    if (ports[i].ibis().equals(ident) &&
                            ports[i].name().equals(name)) {
                        return ports[i];
                    }
                }
                return null;
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
        return r;
    }

    public ReceivePortIdentifier lookup(String portname) throws IOException {
        return ibis.registry().lookupReceivePort(portname);
    }

    public ReceivePortIdentifier[] lookup(String[] portnames)
        throws IOException {
        return ibis.registry().lookupReceivePorts(portnames);
    }

    public ReceivePortIdentifier lookup_wait(String portname, long timeoutMillis) {
        ReceivePortIdentifier rpi = null;
        try {
            rpi = ibis.registry().lookupReceivePort(portname, timeoutMillis);
        } catch (Exception e) {
            // ignored
        }
        return rpi;
    }

    /* Only allowed when not stealing. And with a closed world */
    private void barrier() {
        IbisIdentifier ident = ibis.identifier();
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

                    WriteMessage writeMessage = v.newMessage();
                    writeMessage.writeByte(Protocol.BARRIER_REPLY);
                    writeMessage.finish();
                }
            } else {
                Victim v;

                synchronized (s) {
                    v = s.victims.getVictim(s.masterIdent);
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
                     * Imediately reset gotBarrierReply, we know that a reply
                     * has arrived.
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
        Victim mp = null;

        synchronized (s) {
            mp = s.victims.getVictim(s.masterIdent);
        }

        if (mp == null) return; // node might have crashed

        try {
            WriteMessage writeMessage;
            commLogger.debug("SATIN '" + s.ident
                + "': sending exit ACK message to " + s.masterIdent);

            writeMessage = mp.newMessage();
            writeMessage.writeByte(Protocol.EXIT_REPLY);
            if (STATS) {
                s.stats.fillInStats();
                writeMessage.writeObject(s.stats);
            }
            writeMessage.finish();
        } catch (IOException e) {
            ftLogger.info("SATIN '" + s.ident
                + "': could not send exit message to " + s.masterIdent, e);
            try {
                ibis.registry().maybeDead(s.masterIdent);
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

        int poolSize = ibis.totalNrOfIbisesInPool();

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
            + ident.ibis());

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
            ibis.disableResizeUpcalls();
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
