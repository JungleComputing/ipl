package ibis.impl.net.bytes;

import ibis.impl.net.InterruptedIOException;
import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetReceiveBufferFactoryDefaultImpl;
import ibis.io.Conversion;

import java.io.IOException;

/**
 * The ID input implementation.
 */
public final class BytesInput extends NetInput implements Settings {

        private final static int splitThreshold = 8;


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
        private NetAllocator a2 = new NetAllocator(2);
        private NetAllocator a4 = new NetAllocator(4);
        private NetAllocator a8 = new NetAllocator(8);

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

        private volatile Integer activeNum = null;

	private int waiters = 0;

	BytesInput(NetPortType pt, NetDriver driver, String context, NetInputUpcall inputUpcall) {
		super(pt, driver, context, inputUpcall);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver, upcallFunc == null ? null : this);
			this.subInput = subInput;
		}

		subInput.setupConnection(cnx);
                log.out();
	}

        protected void initReceive(Integer num) {
                log.in();
                        mtu          = Math.min(maxMtu, subInput.getMaximumTransfertUnit());
                        /*
                        if (mtu == 0) {
                                mtu = maxMtu;
                        }
                        */
                        headerOffset = subInput.getHeadersLength();
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

	/*
	 * {@inheritDoc}
	 *
	 * Do not make this synchronized. The calling <code>poll</code> will
	 * ensure concurrency issues. If this is called synchronized and
	 * block=true, deadlock may ensue with concurrent accept calls.
	 */
	public Integer doPoll(boolean block) throws IOException {
                log.in();
                if (activeNum != null) {
                        throw new Error("invalid call");
                }

                if (subInput == null)
                        return null;

                Integer result = subInput.poll(block);

                log.out();
		return result;
	}

        public void inputUpcall(NetInput input, Integer spn) throws IOException {
                log.in();
		Thread me = Thread.currentThread();
                synchronized(this) {
                        while (activeNum != null) {
                                try {
					waiters++;
                                        wait();
					waiters--;
                                } catch (InterruptedException e) {
                                        throw new InterruptedIOException(e);
                                }
                        }

                        if (spn == null) {
                                throw new Error("invalid connection num");
                        }

                        activeNum = spn;
                        activeUpcallThread = me;
                        initReceive(spn);
                }

                upcallFunc.inputUpcall(this, spn);

                synchronized(this) {
                        if (activeNum == spn && activeUpcallThread == me) {
                                activeNum = null;
                                activeUpcallThread = null;
				if (waiters > 0) {
					notify();
					// notifyAll();
				}
                        }
                }
                log.out();
        }

	private void pumpBuffer() throws IOException {
                log.in();
		buffer       = subInput.readByteBuffer(mtu);
		bufferOffset = dataOffset;
                log.out();
	}

	private void freeBuffer() {
                log.in();
		if (buffer != null) {
                        log.disp(bufferOffset+"/"+buffer.length);
			buffer.free();
			buffer       = null;
                        bufferOffset =    0;
		}
                log.out();
	}

	private void freeBufferIfNeeded() {
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

        private boolean ensureLength(int l) throws IOException {
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
	public void doFinish() throws IOException {
                log.in();
		subInput.finish();
                freeBuffer();

                synchronized(this) {
                        activeNum = null;
                        activeUpcallThread = null;
                        notifyAll();
                        // notify();
                }

                log.out();
	}

        public synchronized void doClose(Integer num) throws IOException {
                log.in();
                if (subInput != null) {
                        subInput.close(num);
                }
                log.out();
        }

	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {
                log.in();
		if (subInput != null) {
			subInput.free();
		}
                log.out();
	}


        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IOException {
                log.in();
                freeBuffer();
                NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
                log.out();
                return b;
        }

        public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
                log.in();
                freeBuffer();
                subInput.readByteBuffer(buffer);
                log.out();
        }

	public boolean readBoolean() throws IOException {
                log.in();
                boolean v = false;

                if (mtu > 0) {
                        if (buffer == null) {
                                pumpBuffer();
                        }

                        v = Conversion.defaultConversion.byte2boolean(buffer.data[bufferOffset++]);
                        freeBufferIfNeeded();
                } else {
                        v = Conversion.defaultConversion.byte2boolean(subInput.readByte());
                }

                log.out();
                return v;
        }


	public byte readByte() throws IOException {
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


	public char readChar() throws IOException {
                log.in();
                char v = 0;

                if (mtu > 0) {
                        if (ensureLength(2)) {
                                v = Conversion.defaultConversion.byte2char(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2char(b, 0);
                                a2.free(b);
                        }

                } else {
                        byte [] b = a2.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2char(b, 0);
                        a2.free(b);
                }

                log.out();
                return v;
        }


	public short readShort() throws IOException {
                log.in();
                short v = 0;

                if (mtu > 0) {
                        if (ensureLength(2)) {
                                v = Conversion.defaultConversion.byte2short(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2short(b, 0);
                                a2.free(b);
                        }

                } else {
                        byte [] b = a2.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2short(b, 0);
                        a2.free(b);
                }

                log.out();
                return v;
        }


	public int readInt() throws IOException {
                log.in();
                int v = 0;

                if (mtu > 0) {
                        if (ensureLength(4)) {
                                v = Conversion.defaultConversion.byte2int(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2int(b, 0);
                                a4.free(b);
                        }

                } else {
                        byte [] b = a4.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2int(b, 0);
                        a4.free(b);
                }

                log.out();
                return v;
        }


	public long readLong() throws IOException {
                log.in();
                long v = 0;

                if (mtu > 0) {
                        if (ensureLength(8)) {
                                v = Conversion.defaultConversion.byte2long(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2long(b, 0);
                                a8.free(b);
                        }

                } else {
                        byte [] b = a8.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2long(b, 0);
                        a8.free(b);
                }

                log.out();
                return v;
        }


	public float readFloat() throws IOException {
                log.in();
                float v = 0;

                if (mtu > 0) {
                        if (ensureLength(4)) {
                                v = Conversion.defaultConversion.byte2float(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2float(b, 0);
                                a4.free(b);
                        }

                } else {
                        byte [] b = a4.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2float(b, 0);
                        a4.free(b);
                }

                log.out();
                return v;
        }


	public double readDouble() throws IOException {
                log.in();
                double v = 0;

                if (mtu > 0) {
                        if (ensureLength(8)) {
                                v = Conversion.defaultConversion.byte2double(buffer.data, bufferOffset);
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

                                v = Conversion.defaultConversion.byte2double(b, 0);
                                a8.free(b);
                        }

                } else {
                        byte [] b = a8.allocate();
                        subInput.readArray(b);
                        v = Conversion.defaultConversion.byte2double(b, 0);
                        a8.free(b);
                }

                log.out();
                return v;
        }


	public String readString() throws IOException {
                log.in();
                final int l = readInt();
                char [] a = new char[l];
                readArray(a, 0, l);

                String s = new String(a);
                log.out();

                return s;
        }


	public Object readObject() throws IOException, ClassNotFoundException {
                log.in();
                Object o = subInput.readObject();
                log.out();
                return o;
        }

	public void readArray(boolean [] ub, int o, int l) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                Conversion.defaultConversion.byte2boolean(buffer.data, bufferOffset, ub, o, l);
                                bufferOffset += l;
                                freeBufferIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                pumpBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.length - bufferOffset);
                                        Conversion.defaultConversion.byte2boolean(buffer.data, bufferOffset, ub, o, copyLength);
                                        o += copyLength;
                                        l -= copyLength;
                                        bufferOffset += copyLength;
                                        freeBufferIfNeeded();
                                }
                        }
                } else {
                        if (l <= anThreshold) {
                                byte [] b = an.allocate();
                                subInput.readArray(b, 0, l);
                                Conversion.defaultConversion.byte2boolean(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2boolean(b, 0, ub, o, l);
                        }
                }
                log.out();
        }


	public void readArray(byte [] ub, int o, int l) throws IOException {
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
                        subInput.readArray(ub, o, l);
                }
                log.out();
        }


	public void readArray(char [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2char(buffer.data, bufferOffset, ub, o, l);
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

                                                Conversion.defaultConversion.byte2char(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2char(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2char(b, 0, ub, o, l);
                        }
                }
                log.out();
        }


	public void readArray(short [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2short(buffer.data, bufferOffset, ub, o, l);
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

                                                Conversion.defaultConversion.byte2short(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2short(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2short(b, 0, ub, o, l);
                        }
                }
                log.out();
        }


	public void readArray(int [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2int(buffer.data, bufferOffset, ub, o, l);
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
 
                                                Conversion.defaultConversion.byte2int(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2int(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2int(b, 0, ub, o, l);
                        }
                }

                log.out();
        }


	public void readArray(long [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2long(buffer.data, bufferOffset, ub, o, l);
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

                                                Conversion.defaultConversion.byte2long(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2long(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2long(b, 0, ub, o, l);
                        }
                }
                log.out();
        }


	public void readArray(float [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2float(buffer.data, bufferOffset, ub, o, l);
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

                                                Conversion.defaultConversion.byte2float(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2float(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2float(b, 0, ub, o, l);
                        }
                }
                log.out();
        }

	public void readArray(double [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.byte2double(buffer.data, bufferOffset, ub, o, l);
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

                                                Conversion.defaultConversion.byte2double(buffer.data, bufferOffset, ub, o, copyLength);
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
                                subInput.readArray(b, 0, l*f);
                                Conversion.defaultConversion.byte2double(b, 0, ub, o, l);
                                an.free(b);
                        } else {
                                byte [] b = new byte[f*l];
                                subInput.readArray(b);
                                Conversion.defaultConversion.byte2double(b, 0, ub, o, l);
                        }
                }
                log.out();
        }

	public void readArray(Object [] ub, int o, int l) throws IOException, ClassNotFoundException {
                log.in();
                for (int i = 0; i < l; i++) {
                        ub[o+i] = readObject();
                }
                log.out();
        }
}
