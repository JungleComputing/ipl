package ibis.impl.net.multi;

import ibis.impl.net.InterruptedIOException;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbisIdentifier;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetServiceLink;
import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public class MultiPoller extends NetPoller {

        protected final static class Lane {
                ReceiveQueue  queue        = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ServiceThread thread       = null;
                ObjectInputStream  is      = null;
                ObjectOutputStream os      = null;
                String             subContext = null;

        }

        private final class ServiceThread extends Thread {
                private          Lane    lane =  null;
                private volatile boolean exit = false;

                public ServiceThread(String name, Lane lane) throws IOException {
                        super("ServiceThread: "+name);
                        this.lane = lane;
                }

                public void run() {
                        log.in();
                        while (!exit) {
                                try {
                                        int newMtu          = lane.is.readInt();
                                        int newHeaderLength = lane.is.readInt();

                                        synchronized(lane) {
                                                lane.mtu          = newMtu;
                                                lane.headerLength = newHeaderLength;
                                        }

                                        lane.os.writeInt(3);
                                        lane.os.flush();
                                } catch (InterruptedIOException e) {
                                        break;
                                } catch (ConnectionClosedException e) {
                                        break;
                                } catch (java.io.EOFException e) {
                                        break;
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        throw new Error(e);
                                }
                        }
                        log.out();
                }

                public void end() {
                        log.in();
                        exit = true;
                        this.interrupt();
                        log.out();
                }
        }


        /**
         * Our extension to the set of inputs.
         */
        protected Hashtable laneTable = null;

        private MultiPlugin plugin  = null;

        public MultiPoller(NetPortType pt, NetDriver driver, String context, NetInputUpcall inputUpcall)
                throws IOException {
                super(pt, driver, context, inputUpcall);
		init();
	}

        public MultiPoller(NetPortType pt, NetDriver driver, String context, boolean decouplePoller, NetInputUpcall inputUpcall)
                throws IOException {
                super(pt, driver, context, decouplePoller, inputUpcall);
		init();
	}

	private void init() throws IOException {
                laneTable = new Hashtable();

                String pluginName = getProperty("Plugin");
                //System.err.println("multi-protocol plugin: "+pluginName);
                if (pluginName != null) {
                        plugin = ((Driver)driver).loadPlugin(pluginName);
                }
                //System.err.println("multi-protocol plugin loaded");
        }

	/*
	 * Deprecate?

        ***
         * {@inheritDoc}
         **
        protected void selectInput(Integer spn) throws ConnectionClosedException {
                log.in();
                Lane lane = (Lane)laneTable.get(spn);
                if (lane == null) {
                        throw new ConnectionClosedException("connection "+spn+" closed");
                }

                activeQueue = lane.queue;
                if (activeQueue == null) {
                        throw new ConnectionClosedException("connection "+spn+" closed");
                }
                log.out();
        }
	*/


	protected NetInput newPollerSubInput(Object key, ReceiveQueue q)
	    	throws IOException {
	    NetInput ni = q.getInput();

	    if (ni != null) {
		return ni;
	    }

	    Lane      lane          = (Lane)key;
	    String    subContext    = lane.subContext;
	    String    subDriverName = getProperty(subContext, "Driver");
	    NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);

	    if (upcallFunc == null) {
		System.err.println(ibis.impl.net.NetIbis.hostName() + "-" + this + ": Create a MultiPoller downcall with ReceiveQueue " + q + " upcallFunc " + upcallFunc + " subDriver " + subDriver + " subContext " + subContext);
	    }
	    return newSubInput(subDriver, subContext, q);
	}


        /**
         * {@inheritDoc}
         */
        public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
		Integer num  = cnx.getNum();
		NetServiceLink          link = cnx.getServiceLink();

		ObjectInputStream       is      = new ObjectInputStream (link.getInputSubStream (this, "multi"));
		ObjectOutputStream      os      = new ObjectOutputStream(link.getOutputSubStream(this, "multi"));

		os.flush();

		NetIbisIdentifier       localId         = (NetIbisIdentifier)driver.getIbis().identifier();
		NetIbisIdentifier       remoteId;
		try {
			remoteId        = (NetIbisIdentifier)is.readObject();
		} catch (ClassNotFoundException e) {
			throw new Error("Cannot find clss NetIbisIdentifier", e);
		}

		os.writeObject(localId);
		os.flush();

		String          subContext      = (plugin!=null)?plugin.getSubContext(false, localId, remoteId, os, is):null;

		Lane    lane = new Lane();
		lane.subContext   = subContext;

		setupConnection(cnx, lane);

		ReceiveQueue q = (ReceiveQueue)inputMap.get(lane);

		lane.is           = is;
		lane.os           = os;
		lane.queue        = q;
		lane.mtu          = is.readInt();
		lane.headerLength = is.readInt();
		lane.thread       = new ServiceThread("subcontext = "+subContext+", spn = "+num, lane);

		laneTable.put(num, lane);

		lane.thread.start();

                log.out();
        }

        protected Object getKey(Integer num) {
                log.in();
                Lane lane = (Lane)laneTable.get(num);
                Object key = lane.subContext;
                log.out();

                return key;
        }


        /**
         * {@inheritDoc}
         */
        protected void selectConnection(ReceiveQueue rq) {
                log.in();
                Lane lane = (Lane)laneTable.get(rq.activeNum());
                synchronized (lane) {
                        mtu          = lane.mtu;
                        headerOffset = lane.headerLength;
                }
                log.out();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void closeConnection(ReceiveQueue rq, Integer num) throws IOException {
                log.in();
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

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
         * {@inheritDoc}
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
