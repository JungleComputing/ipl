/* $Id$ */

package ibis.impl.tcp;

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
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import smartsockets.direct.SocketAddressSet;
import smartsockets.hub.servicelink.ServiceLink;
import smartsockets.virtual.VirtualSocketFactory;

public final class TcpIbis extends Ibis implements Config {

    private TcpIbisIdentifier ident;

   // private SocketAddressSet myAddress;

    private NameServer nameServer;

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
        
        // NOTE: moved here from the static initializer, since we may want to 
        //       configure the thing differently for every TcpIbis instance in 
        //       this process. Having a single -static- socketfactory doesn't 
        //       work then....        
        
        HashMap properties = new HashMap();        
        socketFactory = VirtualSocketFactory.createSocketFactory(properties, 
                true);
              
       // myAddress = socketFactory.getLocalHost();
        
      //  if (myAddress == null) {
      //      System.err.println("ERROR: could not get my own network address, "
       //             + "exiting.");
        //    System.exit(1);
       // }
        
        //name = "ibis@" + myAddress;
        
        //ident = new TcpIbisIdentifier(name, myAddress);

   
        tcpPortHandler = new TcpPortHandler(socketFactory);                  
        
        ident = tcpPortHandler.me;
        
        //    if (DEBUG) {
        System.err.println("Created IbisIdentifier " + ident);
        //    }

        VirtualSocketFactory.registerSocketFactory("Factory for Ibis: " 
                + ident.name(), socketFactory);
       
        nameServer = NameServer.loadNameServer(this, resizeHandler != null);

        // Bit of a hack to improve the visualization
        try { 
            ServiceLink sl = socketFactory.getServiceLink();
        
            if (sl != null) {
                sl.registerProperty("ibis", name);
            }
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Failed to register ibis property with " +
                        "nameserver (not very important...)");
            }
        }
        
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

    public PortType getPortType(String nm) {
        return (PortType) portTypeList.get(nm);
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
        
        socketFactory.printStatistics("Factory for Ibis: " + name);
        
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

    public void printStatistics() { 
        socketFactory.printStatistics(ident.name());
    }

    class TcpShutdown extends Thread {
        public void run() {
            end();
        }
    }
}
