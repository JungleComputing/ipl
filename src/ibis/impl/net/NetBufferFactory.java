package ibis.ipl.impl.net;

/**
 * Interface to specify a NetBufferFactory.
 * Instantiate this to create custom NetBuffers.
 */
public class NetBufferFactory {

    final static boolean DEBUG = true;
    final static boolean STATISTICS = false;

    private int                  mtu       =    0;
    private NetBuffer            freeList  = null;
    private NetBuffer            freeWrapperList;
    private NetBufferFactoryImpl impl      = null;
    private NetAllocator         allocator = null;

    private int	created;
    private int	cached;
    private int	uncached;
    private int	uncachedWrapper;

    /**
     * Constructor
     */
    public NetBufferFactory() {
	this(0);
    }

    /**
     * Constructor
     *
     * @param mtu if <CODE>mtu</CODE> does not equal 0, all buffers are created
     *        with length <CODE>mtu</CODE>. The factory is then capable of
     *        recycling buffers if the <CODE>free()</CODE> method is invoked
     *        on them.
     */
    public NetBufferFactory(int mtu) {
	this(mtu, null, null);
    }

    /**
     * Constructor
     *
     * @param impl the NetBufferFactoryImpl. If <CODE>impl</CODE> equals null,
     *        the {@link NetBufferFactoryDefaultImpl} is used
     */
    public NetBufferFactory(NetBufferFactoryImpl impl) {
	this(0, impl, null);
    }

    /**
     * Constructor
     *
     * @param mtu if <CODE>mtu</CODE> does not equal 0, all buffers are created
     *        with length <CODE>mtu</CODE>. The factory is then capable of
     *        recycling buffers if the <CODE>free()</CODE> method is invoked
     *        on them.
     * @param impl the NetBufferFactoryImpl. If <CODE>impl</CODE> equals null,
     *        the {@link NetBufferFactoryDefaultImpl} is used
     */
    public NetBufferFactory(int mtu, NetBufferFactoryImpl impl) {
	this(mtu, impl, null);
    }

    /**
     * Constructor
     *
     * @param mtu if <CODE>mtu</CODE> does not equal 0, all buffers are created
     *        with length <CODE>mtu</CODE>. The factory is then capable of
     *        recycling buffers if the <CODE>free()</CODE> method is invoked
     *        on them.
     * @param impl the NetBufferFactoryImpl. If <CODE>impl</CODE> equals null,
     *        the {@link NetBufferFactoryDefaultImpl} is used
     * @param allocator an allocator to allocate the byte buffer inside the
     *        {@link NetBuffer}.
     *        If <CODE>mtu</CODE> does not equal 0, the <CODE>allocator</CODE>
     *        is responsible for creating data of size at least <CODE>mtu</CODE>
     */
    public NetBufferFactory(int mtu,
			    NetBufferFactoryImpl impl,
			    NetAllocator allocator) {
	this.mtu = mtu;
	if (impl == null) {
	    this.impl = new NetBufferFactoryDefaultImpl();
	} else {
	    this.impl = impl;
	}

	/* Use an allocator to get data blocks. This provides an easy test
	 * at free() time whether the data block can be reused. */
	if (allocator == null) {
	    if (DEBUG) {
		System.err.println(this + ": Install a new allocator, mtu " + mtu);
		Thread.dumpStack();
	    }
	    allocator = new NetAllocator(mtu);
	} else {
	    if (DEBUG) {
		System.err.println(this + ": Install predefined allocator, mtu " + mtu);
		Thread.dumpStack();
	    }
	}
	this.allocator = allocator;
    }


    /**
     * @method
     * Test type compatibility of a buffer with this factory
     *
     * @param buffer if the class of buffer is an instance of the buffer class
     *        that is manufactured by our Impl, return true.
     */
    public boolean isSuitableClass(NetBuffer buffer) {
	return impl != null && impl.isSuitableClass(buffer);
    }


    /**
     * Change the <CODE>mtu</CODE>.
     * The free list is discarded if the value of the mtu has changed.
     *
     * @param mtu the new maximum transfer unit.
     */
    synchronized
    public void setMaximumTransferUnit(int mtu) {
	if (mtu != this.mtu) {
	    /* Clear both freelists. The GC will clean them up. */
	    freeList = null;
	    freeWrapperList = null;
	    this.mtu = mtu;
	    if (DEBUG) {
		System.err.println(this + ": Override with a new allocator, mtu " + mtu);
		Thread.dumpStack();
	    }
	    allocator = new NetAllocator(mtu);
	}
    }


    /**
     * Get the current mtu.
     *
     * @return the current maximum transfer unit.
     */
    public int getMaximumTransferUnit() {
	return mtu;
    }


    /* Call this synchronized */
    private NetBuffer createNewBuffer(byte[] data,
				      int length,
				      NetAllocator allocator)
	    throws NetIbisException {

	if (data == null) {
	    data = allocator.allocate();
	    if (data.length < length) {
		throw new NetIbisException(this + ": allocator blockSize " + data.length + " misfit with requested packet length" + length);
	    }
	}

	NetBuffer b = impl.createBuffer(data, length, allocator);
	b.factory = this;

	if (STATISTICS) {
	    created++;
	}

	return b;
    }


    /**
     * Create a NetBuffer of length <CODE>mtu</CODE>.
     *
     * Call this synchronized.
     */
    private NetBuffer createBuffer(NetAllocator allocator)
	    throws NetIbisException {
	if (mtu == 0) {
	    throw new NetIbisException("Need an mtu to create NetBuffer without explicit length");
	}

	NetBuffer b = freeList;
	if (b == null) {
	    b = createNewBuffer(null, mtu, allocator);
	} else {
	    freeList = b.next;
	    if (STATISTICS) {
		uncached++;
	    }
	}

	if (STATISTICS) {
	    if (DEBUG && ((created + uncached + uncachedWrapper) % 1000) == 0) {
		System.err.println(this + ": buffers created " + created + " cached " + cached + " uncached " + uncached + " uncached/wrapper " + uncachedWrapper);
	    }
	}

	return b;
    }


    /**
     * Create a NetBuffer of length <CODE>mtu</CODE>.
     */
    synchronized public NetBuffer createBuffer() throws NetIbisException {
	return createBuffer(allocator);
    }


    /**
     * Free a NetBuffer. If mtu does not equal 0, the buffer is linked into
     * the freeList.
     *
     * @param buffer the {@link NetBuffer} to be released
     */
    synchronized public void free(NetBuffer buffer) throws NetIbisException {
	if (buffer.factory != this) {
	    throw new NetIbisException("Cannot recycle NetBuffer that is not manufactured by me");
	}

	if (mtu == 0) {
	    if (buffer.allocator != null) {
		buffer.allocator.free(buffer.data);
	    }
	    /* Leave the wrapper to the GC */
	    return;
	}

	if (STATISTICS) {
	    cached++;
	}

	buffer.reset();

	if (buffer.allocator == null || allocator == buffer.allocator) {

	    buffer.length = mtu;
	    buffer.base   = 0;

	    buffer.next = freeList;
	    freeList = buffer;

	} else {
	    buffer.allocator.free(buffer.data);
	    buffer.data = null;

	    buffer.next = freeWrapperList;
	    freeWrapperList = buffer;
	}
    }


    /**
     * Create a NetBuffer.
     *
     * @param length the length of the data stored in the buffer
     */
    public NetBuffer createBuffer(int length) throws NetIbisException {
	if (mtu != 0 && length <= mtu) {
	    NetBuffer b = createBuffer(allocator);
	    b.length = length;
	    return b;
	}

	return createNewBuffer(null, length, allocator);
    }


    /**
     * Create a NetBuffer.
     *
     * @param length the length of the data stored in the buffer
     */
    synchronized
    public NetBuffer createBuffer(byte[] data,
				  int length,
				  NetAllocator allocator)
	    throws NetIbisException {

	if (data != null) {
	    NetBuffer buffer = freeWrapperList;
	    if (buffer == null) {
		buffer = impl.createBuffer(data, length, allocator);
		buffer.factory = this;
	    } else {
		freeWrapperList = buffer.next;
		buffer.data = data;
	    }
	    buffer.length = length;
	    if (STATISTICS) {
		uncachedWrapper++;
	    }
	    return buffer;
	}

	if (mtu != 0 && length <= mtu) {
	    NetBuffer b = createBuffer(allocator);
	    b.length = length;
	    return b;
	}

	return createNewBuffer(data, length, allocator);
    }

}
