package ibis.ipl.impl.net;

import java.util.HashMap;

/**
 * Interface to specify a NetBufferFactory.
 * Instantiate this to create custom NetBuffers.
 */
public class NetBufferFactory {

        /**
         * Activate debugging features.
         */
        final static boolean DEBUG = false;

        /**
         * Activate buffer allocation statistics.
         */
        final static boolean STATISTICS = false;

        /**
         * Indicate the max length of buffer-caching list.
         */
        final static int bufferCacheSize = 16;

        /**
         * Store the buffer cache maps.
         *
         * This map is indexed by {@link #impl} class name.
         */
        static private final HashMap sharedFreeMap        = new HashMap();

        /**
         * Store the buffer wrapper cache maps.
         *
         * This map is indexed by {@link #impl} class name.
         * <BR><B>Note:</B>&nbsp;This attribute should only be assigned once in the constructor.
         * <BR><B>Note 2:</B>&nbsp;Access to this map should be synchronized on the map object.
         */
        static private final HashMap sharedFreeWrapperMap = new HashMap();

        /**
         * Local reference to the current buffer cache map.
         *
         * This map is indexed by the <code>Integer(mtu)</code>.
         * <BR><B>Note:</B>&nbsp;This attribute should only be assigned once in the constructor.
         * <BR><B>Note 2:</B>&nbsp;Access to this map should be synchronized on the map object.
         */
        private HashMap freeMap        = null;

        /**
         * Local reference to the current buffer wrapper cache map.
         *
         * This map is indexed by the <code>Integer(mtu)</code>.
         */
        private HashMap freeWrapperMap = null;

        /**
         * Store the current mtu.
         */
        private int     mtu = 0;

        /**
         * Local reference to the current buffer cache list.
         */
        private BufferList           freeList  = null;

        /**
         * Local reference to the current buffer wrapper cache list.
         */
        private BufferList           freeWrapperList = null;

        /**
         * Store the actual factory {@linkplain NetBufferFactoryImpl implementation}.
         */
        private NetBufferFactoryImpl impl      = null;

        /**
         * Store the current memory block allocator.
         *
         * <BR><B>Note:</B>&nbsp;This allocator should be changed when the {@link #mtu} is changed.
         */
        private NetAllocator         allocator = null;

        private int	created;
        private int	cached;
        private int	uncached;
        private int	uncachedWrapper;

        /**
         * Provide a buffer cache list.
         */
        protected class BufferList {

                /**
                 * Reference the head of the buffer list.
                 */
                NetBuffer buffer = null;

                /**
                 * Store the number of buffers currently in the list.
                 *
                 * <BR><B>Invariant:</B>&nbsp; <code>nb <= {@link #bufferCacheSize}</code>
                 */
                int       nb     =    0;
        }

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

                String className = impl.getClass().getName();

                synchronized(sharedFreeMap) {
                        freeMap = (HashMap)sharedFreeMap.get(className);
                        if (freeMap == null) {
                                freeMap = new HashMap();
                                sharedFreeMap.put(className, freeMap);
                        }
                }

                synchronized(sharedFreeWrapperMap) {
                        freeWrapperMap = (HashMap)sharedFreeWrapperMap.get(className);
                        if (freeWrapperMap == null) {
                                freeWrapperMap = new HashMap();
                                sharedFreeWrapperMap.put(className, freeWrapperMap);
                        }
                }


                synchronized(freeMap) {
                        freeList = (BufferList)freeMap.get(new Integer(mtu));
                        if (freeList == null) {
                                freeList = new BufferList();
                                freeMap.put(new Integer(mtu), freeList);
                        }
                }

                synchronized(freeWrapperMap) {
                        freeWrapperList = (BufferList)freeWrapperMap.get(new Integer(mtu));
                        if (freeWrapperList == null) {
                                freeWrapperList = new BufferList();
                                freeWrapperMap.put(new Integer(mtu), freeWrapperList);
                        }
                }

                if (impl == null) {
                        this.impl = new NetBufferFactoryDefaultImpl();
                } else {
                        this.impl = impl;
                }

                /* Use an allocator to get data blocks. This provides an easy test
                 * at free() time whether the data block can be reused. */
                if (allocator == null) {
                        if (mtu > 0) {
                                if (DEBUG) {
                                        System.err.println(this + ": Install a new allocator, mtu " + mtu);
                                        Thread.dumpStack();
                                }
                                allocator = new NetAllocator(mtu);
                        }

                } else {
                        if (DEBUG) {
                                System.err.println(this + ": Install predefined allocator, mtu " + mtu);
                                Thread.dumpStack();
                        }
                }
                this.allocator = allocator;
        }


        /**
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
                        this.mtu = mtu;
                        synchronized(freeMap) {
                                freeList = (BufferList)freeMap.get(new Integer(mtu));
                                if (freeList == null) {
                                        freeList = new BufferList();
                                        freeMap.put(new Integer(mtu), freeList);
                                }
                        }

                        synchronized(freeWrapperMap) {
                                freeWrapperList = (BufferList)freeWrapperMap.get(new Integer(mtu));
                                if (freeWrapperList == null) {
                                        freeWrapperList = new BufferList();
                                        freeWrapperMap.put(new Integer(mtu), freeWrapperList);
                                }
                        }

                        if (DEBUG) {
                                System.err.println(this + ": Override with a new allocator, mtu " + mtu);
                                Thread.dumpStack();
                        }
                        if (mtu > 0) {
                                allocator = new NetAllocator(mtu);
                        } else {
                                allocator = null;
                        }
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


        /**
	 * Call this synchronized
	 *
	 * @exception throws a java.lang.IllegalArgumentException if the
	 * 	requested buffer length exceeds the factory block size
	 */
        private NetBuffer createNewBuffer(byte[] data,
                                          int length,
                                          NetAllocator allocator) {

                if (data == null) {
                        if (allocator != null) {

                                data = allocator.allocate();
                                if (data.length < length) {
                                        throw new IllegalArgumentException(this + ": allocator blockSize " + data.length + " misfit with requested packet length" + length);
                                }
                        } else {
                                data = new byte[length];
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
	 *
	 * @exception throws a java.lang.IllegalArgumentException if the
	 * 	factory has no default mtu
         */
        private NetBuffer createBuffer(NetAllocator allocator) {
                if (mtu == 0) {
                        throw new IllegalArgumentException("Need an mtu to create NetBuffer without explicit length");
                }

                NetBuffer b = null;

                synchronized(freeList) {
                        b = freeList.buffer;
                        if (b == null) {
                                b = createNewBuffer(null, mtu, allocator);
                        } else {
                                freeList.buffer = b.next;
                                freeList.nb--;
                                if (STATISTICS) {
                                        uncached++;
                                }
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
        synchronized public NetBuffer createBuffer() {
                return createBuffer(allocator);
        }


        /**
         * Free a NetBuffer. If mtu does not equal 0, the buffer is linked into
         * the freeList.
         *
         * @param buffer the {@link NetBuffer} to be released
         */
        synchronized public void free(NetBuffer buffer) {
                if (buffer.factory != this) {
                        throw new Error("Cannot recycle NetBuffer that is not manufactured by me");
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

                        synchronized(freeList){
                                if (freeList.nb < bufferCacheSize) {
                                        buffer.next = freeList.buffer;
                                        freeList.buffer = buffer;
                                        freeList.nb++;
                                } else {
                                        if (buffer.allocator != null) {
                                                buffer.allocator.free(buffer.data);
                                                buffer.data = null;
                                        }
                                }
                        }
                } else {
                        buffer.allocator.free(buffer.data);
                        buffer.data = null;

                        synchronized(freeWrapperList) {
                                if (freeWrapperList.nb < bufferCacheSize) {
                                        buffer.next = freeWrapperList.buffer;
                                        freeWrapperList.buffer = buffer;
                                        freeWrapperList.nb++;
                                }
                        }
                }
        }


        /**
         * Create a NetBuffer.
         *
         * @param length the length of the data stored in the buffer
         */
        public NetBuffer createBuffer(int length) {
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
                                              NetAllocator allocator) {

                if (data != null) {
                        NetBuffer buffer = null;
                        synchronized(freeWrapperList) {
                                buffer = freeWrapperList.buffer;
                                if (buffer == null) {
                                        buffer = impl.createBuffer(data, length, allocator);
                                        buffer.factory = this;
                                } else {
                                        freeWrapperList.buffer = buffer.next;
                                        freeWrapperList.nb--;
                                        buffer.data = data;
                                }
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
