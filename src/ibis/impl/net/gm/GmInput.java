package ibis.impl.net.gm;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBufferedInput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferFactoryImpl;
import ibis.impl.net.NetReceiveBufferFactoryDefaultImpl;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;


/**
 * The GM input implementation (block version).
 */
public final class GmInput extends NetBufferedInput {

        // protected NetMessageStat newMessageStat(boolean on, String moduleName) {
// System.err.println(" ********** GM Message stats always on");
                // return new NetMessageStat(true, moduleName);
        // }


	/**
	 * The peer {@link ibis.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private volatile Integer      spn  	      = null;

	/**
	 * The buffer block allocator.
	 */
	private NetAllocator          allocator       = null;

        private long                  deviceHandle   =    0;
	private long                  inputHandle    =    0;
        private int                   lnodeId        =   -1;
        private int                   lportId        =   -1;
        private int                   lmuxId         =   -1;
        private int                   lockId         =   -1;
        private int []                lockIds        = null;
        private int                   rnodeId        =   -1;
        private int                   rportId        =   -1;
        private int                   rmuxId         =   -1;
        private int                   blockLen       =    0;
        private boolean               firstBlock     = true;

        private Driver               gmDriver     = null;


	native long nInitInput(long deviceHandle) throws IOException;
        native int  nGetInputNodeId(long inputHandle) throws IOException;
        native int  nGetInputPortId(long inputHandle) throws IOException;
        native int  nGetInputMuxId(long inputHandle) throws IOException;
        native void nConnectInput(long inputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IOException;

	native int nPostByte(long inputHandle) throws IOException;

        native int nPostBuffer(long inputHandle, byte []b, int base, int length) throws IOException;

        native int nPostBooleanBuffer(long inputHandle, boolean []b, int base, int length) throws IOException;

        native int nPostByteBuffer(long inputHandle, byte []b, int base, int length) throws IOException;

        native int nPostShortBuffer(long inputHandle, short []b, int base, int length) throws IOException;

        native int nPostCharBuffer(long inputHandle, char []b, int base, int length) throws IOException;

        native int nPostIntBuffer(long inputHandle, int []b, int base, int length) throws IOException;

        native int nPostLongBuffer(long inputHandle, long []b, int base, int length) throws IOException;

        native int nPostFloatBuffer(long inputHandle, float []b, int base, int length) throws IOException;

        native int nPostDoubleBuffer(long inputHandle, double []b, int base, int length) throws IOException;

	native void nCloseInput(long inputHandle) throws IOException;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param driver the GM driver instance.
	 */
	GmInput(NetPortType pt, NetDriver driver, String context)
		throws IOException {
                super(pt, driver, context);

                gmDriver = (Driver)driver;

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                inputHandle = nInitInput(deviceHandle);
                Driver.gmAccessLock.unlock();

		NetBufferFactoryImpl impl = new NetReceiveBufferFactoryDefaultImpl();
		factory = new NetBufferFactory(Driver.byteBufferSize, impl);
	}

	int getLockId() {
	    return lockId;
	}

	/*
	 * Sets up an incoming GM connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                // System.err.println("setupConnection--> upcallFunc " + upcallFunc);
                if (this.spn != null) {
                        throw new Error("connection already established");
                }

                Driver.gmAccessLock.lock(false);
                lnodeId = nGetInputNodeId(inputHandle);
                lportId = nGetInputPortId(inputHandle);
                lmuxId  = nGetInputMuxId(inputHandle);
                lockId  = lmuxId*2 + 2;

                //System.err.println("initializing lockIds");
                lockIds = new int[2];
                lockIds[0] = lockId; // input lock
                lockIds[1] = 0;      // main  lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                Hashtable rInfo = null;


		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
		os.flush();

		ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream (this, "gm"));
		os.writeObject(lInfo);
		os.flush();

		try {
                        rInfo = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

		rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
		rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
		rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

		Driver.gmAccessLock.lock(false);
		nConnectInput(inputHandle, rnodeId, rportId, rmuxId);
		Driver.gmAccessLock.unlock();

		os.write(1);
		os.flush();
		is.read();

		os.close();
		is.close();

                mtu = Driver.mtu;

		allocator = new NetAllocator(mtu);
		this.spn = cnx.getNum();
                startUpcallThread();
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


int rcvd;
int plld;

        /**
         * {@inheritDoc}
         */
	public Integer doPoll(boolean block) throws IOException {
                log.in();
                if (spn == null) {
                        return null;
		}

                if (block) {
                        pump(lockId, lockIds);
                } else {
                        if (!gmDriver.tryPump(lockId, lockIds)) {
                                System.err.println("poll failed");
                                return null;
                        }
                }

                log.out();

                if (spn == null) {
                        throw new Error("unexpected connection closed");
                }
plld++;

                return spn;
	}

        public void initReceive(Integer num) throws IOException {
                firstBlock = true;
                super.initReceive(num);
        }


	/**
	 * {@inheritDoc}
	 *
	 * The buffering in NetBuffererdOutput confuses Ibis serialization.
	 * Provide a special implementation that bypasses that buffer.
	 */
	/*
	 * Guess we will try to use NetBuffering after all...
	public byte readByte() throws IOException {
	    if (firstBlock) {
		firstBlock = false;
	    } else {
		// Request reception
		pump(lockId, lockIds);
	    }


	    Driver.gmReceiveLock.lock();

	    Driver.gmAccessLock.lock(true);
	    int result = nPostByte(inputHandle);
	    Driver.gmAccessLock.unlock();

	    Driver.gmReceiveLock.unlock();
// System.err.println("Read single byte=" + ((byte)(result & 0xFF)));
// Thread.dumpStack();

	    return (byte)(result & 0xFF);
	}
	*/

	/**
	 * {@inheritDoc}
	 */
	public void receiveByteBuffer(NetReceiveBuffer b) throws IOException {
                log.in();

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        gmDriver.blockingPump(lockId, lockIds);
                }

                Driver.gmReceiveLock.lock();

                Driver.gmAccessLock.lock(true);
                int result = nPostBuffer(inputHandle, b.data, 0, b.data.length);
                Driver.gmAccessLock.unlock();

// System.err.print("<_");
// System.err.println("Post byte buffer request length " + b.length + " data.length " + b.data.length + " result " + result);
// Thread.dumpStack();
                if (result == 0) {
			// A rendez-vous message. Receive the data part.
                        /* Ack completion */
                        gmDriver.blockingPump(lockId, lockIds);

                        /* Communication transmission */
                        gmDriver.blockingPump(lockId, lockIds);

                        if (b.length == 0) {
                                b.length = blockLen;
                        } else if (b.length != blockLen) {
				throw new Error("length mismatch: got "+blockLen+" bytes, "+b.length+" bytes were required");
                        }

                } else {
                        if (b.length == 0) {
                                b.length = result;
                        } else if (b.length != result) {
				throw new Error("length mismatch: got "+result+" bytes, "+b.length+" bytes were required");
                        }
                }
// System.err.print(rcvd + ">_");
// System.err.println("Received byte buffer " + b + " offset " + b.base + " size " + b.length);
// Thread.dumpStack();

rcvd++;
                Driver.gmReceiveLock.unlock();
                log.out();
        }


        public void doFinish() throws IOException {
                log.in();
                //
                log.out();
        }

        public synchronized void doClose(Integer num) throws IOException {
                log.in();
                if (spn == num) {
                        Driver.gmAccessLock.lock(true);
                        Driver.gmLockArray.deleteLock(lockId);

                        if (inputHandle != 0) {
// System.err.println(this + ".doClose(): call nCloseInput");
// Thread.dumpStack();
                                nCloseInput(inputHandle);
                                inputHandle = 0;
                        }

                        if (deviceHandle != 0) {
                                Driver.nCloseDevice(deviceHandle);
                                deviceHandle = 0;
                        }

                        Driver.gmAccessLock.unlock();
                        spn = null;
                }
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {
                log.in();
		spn = null;

                Driver.gmAccessLock.lock(true);
                Driver.gmLockArray.deleteLock(lockId);

                if (inputHandle != 0) {
                        nCloseInput(inputHandle);
// System.err.println(this + ".doFree(): called nCloseInput");
// Thread.dumpStack();
                        inputHandle = 0;
                }

                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }

                Driver.gmAccessLock.unlock();
                log.out();
	}

	public void readArray(boolean [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostBooleanBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }

	/**
	 * {@inheritDoc}
	 *
	 * We provide our own implementation of read/writeArray(byte[])
	 * because super's uses its own buffering, which is incompatible
	 * with the buffering used in NetGM.
	 */
	/* I hope we can use the buffering of the upper layer after all.
	 * Otherwise, havoc and panic.
	 */
	public void readArray(byte [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();
// System.err.println(this + ": read Byte array, off " + o + " len " + l);
// Thread.dumpStack();

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        // Request reception
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostByteBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();
// System.err.println(this + ": in readArray(byte[]..); result " + result + " chunk " + _l + " currently l " + l);

                        if (result == 0) {
                                // Ack completion
                                pump(lockId, lockIds);

                                // Communication transmission
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }

	public void readArray(char [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                l <<= 1;
                o <<= 1;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostCharBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }


	public void readArray(short [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                l <<= 1;
                o <<= 1;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }
// System.err.println(Thread.currentThread() + ": Rcve/start short array; byte offset " + o + " size " + l);
// Thread.dumpStack();

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

			int result;
                        Driver.gmAccessLock.lock(true);
                        result = nPostShortBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();
// System.err.println(Thread.currentThread() + ": receive chunk of short, result " + result);

                        if (result == 0) {
// System.err.println(Thread.currentThread() + ": ack completion, this would be a rendez-vous msg");
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }
// System.err.println(Thread.currentThread() + ": Done rcve short array; left " + l + " size " + b.length);
// for (int i = 0; i < b.length; i++) { System.err.print(b[i] + ","); } System.err.println();

                log.out();
        }


	public void readArray(int [] b, int o, int l) throws IOException {
                log.in();

                l <<= 2;
                o <<= 2;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }
// System.err.println("Rcve/start int array; byte offset " + o + " size " + l);
// Thread.dumpStack();

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostIntBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }
// System.err.print("Rcvd int array: "); for (int i = 0; i < Math.min(b.length, 32); i++) System.err.print(b[i] + " "); System.err.println();

                log.out();
        }


	public void readArray(long [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                l <<= 3;
                o <<= 3;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostLongBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }


	public void readArray(float [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                l <<= 2;
                o <<= 2;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostFloatBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }


	public void readArray(double [] b, int o, int l) throws IOException {
                log.in();
                freeBuffer();

                l <<= 3;
                o <<= 3;

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump(lockId, lockIds);
                }

                while (l > 0) {
                        int _l = Math.min(l, mtu);

                        Driver.gmReceiveLock.lock();

                        Driver.gmAccessLock.lock(true);
                        int result = nPostDoubleBuffer(inputHandle, b, o, _l);
                        Driver.gmAccessLock.unlock();

                        if (result == 0) {
                                /* Ack completion */
                                pump(lockId, lockIds);

                                /* Communication transmission */
                                pump(lockId, lockIds);
                        }

                        Driver.gmReceiveLock.unlock();

                        l -= _l;
                        o += _l;
                }

                log.out();
        }
}
