/* $Id$ */

package ibis.impl.tcp;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.TypedProperties;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;

import smartsockets.hub.servicelink.ServiceLink;
import smartsockets.virtual.VirtualSocketFactory;

public final class TcpIbis extends Ibis implements Config {

    ibis.impl.IbisIdentifier ident;

    private ibis.impl.Registry registry;

    // private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean resizeUpcallerEnabled = false;

    private boolean busyUpcaller = false;

    TcpPortHandler tcpPortHandler;

    private boolean ended = false;

    private VirtualSocketFactory socketFactory;

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
            System.out.println("" + this.ident.getId() + ": created PortType '" + nm
                    + "'");
        }

        return resultPort;
    }

    long getSeqno(String nm) throws IOException {
        return registry.getSeqno(nm);
    }

    public Registry registry() {
        return registry;
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
        
        // NOTE: moved here from the static initializer, since we may want to 
        //       configure the thing differently for every TcpIbis instance in 
        //       this process. Having a single -static- socketfactory doesn't 
        //       work then....        
        
        HashMap properties = new HashMap();        
        socketFactory = VirtualSocketFactory.createSocketFactory(properties, 
                true);

        tcpPortHandler = new TcpPortHandler(this, socketFactory);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF(tcpPortHandler.sa.toString());
        out.flush();
        out.close();

        registry = ibis.impl.Registry.loadRegistry(this);

        // TODO: fix for more than one Ibis instance in a jvm
        VirtualSocketFactory.registerSocketFactory("Factory for Ibis",
                socketFactory);
       
        ident = registry.init(this, resizeHandler != null, bos.toByteArray());

        //    if (DEBUG) {
        System.err.println("Created IbisIdentifier " + ident);
        //    }

        // Bit of a hack to improve the visualization
        try { 
            ServiceLink sl = socketFactory.getServiceLink();
        
            if (sl != null) {
                sl.registerProperty("ibis", ident.toString());
            }
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Failed to register ibis property with " +
                        "nameserver (not very important...)");
            }
        }
        
        if (DEBUG) {
            System.err.println("Out of TcpIbis.init(), ident = " + ident);
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
        
        socketFactory.printStatistics("Factory for Ibis");
        
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

    public void printStatistics() { 
        socketFactory.printStatistics(ident.toString());
    }

    class TcpShutdown extends Thread {
        public void run() {
            end();
        }
    }
}
