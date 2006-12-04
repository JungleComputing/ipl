/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;

public final class TcpIbis extends Ibis implements Config {

    private TcpIbisIdentifier ident;

    private InetAddress myAddress;

    private NameServer nameServer;

    // private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean resizeUpcallerEnabled = false;

    private boolean busyUpcaller = false;

    private ArrayList joinedIbises = new ArrayList();

    private ArrayList leftIbises = new ArrayList();

    private ArrayList diedIbises = new ArrayList();

    private ArrayList mustLeaveIbises = new ArrayList();

    TcpPortHandler tcpPortHandler;

    private boolean ended = false;

    private boolean i_joined = false;

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }

    public TcpIbis() {
        /*
        try {
            Runtime.getRuntime().addShutdownHook(new TcpShutdown());
        } catch (Exception e) {
            System.err.println("Warning: could not register tcp shutdown hook");
        }
        */
    }

    protected PortType newPortType(String nm, StaticProperties p)
            throws IOException, IbisException {

        TcpPortType resultPort = new TcpPortType(this, nm, p);
        p = resultPort.properties();

        if (nameServer.newPortType(nm, p)) {
            /* add type to our table */
            portTypeList.put(nm, resultPort);

            if (DEBUG) {
                System.out.println(this.name + ": created PortType '" + nm
                        + "'");
            }
        }

        return resultPort;
    }

    long getSeqno(String nm) throws IOException {
        return nameServer.getSeqno(nm);
    }

    public Registry registry() {
        return nameServer;
    }

    public StaticProperties properties() {
        return staticProperties(implName);
    }

    public IbisIdentifier identifier() {
        return ident;
    }

    protected void init() throws IOException {
        if (DEBUG) {
            System.err.println("In TcpIbis.init()");
        }
        // poolSize = 1;

        myAddress = IPUtils.getLocalHostAddress();
        if (myAddress == null) {
            System.err.println("ERROR: could not get my own IP address, "
                    + "exiting.");
            System.exit(1);
        }
        ident = new TcpIbisIdentifier(name, myAddress);

        if (DEBUG) {
            System.err.println("Created IbisIdentifier " + ident);
        }

        tcpPortHandler
                = new TcpPortHandler(ident, IbisSocketFactory.getFactory());

        nameServer = NameServer.loadNameServer(this, resizeHandler != null);

        if (resizeHandler != null) {
            Thread p = new Thread("ResizeUpcaller") {
                public void run() {
                    resizeUpcaller();
                }
            };
            p.setDaemon(true);
            p.start();
        }

        if (DEBUG) {
            System.err.println("Out of TcpIbis.init()");
        }
    }

    private void resizeUpcaller() {
        for (;;) {
            synchronized(this) {
                while (! resizeUpcallerEnabled || emptyArrays()) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // nothing
                    }
                }
                busyUpcaller = true;
            }

            upcaller();

            synchronized(this) {
                busyUpcaller = false;
                notifyAll();
            }
        }
    }

    /**
     * this method forwards the join to the application running on top of ibis.
     */
    public synchronized void joined(IbisIdentifier joinIdent) {
        if (resizeHandler != null) {
            joinedIbises.add(joinIdent);
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }
        if (DEBUG) {
            System.out.println(name + ": Ibis '" + joinIdent
                    + "' joined");
        }
        // poolSize++;
    }

    /**
     * this method forwards the leave to the application running on top of
     * ibis.
     */
    public synchronized void left(IbisIdentifier leaveIdent) {
        if (resizeHandler != null) {
            leftIbises.add(leaveIdent);
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }
        if (DEBUG) {
            System.out.println(name + ": Ibis '" + leaveIdent
                    + "' left");
        }
        // poolSize--;
    }

    /**
     * this method forwards the died to the application running on top of
     * ibis.
     */
    public synchronized void died(IbisIdentifier[] corpses) {
        if (resizeHandler != null) {
            for (int i = 0; i < corpses.length; i++) {
                diedIbises.add(corpses[i]);
            }
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }
        if (DEBUG) {
            for (int i = 0; i < corpses.length; i++) {
                System.out.println(name + ": Ibis '" + corpses[i]
                        + "' died");
            }
        }
        // poolSize -= corpses.length;
    }

    /**
     * This method forwards the mustLeave to the application running on top of
     * ibis.
     */
    public synchronized void mustLeave(IbisIdentifier[] ibisses) {
        if (resizeHandler != null) {
            for (int i = 0; i < ibisses.length; i++) {
                mustLeaveIbises.add(ibisses[i]);
            }
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }

        if (DEBUG) {
            for (int i = 0; i < ibisses.length; i++) {
                System.out.println(name + ": Ibis '" + ibisses[i]
                        + "' died");
            }
        }
        // poolSize -= corpses.length;
    }

    public PortType getPortType(String nm) {
        return (PortType) portTypeList.get(nm);
    }

    private synchronized boolean emptyArrays() {
        return joinedIbises.size() == 0
            && leftIbises.size() == 0
            && diedIbises.size() == 0
            && mustLeaveIbises.size() == 0;
    }

    public synchronized void enableResizeUpcalls() {
        resizeUpcallerEnabled = true;
        notifyAll();

        if (resizeHandler != null && !i_joined) {
            while (!i_joined) {
                try {
                    wait();
                } catch (Exception e) {
                    /* ignore */
                }
            }
        }
    }

    private void upcaller() {

        TcpIbisIdentifier id = null;

        if (resizeHandler != null) {
            while (true) {
                synchronized (this) {
                    if (joinedIbises.size() == 0) {
                        break;
                    }
                    // poolSize++;
                    id = (TcpIbisIdentifier) joinedIbises.remove(0);
                }
                // Don't hold the lock during user upcall
                resizeHandler.joined(id);
                if (id.equals(this.ident)) {
                    synchronized(this) {
                        i_joined = true;
                        notifyAll();
                    }
                }
            }

            while (true) {
                synchronized (this) {
                    if (leftIbises.size() == 0) {
                        break;
                    }
                    // poolSize--;
                    id = (TcpIbisIdentifier) leftIbises.remove(0);
                }
                // Don't hold the lock during user upcall
                resizeHandler.left(id);

            }
            while (true) {
                synchronized (this) {
                    if (diedIbises.size() == 0) {
                        break;
                    }
                    // poolSize--;
                    id = (TcpIbisIdentifier) diedIbises.remove(0);
                }
                // Don't hold the lock during user upcall
                resizeHandler.died(id);
            }

            IbisIdentifier[] ids = new IbisIdentifier[0];
            synchronized(this) {
                if (mustLeaveIbises.size() != 0) {
                    ids = (IbisIdentifier[]) mustLeaveIbises.toArray(ids);
                    mustLeaveIbises.clear();
                }
            }
            if (ids.length != 0) {
                resizeHandler.mustLeave(ids);
            }
        }
    }

    public synchronized void disableResizeUpcalls() {
        while (busyUpcaller) {
            try {
                wait();
            } catch(Exception e) {
                // nothing
            }
        }
        resizeUpcallerEnabled = false;
    }

    public void end() {
        synchronized (this) {
            if (ended) {
                return;
            }
            ended = true;
        }
        try {
            if (nameServer != null) {
                nameServer.leave();
            }
            if (tcpPortHandler != null) {
                tcpPortHandler.quit();
            }
        } catch (Exception e) {
            throw new IbisRuntimeException(
                    "TcpIbisNameServerClient: leave failed ", e);
        }
    }

    public void poll() {
        // Empty implementation, as TCP Ibis has interrupts.
    }

    void bindReceivePort(String nm, ReceivePortIdentifier p)
            throws IOException {
        if (! name.equals(ReceivePort.ANONYMOUS)) {
            nameServer.bind(nm, p);
        }
    }

    void unbindReceivePort(String nm) throws IOException {
        if (! name.equals(ReceivePort.ANONYMOUS)) {
            nameServer.unbind(nm);
        }
    }
    
    class TcpShutdown extends Thread {
        public void run() {
            end();
        }
    }
}
