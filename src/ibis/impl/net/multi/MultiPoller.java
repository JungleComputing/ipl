package ibis.ipl.impl.net.multi;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * Provides a generic multiple network input poller.
 */
public final class MultiPoller extends NetPoller {

        private final class Lane {
                NetConnection cnx          = null;
                ReceiveQueue  queue        = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ServiceThread thread       = null;
                ObjectInputStream  is      =  null;
                ObjectOutputStream os      =  null;
        }

        private final class ServiceThread extends Thread {
                private Lane               lane        =  null;
                private boolean            exit        = false;

                public ServiceThread(String name, Lane lane) throws NetIbisException {
                        super("ServiceThread: "+name);
                        this.lane = lane;
                }

                public void run() {
                        while (!exit) {
                                try {
                                        int newMtu          = lane.is.readInt();
                                        int newHeaderLength = lane.is.readInt();

                                        synchronized(lane) {
                                                lane.mtu          = newMtu;
                                                lane.headerLength = newHeaderLength;
                                        }

                                        lane.os.writeInt(1);
                                        lane.os.flush();
                                } catch (NetIbisInterruptedException e) {
                                        break;
                                } catch (NetIbisClosedException e) {
                                        break;
                                } catch (java.io.InterruptedIOException e) {
                                        break;
                                } catch (java.io.EOFException e) {
                                        break;
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        throw new Error(e);
                                }
                        }
                }

                public void end() {
                        exit = true;
                        this.interrupt();
                }
        }


	/**
	 * Our extension to the set of inputs.
	 */
        protected Hashtable laneTable         = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public MultiPoller(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
	    super(pt, driver, context);
	    laneTable         = new Hashtable();
	}


	/**
	 * {@inheritDoc}
	 */
	protected void selectInput(Integer spn) throws NetIbisClosedException {
	    Lane lane = (Lane)laneTable.get(spn);
	    if (lane == null) {
		throw new NetIbisClosedException("connection "+spn+" closed");
	    }

	    activeQueue = lane.queue;
	    if (activeQueue == null) {
		throw new NetIbisClosedException("connection "+spn+" closed");
	    }
	}


        private String getSubContext(NetIbisIdentifier localId, InetAddress localHostAddr, NetIbisIdentifier remoteId, InetAddress remoteHostAddr) {
                String subContext = null;

                if (localId.equals(remoteId)) {
                        subContext = "process";
                } else {
                        byte [] l = localHostAddr.getAddress();
                        byte [] r = remoteHostAddr.getAddress();
                        int n = 0;

                        while (n < 4 && l[n] == r[n])
                                n++;

                        switch (n) {
                        case 4:
                                {
                                        subContext = "node";
                                        break;
                                }

                        case 3:
                                {
                                        subContext = "net_c";
                                        break;
                                }

                        case 2:
                                {
                                        subContext = "net_b";
                                        break;
                                }

                        case 1:
                                {
                                        subContext = "net_a";
                                        break;
                                }

                        default:
                                {
                                        subContext = "internet";
                                        break;
                                }
                        }
                }

                return subContext;
        }


	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                // System.err.println("MultiPoller: setupConnection-->");
                try {
                        NetServiceLink link = cnx.getServiceLink();

                        ObjectInputStream  is = new ObjectInputStream (link.getInputSubStream (this, "multi"));

                        ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream(this, "multi"));
                        os.flush();

                        //System.err.println("MultiPoller: setupConnection - 2");
                        NetIbisIdentifier localId  = (NetIbisIdentifier)driver.getIbis().identifier();
                        NetIbisIdentifier remoteId = (NetIbisIdentifier)is.readObject();

                        os.writeObject(localId);
                        os.flush();

                        //System.err.println("MultiPoller: setupConnection - 3");

                        InetAddress localHostAddr  = InetAddress.getLocalHost();
                        InetAddress remoteHostAddr = (InetAddress)is.readObject();

                        os.writeObject(localHostAddr);
                        os.flush();

                        //System.err.println("MultiPoller: setupConnection - 4");

			NetInput ni = null;
                        String   subContext = getSubContext(localId, localHostAddr, remoteId, remoteHostAddr);
                        ReceiveQueue q = (ReceiveQueue)inputTable.get(subContext);

                        if (q == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                ni                      = newSubInput(subDriver, subContext);
                        } else {
				ni = q.input;
			}

                        //System.err.println("MultiPoller: setupConnection - 5");

			super.setupConnection(cnx, subContext, ni);

                        //System.err.println("MultiPoller: setupConnection - 6");

			if (q == null) {
			    q = (ReceiveQueue)inputTable.get(subContext);
			}

                        Integer num  = cnx.getNum();
                        Lane    lane = new Lane();

                        lane.is           = is;
                        lane.os           = os;
                        lane.cnx          = cnx;
                        lane.queue        = q;
                        lane.mtu          = is.readInt();
                        lane.headerLength = is.readInt();
                        lane.thread       = new ServiceThread("subcontext = "+subContext+", spn = "+num, lane);

                        //System.err.println("MultiPoller: setupConnection - 7");

                        laneTable.put(num, lane);

                        lane.thread.start();
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
                // System.err.println("MultiPoller: setupConnection<--");
	}


	/**
	 * {@inheritDoc}
	 */
	protected void selectConnection(ReceiveQueue ni) {
	    Lane lane = (Lane)laneTable.get(activeNum);
	    synchronized (lane) {
		mtu          = lane.mtu;
		headerOffset = lane.headerLength;
	    }
	}


	/**
	 * {@inheritDoc}
	 *
	public void finish() throws NetIbisException {
                // System.err.println("MultiPoller: finish-->");
		activeQueue.input.finish();
		super.finish();
                // System.err.println("MultiPoller: finish<--");
	}
	*/


	/**
	 * {@inheritDoc}
	 */
        public synchronized void close(Integer num) throws NetIbisException {
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                if (lane.queue.input != null) {
                                        lane.queue.input.close(num);
                                }

                                if (lane.thread != null) {
                                        lane.thread.end();
                                }

                                laneTable.remove(num);

                                if (activeQueue == lane.queue) {
                                        activeQueue = null;
                                        activeNum   = null;
                                        activeUpcallThread = null;
                                        notifyAll();
                                }
                        }
                }

        }


	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
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

	    super.free();
	}

}
