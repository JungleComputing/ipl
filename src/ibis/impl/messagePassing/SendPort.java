/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.DynamicProperties;
import ibis.ipl.PortMismatchException;
import ibis.ipl.Replacer;
import ibis.ipl.StaticProperties;
import ibis.util.ConditionVariable;
import ibis.util.TypedProperties;

import java.io.IOException;

/**
 * messagePassing implementation of SendPort. Does only Byte serialization.
 */
public class SendPort implements ibis.ipl.SendPort {

    protected final static boolean DEBUG = Ibis.DEBUG;

    private final static boolean USE_BCAST
        = TypedProperties.stringPropertyMatch(MPProps.s_broadcast, "native")
            || TypedProperties.booleanProperty(MPProps.s_bc_native, true);

    private final static boolean USE_BCAST_ALL
        = TypedProperties.booleanProperty(MPProps.s_bc_all);

    private final static boolean USE_BCAST_AT_TWO
        = TypedProperties.booleanProperty(MPProps.s_bc_2);

    static {
        if (USE_BCAST && Ibis.myIbis.myCpu == 0) {
            System.err.println("Use native MessagePassing broadcast");
        }
    }

    private final static boolean DEFAULT_SERIALIZE_SENDS = false;

    private final static boolean SERIALIZE_SENDS_PER_CPU;
    static {
        SERIALIZE_SENDS_PER_CPU = TypedProperties.booleanProperty(
                MPProps.s_ser_sends);
    }

    protected PortType type;

    protected SendPortIdentifier ident;

    protected ReceivePortIdentifier[] splitter;

    private int[] connectedCpu;

    protected static final int NO_BCAST_GROUP = -1;

    protected int group = NO_BCAST_GROUP;

    protected ConnectAcker[] syncer;

    private String name;

    protected boolean aMessageIsAlive = false;

    protected int messageCount;

    private ConditionVariable portIsFree;

    private int newMessageWaiters;

    private boolean connecting = false;

    /*
     * If one of the connections is a Home connection, do some polls
     * after our send to see to it that the receive side doesn't have
     * to await a time slice.
     */
    protected boolean homeConnection;

    final private static int homeConnectionPolls = 4;

    protected WriteMessage message = null;

    protected long count;

    ByteOutputStream out;

    SendPort next;

    SendPort() {
        // No-args constructor required by Java
    }

    protected native void ibmp_connect(int dest, byte[] rcvePortId,
            byte[] sendPortId, ConnectAcker sncer, ConnectAcker delayed_syncer,
            int cnt, int grp, int startSeqno);

    protected native void ibmp_disconnect(int remoteCPU, byte[] receiverPortId,
            byte[] sendPortId, ConnectAcker sncer, int cnt);

    public SendPort(PortType type, String name, boolean syncMode,
            boolean makeCopy) throws IOException {
        Ibis.myIbis.lock();
        try {
            Ibis.myIbis.registerSendPort(this);
        } finally {
            Ibis.myIbis.unlock();
        }
        this.name = name;
        this.type = type;
        ident = new SendPortIdentifier(name, type.name());
        portIsFree = Ibis.myIbis.createCV();
        out = new ByteOutputStream(this, syncMode, makeCopy);
        count = 0;
    }

    public void setReplacer(Replacer r) throws IOException {
        // Is replacer unnecessary in MessagePassing? Ceriel?
    }

    public SendPort(PortType type, String name) throws IOException {
        this(type, name, true, false);
    }

    protected synchronized int addConnection(ReceivePortIdentifier rid)
            throws IOException {

        if (rid.cpu < 0) {
            throw new IllegalArgumentException("invalid ReceivePortIdentifier");
        }

        Ibis.myIbis.checkLockOwned();

        if (DEBUG) {
            System.out.println(name + " connecting to " + rid);
        }

        if (!type.name().equals(rid.type())) {
            throw new PortMismatchException(
                    "Cannot connect ports of different PortTypes: "
                            + type.name() + " vs. " + rid.type());
        }

        int my_split;

        if (splitter == null) {
            my_split = 0;
        } else {
            my_split = splitter.length;
        }

        ReceivePortIdentifier[] v = new ReceivePortIdentifier[my_split + 1];
        for (int i = 0; i < my_split; i++) {
            if (splitter[i].cpu == rid.cpu && splitter[i].port == rid.port) {
                throw new Error(
                        "Double connection between two ports not allowed");
            }
            v[i] = splitter[i];
        }
        v[my_split] = rid;
        splitter = v;

        ConnectAcker[] s = new ConnectAcker[my_split + 1];
        for (int i = 0; i < my_split; i++) {
            s[i] = syncer[i];
        }
        s[my_split] = new ConnectAcker();
        syncer = s;

        if (connectedCpu == null) {
            connectedCpu = new int[1];
            connectedCpu[0] = rid.cpu;
        } else {
            boolean isDouble = false;
            for (int i = 0; i < connectedCpu.length; i++) {
                if (connectedCpu[i] == rid.cpu) {
                    isDouble = true;
                    break;
                }
            }
            if (!isDouble) {
                int[] c = new int[connectedCpu.length + 1];
                for (int i = 0; i < connectedCpu.length; i++) {
                    c[i] = connectedCpu[i];
                }
                c[connectedCpu.length] = rid.cpu;
                connectedCpu = c;
            }
        }

        return my_split;
    }

    private native void requestGroupID(ConnectAcker sncer);

    public long getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }

    private boolean requiresTotallyOrderedBcast() {
        StaticProperties p = type.properties();

        if (!Ibis.myIbis.requireNumbered()) {
            // We only support totally ordered broadcast when our Ibis has
            // been required to support it.
            return false;
        }
        if (!p.isProp("Communication", "OneToMany")) {
            return false;
        }
        if (!p.isProp("Communication", "ManyToOne")) {
            return false;
        }
        if (!p.isProp("Communication", "Numbered")) {
            return false;
        }
        if (splitter.length <= 1) {
            return false;
        }
        if (group == NO_BCAST_GROUP) {
            System.err.println(this + ": switch on totally ordered bcast");
        }

        return true;
    }

    private boolean requiresFastBcast() {
        StaticProperties p = type.properties();

        if (!p.isProp("Communication", "OneToMany")) {
            return false;
        }
        if (splitter.length < Ibis.myIbis.nrCpus / 2) {
            return false;
        }
        /*
         if (USE_BCAST_ALL ? splitter.length != Ibis.myIbis.nrCpus
                : splitter.length >= Ibis.myIbis.nrCpus - 1) {
             return false;
         }
         */
        if (!USE_BCAST_AT_TWO && splitter.length == 1) {
            return false;
        }
        if (group == NO_BCAST_GROUP) {
            System.err.println(this
                    + ": switch on fast bcast. Consider disabling ordering");
        }

        return true;
    }

    /* This array is queried from native code. LEAVE IT ALONE! */
    private static boolean[] hasHomeBcast = new boolean[1];

    synchronized static void setHomeBcast(int group,
            boolean hasHomeBcastConnection) {
        if (hasHomeBcast.length < group + 1) {
            boolean[] h = new boolean[group + 1];
            for (int i = 0; i < hasHomeBcast.length; i++) {
                h[i] = hasHomeBcast[i];
            }
            hasHomeBcast = h;
        }
        hasHomeBcast[group] = hasHomeBcastConnection;
    }

    protected void checkBcastGroup() throws IOException {
        /*
         * Distinguish two cases where native broadcast is required:
         * 1. Totally ordered broadcast.
         *    This requires:
         *      OneToMany
         *      ManyToOne
         *      Numbered
         *      <STANDOUT>more than one</STANDOUT> connection
         * or
         * 2. Fast native broadcast
         *    This requires:
         *       OneToMany
         *       Connected to a good many of platforms
         */
        if (!USE_BCAST) {
            return;
        }

        boolean total = requiresTotallyOrderedBcast();
        if (!total && !requiresFastBcast()) {
            group = NO_BCAST_GROUP;
            return;
        }

        boolean hasHomeBcastConnection = false;
        for (int i = 0, n = splitter.length; i < n; i++) {
            ReceivePortIdentifier ri = splitter[i];
            if (ri.cpu == Ibis.myIbis.myCpu) {
                hasHomeBcastConnection = true;
                if (false && (!USE_BCAST_ALL || !total)) {
                    group = NO_BCAST_GROUP;
                    return;
                }
            }
        }

        /*
         * This is a bcast group, new or existing.
         */
        if (group == NO_BCAST_GROUP) {
            /* A new bcast group. Apply for a bcast group id with the
             * group id server. */
            ConnectAcker s = new ConnectAcker();
            requestGroupID(s);

            s.waitPolling();
            if (group == NO_BCAST_GROUP) {
                throw new IOException("Retrieval of group ID failed");
            }

            if (DEBUG || Ibis.BCAST_VERBOSE) {
                System.err.println(ident + ": have broadcast group " + group
                        + " receiver(s) ");
                for (int i = 0, n = splitter.length; i < n; i++) {
                    System.err.println("    " + splitter[i]);
                }
            }
        }

        setHomeBcast(group, hasHomeBcastConnection);
    }

    private native void sendBindGroupRequest(int to, byte[] senderId, int grp)
            throws IOException;

    public void connect(ibis.ipl.ReceivePortIdentifier receiver, long timeout)
            throws IOException {

        Ibis.myIbis.lock();
        if (connecting) {
            throw new Error("No concurrent connecting");
        }
        connecting = true;
        try {
            ReceivePortIdentifier rid = (ReceivePortIdentifier) receiver;

            // Add the new receiver to our tables.
            int my_split = addConnection(rid);

            int oldGroup = group;

            checkBcastGroup();

            if (group != NO_BCAST_GROUP && oldGroup == NO_BCAST_GROUP) {
                /* The extant connections are not aware that this is now
                 * a broadcast group. Notify them. */
                for (int i = 0, n = splitter.length; i < n; i++) {
                    ReceivePortIdentifier ri = splitter[i];
                    if (!ri.equals(rid)) {
                        sendBindGroupRequest(ri.cpu, ident.getSerialForm(),
                                group);
                    }
                }
            }

            if (DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Now do native connect call to " + rid + "; me = "
                        + ident);
            }
            syncer[my_split].setAcks(1);
            ibmp_connect(rid.cpu, rid.getSerialForm(), ident.getSerialForm(),
                    syncer[my_split], null, messageCount, group,
                    out.getMsgSeqno());
            if (DEBUG) {
                System.err.println(Ibis.myIbis.myCpu + "-"
                        + Thread.currentThread()
                        + "Done native connect call to " + rid + "; me = "
                        + ident + " syncer[" + my_split + "] "
                        + syncer[my_split]);
            }

            if (!syncer[my_split].waitPolling(timeout)) {
                throw new ConnectionTimedOutException(
                        "No connection to " + rid);
            }
            if (!syncer[my_split].accepted()) {
                throw new ConnectionRefusedException("No connection to " + rid
                        + " syncer[" + my_split + "] " + syncer);
            }

            if (ident.ibis().equals(receiver.ibis())) {
                homeConnection = true;
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("This IS a home connection, my Ibis "
                            + ident.ibis() + " their Ibis " + receiver.ibis());
                }
            } else {
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("This is NOT a home connection, my Ibis "
                            + ident.ibis() + " their Ibis "
                            + receiver.ibis());
                }
            }
        } finally {
            connecting = false;
            Ibis.myIbis.unlock();
        }
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver)
            throws IOException {
        connect(receiver, 0);
    }

    ibis.ipl.WriteMessage cachedMessage() throws IOException {
        if (message == null) {
            message = new WriteMessage(this);
        }

        return message;
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {

        Ibis.myIbis.lock();
        while (aMessageIsAlive) {
            newMessageWaiters++;
            try {
                portIsFree.cv_wait();
            } catch (InterruptedException e) {
                // ignore
            }
            newMessageWaiters--;
        }

        if (false && type.numbered && group == NO_BCAST_GROUP) {
            throw new IOException("Numbered port type but no group?");
        }

        aMessageIsAlive = true;

        if (SERIALIZE_SENDS_PER_CPU) {
            Ibis.myIbis.sendSerializer.lockAll(connectedCpu);
        }

        ibis.ipl.WriteMessage m = cachedMessage();

        Ibis.myIbis.unlock();
        if (DEBUG) {
            System.err.println("Create a new writeMessage SendPort " + this
                    + " serializationType " + type.serializationType
                    + " message " + m);
        }

        return m;
    }

    void finishMessage() throws IOException {
        Ibis.myIbis.checkLockOwned();
        if (SERIALIZE_SENDS_PER_CPU) {
            Ibis.myIbis.sendSerializer.unlockAll(connectedCpu);
        }
    }

    void registerSend() throws IOException {
        messageCount++;
        if (homeConnection) {
            for (int i = 0; i < homeConnectionPolls; i++) {
                while (Ibis.myIbis.pollLocked()) { /* do noting */
                }
            }
        }
    }

    void reset() {
        Ibis.myIbis.checkLockOwned();
        aMessageIsAlive = false;
        if (newMessageWaiters > 0) {
            portIsFree.cv_signal();
        }
    }

    public DynamicProperties properties() {
        return DynamicProperties.NoDynamicProperties;
    }

    public String name() {
        return name;
    }

    public ibis.ipl.SendPortIdentifier identifier() {
        return ident;
    }

    public ibis.ipl.ReceivePortIdentifier[] connectedTo() {
        ibis.ipl.ReceivePortIdentifier[] r
            = new ibis.ipl.ReceivePortIdentifier[splitter.length];
        for (int i = 0; i < splitter.length; i++) {
            r[i] = splitter[i];
        }
        return r;
    }

    public ibis.ipl.ReceivePortIdentifier[] lostConnections() {
        return null; /* Or should this be an empty array or? */
    }

    public void disconnect(ibis.ipl.ReceivePortIdentifier r)
            throws IOException {

        if (splitter == null) {
            throw new IOException("disconnect: no connections");
        }

        Ibis.myIbis.lock();
        try {
            int n = splitter.length;
            boolean found = false;
            for (int i = 0; i < n; i++) {
                ReceivePortIdentifier rid = splitter[i];
                if (rid.equals(r)) {
                    byte[] sf = ident.getSerialForm();
                    ibmp_disconnect(rid.cpu, rid.getSerialForm(), sf, null,
                            messageCount);
                    ReceivePortIdentifier[] v
                        = new ReceivePortIdentifier[n - 1];
                    for (int j = 0; j < n - 1; j++) {
                        v[j] = splitter[j];
                    }
                    if (i < n - 1) {
                        v[i] = splitter[n - 1];
                    }
                    splitter = v;
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("disconnect: no connection to " + r);
            }
        } finally {
            Ibis.myIbis.unlock();
        }
    }

    void closeLocked() throws IOException {
        if (DEBUG) {
            System.out.println(Ibis.myIbis.name()
                    + ": ibis.ipl.SendPort.close " + this + " start");
            Thread.dumpStack();
        }

        try {
            if (splitter != null) {
                byte[] sf = ident.getSerialForm();
                for (int i = 0; i < splitter.length; i++) {
                    ReceivePortIdentifier rid = splitter[i];
                    ibmp_disconnect(rid.cpu, rid.getSerialForm(), sf, null,
                            messageCount);
                }
                splitter = null;
            }
        } finally {
            Ibis.myIbis.unregisterSendPort(this);
        }

        if (DEBUG) {
            System.out.println(Ibis.myIbis.name()
                    + ": ibis.ipl.SendPort.close " + this + " DONE");
        }

    }

    public void close() throws IOException {

        Ibis.myIbis.lock();
        try {
            closeLocked();
        } finally {
            Ibis.myIbis.unlock();
        }
    }

}
