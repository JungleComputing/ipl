/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.StaticProperties;
import ibis.util.ConditionVariable;
import ibis.util.Monitor;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * messagePassing Ibis.
 * This Ibis interfaces between the Ibis ipl and native code that performs
 * message passing. Two implementation examples are panda and MPI.
 */
public class Ibis extends ibis.ipl.Ibis {

    static final boolean DEBUG = TypedProperties.booleanProperty(
            MPProps.s_debug);

    static final boolean DEBUG_RUTGER = false;

    static final boolean CHECK_LOCKS = DEBUG;

    static final boolean STATISTICS = true;

    static final boolean BCAST_VERBOSE = false;

    private boolean i_joined = false;

    static Ibis myIbis;

    int nrCpus;

    int myCpu;

    private IbisIdentifier ident;

    private Registry registry;

    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private Vector ibisNameService = new Vector();

    private Poll rcve_poll;

    private Monitor monitor = new Monitor();

    ConditionVariable createCV() {
        // return new ConditionVariable(this);
        return monitor.createCV();
    }

    private IbisWorld world;

    private PortHash[] sendPorts;

    private PortHash rcvePorts;

    private PortHash groupSendPorts;

    private PortHash groupRcvePorts;

    SendSerializer sendSerializer;

    int sendPort;

    int receivePort;

    protected native String[] ibmp_init(String[] args);

    protected native void ibmp_start();

    protected native void ibmp_end();

    private boolean requireNumbered;

    Ibis() throws IbisException {

        if (DEBUG) {
            System.err.println("Turn on Ibis.DEBUG");
        }

    }

    boolean broadcastSupported() {
        return false;
    }

    protected ibis.ipl.PortType newPortType(String nm, StaticProperties p)
            throws IbisException {
        lock();
        PortType tp = new PortType(nm, p);
        portTypeList.put(nm, tp);
        unlock();

        return tp;
    }

    public ibis.ipl.Registry registry() {
        return registry;
    }

    ReceivePortNameServer createReceivePortNameServer() throws IOException {
        return new ReceivePortNameServer();
    }

    ReceivePortNameServerClient createReceivePortNameServerClient() {
        return new ReceivePortNameServerClient();
    }

    boolean getInputStreamMsg(int tags[]) {
        return ByteInputStream.getInputStreamMsg(tags);
    }

    public StaticProperties properties() {
        return staticProperties(implName);
    }

    public ibis.ipl.IbisIdentifier identifier() {
        return ident;
    }

    private native void send_join(int to, byte[] serialForm);

    private native void send_leave(int to, byte[] serialForm);

    /* Called from native */
    private void join_upcall(byte[] serialForm) throws IOException {
        checkLockOwned();

        IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
        if (DEBUG) {
            System.err.println("Receive join message " + id.name()
                    + "; now world = " + world + "; serialForm["
                    + serialForm.length + "] = " + serialForm);
        }
        ibisNameService.add(id);
        world.join(id);
    }

    /* Called from native */
    private void leave_upcall(byte[] serialForm) {
        checkLockOwned();
        try {
            IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
            ibisNameService.remove(id);
            world.leave(id);
        } catch (IOException e) {
            // just ignore the leave call, then
        }
    }

    public void joined(ibis.ipl.IbisIdentifier id) {
        if (DEBUG) {
            System.err.println(myCpu + ": An Ibis.join call for " + id);
            Thread.dumpStack();
        }
        if (resizeHandler != null) {
            resizeHandler.joined(id);
            if (!i_joined && id.equals(ident)) {
                synchronized (this) {
                    i_joined = true;
                    notifyAll();
                }
            }
        }
    }

    public void left(ibis.ipl.IbisIdentifier id) {
        if (resizeHandler != null) {
            resizeHandler.left(id);
        }
    }

    public void died(ibis.ipl.IbisIdentifier[] ids) {
        if (resizeHandler != null) {
            for (int i = 0; i < ids.length; i++) {
                resizeHandler.died(ids[i]);
            }
        }
    }

    public void mustLeave(ibis.ipl.IbisIdentifier[] ids) {
        if (resizeHandler != null) {
            resizeHandler.mustLeave(ids);
        }
    }

    private static void dumpStack() {
        new Throwable().printStackTrace();
    }

    protected void init() throws IbisException, IOException {

        if (myIbis != null) {
            throw new IbisException("Only one Ibis allowed");
        }
        myIbis = this;

        rcve_poll = new Poll();

        registry = new Registry();

        requireNumbered = combinedprops.isProp("Communication", "Numbered");

        /* Fills in:
         nrCpus;
         myCpu;
         */
        ibmp_init(null);
        if (DEBUG) {
            System.err.println(Thread.currentThread() + "ibp lives...");
            System.err.println(Thread.currentThread() + "Ibis.poll = "
                    + rcve_poll);
        }

        world = new IbisWorld();

        /*
         * This is an 1.3 feature; cannot we use it please?
        Runtime.getRuntime().addShutdownHook(
                new Thread("MP Ibis ShutdownHook") {
                    public void run() {
                        try {
                            end();
                        } catch (IOException e) {
                            System.err.println(
                                    "Ibis ShutdownHook catches " + e);
                        }
                    }
                });
        */

        ident = new IbisIdentifier(name + "@" + myCpu, myCpu);

        sendPorts = new PortHash[myIbis.nrCpus];
        for (int i = 0; i < myIbis.nrCpus; i++) {
            sendPorts[i] = new PortHash();
        }
        rcvePorts = new PortHash();
        groupSendPorts = new PortHash();
        groupRcvePorts = new PortHash();

        ibmp_start();
        rcve_poll.wakeup();

        sendSerializer = new SendSerializer();

        registry.init();
        if (DEBUG) {
            System.err.println(Thread.currentThread() + "Registry lives...");
        }

        myIbis.lock();
        try {
            if (DEBUG) {
                System.err.println("myCpu " + myCpu + " nrCpus " + nrCpus
                        + " world " + world);
            }

            for (int i = 0; i < nrCpus; i++) {
                if (i != myCpu) {
                    if (DEBUG) {
                        System.err.println("Send join message to " + i);
                    }
                    send_join(i, ident.getSerialForm());
                }
            }

            ibisNameService.add(ident);
            world.join(ident);
        } finally {
            myIbis.unlock();
        }

        if (DEBUG) {
            System.err.println(Thread.currentThread() + "Ibis lives... "
                    + ident);
        }
    }

    public void enableResizeUpcalls() {
        myIbis.lock();
        world.open();
        myIbis.unlock();

        if (resizeHandler != null) {
            synchronized (this) {
                while (!i_joined) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }

    public void disableResizeUpcalls() {
        myIbis.lock();
        world.close();
        myIbis.unlock();
    }

    final void waitPolling(PollClient client, long timeout, int preempt)
            throws IOException {
        rcve_poll.waitPolling(client, timeout, preempt);
    }

    private native long currentTime();

    private native double t2d(long t);

    final void lock() {
        monitor.lock();
    }

    final void unlock() {
        monitor.unlock();
    }

    final void checkLockOwned() {
        if (CHECK_LOCKS) {
            monitor.checkImOwner();
        }
    }

    final void checkLockNotOwned() {
        if (CHECK_LOCKS) {
            monitor.checkImNotOwner();
        }
    }

    IbisIdentifier lookupIbis(String nm, int cpu) throws IOException {
        if (myIbis.ident.name().equals(nm)) {
            return myIbis.ident;
        }

        for (int i = 0; i < ibisNameService.size(); i++) {
            IbisIdentifier id = (IbisIdentifier) ibisNameService.get(i);
            if (id.name().equals(nm)) {
                return id;
            }
        }

        return null;
    }

    IbisIdentifier lookupIbis(byte[] serialForm) throws IOException {

        IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
        if (lookupIbis(id.name(), id.getCPU()) == null) {
            ibisNameService.add(id);
        }

        return id;
    }

    PortType getPortTypeLocked(String nm) {
        return (PortType) portTypeList.get(nm);
    }

    public ibis.ipl.PortType getPortType(String nm) {
        myIbis.lock();
        PortType tp = getPortTypeLocked(nm);
        myIbis.unlock();

        return tp;
    }

    void bindSendPort(ShadowSendPort p, int cpu, int port) {
        checkLockOwned();
        sendPorts[cpu].bind(port, p);
    }

    void bindReceivePort(ReceivePort p, int port) {
        checkLockOwned();
        rcvePorts.bind(port, p);
    }

    ShadowSendPort lookupSendPort(int cpu, int port) {
        checkLockOwned();
        return (ShadowSendPort) sendPorts[cpu].lookup(port);
    }

    void unbindSendPort(int cpu, int port) {
        checkLockOwned();
        sendPorts[cpu].unbind(port);
    }

    ReceivePort lookupReceivePort(int port) {
        checkLockOwned();
        return (ReceivePort) rcvePorts.lookup(port);
    }

    void bindGroup(int group, ReceivePort rp, ShadowSendPort sp) {
        if (BCAST_VERBOSE) {
            System.err.println("Bind group " + group + " to (" + rp + "," + sp
                    + ")");
        }
        checkLockOwned();
        ReceivePort[] rps = (ReceivePort[]) groupRcvePorts.lookup(group);
        if (rps == null) {
            rps = new ReceivePort[1];
        } else {
            ReceivePort[] a = new ReceivePort[rps.length + 1];
            for (int i = 0; i < rps.length; i++) {
                a[i] = rps[i];
            }
        }
        rps[rps.length - 1] = rp;
        groupRcvePorts.bind(group, rps);

        ShadowSendPort[] sps = (ShadowSendPort[]) groupSendPorts.lookup(group);
        if (sps == null) {
            sps = new ShadowSendPort[1];
        } else {
            ShadowSendPort[] a = new ShadowSendPort[sps.length + 1];
            for (int i = 0; i < sps.length; i++) {
                a[i] = sps[i];
            }
        }
        sps[sps.length - 1] = sp;
        groupSendPorts.bind(group, sps);
    }

    void unbindGroup(int group, ReceivePort rp, ShadowSendPort sp) {
        if (BCAST_VERBOSE) {
            System.err.println("Unbind group " + group + " to (" + rp + ","
                    + sp + ")");
        }
        boolean found;
        ReceivePort[] rps = lookupGroupReceivePort(group);
        found = false;
        for (int i = 0; i < rps.length; i++) {
            if (rps[i].equals(rp)) {
                rps[i] = null;
                found = true;
                break;
            }
        }
        if (!found) {
            throw new Error("Try to unbind nonbound receive port; group "
                    + group);
        }

        found = false;
        ShadowSendPort[] sps = lookupGroupSendPort(group);
        for (int i = 0; i < sps.length; i++) {
            if (sps[i].equals(sp)) {
                sps[i] = null;
                found = true;
                break;
            }
        }
        if (!found) {
            throw new Error("Try to unbind nonbound shadow send port; group "
                    + group);
        }
    }

    boolean requireNumbered() {
        return requireNumbered;
    }

    private ReceivePort[] lookupGroupReceivePort(int group) {
        checkLockOwned();
        return (ReceivePort[]) groupRcvePorts.lookup(group);
    }

    private ShadowSendPort[] lookupGroupSendPort(int group) {
        checkLockOwned();
        return (ShadowSendPort[]) groupSendPorts.lookup(group);
    }

    int[] inputStreamMsgTags = new int[7];

    final boolean inputStreamPoll() throws IOException {
        if (getInputStreamMsg(inputStreamMsgTags)) {
            receiveFragment(inputStreamMsgTags[0], inputStreamMsgTags[1],
                    inputStreamMsgTags[2], inputStreamMsgTags[3],
                    inputStreamMsgTags[4], inputStreamMsgTags[5],
                    inputStreamMsgTags[6]);
            return true;
        }

        return false;
    }

    private void receiveFragment(int src_cpu, int src_port, int dest_port,
            int msgHandle, int msgSize, int msgSeqno, int group)
            throws IOException {
        checkLockOwned();

        if (Ibis.DEBUG_RUTGER) {
            System.err.println(Thread.currentThread()
                    + "receiveFragment, group " + group);
        }

        if (group != SendPort.NO_BCAST_GROUP) {
            ReceivePort[] port = lookupGroupReceivePort(group);
            if (port == null) {
                if (DEBUG) {
                    System.err.println(
                            "Finish&clear this bcast fragment. It is not for us.");
                }
                ByteInputStream.resetMsg(msgHandle);
                return;
            }

            ShadowSendPort[] origin = lookupGroupSendPort(group);
            if (src_cpu == myCpu) {
                // Panda gives you pointers into the buffers that you handed
                // it for sending off. Make a copy here.
                // msgHandle = copyNativeMessage(msgHandle);
                // System.err.println("copy the Group message in native code");
            }

            if (Ibis.DEBUG_RUTGER) {
                System.err.println(Thread.currentThread()
                        + ": receiveFragment/group port " + port
                        + " group origin " + origin);
            }

            for (int i = 0; i < port.length - 1; i++) {
                if (origin[i] == null) {
                    throw new IOException(
                            "Receive message from sendport we're not connected to");
                }
                if (!origin[i].acceptableSeqno(msgSeqno)) {
                    if (DEBUG) {
                        System.err.println(
                                "Ignore bcast message that arrives early");
                    }
                } else {
                    port[i].receiveFragment(origin[i],
                            ByteInputStream.cloneMsg(msgHandle), msgSize,
                            msgSeqno);
                }
            }
            int x = port.length - 1;
            if (origin[x] == null) {
                throw new IOException(
                        "Receive message from sendport we're not connected to");
            }
            if (!origin[x].acceptableSeqno(msgSeqno)) {
                if (DEBUG) {
                    System.err.println(
                            "Ignore bcast message that arrives early");
                }
            } else {
                port[x].receiveFragment(origin[x], msgHandle, msgSize,
                                msgSeqno);
            }

        } else {
            ReceivePort port = lookupReceivePort(dest_port);
            ShadowSendPort origin = lookupSendPort(src_cpu, src_port);
            if (Ibis.DEBUG_RUTGER) {
                System.err.println(Thread.currentThread()
                        + ": receiveFragment port " + port + " origin "
                        + origin);
            }

            if (origin == null) {
                throw new IOException(
                        "Receive message from sendport we're not connected to");
            }

            port.receiveFragment(origin, msgHandle, msgSize, msgSeqno);
        }
    }

    boolean pollLocked() throws IOException {
        return rcve_poll.poll();
    }

    public void poll() throws IOException {
        try {
            myIbis.lock();
            pollLocked();
        } finally {
            myIbis.unlock();
        }
    }

    public static void resetStats() {
        myIbis.rcve_poll.reset_stats();
    }

    private native void ibmp_report(int out);

    private void report() {
        ConditionVariable.report(System.out);
        ReceivePort.report(System.out);
        if (rcve_poll != null) {
            rcve_poll.report(System.out);
        }
        // IbisSerializationOutputStream.statistics();
        ibmp_report(1);
    }

    private boolean ended = false;

    private class ReceivePortShutter extends Syncer {

        public boolean satisfied() {
            return receivePortList == null;
        }

    }

    private SendPort sendPortList = null;

    ReceivePort receivePortList = null;

    void registerSendPort(SendPort p) {
        checkLockOwned();
        p.next = sendPortList;
        sendPortList = p;
    }

    void registerReceivePort(ReceivePort p) {
        checkLockOwned();
        p.next = receivePortList;
        receivePortList = p;
    }

    void unregisterSendPort(SendPort p) {
        checkLockOwned();
        SendPort prev = null;
        SendPort scan = sendPortList;
        while (scan != null && scan != p) {
            prev = scan;
            scan = scan.next;
        }
        if (scan == null) {
            throw new Error("Unregister a SendPort " + p
                    + " that is not registered");
        }
        if (prev == null) {
            sendPortList = p.next;
        } else {
            prev.next = p.next;
        }
    }

    void unregisterReceivePort(ReceivePort p) {
        checkLockOwned();
        ReceivePort prev = null;
        ReceivePort scan = receivePortList;
        while (scan != null && scan != p) {
            prev = scan;
            scan = scan.next;
        }
        if (scan == null) {
            throw new Error("Unregister a ReceivePort " + p
                    + " that is not registered");
        }
        if (prev == null) {
            receivePortList = p.next;
        } else {
            prev.next = p.next;
        }
    }

    public void end() throws IOException {

        if (myIbis != null) {
            myIbis.lock();
            try {
                if (ended || registry == null) {
                    return;
                }
                ended = true;
            } finally {
                myIbis.unlock();
            }
            registry.end();

            myIbis.lock();
            try {
                while (sendPortList != null) {
                    sendPortList.closeLocked();
                }

                ReceivePortShutter receivePortShutter = new ReceivePortShutter();

                receivePortShutter.waitPolling(1000);

                while (receivePortList != null) {
                    receivePortList.closeLocked(true);
                }

                ibisNameService.remove(ident);
                try {
                    byte[] sf = ident.getSerialForm();
                    for (int i = 0; i < nrCpus; i++) {
                        if (i != myCpu) {
                            send_leave(i, sf);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Cannot send leave msg");
                }
                world.leave(ident);

                // report();

                // ReceivePort.end();

                ibmp_end();

            } finally {
                myIbis.unlock();
            }
        }
    }
}
