package ibis.ipl.impl.net;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Provides a generic multiple network output poller.
 */
public abstract class NetSplitter extends NetOutput {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected HashMap   outputMap = null;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output the controlling output.
	 */
	public NetSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		outputMap = new HashMap();
	}


	/**
         * Actually establish a connection with a remote port and
         * register an upcall function for incoming message
         * notification.
	 *
         * @param cnx the connection attributes.
         * @param key the connection key in the splitter {@link #outputMap map}.
         * @param no the connection's output.
	 */
	public synchronized void setupConnection(NetConnection cnx, Object key, NetOutput no) throws NetIbisException {
                log.in();
                no.setupConnection(cnx);
                if (outputMap.get(key) == null) {
                        outputMap.put(key, no);
                }
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws NetIbisException {
                log.in();
                super.initSend();

		Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.initSend();
		} while (i.hasNext());
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	public void send() throws NetIbisException {
                log.in();
                super.send();
                Iterator i = outputMap.values().iterator();
                do {
                        NetOutput no = (NetOutput)i.next();
                        no.send();
                } while (i.hasNext());
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
                log.in();
                super.finish();
		Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
                log.out();
	}


	/*
	 * {@inheritDoc}
         */
	public void free() throws NetIbisException {
                log.in();
		if (outputMap != null) {
			Iterator i = outputMap.values().iterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
                                i.remove();
			}
		}

		super.free();
                log.out();
	}
        
        protected abstract Object getKey(Integer num);
        public abstract void closeConnection(Integer num) throws NetIbisException;


        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
                closeConnection(num);
                log.out();
        }

        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByteBuffer(buffer);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeBoolean(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByte(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeChar(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeShort(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeInt(v);
		} while (i.hasNext());
                log.out();
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeLong(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeFloat(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeDouble(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeString(String v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeString(v);
		} while (i.hasNext());
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeObject(v);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(byte [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }
        public void writeArray(char [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(short [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(int [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(long [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(float [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(double [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }

        public void writeArray(Object [] b, int o, int l) throws NetIbisException {
                log.in();
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArray(b, o, l);
		} while (i.hasNext());
                log.out();
        }
}
