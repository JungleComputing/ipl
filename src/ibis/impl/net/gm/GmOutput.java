package ibis.impl.net.gm;

import ibis.impl.net.InterruptedIOException;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferFactoryImpl;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.util.ConditionVariable;

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
    private Integer	rpn          = null;

    private long	deviceHandle =  0;
    private long	outputHandle =  0;
    private int		lnodeId      = -1;
    private int		lportId      = -1;
    private int		lmuxId       = -1;
    private int		lockId       = -1;
    private int[]	lockIds      = null;
    private int		rnodeId      = -1;
    private int		rportId      = -1;
    private int		rmuxId       = -1;
    private Driver	gmDriver     = null;
    private boolean	mustFlush;
    private int		toFlush;
    private int		flushing;
    private int		sentMessages;

    private boolean	closing = false;
    private ConditionVariable flushFinished = Driver.gmAccessLock.createCV();


    native long nInitOutput(long deviceHandle) throws IOException;
    native int  nGetOutputNodeId(long outputHandle) throws IOException;
    native int  nGetOutputPortId(long outputHandle) throws IOException;
    native int  nGetOutputMuxId(long outputHandle) throws IOException;
    native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IOException;

    native boolean nTryFlush(long outputHandle, int length) throws IOException; 

    native void nSendRequest(long outputHandle, int base, int length) throws IOException;
    native int nSendBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
    native void nSendBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

    native int nSendBooleanBufferIntoRequest(long outputHandle, boolean []b, int base, int length) throws IOException;
    native void nSendBooleanBuffer(long outputHandle, boolean []b, int base, int length) throws IOException;

    native int nSendByteBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
    native void nSendByteBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

     native int nSendShortBufferIntoRequest(long outputHandle, short []b, int base, int length) throws IOException;
    native void nSendShortBuffer(long outputHandle, short []b, int base, int length) throws IOException;

     native int nSendCharBufferIntoRequest(long outputHandle, char []b, int base, int length) throws IOException;
    native void nSendCharBuffer(long outputHandle, char []b, int base, int length) throws IOException;

     native int nSendIntBufferIntoRequest(long outputHandle, int []b, int base, int length) throws IOException;
    native void nSendIntBuffer(long outputHandle, int []b, int base, int length) throws IOException;

     native int nSendLongBufferIntoRequest(long outputHandle, long []b, int base, int length) throws IOException;
    native void nSendLongBuffer(long outputHandle, long []b, int base, int length) throws IOException;

     native int nSendFloatBufferIntoRequest(long outputHandle, float []b, int base, int length) throws IOException;
    native void nSendFloatBuffer(long outputHandle, float []b, int base, int length) throws IOException;

     native int nSendDoubleBufferIntoRequest(long outputHandle, double []b, int base, int length) throws IOException;
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

	Driver.gmAccessLock.lock();
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

    public synchronized void setupConnection(NetConnection cnx)
	    throws IOException {
	log.in();
	if (this.rpn != null) {
		throw new Error("connection already established");
	}

	Driver.gmAccessLock.lock();
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

	try {
	    rInfo = (Hashtable)is.readObject();
	} catch (ClassNotFoundException e) {
	    throw new Error(e);
	}

	rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
	rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
	rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

	ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
	os.writeObject(lInfo);
	os.flush();

	Driver.gmAccessLock.lock();
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
    /*
    private void pump(int[] lockIds) throws IOException {
	boolean interrupted;
	do {
	    try {
		Driver.blockingPump(lockIds);
		interrupted = false;
	    } catch (InterruptedIOException e) {
		// try once more
		interrupted = true;
		if (Driver.VERBOSE_INTPT) {
		    System.err.println(this + ": ********** Catch InterruptedIOException " + e);
		}
	    }
	} while (interrupted);
    }
    */


    /**
     * Pump in the face of InterruptedIOExceptions
     */
    private void pump() throws IOException {
	boolean interrupted;
	do {
	    try {
		Driver.blockingPump(lockId, lockIds);
		interrupted = false;
	    } catch (InterruptedIOException e) {
		// try once more
		interrupted = true;
		if (Driver.VERBOSE_INTPT) {
		    System.err.println(this + ": ********** Catch InterruptedIOException " + e);
		}
	    }
	} while (interrupted);
    }


    /**
     * Flush the buffers that the native layer has built up.
     */
    /* Must hold gmAccessLock on entry/exit */
    private void flushAllBuffers() throws IOException {
	if (! mustFlush) {
	    return;
	}

	sentMessages++;

// System.err.println(this + ": Now flush the buffers");
// Thread.dumpStack();
	if (Driver.TIMINGS) Driver.t_native_flush.start();
	nFlush(outputHandle);
	if (Driver.TIMINGS) Driver.t_native_flush.stop();
	mustFlush = false;
	toFlush = 0;
	/* Wait for buffer send completion */
	pump();

	if (closing) {
	    flushFinished.cv_signal();
	}
    }

    public void flushBuffer() throws IOException {
// Thread.dumpStack();
	flushing++;
	super.flushBuffer();
// System.err.println(this + ": past super.flushBuffer()");
	Driver.gmAccessLock.lock(Driver.PRIORITY);
	flushAllBuffers();
	Driver.gmAccessLock.unlock();
	flushing--;

	if (closing) {
	    flushFinished.cv_signal();
	}
    }


    private void flushBufferLocked() throws IOException {
// Thread.dumpStack();
	flushing++;
	Driver.gmAccessLock.unlock();
	super.flushBuffer();
// System.err.println(this + ": past super.flushBuffer()");
	Driver.gmAccessLock.lock();
	flushAllBuffers();
	flushing--;

	if (closing) {
	    flushFinished.cv_signal();
	}
    }


    /* Must hold gmAccessLock on entry/exit */
    private boolean tryFlush(int length) throws IOException {
	if (outputHandle == 0) {
	    throw new ibis.ipl.ConnectionClosedException("Output handle cleared while a send is going on");
	}
	boolean mustFlush = (toFlush + length + available()
			      + ibis.io.Conversion.INT_SIZE > Driver.packetMTU);
	if (mustFlush) {
	    flushBufferLocked();
	}
	return mustFlush;
    }


    /* Must hold gmAccessLock on entry/exit */
    private void sendRequest(int offset, int length) throws IOException {
	if (flushing > 0) {
	    flushAllBuffers();
	} else {
	    flushBufferLocked();
	}

	if (outputHandle == 0) {
	    throw new ibis.ipl.ConnectionClosedException("Output handle cleared while a send is going on");
	}

// System.err.print("[");
// System.err.println("Post a request");
	/* Post the 'request' */
	if (Driver.TIMINGS) Driver.t_native_send.start();
	nSendRequest(outputHandle, offset, length);
	if (Driver.TIMINGS) Driver.t_native_send.stop();

// System.err.println("Wait for request sent completion");
	// Nonono unneccessary pump();		// Wait for 'request' send completion

// System.err.println("Wait for rendez-vous ack");
	pump();		// Wait for 'ack' completion
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

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	boolean locked = true;

	try {

	    if (b.length > mtu) {
		throw new Error("Buffer size exteeds mtu");
	    }

	    if (b.length > Driver.packetMTU) {
		/* Post the 'request' */
// System.err.print("[");
// System.err.println("Post a request");
		sendRequest(b.base, b.length);
// System.err.println(this + ": sent RNDVZ req size " + b.length + " offset " + b.base);

		/* Post the 'buffer' */
		if (Driver.TIMINGS) Driver.t_native_send.start();
		nSendBuffer(outputHandle, b.data, b.base, b.length);
		if (Driver.TIMINGS) Driver.t_native_send.stop();
// System.err.println(this + ": sent RNDVZ data size " + b.length + " offset " + b.base);

		pump();		// Wait for 'buffer' send completion
// System.err.println(this + ": received RNDVZ ack size " + b.length + " offset " + b.base);
// System.err.print("]");

	    } else {
// System.err.print("<*");
// System.err.println("Send lockId " + lockId + " byte buffer " + b + " offset " + b.base + " size " + b.length);
// Thread.dumpStack();
		if (Driver.TIMINGS) Driver.t_native_send.start();
		toFlush = nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length);
		if (Driver.TIMINGS) Driver.t_native_send.stop();

		mustFlush = true;
// System.err.print(">*");
	    }

	    if (! b.ownershipClaimed) {
		Driver.gmAccessLock.unlock();
		b.free();
		locked = false;
	    }

	} finally {
	    if (locked) {
		Driver.gmAccessLock.unlock();
	    }
	    log.out();
	}
    }

    public synchronized void close(Integer num) throws IOException {
	log.in();
	if (rpn == num) {
	    Driver.gmAccessLock.lock();
	    if (rpn != null) {
		closing = true;
if (false)
		while (toFlush > 0 || flushing > 0) {
		    try {
			flushFinished.cv_wait();
		    } catch (InterruptedException e) {
			// Ignore
		    }
		}

		Driver.gmLockArray.deleteLock(lockId);

		if (outputHandle != 0) {
		    nCloseOutput(outputHandle);
		    outputHandle = 0;
		}

		if (deviceHandle != 0) {
		    Driver.nCloseDevice(deviceHandle);
		    deviceHandle = 0;
		}
		rpn = null;
	    } else {
		System.err.println(this + ": already closed...");
	    }
	    Driver.gmAccessLock.unlock();
	}
	log.out();
    }


    public void free() throws IOException {
	log.in();
	if (rpn != null) {
	    close(rpn);
	}

	super.free();
	log.out();
    }

    public void writeArray(boolean [] b, int o, int l) throws IOException {

	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {
// System.err.println("Send boolean array; byte offset " + o + " size " + l);

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendBooleanBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendBooleanBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
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

	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {
// System.err.println(this + ": Send byte array; byte offset " + o + " size " + l);
// Thread.dumpStack();

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);
// System.err.println(this + ": send Rndz-vous msg size " + l + " packet size " + _l);

		    sendRequest(o, _l);

		    // Post the 'buffer'
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendByteBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendByteBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(char [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 1;
	    o <<= 1;
// System.err.println("Send char array; byte offset " + o + " size " + l);

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendCharBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendCharBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(short [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 1;
	    o <<= 1;
// System.err.println(this + ": Send short array; byte offset " + o + " size " + l);
// Thread.dumpStack();
// for (int i = 0; i < b.length; i++) { System.err.print(b[i] + ","); } System.err.println();

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendShortBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

// System.err.println("Locked access lock");
		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendShortBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(int [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 2;
	    o <<= 2;
// System.err.println("Send int array; byte offset " + o + " size " + l);
// for (int i = 0; i < Math.min(b.length, 32); i++) System.err.print(b[i] + " "); System.err.println();

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendIntBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
// System.err.println("Send int array; byte offset " + o + " size " + l);
// Thread.dumpStack();
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendIntBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(long [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 3;
	    o <<= 3;
// System.err.println("Send long array; byte offset " + o + " size " + l);

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendLongBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendLongBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(float [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 2;
	    o <<= 2;
// System.err.println("Send float array; byte offset " + o + " size " + l);

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendFloatBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendFloatBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }

    public void writeArray(double [] b, int o, int l) throws IOException {
	log.in();

	Driver.gmAccessLock.lock(Driver.PRIORITY);
	try {

	    l <<= 3;
	    o <<= 3;
// System.err.println("Send double array; byte offset " + o + " size " + l);

	    while (l > 0) {
		int _l = 0;

		if (l > Driver.packetMTU) {
		    _l = Math.min(l, mtu);

		    sendRequest(o, _l);

		    /* Post the 'buffer' */
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    nSendDoubleBuffer(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    pump();	// Wait for 'buffer' send completion

		} else {
		    _l = l;

		    tryFlush(_l);
		    if (Driver.TIMINGS) Driver.t_native_send.start();
		    toFlush = nSendDoubleBufferIntoRequest(outputHandle, b, o, _l);
		    if (Driver.TIMINGS) Driver.t_native_send.stop();

		    mustFlush = true;
		}

		l -= _l;
		o += _l;
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}
    }


}
