package ibis.ipl.impl.net.bytes;

import ibis.ipl.impl.net.*;

import java.util.HashMap;

/**
 * The ID input implementation.
 */
public final class BytesInput extends NetInput implements Settings {

        private final int splitThreshold = 8;


	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

        /*
         * 
         */
        private NetAllocator a2 = new NetAllocator(2, 1024);
        private NetAllocator a4 = new NetAllocator(4, 1024);
        private NetAllocator a8 = new NetAllocator(8, 1024);

        /**
         * Pre-allocation threshold.
         * Note: must be a multiple of 8.
         */
        private int          anThreshold = 8 * 256;
        private NetAllocator an = null;

	/**
	 * The current buffer.
	 */
        private NetReceiveBuffer buffer          = null;

	/**
	 * The current memory block allocator.
	 */
        private NetAllocator     bufferAllocator = null;

	/**
	 * The buffer offset of the payload area.
	 */
	protected int dataOffset = 0;

	/**
	 * The current buffer offset for extracting user data.
	 */
	private int bufferOffset = 0;

        private volatile Thread activeUpcallThread = null;

	BytesInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver);
			this.subInput = subInput;
		}
		
                if (upcallFunc != null) {
                        subInput.setupConnection(cnx, this);
                } else {
                        subInput.setupConnection(cnx, null);
                }
                log.out();
	}

        protected void initReceive() {
                log.in();
                if (mtu != 0) {
			if (bufferAllocator == null || bufferAllocator.getBlockSize() != mtu) {
				bufferAllocator = new NetAllocator(mtu);
			}
			if (factory == null) {
			    factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl(), bufferAllocator);
			} else {
			    factory.setMaximumTransferUnit(mtu);
			}
		}

                dataOffset = getHeadersLength();
                log.out();
        }

	public synchronized Integer poll(boolean block) throws NetIbisException {
                log.in();
                if (activeNum != null) {
                        throw new Error("invalid call");
                }

                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll(block);
                if (result != null) {
                        mtu          = Math.min(maxMtu, subInput.getMaximumTransfertUnit());
                        /*
                        if (mtu == 0) {
                                mtu = maxMtu;
                        }
                        */
                        headerOffset = subInput.getHeadersLength();
                        initReceive();
                }
                log.out();
		return result;
	}
	
        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                log.in();
                synchronized(this) {
                        while (activeNum != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
                        
                        if (spn == null) {
                                throw new Error("invalid connection num");
                        }
                        
                        activeNum = spn;
                        activeUpcallThread = Thread.currentThread();
                }

                mtu = Math.min(maxMtu, subInput.getMaximumTransfertUnit());
                /*
                if (mtu == 0) {
                        mtu = maxMtu;
                }
                */
                headerOffset = subInput.getHeadersLength();
                initReceive();
                
                upcallFunc.inputUpcall(this, spn);

                synchronized(this) {
                        if (activeNum == spn && activeUpcallThread == Thread.currentThread()) {
                                activeNum = null;
                                activeUpcallThread = null;
                                notifyAll();
                        }
                }
                log.out();
        }

	private void pumpBuffer() throws NetIbisException {
                log.in();
		buffer       = subInput.readByteBuffer(mtu);
		bufferOffset = dataOffset;
                log.out();
	}

	private void freeBuffer() throws NetIbisException {
                log.in();
		if (buffer != null) {
                        log.disp(bufferOffset+"/"+buffer.length);
			buffer.free();
			buffer       = null;
                        bufferOffset =    0;
		}
                log.out();
	}

	private void freeBufferIfNeeded() throws NetIbisException {
                log.in();
		if (buffer != null && bufferOffset == buffer.length) {
                        log.disp(bufferOffset+"/"+buffer.length+" ==> flushing");
			buffer.free();
			buffer       = null;
                        bufferOffset =    0;
		} else {
                        if (buffer != null) {
                                log.disp(bufferOffset+"/"+buffer.length);
                        } else {
                                log.disp("buffer already flushed");
                        }
                }        
                log.out();
	}

        private boolean ensureLength(int l) throws NetIbisException {
                log.in();
                log.disp("param l = "+l);
                
                if (l > mtu - dataOffset) {
                        log.disp("split mandatory");
                        
                        log.out();
                        return false;
                }
                
                if (buffer == null) {
                        log.disp("no split needed but buffer allocation required");                        
                        pumpBuffer();
                } else {
                        //final int availableLength = buffer.length - bufferOffset;
                        final int availableLength = mtu - bufferOffset;
                        log.disp("availableLength = "+ availableLength);
                        
                        if (l > availableLength) {
                                if (l - availableLength > splitThreshold) {
                                        log.disp("split required");
                                        log.out();
                                        return false;
                                } else {
                                        log.disp("split avoided, buffer allocation required");                        
                                        freeBuffer();
                                        pumpBuffer();
                                }
                        }
                }

                log.out();
                return true;
        }
        



	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
                log.in();
		super.finish();
		subInput.finish();
                freeBuffer();
                
                synchronized(this) {
                        activeNum = null;
                        activeUpcallThread = null;
                        notifyAll();
                }
                
                log.out();
	}

        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
                if (subInput != null) {
                        subInput.close(num);
                }
                log.out();
        }
        
	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                log.in();
		if (subInput != null) {
			subInput.free();
		}

		super.free();
                log.out();
	}
	

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                freeBuffer();
                NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
                log.out();
                return b;
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                log.in();
                freeBuffer();
                subInput.readByteBuffer(buffer);
                log.out();
        }

	public boolean readBoolean() throws NetIbisException {
                log.in();
                boolean v = false;
                
                if (mtu > 0) {
                        if (buffer == null) {
                                pumpBuffer();
                        }

                        v = NetConvert.byte2boolean(buffer.data[bufferOffset++]);
                        freeBufferIfNeeded();
                } else {
                        v = NetConvert.byte2boolean(subInput.readByte());
                }

                log.out();
                return v;
        }
        

	public byte readByte() throws NetIbisException {
                log.in();
                byte v = 0;
                
                if (mtu > 0) {
                        if (buffer == null) {
                                pumpBuffer();
                        }

                        v = buffer.data[bufferOffset++];
                        freeBufferIfNeeded();
                } else {
                        v = subInput.readByte();
                }                

                log.out();
                return v;
        }
        

	public char readChar() throws NetIbisException {
                log.in();
                char v = 0;

                if (mtu > 0) {
                        if (ensureLength(2)) {
                                v = NetConvert.readChar(buffer.data, bufferOffset);
                                bufferOffset += 2;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a2.allocate();

                                if (buffer == null) {
                                        pumpBuffer();
                                }
                        
                                b[0] = buffer.data[bufferOffset++];
                                freeBuffer();

                                pumpBuffer();
                                b[1] = buffer.data[bufferOffset++];
                                freeBufferIfNeeded();

                                v = NetConvert.readChar(b);
                                a2.free(b);
                        }

                } else {
                        byte [] b = a2.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readChar(b);
                        a2.free(b);
                }

                log.out();
                return v;
        }


	public short readShort() throws NetIbisException {
                log.in();
                short v = 0;

                if (mtu > 0) {
                        if (ensureLength(2)) {
                                v = NetConvert.readShort(buffer.data, bufferOffset);
                                bufferOffset += 2;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a2.allocate();

                                if (buffer == null) {
                                        pumpBuffer();
                                }
                        
                                b[0] = buffer.data[bufferOffset++];
                                freeBuffer();

                                pumpBuffer();
                                b[1] = buffer.data[bufferOffset++];
                                freeBufferIfNeeded();

                                v = NetConvert.readShort(b);
                                a2.free(b);
                        }
                        
                } else {
                        byte [] b = a2.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readShort(b);
                        a2.free(b);
                }

                log.out();
                return v;
        }


	public int readInt() throws NetIbisException {
                log.in();
                int v = 0;

                if (mtu > 0) {
                        if (ensureLength(4)) {
                                v = NetConvert.readInt(buffer.data, bufferOffset);
                                bufferOffset += 4;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a4.allocate();

                                for (int i = 0; i < 4; i++) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }                        
                                        
                                        b[i] = buffer.data[bufferOffset++];
                                        freeBufferIfNeeded();
                                }

                                v = NetConvert.readInt(b);
                                a4.free(b);
                        }

                } else {
                        byte [] b = a4.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readInt(b);
                        a4.free(b);
                }                

                log.out();
                return v;
        }


	public long readLong() throws NetIbisException {
                log.in();
                long v = 0;

                if (mtu > 0) {
                        if (ensureLength(8)) {
                                v = NetConvert.readLong(buffer.data, bufferOffset);
                                bufferOffset += 8;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a8.allocate();

                                for (int i = 0; i < 8; i++) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }                        
                                        
                                        b[i] = buffer.data[bufferOffset++];
                                        freeBufferIfNeeded();
                                }

                                v = NetConvert.readLong(b);
                                a8.free(b);
                        }

                } else {
                        byte [] b = a8.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readLong(b);
                        a8.free(b);
                }
                
                log.out();
                return v;
        }

	
	public float readFloat() throws NetIbisException {
                log.in();
                float v = 0;

                if (mtu > 0) {
                        if (ensureLength(4)) {
                                v = NetConvert.readFloat(buffer.data, bufferOffset);
                                bufferOffset += 4;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a4.allocate();

                                for (int i = 0; i < 4; i++) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }                        
                                        
                                        b[i] = buffer.data[bufferOffset++];
                                        freeBufferIfNeeded();
                                }

                                v = NetConvert.readFloat(b);
                                a4.free(b);
                        }

                } else {
                        byte [] b = a4.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readFloat(b);
                        a4.free(b);
                }
                
                log.out();
                return v;
        }


	public double readDouble() throws NetIbisException {
                log.in();
                double v = 0;

                if (mtu > 0) {
                        if (ensureLength(8)) {
                                v = NetConvert.readDouble(buffer.data, bufferOffset);
                                bufferOffset += 8;
                                freeBufferIfNeeded();
                        } else {
                                byte [] b = a8.allocate();

                                for (int i = 0; i < 8; i++) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }                        
                                        
                                        b[i] = buffer.data[bufferOffset++];
                                        freeBufferIfNeeded();
                                }

                                v = NetConvert.readDouble(b);
                                a8.free(b);
                        }

                } else {
                        byte [] b = a8.allocate();
                        subInput.readArrayByte(b);
                        v = NetConvert.readDouble(b);
                        a8.free(b);
                }
                
                log.out();
                return v;
        }


	public String readString() throws NetIbisException {
                log.in();
                final int l = readInt();
                char [] a = new char[l];
                readArraySliceChar(a, 0, l);
                
                String s = new String(a);
                log.out();

                return s;
        }


	public Object readObject() throws NetIbisException {
                log.in();
                Object o = subInput.readObject();
                log.out();
                return o;
        }

	public void readArraySliceBoolean(boolean [] ub, int o, int l) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                NetConvert.readArraySliceBoolean(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += l;
                                freeBufferIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.length - bufferOffset);
                                        NetConvert.readArraySliceBoolean(buffer.data, bufferOffset, ub, o, copyLength);
                                        o += copyLength;
                                        l -= copyLength;
                                        bufferOffset += copyLength;
                                        freeBufferIfNeeded();
                                }
                        }
                } else {
                        if (l <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l);
                                NetConvert.readArraySliceBoolean(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceBoolean(b, ub, o, l);
                        }
                }
                log.out();
        }


	public void readArraySliceByte(byte [] ub, int o, int l) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                System.arraycopy(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += l;
                                freeBufferIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.length - bufferOffset);
                                        System.arraycopy(buffer.data, bufferOffset, ub, o, copyLength);
                                        o += copyLength;
                                        l -= copyLength;
                                        bufferOffset += copyLength;
                                        freeBufferIfNeeded();
                                }
                        }

                } else {
                        subInput.readArraySliceByte(ub, o, l);
                }
                log.out();
        }


	public void readArraySliceChar(char [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceChar(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readChar();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceChar(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceChar(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceChar(b, ub, o, l);
                        }
                }
                log.out();
        }
        

	public void readArraySliceShort(short [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceShort(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readShort();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;
                                                log.disp("copyLength = "+copyLength);

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceShort(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceShort(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceShort(b, ub, o, l);
                        }
                }
                log.out();
        }
        

	public void readArraySliceInt(int [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceInt(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readInt();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;
                                                log.disp("copyLength = "+copyLength);

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceInt(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceInt(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceInt(b, ub, o, l);
                        }
                }

                log.out();
        }
        

	public void readArraySliceLong(long [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceLong(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readLong();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceLong(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceLong(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceLong(b, ub, o, l);
                        }
                }
                log.out();
        }
        

	public void readArraySliceFloat(float [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceFloat(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readFloat();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceFloat(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceFloat(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceFloat(b, ub, o, l);
                        }
                }
                log.out();
        }        

	public void readArraySliceDouble(double [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.readArraySliceDouble(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += f*l;
                                freeBufferIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                ub[o+i] = readDouble();
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        pumpBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.length - bufferOffset) / f;

                                                if (copyLength == 0) {
                                                        freeBuffer();
                                                        continue;
                                                }
                                        
                                                NetConvert.readArraySliceDouble(buffer.data, bufferOffset, ub, o, copyLength);
                                                o += copyLength;
                                                l -= copyLength;
                                                bufferOffset += f*copyLength;
                                                freeBufferIfNeeded();
                                        }
                                }
                        }

                } else {
                        if (l*f <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArraySliceByte(b, 0, l*f);
                                NetConvert.readArraySliceDouble(b, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArrayByte(b);
                                NetConvert.readArraySliceDouble(b, ub, o, l);
                        }
                }
                log.out();
        }
        
	public void readArraySliceObject(Object [] ub, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        ub[o+i] = readObject();
                }
                log.out();
        }       
}
