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

public final class NioIbis extends Ibis implements Config {

    static final String prefix = "ibis.nio.";

    static final String s_numbered = prefix + "numbered";

    static final String s_spi = prefix + "spi";

    static final String s_rpi = prefix + "rpi";

    static final String s_debug = prefix + "debug";

    static final String s_log = prefix + "log";

    static final String[] properties = { s_numbered, s_spi, s_rpi, s_debug,
            s_log };

    static final String[] unchecked = { s_spi + ".", s_rpi + "." };

    private NioIbisIdentifier ident;

    NameServer nameServer;

    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean open = false;

    private ArrayList joinedIbises = new ArrayList();

    private ArrayList leftIbises = new ArrayList();

    private ArrayList diedIbises = new ArrayList();

    ChannelFactory factory;

    private boolean ended = false;

    private SendReceiveThread sendReceiveThread = null;

    private boolean i_joined = false;

    public NioIbis() throws IbisException {
        TypedProperties.checkProperties(prefix, properties, unchecked);
        try {
            Runtime.getRuntime().addShutdownHook(new NioShutdown());
        } catch (Exception e) {
            if (DEBUG) {
                Debug.message("general", this,
                        "!could not register nio shutdown hook");
            }
        }
    }

    synchronized protected PortType newPortType(String name, StaticProperties p)
            throws IOException, IbisException {

        NioPortType resultPort = new NioPortType(this, name, p);
        p = resultPort.properties();

        if (nameServer.newPortType(name, p)) {
            /* add type to our table */
            portTypeList.put(name, resultPort);

            if (DEBUG) {
                Debug.message("connections", this, "created PortType `" + name
                        + "'");
            }
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
        return ident;
    }

    public String toString() {
        return ident.toString();
    }

    protected void init() throws IbisException, IOException {
        if (DEBUG) {
            System.err.println("NioIbis: Debugging support enabled");
            Debug.setName(name);
        }

        if (DEBUG) {
            Debug.enter("general", this, "initializing NioIbis");
        }

        poolSize = 1;

        ident = new NioIbisIdentifier(name);

        if (DEBUG) {
            Debug.message("general", this, "created IbisIdentifier" + ident);
        }

        nameServer = NameServer.loadNameServer(this);

        factory = new TcpChannelFactory();

        if (DEBUG) {
            Debug.exit("general", this, "initialized NioIbis");
        }
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

            if (DEBUG) {
                Debug.message("general", this, "ibis '" + joinIdent.name()
                        + "' joined");
            }

            poolSize++;
        }

        if (resizeHandler != null) {
            resizeHandler.joined(joinIdent);
            if (!i_joined && joinIdent.equals(ident)) {
                synchronized (this) {
                    i_joined = true;
                    notifyAll();
                }
            }
        }
    }

    /**
     * this method forwards the leave to the application running on top of
     * ibis.
     */
    public void left(IbisIdentifier leaveIdent) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                leftIbises.add(leaveIdent);
                return;
            }

            if (DEBUG) {
                Debug.message("general", this, "ibis '" + leaveIdent.name()
                        + "' left");
            }

            poolSize--;
        }

        if (resizeHandler != null) {
            resizeHandler.left(leaveIdent);
        }
    }

    /**
     * this method forwards the died to the application running on top of
     * ibis.
     */
    public void died(IbisIdentifier[] corpses) {
        synchronized (this) {
            if (!open && resizeHandler != null) {
                for (int i = 0; i < corpses.length; i++) {
                    diedIbises.add(corpses[i]);
                }
                return;
            }

            if (DEBUG) {
                for (int i = 0; i < corpses.length; i++) {
                    Debug.message("general", this, "ibis '" + corpses[i].name()
                            + "' died");
                }
            }

            poolSize -= corpses.length;
        }

        if (resizeHandler != null) {
            for (int i = 0; i < corpses.length; i++) {
                resizeHandler.died(corpses[i]);
            }
        }
    }

    public PortType getPortType(String name) {
        return (PortType) portTypeList.get(name);
    }

    public void enableResizeUpcalls() {
        NioIbisIdentifier ident = null;

        if (DEBUG) {
            Debug.enter("general", this, "opening world");
        }

        if (resizeHandler != null) {
            while (true) {
                synchronized (this) {
                    if (joinedIbises.size() == 0) {
                        break;
                    }
                    poolSize++;
                    ident = (NioIbisIdentifier) joinedIbises.remove(0);
                }
                resizeHandler.joined(ident); // Don't hold the lock during user upcall
                if (ident.equals(this.ident)) {
                    i_joined = true;
                }
            }

            while (true) {
                synchronized (this) {
                    if (leftIbises.size() == 0) {
                        break;
                    }
                    poolSize--;
                    ident = (NioIbisIdentifier) leftIbises.remove(0);
                }
                resizeHandler.left(ident); // Don't hold the lock during user upcall

            }
            while (true) {
                synchronized (this) {
                    if (diedIbises.size() == 0) {
                        break;
                    }
                    poolSize--;
                    ident = (NioIbisIdentifier) diedIbises.remove(0);
                }
                resizeHandler.died(ident); // Don't hold the lock during user upcall

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

        if (DEBUG) {
            Debug.exit("general", this, "world opened");
        }
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

            if (DEBUG) {
                Debug.end();
            }
        } catch (Exception e) {
            throw new IbisRuntimeException("NioIbis: end failed ", e);
        }
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
