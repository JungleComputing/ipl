package ibis.ipl.impl.net.multi;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Provides a generic multiple network output poller.
 */
public final class MultiSplitter extends NetSplitter {

        private final class Lane {
                NetConnection cnx          = null;
                NetOutput     output       = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ObjectInputStream  is      = null;
                ObjectOutputStream os      = null;
        }

        protected Hashtable laneTable    = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public MultiSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		laneTable   = new Hashtable();
	}

        private String getSubContext(NetIbisIdentifier localId, InetAddress localHostAddr, NetIbisIdentifier remoteId, InetAddress remoteHostAddr) {
                log.in();
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
                log.out();
                return subContext;
        }


        private void updateSizes() throws NetIbisException {
                log.in();
		Iterator i = null;

                i = laneTable.values().iterator();

                // Pass 1
                while (i.hasNext()) {
                        Lane _lane = (Lane)i.next();

                        try {
                                _lane.os.writeInt(mtu);
                                _lane.os.writeInt(headerOffset);
                                _lane.os.flush();
                        } catch (IOException e) {
                                throw new NetIbisIOException(e);
                        }
                }

                i = laneTable.values().iterator();
                // Pass 2
                while (i.hasNext()) {
                        Lane _lane = (Lane)i.next();

                        try {
                                _lane.is.readInt();
                        } catch (IOException e) {
                                throw new NetIbisIOException(e);
                        }
                }
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                try {
                        NetServiceLink link = null;
                        link = cnx.getServiceLink();

                        ObjectOutputStream 	os 	= new ObjectOutputStream(link.getOutputSubStream(this, "multi"));
                        os.flush();

                        ObjectInputStream  	is 	= new ObjectInputStream (link.getInputSubStream (this, "multi"));

                        NetIbisIdentifier 	localId		= (NetIbisIdentifier)driver.getIbis().identifier();
                        os.writeObject(localId);
                        os.flush();

                        NetIbisIdentifier 	remoteId	= (NetIbisIdentifier)is.readObject();

                        InetAddress	localHostAddr	= InetAddress.getLocalHost();
                        os.writeObject(localHostAddr);
                        os.flush();

                        InetAddress	remoteHostAddr	= (InetAddress)is.readObject();
                        String		subContext	= getSubContext(localId, localHostAddr, remoteId, remoteHostAddr);
                        NetOutput 	no 		= (NetOutput)outputTable.get(subContext);

                        if (no == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                no                      = newSubOutput(subDriver, subContext);
                        }

                        super.setupConnection(cnx, subContext, no);

                        Lane lane = new Lane();
                        lane.os           = os;
                        lane.is           = is;
                        lane.cnx          = cnx;
                        lane.output       = no;
                        lane.headerLength = no.getHeadersLength();
                        lane.mtu          = no.getMaximumTransfertUnit();

                        {
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
                        }

                        laneTable.put(cnx.getNum(), lane);
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
                log.out();
	}


        public synchronized void close(Integer num) throws NetIbisException {
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


	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                log.in();
                if (laneTable != null) {
                        Iterator i = laneTable.values().iterator();
                        while (i.hasNext()) {
                                i.next();
                                i.remove();
                        }
                }

		super.free();
                log.out();
	}

}
