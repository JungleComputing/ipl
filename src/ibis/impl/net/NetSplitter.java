package ibis.impl.net;

import java.io.IOException;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

/**
 * Provides a generic multiple network output poller.
 */
public class NetSplitter extends NetOutput implements NetBufferedOutputSupport {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected HashMap   outputMap = null;
	private NetOutput   singleton;

	protected boolean	writeBufferedSupported = true;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver = null;


	/**
	 * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param driver the driver of this poller.
	 * @param context the context.
	 */
	public NetSplitter(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
		outputMap = new HashMap();
	}


	private void setSingleton() {
	    if (outputMap.values().size() == 1) {
		Collection c = outputMap.values();
		Iterator i = c.iterator();
		singleton = (NetOutput)i.next();
// System.err.println(this + ": this is a singleton splitter");
	    } else {
// System.err.println(this + ": this is a NON-singleton splitter");
		singleton = null;
	    }
	}


	/**
	 * Adds a new input to the output set.
	 *
	 * The MTU and the header offset is updated by this function.
	 *
	 * @param output the output.
	 */
	private void addOutput(Integer rpn, NetOutput output) {
                log.in();
		int _mtu = output.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = output.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}

		outputMap.put(rpn, output);
                log.out();
	}

	protected void setWriteBufferedSupported() {
	    writeBufferedSupported = false;
	    Collection c = outputMap.values();
	    Iterator i = c.iterator();

	    while (i.hasNext()) {
		NetOutput output  = (NetOutput)i.next();
		if (! output.writeBufferedSupported()) {
		    writeBufferedSupported = false;
		    break;
		}
	    }
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean writeBufferedSupported() {
	    return writeBufferedSupported;
	}


	/**
	 * Actually establish a connection with a remote port.
	 *
	 * @param cnx the connection attributes.
	 * @exception IOException if the connection setup fails.
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
		if (subDriver == null) {
			String subDriverName = getProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetOutput no = newSubOutput(subDriver);
		setupConnection(cnx, cnx.getNum(), no);

		int _mtu = no.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = no.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}
                log.out();
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
	public synchronized void setupConnection(NetConnection cnx, Object key, NetOutput no) throws IOException {
                log.in();
                no.setupConnection(cnx);
                if (outputMap.get(key) == null) {
                        outputMap.put(key, no);
			setSingleton();
                }
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IOException {
                log.in();
                super.initSend();
// System.err.println(this + ": in initSend(); notify singleton " + singleton);

		if (singleton != null) {
		    singleton.initSend();
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.initSend();
		    } while (i.hasNext());
		}
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	public int send() throws IOException {
                log.in();
                super.send();
		if (singleton != null) {
		    singleton.send();
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.send();
		    } while (i.hasNext());
		}
                log.out();
		return 0;
	}


	/**
	 * {@inheritDoc}
	 */
	public long finish() throws IOException {
                log.in();
                super.finish();
		if (singleton != null) {
		    singleton.finish();
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.finish();
		    } while (i.hasNext());
		}
                log.out();

		// TODO: return byte count of message
		return 0;
	}


	/*
	 * {@inheritDoc}
         */
	public void free() throws IOException {
                log.in();
		if (outputMap != null) {
			Iterator i = outputMap.values().iterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
                                i.remove();
			}

			setSingleton();
		}

		super.free();
                log.out();
	}
        

        protected Object getKey(Integer num) {
                return num;
        }


        public synchronized void closeConnection(Integer num) throws IOException {
                NetOutput output = (NetOutput)outputMap.get(num);
                if (output != null) {
                        output.close(num);
                }
        }


        public synchronized void close(Integer num) throws IOException {
                log.in();
                closeConnection(num);
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public void flushBuffer() throws IOException {
	    log.in();
	    if (! writeBufferedSupported) {
		throw new IOException("writeBuffered not supported");
	    }

	    if (singleton != null) {
		NetBufferedOutputSupport bo =
		    (NetBufferedOutputSupport)singleton;
		bo.flushBuffer();
	    } else {
		Iterator i = outputMap.values().iterator();
		do {
		    NetOutput no = (NetOutput)i.next();
		    NetBufferedOutputSupport bo =
			(NetBufferedOutputSupport)no;
		    bo.flushBuffer();
		} while (i.hasNext());
	    }
	    log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void writeBuffered(byte[] data, int offset, int length)
		throws IOException {
	    log.in();
	    if (! writeBufferedSupported) {
		throw new IOException("writeBuffered not supported");
	    }

	    if (singleton != null) {
		NetBufferedOutputSupport bo =
		    (NetBufferedOutputSupport)singleton;
		bo.writeBuffered(data, offset, length);
// System.err.println(this + ": writeBuffered to singleton " + bo);
	    } else {
		Iterator i = outputMap.values().iterator();
		do {
		    NetOutput no = (NetOutput)i.next();
		    NetBufferedOutputSupport bo =
			(NetBufferedOutputSupport)no;
		    bo.writeBuffered(data, offset, length);
// System.err.println(this + ": writeBuffered to NON-singleton " + bo + (i.hasNext() ? "" : "NO ") + " more to come");
		} while (i.hasNext());
	    }
	    log.out();
	}


	/**
	 * {@inheritDoc}
	 */
        public void writeByteBuffer(NetSendBuffer buffer) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeByteBuffer(buffer);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeByteBuffer(buffer);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeBoolean(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeBoolean(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeByte(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeByte(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeChar(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeChar(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeShort(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeShort(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeInt(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeInt(v);
		    } while (i.hasNext());
		}
                log.out();
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeLong(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeLong(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeFloat(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeFloat(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeDouble(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeDouble(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeString(String v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeString(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeString(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeObject(v);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeObject(v);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(boolean [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
		log.out();
        }

        public void writeArray(byte [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }
        public void writeArray(char [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(short [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(int [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(long [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(float [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(double [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }

        public void writeArray(Object [] b, int o, int l) throws IOException {
                log.in();
		if (singleton != null) {
		    singleton.writeArray(b, o, l);
		} else {
		    Iterator i = outputMap.values().iterator();
		    do {
			    NetOutput no = (NetOutput)i.next();
			    no.writeArray(b, o, l);
		    } while (i.hasNext());
		}
                log.out();
        }
}
