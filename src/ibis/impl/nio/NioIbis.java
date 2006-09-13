/* $Id$ */

package ibis.impl.nio;

import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;

public final class NioIbis extends Ibis implements Config {

    static final String prefix = "ibis.nio.";

    static final String s_numbered = prefix + "numbered";

    static final String s_spi = prefix + "spi";

    static final String s_rpi = prefix + "rpi";

    static final String[] properties = { s_numbered, s_spi, s_rpi };

    static final String[] unchecked = { s_spi + ".", s_rpi + "." };

    private Logger logger = ibis.util.GetLogger.getLogger(NioIbis.class);

    private NioIbisIdentifier identifier;

    NameServer nameServer;

    // private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean resizeUpcallerEnabled = false;

    private boolean busyUpcaller = false;

    private ArrayList joinedIbises = new ArrayList();

    private ArrayList leftIbises = new ArrayList();

    private ArrayList diedIbises = new ArrayList();

    private ArrayList mustLeaveIbises = new ArrayList();

    ChannelFactory factory;

    private boolean ended = false;

    private SendReceiveThread sendReceiveThread = null;

    private boolean i_joined = false;

    public NioIbis() throws IbisException {
        TypedProperties.checkProperties(prefix, properties, unchecked);
        /*
        try {
            Runtime.getRuntime().addShutdownHook(new NioShutdown());
        } catch (Exception e) {
            logger.error("could not register nio shutdown hook");
        }
        */
    }

    synchronized protected PortType newPortType(String name, StaticProperties p)
            throws IOException, IbisException {

        NioPortType resultPort = new NioPortType(this, name, p);
        p = resultPort.properties();

        if (nameServer.newPortType(name, p)) {
            /* add type to our table */
            portTypeList.put(name, resultPort);
        }
        return resultPort;
    }

    long getSeqno(String name) throws IOException {
        return nameServer.getSeqno(name);
    }

    public Registry registry() {
        return nameServer;
    }

    public IbisIdentifier identifier() {
        return identifier;
    }

    public String toString() {
        return identifier.toString();
    }

    protected void init() throws IbisException, IOException {
        // poolSize = 1;

        identifier = new NioIbisIdentifier(name);

        logger.info("creating and initializing (Nio)Ibis: " + identifier);

        nameServer = NameServer.loadNameServer(this, resizeHandler != null);

        factory = new TcpChannelFactory();

        if (resizeHandler != null) {
            Thread p = new Thread("ResizeUpcaller") {
                public void run() {
                    resizeUpcaller();
                }
            };
            p.setDaemon(true);
            p.start();
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
     * This method forwards the join to the application running on top of ibis.
     */
    public synchronized void joined(IbisIdentifier joinIdent) {
        if (resizeHandler != null) {
            joinedIbises.add(joinIdent);
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info(name + ": Ibis '" + joinIdent + "' joined");
        }
        // poolSize++;
    }

    /**
     * This method forwards the leave to the application running on top of
     * ibis.
     */
    public synchronized void left(IbisIdentifier leaveIdent) {
        if (resizeHandler != null) {
            leftIbises.add(leaveIdent);
            if (resizeUpcallerEnabled) {
                notifyAll();
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info(name + ": Ibis '" + leaveIdent + "' left");
        }
        // poolSize--;
    }

    /**
     * This method forwards the died to the application running on top of
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
        if (logger.isInfoEnabled()) {
            for (int i = 0; i < corpses.length; i++) {
                logger.info(name + ": Ibis '" + corpses[i] + "' died");
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

        if (logger.isInfoEnabled()) {
            for (int i = 0; i < ibisses.length; i++) {
                logger.info(name + ": Ibis '" + ibisses[i] + "' died");
            }
        }
        // poolSize -= corpses.length;
    }

    private synchronized boolean emptyArrays() {
        return joinedIbises.size() == 0
            && leftIbises.size() == 0
            && diedIbises.size() == 0
            && mustLeaveIbises.size() == 0;
    }

    public synchronized void enableResizeUpcalls() {

        logger.info("opening world");

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

        logger.info("world opened");
    }

    private void upcaller() {

        NioIbisIdentifier id = null;

        if (resizeHandler != null) {
            while (true) {
                synchronized (this) {
                    if (joinedIbises.size() == 0) {
                        break;
                    }
                    // poolSize++;
                    id = (NioIbisIdentifier) joinedIbises.remove(0);
                }
                // Don't hold the lock during user upcall
                resizeHandler.joined(id);
                if (id.equals(identifier)) {
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
                    id = (NioIbisIdentifier) leftIbises.remove(0);
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
                    id = (NioIbisIdentifier) diedIbises.remove(0);
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

    public PortType getPortType(String name) {
        return (PortType) portTypeList.get(name);
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
            if (factory != null) {
                factory.quit();
            }
            if (sendReceiveThread != null) {
                sendReceiveThread.quit();
            }

        } catch (Exception e) {
            throw new IbisRuntimeException("NioIbis: end failed ", e);
        }
        
        logger.info("NioIbis" + identifier + " DE-initialized");
    }

    /**
     * does nothing.
     */
    public void poll() throws IOException {
    	// nothing
    }

    /**
     * Called when the vm exits
     */
    class NioShutdown extends Thread {
        public void run() {
            end();
        }
    }

    synchronized SendReceiveThread sendReceiveThread() throws IOException {
        if (sendReceiveThread == null) {
            sendReceiveThread = new SendReceiveThread();
        }
        return sendReceiveThread;
    }
}
