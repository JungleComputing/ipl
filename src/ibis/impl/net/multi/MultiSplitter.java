package ibis.ipl.impl.net.multi;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Provides a generic multiple network output poller.
 */
public final class MultiSplitter extends NetOutput {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected Vector    outputVector       = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector           = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector           = null;

	protected Vector    nlsVector          = null;
	protected Vector    localNlsIdVector   = null;
	protected Vector    remoteNlsIdVector  = null;
        protected Hashtable outputTable        = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output  the controlling output.
	 */
	public MultiSplitter(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
		outputVector      = new Vector();
		isVector          = new Vector();
		osVector          = new Vector();
                nlsVector         = new Vector();
                localNlsIdVector  = new Vector();
                remoteNlsIdVector = new Vector();
                outputTable       = new Hashtable();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                try {
                        NetIbisIdentifier localId  = (NetIbisIdentifier)driver.getIbis().identifier();
                        os.writeObject(localId);
                        NetIbisIdentifier remoteId = (NetIbisIdentifier)is.readObject();

                        InetAddress localHostAddr = InetAddress.getLocalHost();
                        os.writeObject(localHostAddr);
                        InetAddress remoteHostAddr = (InetAddress)is.readObject();

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
                

                        NetOutput no = (NetOutput)outputTable.get(subContext);

                        if (no == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                no                      = newSubOutput(subDriver, subContext);
                                outputTable.put(subContext, no);
                                outputVector.add(no);
                        }
                        
                        no.setupConnection(rpn, is, os, nls);

                        {
                                boolean update = false;
                
                                int _mtu = no.getMaximumTransfertUnit();

                                if (mtu == 0  ||  mtu > _mtu) {
                                        update = true;
                                        mtu    = _mtu;
                                }

                                int _headersLength = no.getHeadersLength();

                                if (headerOffset < _headersLength) {
                                        update       = true;
                                        headerOffset = _headersLength;
                                }

                                os.writeInt(mtu);
                                os.writeInt(headerOffset);
                                os.flush();

                                if (update) {
                                        int s = osVector.size();

                                        // Pass 1
                                        for (int i = 0; i < s; i++) {
                                                ObjectOutputStream _os          = (ObjectOutputStream)osVector.elementAt(i);
                                                int                _remoteNlsId = ((Integer)remoteNlsIdVector.elementAt(i)).intValue();
                                                synchronized(os) {
                                                        _os.writeInt(_remoteNlsId);
                                                        _os.writeInt(mtu);
                                                        _os.writeInt(headerOffset);
                                                }
                                        }

                                        // Pass 2
                                        for (int i = 0; i < s; i++) {
                                                ObjectInputStream  _is         = (ObjectInputStream)isVector.elementAt(i);
                                                NetServiceListener _nls        = (NetServiceListener)nlsVector.elementAt(i);
                                                int                _localNlsId = ((Integer)localNlsIdVector.elementAt(i)).intValue();

                                                _nls.acquire(_localNlsId);
                                                _is.readInt();
                                                _nls.release();
                                        }
                                }
                        }

                        osVector.add(os);
                        isVector.add(is);
                        nlsVector.add(nls);
                        int localNlsId = nls.getId();
                        localNlsIdVector.add(new Integer(localNlsId));
                        os.writeInt(localNlsId);
                        os.flush();
                        int remoteNlsId = is.readInt();
                        remoteNlsIdVector.add(new Integer(remoteNlsId));
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new IbisIOException(e);
                }
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IbisIOException {
                super.initSend();
                
		Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.initSend();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
                super.finish();
		Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws IbisIOException {
		if (outputVector != null) {
			Iterator i = outputVector.listIterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
			}
			outputVector = null;
		}
		
		isVector     = null;
		osVector     = null;

		super.free();
	}		

        public void writeByteBuffer(NetSendBuffer buffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByteBuffer(buffer);
		} while (i.hasNext());
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeBoolean(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByte(v);
		} while (i.hasNext());
        }
        
        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeChar(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeShort(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeInt(v);
		} while (i.hasNext());
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeLong(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeFloat(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeDouble(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeString(String v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeString(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeObject(v);
		} while (i.hasNext());
        }

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceBoolean(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceByte(b, o, l);
		} while (i.hasNext());
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceChar(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceShort(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceInt(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceLong(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceFloat(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceDouble(b, o, l);
		} while (i.hasNext());
        }	

        public void writeArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceObject(b, o, l);
		} while (i.hasNext());
        }	
}
