package ibis.ipl.impl.net;

import java.util.Iterator;
import java.util.Hashtable;

/**
 * Provides a generic multiple network output poller.
 */
public abstract class NetSplitter extends NetOutput {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected Hashtable   outputTable = null;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output  the controlling output.
	 */
	public NetSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		outputTable = new Hashtable();
	}


	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx, Object key, NetOutput no) throws NetIbisException {

	    no.setupConnection(cnx);
	    if (outputTable.get(key) == null) {
		outputTable.put(key, no);
	    }
	}


	/**
	 * {@inheritDoc}
	 */
	public void send() throws NetIbisException {
	    // System.err.println(this + ": send-->");
// System.err.print(">");
// Thread.dumpStack();
	    super.send();
	    Iterator i = outputTable.values().iterator();
	    do {
		NetOutput no = (NetOutput)i.next();
		no.send();
	    } while (i.hasNext());
	    // System.err.println(this + ": send<--");
	}


	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
                super.finish();
		Iterator i = outputTable.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
	}


	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws NetIbisException {
System.err.println(this + ": free()");
Thread.dumpStack();
		if (outputTable != null) {
			Iterator i = outputTable.values().iterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
                                i.remove();
			}
		}
		
		super.free();
	}		

        public synchronized void close(Integer num) throws NetIbisException {
                if (outputTable != null) {
                        NetOutput no = (NetOutput)outputTable.get(num);
                        no.close(num);
                        outputTable.remove(num);
                }
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
