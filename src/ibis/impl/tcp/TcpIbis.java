/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.ResizeHandler;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

public final class TcpIbis extends Ibis implements Config {

    ibis.impl.IbisIdentifier ident;

    InetSocketAddress myAddress;

    private ibis.impl.Registry registry;

    // private int poolSize;

    private boolean resizeUpcallerEnabled = false;

    private boolean busyUpcaller = false;

    TcpPortHandler tcpPortHandler;

    private boolean ended = false;

    private boolean i_joined = false;

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }

    public TcpIbis(ResizeHandler r, StaticProperties p1, StaticProperties p2)
            throws IOException {
        super(r, p1, p2);
        if (DEBUG) {
            System.err.println("In TcpIbis constructor");
        }
        InetAddress addr = IPUtils.getLocalHostAddress();
        if (addr == null) {
            System.err.println("ERROR: could not get my own IP address, "
                    + "exiting.");
            System.exit(1);
        }

        registry = ibis.impl.Registry.loadRegistry(this);

        tcpPortHandler
                = new TcpPortHandler(this, IbisSocketFactory.getFactory());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF(myAddress.getAddress().getHostAddress());
        out.writeInt(myAddress.getPort());
        out.flush();
        out.close();

        ident = registry.init(this, resizeHandler != null, bos.toByteArray());

        if (DEBUG) {
            System.err.println("Out of TcpIbis constructor, ident = " + ident);
        }
        /*
        try {
            Runtime.getRuntime().addShutdownHook(new TcpShutdown());
        } catch (Exception e) {
            System.err.println("Warning: could not register tcp shutdown hook");
        }
        */
    }

    protected PortType newPortType(StaticProperties p)
            throws PortMismatchException {

        TcpPortType resultPort = new TcpPortType(this, p);
        p = resultPort.properties();

        if (DEBUG) {
            System.out.println("" + this.ident.getId() + ": created PortType "
                    + "with properties " + p);
        }

        return resultPort;
    }

    long getSeqno(String nm) throws IOException {
        return registry.getSeqno(nm);
    }

    public Registry registry() {
        return registry;
    }

    public IbisIdentifier identifier() {
        return ident;
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
            if (registry != null) {
                registry.leave();
            }
            if (tcpPortHandler != null) {
                tcpPortHandler.quit();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Registry: leave failed ", e);
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
