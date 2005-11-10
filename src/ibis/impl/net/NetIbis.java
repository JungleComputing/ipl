/* $Id$ */

package ibis.impl.net;

import ibis.connect.IbisSocketFactory;
import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;
import ibis.util.Timer;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Provides a generic {@link Ibis} implementation for pluggable network driver
 * support.
 */
public final class NetIbis extends Ibis {

    public static final boolean DEBUG_RUTGER = false;

    static final String prefix = "ibis.net.";

    public static final String alloc_stats = prefix + "allocator.stats";

    public static final String alloc_thres = prefix + "allocator.bigThr";

    public static final String input_exc_v = prefix + "input.exc.verbose";

    public static final String poll_single_dyn = prefix
            + "poller.singleton.dynamic";

    public static final String poll_single_v = prefix
            + "poller.singleton.verbose";

    public static final String cfg_filenm = prefix + "config.filename";

    public static final String cfg_file = prefix + "config.file";

    public static final String port_single = prefix + "porttype.singleton";

    public static final String multi_gen = prefix + "multi.gen";

    public static final String port_yield = prefix + "port.yield";

    private static final String[] properties = { alloc_stats, alloc_thres,
            input_exc_v, poll_single_dyn, poll_single_v,
            cfg_filenm, cfg_file, port_single, port_yield };

    private static final String[] excludes = { prefix + "bytes.",
            prefix + "gm.", prefix + "rel.", prefix + "tcp_blk." };

    /**
     * The compiler name.
     */
    private static final String COMPILER
        = java.lang.System.getProperty("java.lang.compiler");

    /**
     * The Ibis socket factory
     */
    public static IbisSocketFactory socketFactory
        = IbisSocketFactory.getFactory();

    /**
     * The driver loading mode.
     *
     * <DL>
     * <DT><CODE>true</CODE><DD>known drivers are statically loaded at
     * initialization time.
     * <DT><CODE>false</CODE><DD>every driver is loaded dynamically.
     * </DL>
     * Note: if {@linkplain #COMPILER} is set and equals <code>manta</code>, this
     * flag is automatically set to <code>true</code>
     */
    private static final boolean staticDriverLoading = (COMPILER != null
            && COMPILER.equals("manta")) || false;

    /**
     * The cache for previously created port types.
     */
    private Hashtable portTypeTable = new Hashtable();

    /**
     * The cache for previously loaded drivers.
     */
    private Hashtable driverTable = new Hashtable();

    /**
     * This {@link ibis.impl.net.NetIbis} instance identifier for the
     * <I>name server</I>.
     */
    private NetIbisIdentifier identifier = null;

    private boolean i_joined = false;

    /**
     * This {@link ibis.impl.net.NetIbis} instance <I>name server</I> client.
     */
    protected NameServer nameServer = null;

    /**
     * The openness of our <I>world</I>.
     * <DL>
     * <DT><CODE>true</CODE><DD>Any {@link ibis.impl.net.NetIbis} instance can
     * connect to this {@link ibis.impl.net.NetIbis} instance
     * <DT><CODE>false</CODE><DD>No {@link ibis.impl.net.NetIbis} instance can
     * connect to this {@link ibis.impl.net.NetIbis} instance
     * </DL>
     */
    private boolean open = false;

    // private int poolSize = 0;

    /**
     * The {@link ibis.impl.net.NetIbis} instances that attempted to join our
     * nameServer pool while our world was {@linkplain #open closed}.
     */
    private Vector joinedIbises = new Vector();

    /**
     * The {@link ibis.impl.net.NetIbis} instances that attempted to leave our
     * nameServer pool while our world was {@linkplain #open closed}.
     */
    private Vector leftIbises = new Vector();

    /**
     * The {@link ibis.impl.net.NetIbis} instances that died while our world
     * was {@linkplain #open closed}.
     */
    private Vector diedIbises = new Vector();

    /**
     * Make end() reentrant
     */
    private boolean ended = false;

    /**
     * Maintain linked lists of our send and receive ports so they can
     * be forced-close at Ibis end.
     */
    private NetSendPort sendPortList = null;

    private NetReceivePort receivePortList = null;

    /**
     * The {@link ibis.impl.net.NetIbis} {@linkplain
     * ibis.impl.net.NetBank bank}.
     *
     * This {@linkplain ibis.impl.net.NetBank bank} can be used as general
     * purpose and relatively safe repository for global object
     * instances.
     */
    private NetBank bank = new NetBank();

    public static final ibis.util.PoolInfo poolInfo;
    static {
        ibis.util.PoolInfo p = null;
        try {
            p = ibis.util.PoolInfo.createPoolInfo();
        } catch (RuntimeException e) {
            // OK, no pool, so this better not be closed world
            InetAddress addr = IPUtils.getLocalHostAddress();
            hostName = addr.getHostName();
        }
        poolInfo = p;
        if (poolInfo != null) {
            hostName = poolInfo.hostName();
        }
    }

    private int closedPoolRank = -1;

    private int closedPoolSize = 0;

    /**
     * The master {@link Ibis} instance for this process.
     */
    static NetIbis globalIbis;

    private static String hostName;

    public static String hostName() {
        return hostName;
    }

    private static final Timer nowTimer = Timer.createTimer();

    private static final long t_start = nowTimer.currentTimeNanos();

    public static float now() {
        return (nowTimer.currentTimeNanos() - t_start) / 1.0E09F;
    }

    /**
     * Thread.yield() may be broken. In that case use sleep(0, 1);
     */
    private final static boolean USE_SLEEP_FOR_YIELD = false;

    /**
     * Thread.yield() may be broken. In that case use sleep(0, 1);
     */
    public static void yield() {
        if (USE_SLEEP_FOR_YIELD) {
            try {
                Thread.sleep(0, 1);
            } catch (InterruptedException e) {
                // If interrupted, wake up
            }
        } else {
            Thread.yield();
        }
    }

    /**
     * Default constructor.
     *
     * May load compile-time known drivers.
     *
     * @exception IbisConfigurationException if the requested driver class
     * 		could not be loaded or the requested driver instance
     * 		could not be initialized.
     */
    public NetIbis() {
        TypedProperties.checkProperties(prefix, properties, excludes);
        if (globalIbis == null) {
            globalIbis = this;
        }

        if (false) {
            // This leads to port.close() throws ConcurrentModificationExceptions
            Runtime.getRuntime().addShutdownHook(
                    new Thread("NetIbis ShutdownHook") {
                        public void run() {
                            try {
                                end();
                            } catch (IOException e) {
                                // Leave it be, if it does not want
                            }
                        }
                    });
        }

        if (staticDriverLoading) {
            String[] drivers = { "gen", "bytes", "id", "pipe", "udp", "muxer",
                    "tcp", "tcp_blk", "rel"
                    // , "gm"
            };
            for (int i = 0; i < drivers.length; i++) {
                try {
                    Class clazz = Class.forName("ibis.impl.net." + drivers[i]
                            + ".Driver");
                    NetDriver d = (NetDriver) clazz.newInstance();
                    d.setIbis(this);
                } catch (java.lang.Exception e) {
                    throw new IbisConfigurationException(
                            "Cannot instantiate class " + drivers[i], e);
                }
            }
        }

        if (poolInfo == null) {
            closedPoolRank = joinedIbises.indexOf(identifier());
        } else {
            closedPoolSize = poolInfo.size();
            closedPoolRank = poolInfo.rank();
            // System.err.println("I am node " + closedPoolRank + " out of " + closedPoolSize);
        }
    }

    /**
     * Returns the {@link ibis.impl.net.NetBank}.
     *
     * @return The {@link ibis.impl.net.NetBank}.
     */
    public NetBank getBank() {
        return bank;
    }

    /**
     * Returns an instance of the driver corresponding to the given name.
     *
     * If the driver has not been loaded, it is instanciated on the fly. The
     * driver's name is the suffix to append to the NetIbis package to get
     * access to the driver's package. The driver's package must contain a
     * class named <CODE>Driver</CODE> which extends {@link
     * ibis.impl.net.NetDriver}.
     *
     * @param name the driver's name.
     * @return The driver instance.
     * @exception IbisConfigurationException if the requested driver class
     * 		could not be loaded or the requested driver instance
     * 		could not be initialized.
     */
    /* This must be synchronized: two threads may attempt to create a
     * port concurrently; if not synch, the driver may be created twice
     *							RFHH
     */
    synchronized public NetDriver getDriver(String name) {
        NetDriver driver = (NetDriver) driverTable.get(name);

        if (driver == null) {
            String clsName = getClass().getPackage().getName() + "." + name
                    + ".Driver";
            try {
                Class cls = Class.forName(clsName);
                Class[] clsArray = { getClass() };
                Constructor cons = cls.getConstructor(clsArray);
                Object[] objArray = { this };

                driver = (NetDriver) cons.newInstance(objArray);
            } catch (Exception e) {
                throw new IbisConfigurationException(
                        "Cannot create NetIbis driver " + clsName, e);
            }

            driverTable.put(name, driver);
        }

        return driver;
    }

    /**
     * Extracts a driver stack from the properties, and create a new
     * property set including this driver stack.
     * @param sp the properties.
     * @return the resulting properties.
     */
    private StaticProperties extractDriverStack(StaticProperties sp) {
        StaticProperties s = (StaticProperties) sp.clone();
        String serialization = s.find("Serialization");
        String path = "/";
        if (serialization != null && !serialization.equals("byte")) {
            // Add serialization driver
            String top = "ser";
            try {
                s.add(path + ":Driver", top);
            } catch (IbisRuntimeException e) {
                return sp;
            }
            path = path + top;
        }
        String driver = sp.find("IbisName");
        if (driver == null) {
            StaticProperties p = properties();
            if (p != null) {
                driver = p.find("IbisName");
            }
        }
        if (driver != null && driver.startsWith("net.")) {
            driver = driver.substring("net.".length());
            while (true) {
                int dot = driver.indexOf('.');
                int end = dot;
                if (end == -1) {
                    end = driver.length();
                }
                String top = driver.substring(0, end);
                // System.err.println("Now register static property \"" + (path + ":Driver") + "\" as \"" + top + "\"");
                try {
                    s.add(path + ":Driver", top);
                } catch (IbisRuntimeException e) {
                    return sp;
                }
                if (dot == -1) {
                    break;
                }
                if (path.equals("/")) {
                    path = path + top;
                } else {
                    path = path + "/" + top;
                }
                driver = driver.substring(dot + 1);
            }
        }
        return s;
    }

    /**
     * Creates a {@linkplain PortType port type} from a name and a set of
     * {@linkplain StaticProperties properties}.
     *
     * @param name the name of the type.
     * @param sp   the properties of the type.
     * @return     The port type.
     * @exception  IbisException if the name server refused to register the
     *             new type.
     */
    synchronized protected PortType newPortType(String name, StaticProperties sp)
            throws IOException, IbisException {
        sp = extractDriverStack(sp);
        NetPortType newPortType = new NetPortType(this, name, sp);
        sp = newPortType.properties();

        if (nameServer.newPortType(name, sp)) {
            portTypeTable.put(name, newPortType);
        }

        return newPortType;
    }

    long getSeqno(String name) throws IOException {
        return nameServer.getSeqno(name);
    }

    /**
     * Returns the <I>name server</I> {@linkplain #nameServer client}
     * {@linkplain Registry registry}.
     *
     * @return A reference to the instance's registry access.
     */
    public Registry registry() {
        return nameServer;
    }

    /**
     * Returns the {@linkplain Ibis} instance {@link #identifier}.
     *
     * @return The instance's identifier
     */
    public IbisIdentifier identifier() {
        return identifier;
    }

    /**
     * Initializes the NetIbis instance.
     *
     * This function should be called before any attempt to use the NetIbis
     * instance.
     * <B>This function is not automatically called by the constructor</B>.
     *
     * @exception IbisConfigurationException if the system-wide Ibis
     * 		properties where not correctly set.
     * @exception IOException if the local host name cannot be found or if
     * 		the <I>name server</I> cannot be reached.
     */
    protected void init() throws IbisException, IOException {

        /* Builds the instance identifier out of our {@link InetAddress}. */
        InetAddress addr = IPUtils.getLocalHostAddress();
        identifier = new NetIbisIdentifier(name, addr);

        /* Connects to the <I>name server<I> */
        nameServer = NameServer.loadNameServer(this, resizeHandler != null);
    }

    public void joined(IbisIdentifier joinIdent) {
        synchronized (this) {
            joinedIbises.add(joinIdent);
            if (!open || resizeHandler == null) {
                return;
            }

            // poolSize++;
        }

        if (resizeHandler != null) {
            resizeHandler.joined(joinIdent);
            if (!i_joined && joinIdent.equals(identifier)) {
                synchronized (this) {
                    i_joined = true;
                    notifyAll();
                }
            }
        }
    }

    public void left(IbisIdentifier leaveIdent) {
        synchronized (this) {
            if (!open && resizeHandler == null) {
                leftIbises.add(leaveIdent);
                return;
            }

            // poolSize--;
        }

        if (resizeHandler != null) {
            resizeHandler.left(leaveIdent);
        }
    }

    public void died(IbisIdentifier[] corpses) {
        synchronized (this) {
            if (!open && resizeHandler == null) {
                for (int i = 0; i < corpses.length; i++) {
                    diedIbises.add(corpses[i]);
                }
                return;
            }

            // poolSize -= corpses.length;
        }

        if (resizeHandler != null) {
            for (int i = 0; i < corpses.length; i++) {
                resizeHandler.died(corpses[i]);
            }
        }
    }

    public void enableResizeUpcalls() {
        if (resizeHandler != null) {
            while (joinedIbises.size() > 0) {
                Object ibisId = joinedIbises.remove(0);
                NetIbisIdentifier id = (NetIbisIdentifier) ibisId;
                resizeHandler.joined(id);
                if (id.equals(identifier)) {
                    i_joined = true;
                }
                // poolSize++;
            }

            while (leftIbises.size() > 0) {
                resizeHandler.left((NetIbisIdentifier) leftIbises.remove(0));
                // poolSize--;
            }

            while (diedIbises.size() > 0) {
                resizeHandler.died((NetIbisIdentifier) diedIbises.remove(0));
                // poolSize--;
            }
        }

        synchronized (this) {
            open = true;
            if (resizeHandler != null && !i_joined) {
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

    public synchronized void disableResizeUpcalls() {
        synchronized (this) {
            open = false;
        }
    }

    /**
     * Returns the {@linkplain PortType port type} corresponding to the
     * given type name.
     *
     * @param  name the name of the requested port type.
     * @return A reference to the port type or <code>null</CODE> if the
     * 		given name is not the name of a valid port type.
     */
    synchronized public PortType getPortType(String name) {
        return (PortType) portTypeTable.get(name);
    }

    synchronized void register(NetSendPort p) {
        p.next = sendPortList;
        sendPortList = p;
    }

    synchronized void register(NetReceivePort p) {
        p.next = receivePortList;
        receivePortList = p;
    }

    synchronized void unregister(NetSendPort p) {
        NetSendPort prev = null;
        NetSendPort scan = sendPortList;
        while (scan != null && scan != p) {
            prev = scan;
            scan = scan.next;
        }
        if (scan == null) {
            throw new Error("Unregister a NetSendPort that is not registered");
        }
        if (prev == null) {
            sendPortList = p.next;
        } else {
            prev.next = p.next;
        }
    }

    synchronized void unregister(NetReceivePort p) {
        NetReceivePort prev = null;
        NetReceivePort scan = receivePortList;
        while (scan != null && scan != p) {
            prev = scan;
            scan = scan.next;
        }
        if (scan == null) {
            throw new Error(
                    "Unregister a NetReceivePort that is not registered");
        }
        if (prev == null) {
            receivePortList = p.next;
        } else {
            prev.next = p.next;
        }
    }

    /** Requests the NetIbis instance to leave the Name Server pool.
     */
    public void end() throws IOException {
        synchronized (this) {
            if (ended) {
                return;
            }
            ended = true;
        }

        synchronized (this) {
            while (sendPortList != null) {
                sendPortList.close();
            }
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 1000) {
            synchronized (this) {
                if (receivePortList == null) {
                    break;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        synchronized (this) {
            while (receivePortList != null) {
                if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
                    System.err.println("Ibis.end(): Invoke forced close() of "
                            + receivePortList);
                }
                receivePortList.close(-1L);
            }
        }

        nameServer.leave();
    }

    public void poll() throws IOException {
        __.unimplemented__("poll");
    }

    public int closedPoolRank() {
        if (closedPoolRank == -1) {
            closedPoolRank = joinedIbises.indexOf(identifier());
        }
        if (closedPoolRank == -1) {
            throw new Error("closedPoolRank only defined in closed world");
        }

        return closedPoolRank;
    }

    public int closedPoolSize() {
        if (closedPoolSize == 0) {
            throw new Error("closedPoolSize only defined in closed world");
        } else {
            return closedPoolSize;
        }
    }
}
