package ibis.impl.net.gm;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferFactoryImpl;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.impl.net.InterruptedIOException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

/**
 * The GM output implementation (block version).
 */
public final class GmOutput extends NetBufferedOutput {

        /**
         * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
         * local number.
         */
        private volatile Integer rpn          = null;

        private long       deviceHandle =  0;
        private long       outputHandle =  0;
        private int        lnodeId      = -1;
        private int        lportId      = -1;
        private int        lmuxId       = -1;
        private int        lockId       = -1;
        private int []     lockIds      = null;
        private int        rnodeId      = -1;
        private int        rportId      = -1;
        private int        rmuxId       = -1;
        private Driver     gmDriver     = null;
	private boolean    mustFlush;
	private int        flushing;


        native long nInitOutput(long deviceHandle) throws IOException;
        native int  nGetOutputNodeId(long outputHandle) throws IOException;
        native int  nGetOutputPortId(long outputHandle) throws IOException;
        native int  nGetOutputMuxId(long outputHandle) throws IOException;
        native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IOException;

	native void nSendByte(long outputHandle, byte value) throws IOException;

	native boolean nTryFlush(long outputHandle, int length) throws IOException; 

        native void nSendRequest(long outputHandle, int base, int length) throws IOException;
        native void nSendBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
        native void nSendBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

        native void nSendBooleanBufferIntoRequest(long outputHandle, boolean []b, int base, int length) throws IOException;
        native void nSendBooleanBuffer(long outputHandle, boolean []b, int base, int length) throws IOException;

        native void nSendByteBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
        native void nSendByteBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

         native void nSendShortBufferIntoRequest(long outputHandle, short []b, int base, int length) throws IOException;
        native void nSendShortBuffer(long outputHandle, short []b, int base, int length) throws IOException;

         native void nSendCharBufferIntoRequest(long outputHandle, char []b, int base, int length) throws IOException;
        native void nSendCharBuffer(long outputHandle, char []b, int base, int length) throws IOException;

         native void nSendIntBufferIntoRequest(long outputHandle, int []b, int base, int length) throws IOException;
        native void nSendIntBuffer(long outputHandle, int []b, int base, int length) throws IOException;

         native void nSendLongBufferIntoRequest(long outputHandle, long []b, int base, int length) throws IOException;
        native void nSendLongBuffer(long outputHandle, long []b, int base, int length) throws IOException;

         native void nSendFloatBufferIntoRequest(long outputHandle, float []b, int base, int length) throws IOException;
        native void nSendFloatBuffer(long outputHandle, float []b, int base, int length) throws IOException;

         native void nSendDoubleBufferIntoRequest(long outputHandle, double []b, int base, int length) throws IOException;
        native void nSendDoubleBuffer(long outputHandle, double []b, int base, int length) throws IOException;

	native void nFlush(long outputHandle) throws IOException;

        native void nCloseOutput(long outputHandle) throws IOException;

        /**
         * Constructor.
         *
         * @param pt the properties of the output's
         * {@link ibis.impl.net.NetSendPort NetSendPort}.
         * @param driver the GM driver instance.
         */
        GmOutput(NetPortType pt, NetDriver driver, String context)
                throws IOException {
                super(pt, driver, context);

                gmDriver = (Driver)driver;

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                outputHandle = nInitOutput(deviceHandle);
                Driver.gmAccessLock.unlock();

		NetBufferFactoryImpl impl = new NetSendBufferFactoryDefaultImpl();

		mtu = Driver.mtu;

		factory = new NetBufferFactory(mtu, impl);
		// factory = new NetBufferFactory(Driver.byteBufferSize, impl);
		// // setBufferFactory(factory);
		// Try to tune writeBuffered so it takes the copy route
		arrayThreshold = mtu;
        }

        /*
         * Sets up an outgoing GM connection.
         *
         * @param rpn {@inheritDoc}
         * @param is {@inheritDoc}
         * @param os {@inheritDoc}
         */
        public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }

                Driver.gmAccessLock.lock(false);
                lnodeId = nGetOutputNodeId(outputHandle);
                lportId = nGetOutputPortId(outputHandle);
                lmuxId  = nGetOutputMuxId(outputHandle);
                lockId  = lmuxId*2 + 1;

                lockIds = new int[2];
                lockIds[0] = lockId; // output lock
                lockIds[1] = 0;      // main   lock
// System.err.println(this + ": initializing lockIds, my lockId " + lockId);
// Thread.dumpStack();
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                Hashtable rInfo = null;

		ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream (this, "gm"));
		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
		os.flush();

		try {
                        rInfo = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

		rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
		rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
		rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();
		os.writeObject(lInfo);
		os.flush();

		Driver.gmAccessLock.lock(false);
		nConnectOutput(outputHandle, rnodeId, rportId, rmuxId);
		Driver.gmAccessLock.unlock();

		is.read();
		os.write(1);
		os.flush();

		is.close();
		os.close();

                this.rpn = cnx.getNum();

                mtu = Driver.mtu;

                log.out();
        }


	/**
	 * Pump in the face of InterruptedIOExceptions
	 */
	private void pump(int[] lockIds) throws IOException {
	    boolean interrupted;
	    do {
		try {
		    gmDriver.blockingPump(lockIds);
		    interrupted = false;
		} catch (InterruptedIOException e) {
		    // try once more
		    interrupted = true;
		    System.err.println(this + ": ********** Catch InterruptedIOException " + e);
		}
	    } while (interrupted);
	}


	/**
	 * Pump in the face of InterruptedIOExceptions
	 */
	private void pump(int lockId, int[] lockIds) throws IOException {
	    boolean interrupted;
	    do {
		try {
		    gmDriver.blockingPump(lockId, lockIds);
		    interrupted = false;
		} catch (InterruptedIOException e) {
		    // try once more
		    interrupted = true;
		    System.err.println(this + ": ********** Catch InterruptedIOException " + e);
		}
	    } while (interrupted);
	}


	/**
	 * Flush the buffers that the native layer has built up.
	 */
	private void flushAllBuffers() throws IOException {
	    if (! mustFlush) {
		return;
	    }

// System.err.println(this + ": Now flush the buffers");
// Thread.dumpStack();
	    Driver.gmAccessLock.lock(true);
	    nFlush(outputHandle);
	    Driver.gmAccessLock.unlock();
	    mustFlush = false;
	    /* Wait for buffer send completion */
	    pump(lockId, lockIds);
	}

	/**
	 * {@inheritDoc}
	 */
	public void flushBuffer() throws IOException {
// System.err.println(this + ": flushBuffer()");
// Thread.dumpStack();
	    flushing++;
	    super.flushBuffer();
// System.err.println(this + ": past super.flushBuffer()");
	    flushAllBuffers();
	    flushing--;
	}

	/**
	 * Pre=postcondition: caller must have Driver.gmAccesslock
	 */
	private boolean tryFlush(int length) throws IOException {
	    boolean mustFlush = nTryFlush(outputHandle,
					  length + available());
	    if (mustFlush) {
		Driver.gmAccessLock.unlock();
		flushBuffer();
		Driver.gmAccessLock.lock(true);
	    }
	    return mustFlush;
	}

	private void sendRequest(int offset, int length) throws IOException {
	    if (flushing > 0) {
		flushAllBuffers();
	    } else {
		flushBuffer();
	    }

// System.err.print("[");
// System.err.println("Post a request");
	    /* Post the 'request' */
	    Driver.gmAccessLock.lock(true);
	    nSendRequest(outputHandle, offset, length);
	    Driver.gmAccessLock.unlock();

// System.err.println("Wait for request sent completion");
	    /* Wait for 'request' send completion */
	    pump(lockId, lockIds);

// System.err.println("Wait for rendez-vous ack");
	    /* Wait for 'ack' completion */
	    pump(lockId, lockIds);
// System.err.print("]");
	}


        /**
         * {@inheritDoc}
	 *
	 * The byte buffer that has been accumulated by our super is
	 * appended in the message buffer, but in a known place.
	 * The receiver can read it immediately in this way, before
	 * other typed buffers that may have been pushed earlier.
         */
        public void sendByteBuffer(NetSendBuffer b) throws IOException {
                log.in();

                if (b.length > Driver.packetMTU) {
                        /* Post the 'request' */
// System.err.print("[");
// System.err.println("Post a request");
			sendRequest(b.base, b.length);

                        /* Post the 'buffer' */
                        Driver.gmAccessLock.lock(true);
                        nSendBuffer(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'buffer' send completion */
                        gmDriver.blockingPump(lockId, lockIds);
// System.err.print("]");

                } else {
// System.err.print("<*");
// System.err.println("Send lockId " + lockId + " byte buffer " + b + " offset " + b.base + " size " + b.length);
// Thread.dumpStack();
                        Driver.gmAccessLock.lock(true);
                        nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

			/* @@@@@@@@@@@@@@@@@ IS THIS CORRECT?????????????? */
			mustFlush = true;
// System.err.print(">*");
                }

		if (! b.ownershipClaimed) {
			b.free();
		}

                log.out();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void close(Integer num) throws IOException {
                log.in();
// System.err.println(this + ": close");
// Thread.dumpStack();
                if (rpn == num) {
                        Driver.gmAccessLock.lock(true);
                        Driver.gmLockArray.deleteLock(lockId);

                        if (outputHandle != 0) {
                                nCloseOutput(outputHandle);
                                outputHandle = 0;
                        }

                        if (deviceHandle != 0) {
                                Driver.nCloseDevice(deviceHandle);
                                deviceHandle = 0;
                        }
                        Driver.gmAccessLock.unlock();
                        rpn = null;
                }
                log.out();
        }


        /**
         * {@inheritDoc}
         */
        public void free() throws IOException {
                log.in();
                rpn = null;

                Driver.gmAccessLock.lock(true);
                Driver.gmLockArray.deleteLock(lockId);

                if (outputHandle != 0) {
                        nCloseOutput(outputHandle);
                        outputHandle = 0;
                }

                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmAccessLock.unlock();

                super.free();
                log.out();
        }

        public void writeArray(boolean [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();
// System.err.println("Send boolean array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendBooleanBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendBooleanBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

	/**
	 * {@inheritDoc}
	 *
	 * We provide our own implementation of read/writeArray(byte[])
	 * because super's uses its own buffering, which is incompatible
	 * with the buffering used in NetGM.
	 */
        public void writeArray(byte [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

// System.err.println("Send byte array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);
// System.err.println(this + ": send Rndz-vous msg size " + l + " packet size " + _l);

				sendRequest(o, l);

                                // Post the 'buffer'
                                Driver.gmAccessLock.lock(true);
                                nSendByteBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                // Wait for 'buffer' send completion
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendByteBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(char [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 1;
                o <<= 1;
// System.err.println("Send char array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendCharBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendCharBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(short [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 1;
                o <<= 1;
// System.err.println(this + ": Send short array; byte offset " + o + " size " + l);
// Thread.dumpStack();
// for (int i = 0; i < b.length; i++) { System.err.print(b[i] + ","); } System.err.println();

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendShortBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
// System.err.println("Locked access lock");
				tryFlush(_l);
                                nSendShortBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(int [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 2;
                o <<= 2;
// System.err.println("Send int array; byte offset " + o + " size " + l);
// for (int i = 0; i < Math.min(b.length, 32); i++) System.err.print(b[i] + " "); System.err.println();

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendIntBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
// System.err.println("Send int array; byte offset " + o + " size " + l);
// Thread.dumpStack();
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendIntBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(long [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 3;
                o <<= 3;
// System.err.println("Send long array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendLongBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendLongBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(float [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 2;
                o <<= 2;
// System.err.println("Send float array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendFloatBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendFloatBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(double [] b, int o, int l) throws IOException {
                // No, we aggregate and flush explicitly: flushBuffer();

                l <<= 3;
                o <<= 3;
// System.err.println("Send double array; byte offset " + o + " size " + l);

                while (l > 0) {
                        int _l = 0;

                        if (l > Driver.packetMTU) {
                                _l = Math.min(l, mtu);

				sendRequest(o, l);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendDoubleBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send completion */
                                pump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
				tryFlush(_l);
                                nSendDoubleBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

				mustFlush = true;
                        }

                        l -= _l;
                        o += _l;
                }
        }
}
