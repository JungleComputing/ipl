package ibis.impl.net.bytes;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;

import ibis.io.Conversion;

import java.io.IOException;

/**
 * The byte conversion output implementation.
 */
public final class BytesOutput extends NetOutput implements Settings {

        private final int splitThreshold = 8;

	/**
	 * The driver used for the 'real' output.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' output.
	 */
        private NetOutput subOutput = null;

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
	private NetSendBuffer buffer = null;

	/**
	 * The current buffer offset after the headers of the lower layers
	 * into the payload area.
	 */
	protected int dataOffset = 0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 */
	BytesOutput(NetPortType pt, NetDriver driver, String context) {
		super(pt, driver, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
		NetOutput subOutput = this.subOutput;

		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(cnx);

		int _mtu = Math.min(maxMtu, subOutput.getMaximumTransfertUnit());
                /*
                if (_mtu == 0) {
                        _mtu = maxMtu;
                }
                */

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

                if (mtu != 0) {
                        if (factory == null) {
                                factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
                        } else {
                                factory.setMaximumTransferUnit(mtu);
                        }
		}

                log.disp("mtu is ["+mtu+"]");

 		int _headersLength = subOutput.getHeadersLength();

 		if (headerOffset < _headersLength) {
 			headerOffset = _headersLength;
 		}
                log.disp("headerOffset is ["+headerOffset+"]");
                log.out();
	}

	private void flush() throws IOException {
                log.in();
		if (buffer != null) {
                        log.disp(buffer.length+"/"+buffer.data.length);
			subOutput.writeByteBuffer(buffer);
			buffer = null;
		} else {
                        log.disp("buffer already flushed");
                }
                log.out();
	}

        private void flushIfNeeded() throws IOException {
                log.in();
		if (buffer != null && buffer.length == buffer.data.length) {
                        log.disp(buffer.length+"/"+buffer.data.length+" ==> flushing");
                        subOutput.writeByteBuffer(buffer);
			buffer = null;
		} else {
                        if (buffer != null) {
                                log.disp(buffer.length+"/"+buffer.data.length);
                        } else {
                                log.disp("buffer already flushed");
                        }
                }
                log.out();
        }


	/**
	 * Allocate a new buffer.
	 */
	private void allocateBuffer() {
                log.in();
		if (buffer != null) {
			buffer.free();
		}

                buffer = createSendBuffer();
		buffer.length = dataOffset;
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
                        allocateBuffer();
                } else {
                        final int availableLength = mtu - buffer.length;
                        log.disp("availableLength = "+ availableLength);

                        if (l > availableLength) {
                                if (l - availableLength > splitThreshold) {
                                        log.disp("split required");
                                        log.out();
                                        return false;
                                } else {
                                        log.disp("split avoided, buffer allocation required");
                                        flush();
                                        allocateBuffer();
                                }
                        }
                }
                log.out();

                return true;
        }


	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IOException {
                log.in();
                dataOffset = getHeadersLength();
		subOutput.initSend();
                super.initSend();
                log.out();
	}

        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems.
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws IOException{
                log.in();
                super.finish();
                flush();
                subOutput.finish();
                log.out();
        }

        public synchronized void close(Integer num) throws IOException {
                log.in();
		if (subOutput != null) {
                        subOutput.close(num);
                }
                log.out();
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IOException {
                log.in();
		if (subOutput != null) {
			subOutput.free();
		}

		super.free();
                log.out();
	}

        public void writeByteBuffer(NetSendBuffer buffer) throws IOException {
                log.in();
                flush();
                subOutput.writeByteBuffer(buffer);
                log.out();
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (buffer == null) {
                                allocateBuffer();
                        }

                        buffer.data[buffer.length++] = Conversion.defaultConversion.boolean2byte(v);
                        flushIfNeeded();
                } else {
                        subOutput.writeByte(Conversion.defaultConversion.boolean2byte(v));
                }
                log.out();
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (buffer == null) {
                                allocateBuffer();
                        }

                        buffer.data[buffer.length++] = v;
                        flushIfNeeded();
                } else {
                        subOutput.writeByte(v);
                }
                log.out();
        }

        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(2)) {
                                Conversion.defaultConversion.char2byte(v, buffer.data, buffer.length);
                                buffer.length += 2;
                                flushIfNeeded();
                        } else {
                                byte [] b = a2.allocate();
                                Conversion.defaultConversion.char2byte(v, b, 0);

                                if (buffer == null) {
                                        allocateBuffer();
                                }

                                buffer.data[buffer.length++] = b[0];
                                flush();

                                allocateBuffer();
                                buffer.data[buffer.length++] = b[1];
                                flushIfNeeded();
                                a2.free(b);
                        }
                } else {
                        byte [] b = a2.allocate();
                        Conversion.defaultConversion.char2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a2.free(b);
                }
                log.out();
        }


        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(2)) {
                                Conversion.defaultConversion.short2byte(v, buffer.data, buffer.length);
                                buffer.length += 2;
                                flushIfNeeded();
                        } else {
                                byte [] b = a2.allocate();
                                Conversion.defaultConversion.short2byte(v, b, 0);

                                if (buffer == null) {
                                        allocateBuffer();
                                }

                                buffer.data[buffer.length++] = b[0];
                                flush();

                                allocateBuffer();
                                buffer.data[buffer.length++] = b[1];
                                flushIfNeeded();
                                a2.free(b);
                        }
                } else {
                        byte [] b = a2.allocate();
                        Conversion.defaultConversion.short2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a2.free(b);
                }
                log.out();
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(4)) {
                                Conversion.defaultConversion.int2byte(v, buffer.data, buffer.length);
                                buffer.length += 4;
                                flushIfNeeded();
                        } else {
                                byte [] b = a4.allocate();
                                Conversion.defaultConversion.int2byte(v, b, 0);

                                for (int i = 0; i < 4; i++) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        buffer.data[buffer.length++] = b[i];
                                        flushIfNeeded();
                                }

                                a4.free(b);
                        }
                } else {
                        byte [] b = a4.allocate();
                        Conversion.defaultConversion.int2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a4.free(b);
                }
                log.out();
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(8)) {
                                Conversion.defaultConversion.long2byte(v, buffer.data, buffer.length);
                                buffer.length += 8;
                                flushIfNeeded();
                        } else {
                                byte [] b = a8.allocate();
                                Conversion.defaultConversion.long2byte(v, b, 0);

                                for (int i = 0; i < 8; i++) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        buffer.data[buffer.length++] = b[i];
                                        flushIfNeeded();
                                }

                                a8.free(b);
                        }
                } else {
                        byte [] b = a8.allocate();
                        Conversion.defaultConversion.long2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a8.free(b);
                }
                log.out();
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(4)) {
                                Conversion.defaultConversion.float2byte(v, buffer.data, buffer.length);
                                buffer.length += 4;
                                flushIfNeeded();
                        } else {
                                byte [] b = a4.allocate();
                                Conversion.defaultConversion.float2byte(v, b, 0);

                                for (int i = 0; i < 4; i++) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        buffer.data[buffer.length++] = b[i];
                                        flushIfNeeded();
                                }

                                a4.free(b);
                        }
                } else {
                        byte [] b = a4.allocate();
                        Conversion.defaultConversion.float2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a4.free(b);
                }
                log.out();
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(8)) {
                                Conversion.defaultConversion.double2byte(v, buffer.data, buffer.length);
                                buffer.length += 8;
                                flushIfNeeded();
                        } else {
                                byte [] b = a8.allocate();
                                Conversion.defaultConversion.double2byte(v, b, 0);

                                for (int i = 0; i < 8; i++) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        buffer.data[buffer.length++] = b[i];
                                        flushIfNeeded();
                                }

                                a8.free(b);
                        }
                } else {
                        byte [] b = a8.allocate();
                        Conversion.defaultConversion.double2byte(v, b, 0);
                        subOutput.writeArray(b);
                        a8.free(b);
                }
                log.out();
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     v             The string v to write.
	 */
        public void writeString(String v) throws IOException {
                log.in();
                final int l = v.length();
                char []   a = new char[v.length()];

                v.getChars(0, l-1, a, 0);
                writeInt(l);
                writeArray(a, 0, a.length);
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws IOException {
                log.in();
                subOutput.writeObject(v);
                log.out();
        }


        public void writeArray(boolean [] ub, int o, int l) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                Conversion.defaultConversion.boolean2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += l;
                                flushIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.data.length - buffer.length);
                                        Conversion.defaultConversion.boolean2byte(ub, o, copyLength, buffer.data, buffer.length);
                                        o += copyLength;
                                        l -= copyLength;
                                        buffer.length += copyLength;
                                        flushIfNeeded();
                                }
                        }
                } else {
                        if (l <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.boolean2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l);
                                an.free(b);
                        } else {
				byte[] b = new byte[l];
				Conversion.defaultConversion.boolean2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(byte [] ub, int o, int l) throws IOException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                System.arraycopy(ub, o, buffer.data, buffer.length, l);
                                buffer.length += l;
                                flushIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.data.length - buffer.length);
                                        System.arraycopy(ub, o, buffer.data, buffer.length, copyLength);
                                        o += copyLength;
                                        l -= copyLength;
                                        buffer.length += copyLength;
                                        flushIfNeeded();
                                }
                        }
                } else {
                        subOutput.writeArray(ub, o, l);
                }

                log.out();
        }

        public void writeArray(char [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.char2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeChar(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;

                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.char2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.char2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.CHAR_SIZE];
				Conversion.defaultConversion.char2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(short [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.short2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeShort(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;
                                                log.disp("copyLength = "+copyLength);
                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.short2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.short2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.SHORT_SIZE];
				Conversion.defaultConversion.short2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(int [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.int2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeInt(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;
                                                log.disp("copyLength = "+copyLength);

                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.int2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.int2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.INT_SIZE];
				Conversion.defaultConversion.int2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(long [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.long2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeLong(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;

                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.long2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.long2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.LONG_SIZE];
				Conversion.defaultConversion.long2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }

                }
                log.out();
        }

        public void writeArray(float [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.float2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeFloat(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;

                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.float2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.float2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.FLOAT_SIZE];
				Conversion.defaultConversion.float2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(double [] ub, int o, int l) throws IOException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                Conversion.defaultConversion.double2byte(ub, o, l, buffer.data, buffer.length);
                                buffer.length += f*l;
                                flushIfNeeded();
                        } else {
                                if (mtu < f) {
                                        for (int i = 0; i < l; i++) {
                                                writeDouble(ub[o+i]);
                                        }
                                } else {
                                        while (l > 0) {
                                                if (buffer == null) {
                                                        allocateBuffer();
                                                }

                                                int copyLength = Math.min(f*l, buffer.data.length - buffer.length) / f;

                                                if (copyLength == 0) {
                                                        flush();
                                                        continue;
                                                }

                                                Conversion.defaultConversion.double2byte(ub, o, copyLength, buffer.data, buffer.length);
                                                o += copyLength;
                                                l -= copyLength;
                                                buffer.length += f*copyLength;
                                                flushIfNeeded();
                                        }
                                }
                        }
                } else {
                        if ((l*f) <= anThreshold) {
                                byte [] b = an.allocate();
                                Conversion.defaultConversion.double2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b, 0, l*f);
                                an.free(b);
                        } else {
				byte[] b = new byte[l * Conversion.defaultConversion.DOUBLE_SIZE];
				Conversion.defaultConversion.double2byte(ub, o, l, b, 0);
                                subOutput.writeArray(b);
                        }
                }
                log.out();
        }

        public void writeArray(Object [] ub, int o, int l) throws IOException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeObject(ub[o+i]);
                }
                log.out();
        }
}
