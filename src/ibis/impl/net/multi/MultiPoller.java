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
                private Lane    lane    =  null;
                private boolean exit    = false;

                public ServiceThread(String name, Lane lane) throws NetIbisException {
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
        private Hashtable laneTable = null;

        /**
         * Constructor.
         *
         * @param staticProperties the port's properties.
         * @param driver the driver of this poller.
         */
        public MultiPoller(NetPortType pt, NetDriver driver, String context)
                throws NetIbisException {
                super(pt, driver, context);
                laneTable = new Hashtable();
        }

	/*
	 * Deprecate?

        ***
         * {@inheritDoc}
         **
        protected void selectInput(Integer spn) throws NetIbisClosedException {
                log.in();
                Lane lane = (Lane)laneTable.get(spn);
                if (lane == null) {
                        throw new NetIbisClosedException("connection "+spn+" closed");
                }

                activeQueue = lane.queue;
                if (activeQueue == null) {
                        throw new NetIbisClosedException("connection "+spn+" closed");
                }
                log.out();
        }
	*/

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

        /**
         * {@inheritDoc}
         */
        public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                try {
                        NetServiceLink          link = cnx.getServiceLink();

                        ObjectInputStream       is      = new ObjectInputStream (link.getInputSubStream (this, "multi"));
                        ObjectOutputStream      os      = new ObjectOutputStream(link.getOutputSubStream(this, "multi"));

                        os.flush();

                        NetIbisIdentifier       localId         = (NetIbisIdentifier)driver.getIbis().identifier();
                        NetIbisIdentifier       remoteId        = (NetIbisIdentifier)is.readObject();

                        os.writeObject(localId);
                        os.flush();

                        InetAddress     localHostAddr   = InetAddress.getLocalHost();
                        InetAddress     remoteHostAddr  = (InetAddress)is.readObject();

                        os.writeObject(localHostAddr);
                        os.flush();

                        NetInput        ni              = null;
                        String          subContext      = getSubContext(localId, localHostAddr, remoteId, remoteHostAddr);
                        ReceiveQueue q = (ReceiveQueue)inputTable.get(subContext);

                        if (q == null) {
                                String          subDriverName   = getProperty(subContext, "Driver");
                                NetDriver       subDriver       = driver.getIbis().getDriver(subDriverName);
                                ni = newSubInput(subDriver, subContext);
                        } else {
                                ni = q.input();
                        }

                        super.setupConnection(cnx, subContext, ni);

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

                        laneTable.put(num, lane);

                        lane.thread.start();
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
                log.out();
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
        public synchronized void closeConnection(ReceiveQueue rq, Integer num) throws NetIbisException {
                log.in();
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                if (lane.queue.input() != null) {
                                        lane.queue.input().close(num);
                                }

                                if (lane.thread != null) {
                                        lane.thread.end();
                                }

                                laneTable.remove(num);
                        }
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
                                Lane lane = (Lane)i.next();
                                if (lane != null && lane.thread != null) {
                                        lane.thread.end();
                                }
                                i.remove();
                        }
                }

                super.free();
                log.out();
        }
}
