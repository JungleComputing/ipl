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
public final class MultiSplitter extends NetOutput {

        private final class Lane {
                NetConnection cnx          = null;
                NetOutput     output       = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ObjectInputStream  is      = null;
                ObjectOutputStream os      = null;
        }        

        protected Hashtable laneTable    = null;
        protected Hashtable outputTable  = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output  the controlling output.
	 */
	public MultiSplitter(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
		super(pt, driver, up, context);
		laneTable   = new Hashtable();
                outputTable = new Hashtable();
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
        

        private void updateSizes() throws NetIbisException {
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
        }
        

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                // System.err.println("MultiSplitter: setupConnection-->");
                try {
                        NetServiceLink link = null;
                        link = cnx.getServiceLink();

                        ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream(this, "multi"));
                        os.flush();

                        ObjectInputStream  is = new ObjectInputStream (link.getInputSubStream (this, "multi"));

                        // System.err.println("MultiSplitter: setupConnection - 2");

                        NetIbisIdentifier localId  = (NetIbisIdentifier)driver.getIbis().identifier();
                        os.writeObject(localId);
                        os.flush();
                        
                        NetIbisIdentifier remoteId = (NetIbisIdentifier)is.readObject();

                        // System.err.println("MultiSplitter: setupConnection - 3");

                        InetAddress localHostAddr = InetAddress.getLocalHost();
                        os.writeObject(localHostAddr);
                        os.flush();
                        
                        InetAddress remoteHostAddr = (InetAddress)is.readObject();

                        // System.err.println("MultiSplitter: setupConnection - 4");

                        String subContext = getSubContext(localId, localHostAddr,
                                                          remoteId, remoteHostAddr);
                        NetOutput no = (NetOutput)outputTable.get(subContext);

                        if (no == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                no                      = newSubOutput(subDriver, subContext);
                                outputTable.put(subContext, no);
                        }
                        
                        // System.err.println("MultiSplitter: setupConnection - 5");

                        no.setupConnection(cnx);

                        // System.err.println("MultiSplitter: setupConnection - 6");

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

                        // System.err.println("MultiSplitter: setupConnection - 7");

                        laneTable.put(cnx.getNum(), lane);
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
                // System.err.println("MultiSplitter: setupConnection<--");
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws NetIbisException {
                super.initSend();
                
		Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.initSend();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
                // System.err.println("MultiSplitter: finish-->");
                super.finish();
		Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
                // System.err.println("MultiSplitter: finish<--");
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                if (lane.output != null) {
                                        lane.output.close(num);
                                }
                        }

                        laneTable.remove(num);
                }
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws NetIbisException {
                if (laneTable != null) {
                        Iterator i = laneTable.values().iterator();
                        while (i.hasNext()) {
                                i.next();
                                i.remove();
                        }
                }

		if (outputTable != null) {
			Iterator i = outputTable.values().iterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
			}
		}
		
		super.free();
	}		

        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByteBuffer(buffer);
		} while (i.hasNext());
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeBoolean(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByte(v);
		} while (i.hasNext());
        }
        
        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeChar(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeShort(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeInt(v);
		} while (i.hasNext());
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeLong(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeFloat(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeDouble(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeString(String v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeString(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeObject(v);
		} while (i.hasNext());
        }

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceBoolean(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceByte(b, o, l);
		} while (i.hasNext());
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceChar(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceShort(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceInt(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceLong(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceFloat(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceDouble(b, o, l);
		} while (i.hasNext());
        }	

        public void writeArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceObject(b, o, l);
		} while (i.hasNext());
        }	
}
