package ibis.ipl.impl.net;

import ibis.ipl.IbisIOException;

/**
 * Interface to specify a NetBufferFactory.
 * Instantiate this to create custom NetBuffers.
 */
public class NetBufferFactory {

    private int mtu;
    private NetBuffer freeList;
    private NetBufferFactoryImpl impl;
    private NetAllocator allocator;

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
	this.allocator = allocator;
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
	    freeList = null;
	    this.mtu = mtu;
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


    private NetBuffer createNewBuffer(int length) throws IbisIOException {
	byte[] data;
	if (allocator == null) {
	    data = new byte[length];
	} else {
	    data = allocator.allocate();
	    if (data.length < length) {
		throw new IbisIOException("allocator blockSize misfit with requested packet length");
	    }
	}
	NetBuffer b = impl.createBuffer(data, length, allocator);
	b.factory = this;
	return b;
    }


    /**
     * Create a NetBuffer of length <CODE>mtu</CODE>.
     */
    synchronized public NetBuffer createBuffer() throws IbisIOException {
	if (mtu == 0) {
	    throw new IbisIOException("Need an mtu to create NetBuffer without explicit length");
	}

	NetBuffer b = freeList;
	if (b == null) {
	    b = createNewBuffer(mtu);
	} else {
	    freeList = b.next;
	}

	return b;
    }


    /**
     * Free a NetBuffer. If mtu does not equal 0, the buffer is linked into
     * the freeList.
     *
     * @param buffer the {@link NetBuffer} to be released
     */
    synchronized public void free(NetBuffer buffer) throws IbisIOException {
	if (buffer.factory != this) {
	    throw new IbisIOException("Cannot recycle NetBuffer that is not manufactured by me");
	}

	if (mtu == 0) {
	    /* Leave the buffer to the GC */
	    return;
	}

	buffer.length = mtu;
	buffer.base   = 0;
	buffer.reset();

	buffer.next = freeList;
	freeList = buffer;
    }


    /**
     * Create a NetBuffer.
     *
     * @param length the length of the data stored in the buffer
     */
    public NetBuffer createBuffer(int length) throws IbisIOException {
	if (mtu != 0 && length <= mtu) {
	    NetBuffer b = createBuffer();
	    b.length = length;
	    return b;
	}

	return createNewBuffer(length);
    }

}
