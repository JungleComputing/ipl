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

    private static Logger logger = ibis.util.GetLogger.getLogger(NioIbis.class);

    private NioIbisIdentifier identifier;

    NameServer nameServer;

    // private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean open = false;

    private ArrayList joinedIbises = new ArrayList();

    private ArrayList leftIbises = new ArrayList();

    private ArrayList diedIbises = new ArrayList();

    private ArrayList mustLeaveIbisses = new ArrayList();

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
    }

    /**
     * this method forwards the join to the application running on top of ibis.
     */
    public void joined(IbisIdentifier joinIdent) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                joinedIbises.add(joinIdent);
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info("ibis '" + joinIdent + "' joined");
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

    /**
     * this method forwards the leave to the application running on top of ibis.
     */
    public void left(IbisIdentifier leaveIdent) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                leftIbises.add(leaveIdent);
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info("ibis '" + leaveIdent + "' left");
            }

            // poolSize--;
        }

        if (resizeHandler != null) {
            resizeHandler.left(leaveIdent);
        }
    }

    /**
     * this method forwards the died to the application running on top of ibis.
     */
    public void died(IbisIdentifier[] corpses) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                for (int i = 0; i < corpses.length; i++) {
                    diedIbises.add(corpses[i]);
                }
                return;
            }

            if (logger.isInfoEnabled()) {
                for (int i = 0; i < corpses.length; i++) {
                    logger.info("ibis '" + corpses[i] + "' died");
                }
            }

            // poolSize -= corpses.length;
        }

        if (resizeHandler != null) {
            for (int i = 0; i < corpses.length; i++) {
                resizeHandler.died(corpses[i]);
            }
        }
    }


    /**
     * This method forwards the mustLeave to the application running on top of
     * ibis.
     */
    public void mustLeave(IbisIdentifier[] ibisses) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                for (int i = 0; i < ibisses.length; i++) {
                    mustLeaveIbisses.add(ibisses[i]);
                }
                return;
            }
        }

        if (resizeHandler != null) {
            resizeHandler.mustLeave(ibisses);
        }
    }

    public PortType getPortType(String name) {
        return (PortType) portTypeList.get(name);
    }

    public void enableResizeUpcalls() {
        NioIbisIdentifier ident = null;

        logger.info("opening world");

        if (resizeHandler != null) {
            while (true) {
                synchronized (this) {
                    if (joinedIbises.size() == 0) {
                        break;
                    }
                    // poolSize++;
                    ident = (NioIbisIdentifier) joinedIbises.remove(0);
                }
                resizeHandler.joined(ident); // Don't hold the lock during
                // user upcall
                if (ident.equals(this.identifier)) {
                    i_joined = true;
                }
            }

            while (true) {
                synchronized (this) {
                    if (leftIbises.size() == 0) {
                        break;
                    }
                    // poolSize--;
                    ident = (NioIbisIdentifier) leftIbises.remove(0);
                }
                resizeHandler.left(ident); // Don't hold the lock during user
                // upcall

            }
            while (true) {
                synchronized (this) {
                    if (diedIbises.size() == 0) {
                        break;
                    }
                    // poolSize--;
                    ident = (NioIbisIdentifier) diedIbises.remove(0);
                }
                resizeHandler.died(ident); // Don't hold the lock during user
                // upcall

            }

            IbisIdentifier[] ids = new IbisIdentifier[0];
            synchronized(this) {
                if (mustLeaveIbisses.size() != 0) {
                    ids = (IbisIdentifier[]) mustLeaveIbisses.toArray(ids);
                    mustLeaveIbisses.clear();
                }
            }
            if (ids.length != 0) {
                resizeHandler.mustLeave(ids);
            }
        }

        synchronized (this) {
            open = true;
            if (resizeHandler != null && !i_joined) {
                while (!i_joined) {
                    try {
                        wait();
                    } catch (Exception e) {
                    }
                }
            }
        }

        logger.info("world opened");
    }

    public synchronized void disableResizeUpcalls() {
        open = false;
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
