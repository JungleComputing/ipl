/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.PortMismatchException;
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
            throws PortMismatchException {

        TcpPortType resultPort = new TcpPortType(this, nm, p);
        p = resultPort.properties();

        portTypeList.put(nm, resultPort);

        if (DEBUG) {
            System.out.println(this.name + ": created PortType '" + nm
                    + "'");
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

        if (DEBUG) {
            System.err.println("Out of TcpIbis.init()");
        }
    }

    private synchronized void waitForEnabled() {
        while (! resizeUpcallerEnabled) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
        }
        busyUpcaller = true;
    }

    /**
     * This method forwards the join to the application running on top of ibis.
     */
    public void joined(IbisIdentifier[] joinIdent) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < joinIdent.length; i++) {
                IbisIdentifier id = joinIdent[i];
                resizeHandler.joined(id);
                if (id.equals(this.ident)) {
                    synchronized(this) {
                        i_joined = true;
                        notifyAll();
                    }
                }
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
    }

    /**
     * This method forwards the leave to the application running on top of ibis.
     */
    public void left(IbisIdentifier[] leaveIdent) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < leaveIdent.length; i++) {
                IbisIdentifier id = leaveIdent[i];
                resizeHandler.left(id);
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
    }

    /**
     * This method forwards the died to the application running on top of ibis.
     */
    public void died(IbisIdentifier[] corpses) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < corpses.length; i++) {
                IbisIdentifier id = corpses[i];
                resizeHandler.died(id);
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
    }

    /**
     * This method forwards the mustLeave to the application running on top of
     * ibis.
     */
    public void mustLeave(IbisIdentifier[] ibisses) {
        if (resizeHandler != null) {
            waitForEnabled();
            resizeHandler.mustLeave(ibisses);
            synchronized(this) {
                busyUpcaller = false;
            }
        }
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
            throw new RuntimeException(
                    "TcpIbisNameServerClient: leave failed ", e);
        }
    }

    public void poll() {
        // Empty implementation, as TCP Ibis has interrupts.
    }

    class TcpShutdown extends Thread {
        public void run() {
            end();
        }
    }
}
