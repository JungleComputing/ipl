package ibis.impl.net.multi;

import ibis.impl.net.NetIbis;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbisIdentifier;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetServiceLink;
import ibis.impl.net.NetSplitter;

import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Provides a generic multiple network output poller.
 */
public final class MultiSplitter extends NetSplitter {

	private static final boolean IS_GEN = TypedProperties.booleanProperty(NetIbis.multi_gen, false);

        private final static class Lane {
                NetOutput     output       = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ObjectInputStream  is      = null;
                ObjectOutputStream os      = null;
                String             subContext = null;
        }

        private Hashtable laneTable        = null;

        private MultiPlugin plugin         = null;

	/**
	 * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param driver the driver of this poller.
	 * @param context the context.
	 */
	public MultiSplitter(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
		laneTable   = new Hashtable();

                String pluginName = getProperty("Plugin");
                //System.err.println("multi-protocol plugin: "+pluginName);

                if (pluginName != null) {
                        plugin = ((Driver)driver).loadPlugin(pluginName);
                }
                //System.err.println("multi-protocol plugin loaded");
	}

        private void updateSizes() throws IOException {
                log.in();
		Iterator i = null;
                i = laneTable.values().iterator();

                // Pass 1
                while (i.hasNext()) {
                        Lane _lane = (Lane)i.next();

			_lane.os.writeInt(mtu);
			_lane.os.writeInt(headerOffset);
			_lane.os.flush();
                }

                i = laneTable.values().iterator();
                // Pass 2
                while (i.hasNext()) {
                        Lane _lane = (Lane)i.next();

			int v = _lane.is.readInt();
			if (v != 3) {
				throw new Error("invalid value");
			}
                }
                log.out();
        }


	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
		Integer num  = cnx.getNum();
		NetServiceLink link = cnx.getServiceLink();


		ObjectOutputStream 	os 	= new ObjectOutputStream(link.getOutputSubStream(this, "multi"));
		os.flush();

		ObjectInputStream  	is 	= new ObjectInputStream (link.getInputSubStream (this, "multi"));


		NetIbisIdentifier localId = (NetIbisIdentifier)driver.getIbis().identifier();
		os.writeObject(localId);
		os.flush();

		NetIbisIdentifier remoteId;
		try {
			remoteId = (NetIbisIdentifier)is.readObject();
		} catch (ClassNotFoundException e) {
			throw new Error("Cannot find class NetIbisIdentifier", e);
		}


		String          subContext      = (plugin!=null)?plugin.getSubContext(true, localId, remoteId, os, is):null;
		NetOutput 	no 		= (NetOutput)outputMap.get(subContext);

		if (IS_GEN || no == null) {
			String    subDriverName = getProperty(subContext, "Driver");
			trace.disp("subContext = ["+subContext+"], driver = ["+subDriverName+"]");
			NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
			no                      = newSubOutput(subDriver, subContext);
		}

		super.setupConnection(cnx, subContext, no);

		Lane lane = new Lane();
		lane.os           = os;
		lane.is           = is;
		lane.output       = no;
		lane.headerLength = no.getHeadersLength();
		lane.mtu          = no.getMaximumTransfertUnit();
		lane.subContext   = subContext;

		boolean update = false;

		if (mtu == 0  ||  mtu > lane.mtu) {
			update = true;
			mtu    = lane.mtu;
			if (factory != null) {
			    factory.setMaximumTransferUnit(mtu);
			}
		}

		if (headerOffset < lane.headerLength) {
			update       = true;
			headerOffset = lane.headerLength;
		}

		os.writeInt(mtu);
		os.writeInt(headerOffset);
		os.flush();

		if (update) {
			updateSizes();
		}

		laneTable.put(cnx.getNum(), lane);
                log.out();
	}

        protected Object getKey(Integer num) {
                log.in();
                Lane lane = (Lane)laneTable.get(num);
                Object key = lane.subContext;
                log.out();

                return key;
        }

        public synchronized void closeConnection(Integer num) throws IOException {
                log.in();
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                NetOutput output = lane.output;
                                if (output != null) {
                                        output.close(num);
                                }

                                laneTable.remove(num);
                        }
                }
                log.out();
        }

        /*
        public synchronized void close(Integer num) throws IOException {
                log.in();
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                if (lane.output != null) {
                                        lane.output.close(num);
                                }
                        }

                        laneTable.remove(num);
                }
                log.out();
        }
        */

	/*
	public void free() throws IOException {
                log.in();
                if (laneTable != null) {
                        Iterator i = laneTable.values().iterator();
                        while (i.hasNext()) {
                                i.next();
                                i.remove();
                        }
                }
                log.out();
	}
        */
}
