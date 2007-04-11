/* $Id$ */

package ibis.rmi.impl;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisProperties;
import ibis.ipl.MessageUpcall;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.StubNotFoundException;
import ibis.rmi.registry.Registry;
import ibis.rmi.server.ExportException;
import ibis.rmi.server.RemoteRef;
import ibis.rmi.server.RemoteStub;
import ibis.rmi.server.SkeletonNotFoundException;
import ibis.util.IPUtils;
import ibis.util.Log;
import ibis.util.Timer;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import colobus.Colobus;

public final class RTS {

    static TypedProperties properties
            = new TypedProperties(IbisProperties.getConfigurationProperties());

    static final String prefix = "rmi.";

    static final String s_timer = prefix + "timer";

    static final String[] props = { s_timer };

    public static Logger logger = Logger.getLogger(RTS.class.getName());

    /** Sent when a remote invocation resulted in an exception. */
    public final static byte EXCEPTION = 0;

    /** Sent when a remote invocation did not result in an exception. */
    public final static byte RESULT = 1;

    //keys - impl objects, values - skeletons for those objects

    /**
     * Maps objects to their skeletons.
     * In fact, it maps hashcodes to skeletons. The reason for this is that some
     * objects have strange implementations for hashCode()!
     */
    private static HashMap<Integer, Skeleton> skeletons;

    /**
     * Maps objects to their stubs.
     * In fact, it maps hashcodes to stubs. The reason for this is that some
     * objects have strange implementations for hashCode()!
     */
    private static HashMap<Integer, Stub> stubs;

    /**
     * Maps ReceivePortIdentifiers to sendports that have a connection to that
     * receiveport.
     */
    private static HashMap<ReceivePortIdentifier, SendPort> sendports;

    /**
     * Maps a name to a skeleton. We need this for registry lookup.
     */
    static Hashtable<String, Skeleton> nameHash; // No HashMap, this one should be synchronized.

    /**
     * This array maps skeleton ids to the corresponding skeleton.
     */
    static ArrayList<Skeleton> skeletonArray;

    /**
     * Cache receiveports from stubs, hashed with an IbisIdentifier of an Ibis
     * that has a connection to it.
     */
    private static HashMap<IbisIdentifier, ArrayList<ReceivePort>> receiveports;

    static String hostname;

    private static PortType requestPortType;

    private static PortType replyPortType;

    static Ibis ibis;

    private static ibis.ipl.Registry ibisRegistry;

    private static ThreadLocal<String> clientHost;

    private static ReceivePort skeletonReceivePort = null;

    /** A custom latency timer. */
    private static Timer[] timers;

    private static String[] timerId;

    final static boolean enableRMITimer;

    private static double r10(double d) {
        long ld = (long) (d * 10.0);
        return ld / 10.0;
    }

    public synchronized static Timer createRMITimer(String id) {
        int n = timers == null ? 0 : timers.length;
        Timer[] t = new Timer[n + 1];
        String[] s = new String[n + 1];
        for (int i = 0; i < n; i++) {
            t[i] = timers[i];
            s[i] = timerId[i];
        }
        t[n] = Timer.createTimer();
        s[n] = (id == null) ? "" : id;

        timers = t;
        timerId = s;

        return t[n];
    }

    public synchronized static Timer createRMITimer() {
        return createRMITimer("");
    }

    public static void startRMITimer(Timer t) {
        if (enableRMITimer) {
            t.start();
        }
    }

    public static void stopRMITimer(Timer t) {
        if (enableRMITimer) {
            t.stop();
        }
    }

    public static void printRMITimer(Timer t) {
        if (enableRMITimer && t.nrTimes() > 0) {
            System.out.println(ibis + ": RMI timer: " + t.nrTimes() + " total "
                    + r10(t.totalTimeVal()) + " us; av "
                    + r10(t.averageTimeVal()) + " us");
        }
    }

    public static void resetRMITimer(Timer t) {
        if (enableRMITimer) {
            t.reset();
        }
    }

    public synchronized static void printAllRMITimers() {
        if (enableRMITimer && timers != null) {
            if (false) {
                double total = 0.0;
                int n = 0;

                for (int i = 0; i < timers.length; i++) {
                    total += timers[i].totalTimeVal();
                    n += timers[i].nrTimes();
                }
                System.out.println(ibis + ": RMI upcall: " + n + " total "
                        + r10(total) + " us; av " + r10(total / n) + " us");
            } else {
                for (int i = 0; i < timers.length; i++) {
                    if (timers[i].nrTimes() > 0) {
                        System.out.println(ibis + ": RMI " + timerId[i] + ": "
                                + timers[i].nrTimes() + " total "
                                + r10(timers[i].totalTimeVal()) + " us; av "
                                + r10(timers[i].averageTimeVal()) + " us");
                    }
                }
            }
        }
    }

    public static void printResetRMITimer(Timer t) {
        if (enableRMITimer) {
            printRMITimer(t);
            resetRMITimer(t);
        }
    }

    private static class UpcallHandler implements MessageUpcall {
        private static final Colobus colobus = Colobus.getColobus(UpcallHandler.class.getName());

        public void upcall(ReadMessage r) throws IOException {
            long upcallStartHandle = colobus.fireStartEvent("ibis message upcall");
            Skeleton skel;
            int id = r.readInt();

            if (id == -1) {
                String url = r.readString();
                skel = nameHash.get(url);
            } else {
                skel = skeletonArray.get(id);
            }

            int method = r.readInt();
            int stubID = r.readInt();
            try {
                skel.upcall(r, method, stubID);
            } catch (RemoteException e) {
                System.err.println("RMI upcall handler meets " + e);
                e.printStackTrace(System.err);
                WriteMessage w = skel.stubs[stubID].newMessage();
                try {
                    w.writeByte(EXCEPTION);
                    w.writeObject(e);
                    w.finish();
                } catch(IOException e2) {
                    w.finish(e2);
                    colobus.fireStopEvent(upcallStartHandle, "ibis message upcall");
                    throw e2;
                }
                // } catch (RuntimeException et) {
                // System.err.println("RMI error handling meets " + et);
                // et.printStackTrace(System.err);
                // } catch (IOException et) {
                // System.err.println("RMI error handling meets " + et);
                // et.printStackTrace(System.err);
                // throw et;
                // }
            }
            colobus.fireStopEvent(upcallStartHandle, "ibis message upcall");
        }
    }

    private static UpcallHandler upcallHandler;

    static {
        try {
            skeletons = new HashMap<Integer, Skeleton>();
            stubs = new HashMap<Integer, Stub>();
            sendports = new HashMap<ReceivePortIdentifier, SendPort>();
            nameHash = new Hashtable<String, Skeleton>();
            receiveports = new HashMap<IbisIdentifier, ArrayList<ReceivePort>>();
            skeletonArray = new ArrayList<Skeleton>();
            
            Log.initLog4J("ibis.rmi");

            upcallHandler = new UpcallHandler();

            hostname = IPUtils.getLocalHostAddress().getHostName();
            // InetAddress adres = InetAddress.getByName(h);
            // adres = InetAddress.getByName(adres.getHostAddress());
            // hostname = adres.getHostName();

            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": init RMI RTS --> creating ibis");
            }

            IbisCapabilities reqprops = new IbisCapabilities(
                    IbisCapabilities.WORLDMODEL_OPEN);

            requestPortType = new PortType(
                    PortType.SERIALIZATION_OBJECT, PortType.CONNECTION_MANY_TO_ONE,
                    PortType.SERIALIZATION_REPLACER + "=ibis.rmi.impl.RMIReplacer",
                    PortType.COMMUNICATION_RELIABLE, PortType.RECEIVE_AUTO_UPCALLS);

            replyPortType = new PortType(
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.SERIALIZATION_REPLACER + "=ibis.rmi.impl.RMIReplacer",
                    PortType.SERIALIZATION_OBJECT, PortType.COMMUNICATION_RELIABLE, PortType.RECEIVE_EXPLICIT);

            try {
                ibis = IbisFactory.createIbis(reqprops, null, null, requestPortType,
                        replyPortType);
            } catch (IbisCreationFailedException e) {
                System.err.println("Could not find an Ibis that can run this"
                        + " RMI implementation");
                System.exit(1);
            }

            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": ibis created");
            }

            ibisRegistry = ibis.registry();


            enableRMITimer = properties.booleanProperty(s_timer);

            skeletonReceivePort = ibis.createReceivePort(requestPortType, "//"
                    + hostname + "/rmi_skeleton"
                    + (new java.rmi.server.UID()).toString(), upcallHandler);
            skeletonReceivePort.enableConnections();
            skeletonReceivePort.enableMessageUpcalls();

            clientHost = new ThreadLocal<String>();

            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": init RMI RTS done");
            }

        } catch (Exception e) {
            System.err.println(hostname + ": Could not init RMI RTS " + e);
            e.printStackTrace();
            System.exit(1);
            throw new Error(e);
        }

        if (enableRMITimer) {
            System.err.println("Ibis: Enabled RMI timer");
        }

        /****
         * This is only supported in SDK 1.3 and upwards. Comment out
         * if you run an older SDK.
         */
        Runtime.getRuntime().addShutdownHook(
                new Thread("Ibis RMI RTS ShutdownHook") {
                    public void run() {
                        try {
                            if (enableRMITimer) {
                                printAllRMITimers();
                            }
                            ibis.end();
                            // System.err.println("Ended Ibis");
                        } catch (IOException e) {
                            System.err.println("ibis.end throws " + e);
                        }
                    }
                });
        /* End of 1.3-specific code */
    }

    private static String get_skel_name(Class c) {
        String class_name = c.getName();
        Package pkg = c.getPackage();
        String package_name = pkg != null ? pkg.getName() : null;
        if (package_name == null || package_name.equals("")) {
            return "rmi_skeleton_" + class_name;
        }
        return package_name + ".rmi_skeleton_"
                + class_name.substring(class_name.lastIndexOf('.') + 1);
    }

    private static String get_stub_name(Class c) {
        String class_name = c.getName();
        Package pkg = c.getPackage();
        String package_name = pkg != null ? pkg.getName() : null;
        if (package_name == null || package_name.equals("")) {
            return "rmi_stub_" + class_name;
        }
        return package_name + ".rmi_stub_"
                + class_name.substring(class_name.lastIndexOf('.') + 1);
    }

    private synchronized static Skeleton createSkel(Remote obj)
            throws SkeletonNotFoundException {
        try {
            Skeleton skel;
            Class c = obj.getClass();
            String skel_name = get_skel_name(c);
            // System.out.println("skel_name = " + skel_name);

            // Use the classloader of the original class!
            // Fix is by Fabrice Huet.
            ClassLoader loader = c.getClassLoader();

            Class skel_c = null;
            if (loader != null) {
                skel_c = loader.loadClass(skel_name);
            } else {
                skel_c = Class.forName(skel_name);
            }
            skel = (Skeleton) skel_c.newInstance();

            int skelId = skeletonArray.size();
            skeletonArray.add(skel);
            skel.init(skelId, obj);

            skeletons.put(new Integer(System.identityHashCode(obj)), skel);

            return skel;
        } catch (ClassNotFoundException ec) {
            throw new SkeletonNotFoundException("Cannot find skeleton class",
                    ec);
        } catch (InstantiationException en) {
            throw new SkeletonNotFoundException("Cannot instantiate skeleton",
                    en);
        } catch (IllegalAccessException el) {
            throw new SkeletonNotFoundException("Cannot access skeleton", el);
        }
    }

    public static RemoteStub exportObject(Remote obj, RemoteRef r)
            throws RemoteException {
        Stub stub;
        Class c = obj.getClass();
        Skeleton skel;
        String classname = c.getName();

        synchronized (RTS.class) {
            skel = skeletons.get(new Integer(System.identityHashCode(obj)));
        }
        if (skel == null) {
            //create a skeleton
            skel = createSkel(obj);
        } else {
            throw new ExportException("object already exported");
        }

        //create a stub
        // Use the classloader of the original class!
        // Fix is by Fabrice Huet.
        try {
            ClassLoader loader = obj.getClass().getClassLoader();

            Class stub_c = null;
            if (loader != null) {
                stub_c = loader.loadClass(get_stub_name(c));
            } else {
                stub_c = Class.forName(get_stub_name(c));
            }
            stub = (Stub) stub_c.newInstance();

            stub.init(null, null, 0, skel.skeletonId,
                    skeletonReceivePort.receivePortIdentifier(), false, r);

        } catch (ClassNotFoundException e) {
            throw new StubNotFoundException("class " + get_stub_name(c)
                    + " not found", e);
        } catch (InstantiationException e2) {
            throw new StubNotFoundException("could not instantiate class "
                    + get_stub_name(c), e2);
        } catch (IllegalAccessException e3) {
            throw new StubNotFoundException("illegal access of class "
                    + get_stub_name(c), e3);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Created stub of type rmi_stub_" + classname);
        }

        stubs.put(new Integer(System.identityHashCode(obj)), stub);

        return stub;
    }

    public static synchronized Stub getStub(Object o) {
        return stubs.get(new Integer(System.identityHashCode(o)));
    }

    static synchronized SendPort getSkeletonSendPort(
            ReceivePortIdentifier rpi) throws IOException {
        SendPort s = sendports.get(rpi);
        if (s == null) {
            s = ibis.createSendPort(replyPortType);
            s.connect(rpi);
            sendports.put(rpi, s);
            if (logger.isDebugEnabled()) {
                logger.debug(hostname
                        + ": New skeleton sendport for receiport: " + rpi);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(hostname
                        + ": Reuse skeleton sendport for receiport: " + rpi);
            }
        }
        return s;
    }

    static synchronized SendPort getStubSendPort(
            ReceivePortIdentifier rpi) throws IOException {
        SendPort s = sendports.get(rpi);
        if (s == null) {
            s = ibis.createSendPort(requestPortType);
            s.connect(rpi);
            sendports.put(rpi, s);
            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": New stub sendport for receiport: "
                        + rpi);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": Reuse stub sendport for receiport: "
                        + rpi);
            }
        }
        return s;
    }

    static synchronized ReceivePort getStubReceivePort(IbisIdentifier id)
            throws IOException {
        ArrayList<ReceivePort> a = receiveports.get(id);
        ReceivePort r;

        if (logger.isDebugEnabled()) {
            logger.debug("receiveport wanted for ibis " + id);
        }

        if (a == null || a.size() == 0) {

            r = ibis.createReceivePort(replyPortType, "//" + hostname + "/rmi_stub"
                    + (new java.rmi.server.UID()).toString());
            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": New receiveport: " + r.receivePortIdentifier());
            }
            r.enableConnections();
        } else {
            r = a.remove(a.size() - 1);
            if (logger.isDebugEnabled()) {
                logger.debug(hostname + ": Reuse receiveport: " + r.receivePortIdentifier());
            }
        }
        return r;
    }

    static synchronized void putStubReceivePort(ReceivePort r,
            IbisIdentifier id) {
        if (logger.isDebugEnabled()) {
            logger.debug("receiveport " + r + " returned for ibis " + id);
        }
        ArrayList<ReceivePort> a = receiveports.get(id);
        if (a == null) {
            a = new ArrayList<ReceivePort>();
            receiveports.put(id, a);
        }
        a.add(r);
    }

    public static void createRegistry(int port, Registry reg)
            throws RemoteException {
        String name = "__REGISTRY__" + hostname + ":" + port;
        ReceivePort p;
        try {
            p =ibis.createReceivePort(requestPortType, name, upcallHandler);
        } catch (IOException e) {
            throw new RemoteException("Could not create receive port", e);
        }
        Stub stub = (Stub) getStub(reg);
        stub.skeletonPortId = p.receivePortIdentifier();
        p.enableConnections();
        p.enableMessageUpcalls();
        IbisIdentifier bbb;
        try {
            bbb = ibisRegistry.elect(name);
        } catch(Exception e) {
            throw new RemoteException("Could not elect", e);
        }
        if (! bbb.equals(ibis.ibisIdentifier())) {
            throw new RemoteException(
                    "there already is a registry running on port " + port);
        }
        Skeleton skel = skeletons.get(new Integer(System.identityHashCode(reg)));
        nameHash.put(name, skel);
        if (logger.isDebugEnabled()) {
            logger.debug("Created registry " + name);
        }
    }

    public static Registry lookupRegistry(String host, int port)
            throws IOException {

        if (host == null || host.equals("")) {
            host = hostname;
        }

        String name = "__REGISTRY__" + host + ":" + port;
        Stub result;
        SendPort s = ibis.createSendPort(requestPortType);

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Trying to lookup registry " + name);
        }

        IbisIdentifier owner = ibisRegistry.getElectionResult(name);

        ReceivePortIdentifier dest = s.connect(owner, name);

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Got sendport");
        }

        ReceivePort r = getStubReceivePort(dest.ibisIdentifier());

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Created receiveport for stub  -> id = "
                    + r.receivePortIdentifier());
        }

        WriteMessage wm = s.newMessage();

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Created new WriteMessage");
        }

        wm.writeInt(-1);                // No skel Id known yet
        wm.writeString(name);           // name for skeleton
        wm.writeInt(-1);                // initialization method
        wm.writeInt(0);                 // no stubID yet
        wm.writeObject(r.receivePortIdentifier()); // my receive port
        wm.finish();

        s.disconnect(dest);
        s = getStubSendPort(dest);

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Sent new WriteMessage");
        }

        ReadMessage rm = r.receive();

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Received readMessage");
        }

        int stubID = rm.readInt();      // My stub id

        try {
            result = (Stub) rm.readObject();    // And the stub
        } catch (ClassNotFoundException e) {
            throw new RemoteException("ClassNotFoundException ", e);
        }
        rm.finish();

        result.init(s, r, stubID, result.skeletonId, dest, true, null);

        if (logger.isDebugEnabled()) {
            logger.debug(hostname + ": Found object " + name);
        }
        return (Registry) result;
    }

    public static String getHostname() {
        return hostname;
    }

    public static void setClientHost(String s) {
        clientHost.set(s);
    }

    public static String getClientHost() {
        String o = clientHost.get();
        if (o == null) {
            return "UNKNOWN_HOST";
        }
        return o;
    }

}
