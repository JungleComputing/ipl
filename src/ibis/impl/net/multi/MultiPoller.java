/* $Id$ */

package ibis.impl.net.multi;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbisIdentifier;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetServiceInputStream;
import ibis.impl.net.NetServiceLink;
import ibis.impl.net.NetServicePopupThread;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Provides a generic multiple network input poller.
 */
public class MultiPoller extends NetPoller {

    /*
     * Data structures required:
     * = subInput(s) -- shared between connections of the same plugin type.
     * 		One per plugin type.
     * = ReceiveQueue(s) -- inherited from NetPoller. Handles polling of
     * 		and input from one of our subInputs.
     * 		One per plugin type.
     * = subContext(s) -- key to do multiplexing between plugin types.
     * 		One per plugin type.
     * 		Requires a subContextTable, indexed by plugin type.
     * = Lane(s) -- Connection info.
     * 		One per connection.
     * 		Requires a laneTable, indexed by cnx.getNum().
     */

    protected final static class Lane {
        ReceiveQueue queue = null;

        int headerLength = 0;

        int mtu = 0;

        ServiceThread thread = null;

        ObjectInputStream is = null;

        ObjectOutputStream os = null;

        String subContext = null;

    }

    private final class ServiceThread implements NetServicePopupThread {

        private Lane lane = null;

        private String name;

        public ServiceThread(String name, Lane lane) {
            this.name = "ServiceThread: " + name;
            this.lane = lane;
        }

        public void callBack() throws IOException {
            int newMtu = lane.is.readInt();
            int newHeaderLength = lane.is.readInt();

            synchronized (lane) {
                lane.mtu = newMtu;
                lane.headerLength = newHeaderLength;
            }

            lane.os.writeInt(3);
            lane.os.flush();
        }

        public String getName() {
            return name;
        }

        public void end() {
            log.in();
            log.out();
        }
    }

    /**
     * Our extension to the set of inputs.
     */
    protected Hashtable laneTable = null;

    private MultiPlugin plugin = null;

    protected Lane connectingLane;

    protected Lane singleLane;

    protected Vector subContextTable;

    public MultiPoller(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
        init();
    }

    public MultiPoller(NetPortType pt, NetDriver driver, String context,
            boolean decouplePoller, NetInputUpcall inputUpcall)
            throws IOException {
        super(pt, driver, context, decouplePoller, inputUpcall);
        init();
    }

    private void init() throws IOException {
        laneTable = new Hashtable();
        subContextTable = new Vector();

        String pluginName = getProperty("Plugin");
        //System.err.println("multi-protocol plugin: "+pluginName);
        if (pluginName != null) {
            plugin = ((Driver) driver).loadPlugin(pluginName);
        }
        //System.err.println("multi-protocol plugin loaded");
    }

    /* is called synchronized(this) from NetPoller */
    protected NetInput newPollerSubInput(Object key, ReceiveQueue q)
            throws IOException {
        NetInput ni = q.getInput();

        if (ni != null) {
            /*
             * If there is already a subInput associated with this lane
             * (i.e. this subContext, i.e. the driver stack that belongs
             * to our plugin), use that.
             * Else, go on and create a new subInput.
             */
            // System.err.println(this + ": recycle existing subInput " + ni);
            return ni;
        }

        // Lane      lane          = (Lane)key;
        Lane lane = connectingLane;
        String subContext = lane.subContext;
        String subDriverName = getProperty(subContext, "Driver");
        NetDriver subDriver = driver.getIbis().getDriver(subDriverName);

        // System.err.println(this + ": Create a MultiPoller downcall with ReceiveQueue " + q + " upcallFunc " + upcallFunc + " subDriver " + subDriver + " subContext " + subContext);

        if (false && upcallFunc == null) {
            System.err.println(ibis.impl.net.NetIbis.hostName() + "-" + this
                    + ": Create a MultiPoller downcall with ReceiveQueue " + q
                    + " upcallFunc " + upcallFunc + " subDriver " + subDriver
                    + " subContext " + subContext);
        }

        if (decouplePoller && upcallFunc != null) {
            ni = newSubInput(subDriver, subContext, q);
        } else {
            ni = newSubInput(subDriver, subContext, null);
        }

        return ni;
    }

    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();

        // Ensure that connections are not interleaved. The lock on
        // (this) can be released in the code to disable the singleton
        // fastpath in super.setSingleton(ReceiveQueue, boolean).
        while (waitingConnections > 0) {
            System.err.println(this
                    + ": Bingo, interleaved connections. Protect");
            waitingConnections++;
            try {
                wait();
            } catch (InterruptedException e) {
                // Go on waiting
            }
            waitingConnections--;
        }

        Integer num = cnx.getNum();
        NetServiceLink link = cnx.getServiceLink();

        NetServiceInputStream sis = link.getInputSubStream(this, "multi");
        ObjectInputStream is = new ObjectInputStream(sis);
        ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream(
                this, "multi"));

        os.flush();

        Object ibisId = driver.getIbis().identifier();
        NetIbisIdentifier localId = (NetIbisIdentifier) ibisId;
        NetIbisIdentifier remoteId;
        try {
            remoteId = (NetIbisIdentifier) is.readObject();
        } catch (ClassNotFoundException e) {
            throw new Error("Cannot find clss NetIbisIdentifier", e);
        }

        os.writeObject(localId);
        os.flush();

        String subContext = null;
        if (plugin != null) {
            subContext = plugin.getSubContext(false, localId, remoteId, os, is);
        } else {
            subContext = Driver.defaultSubContext();
        }

        Lane lane = new Lane();
        laneTable.put(num, lane);
        lane.subContext = subContext;
        if (!subContextTable.contains(subContext)) {
            // System.err.println(this + ": now add subContext " + subContext);
            subContextTable.add(subContext);
        }

        if (connectingLane != null) {
            throw new Error(this + ": race in subConnection setup");
        }
        connectingLane = lane;

        // Creates a ReceiveQueue if none exists for this plugin type
        // setupConnection(cnx, lane);
        setupConnection(cnx, subContext);

        connectingLane = null;

        ReceiveQueue q = (ReceiveQueue) inputMap.get(subContext);

        lane.is = is;
        lane.os = os;
        lane.queue = q;
        lane.mtu = is.readInt();
        lane.headerLength = is.readInt();
        lane.thread = new ServiceThread("subcontext = " + subContext
                + ", spn = " + num, lane);

        if (laneTable.values().size() == 1) {
            singleLane = lane;
        } else {
            singleLane = null;
        }

        sis.registerPopup(lane.thread);

        log.out();
    }

    protected boolean isSingleton() {
        return (subContextTable.size() == 1);
    }

    protected Object getKey(Integer num) {
        log.in();
        Lane lane = (Lane) laneTable.get(num);
        Object key = lane.subContext;
        log.out();

        return key;
    }

    protected void selectConnection(ReceiveQueue rq) {
        log.in();
        Lane lane = singleLane;
        if (lane == null) {
            lane = (Lane) laneTable.get(rq.activeNum());
        }
        synchronized (lane) {
            mtu = lane.mtu;
            headerOffset = lane.headerLength;
        }
        log.out();
    }

    public synchronized void closeConnection(ReceiveQueue rq, Integer num)
            throws IOException {
        log.in();
        if (laneTable != null) {
            Lane lane = (Lane) laneTable.get(num);

            if (lane != null) {
                if (lane.queue.getInput() != null) {
                    lane.queue.getInput().close(num);
                }

                if (lane.thread != null) {
                    lane.thread.end();
                }

                laneTable.remove(num);
            }
        }
        log.out();
    }

    /*
    public void free() throws IOException {
        log.in();trace.in();
        if (laneTable != null) {
            Iterator i = laneTable.values().iterator();
            while (i.hasNext()) {
                Lane lane = (Lane)i.next();
                if (lane != null && lane.thread != null) {
                    lane.thread.end();
                }
                i.remove();
            }
        }

        trace.out();log.out();
    }
    */
}
