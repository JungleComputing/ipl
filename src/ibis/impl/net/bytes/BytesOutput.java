package ibis.ipl.impl.net.bytes;

import ibis.ipl.impl.net.*;

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
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 */
	BytesOutput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
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

                System.err.println("mtu is ["+mtu+"]");

 		int _headersLength = subOutput.getHeadersLength();
 
 		if (headerOffset < _headersLength) {
 			headerOffset = _headersLength;
 		}
                log.disp("headerOffset is ["+headerOffset+"]");
                log.out();
	}

	private void flush() throws NetIbisException {
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

        private void flushIfNeeded() throws NetIbisException {
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
	private void allocateBuffer() throws NetIbisException {
                log.in();
		if (buffer != null) {
			buffer.free();
		}

                buffer = createSendBuffer();
		buffer.length = dataOffset;
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
	public void initSend() throws NetIbisException {
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
        public void finish() throws NetIbisException{
                log.in();
                super.finish();
                flush();
                subOutput.finish();
                log.out();
        }

        public synchronized void close(Integer num) throws NetIbisException {
                log.in();
		if (subOutput != null) {
                        subOutput.close(num);
                }
                log.out();
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                log.in();
		if (subOutput != null) {
			subOutput.free();
		}

		super.free();
                log.out();
	}        

        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                log.in();
                flush();
                subOutput.writeByteBuffer(buffer);
                log.out();
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (buffer == null) {
                                allocateBuffer();
                        }
                        
                        buffer.data[buffer.length++] = NetConvert.boolean2byte(v);
                        flushIfNeeded();
                } else {
                        subOutput.writeByte(NetConvert.boolean2byte(v));
                }
                log.out();
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws NetIbisException {
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
        public void writeChar(char v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(2)) {
                                NetConvert.writeChar(v, buffer.data, buffer.length);
                                buffer.length += 2;
                                flushIfNeeded();
                        } else {
                                byte [] b = a2.allocate();
                                NetConvert.writeChar(v, b);

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
                        NetConvert.writeChar(v, b);
                        subOutput.writeArrayByte(b);
                        a2.free(b);
                }
                log.out();
        }
        

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(2)) {
                                NetConvert.writeShort(v, buffer.data, buffer.length);
                                buffer.length += 2;
                                flushIfNeeded();
                        } else {
                                byte [] b = a2.allocate();
                                NetConvert.writeShort(v, b);

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
                        NetConvert.writeShort(v, b);
                        subOutput.writeArrayByte(b);
                        a2.free(b);
                }
                log.out();
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(4)) {
                                NetConvert.writeInt(v, buffer.data, buffer.length);
                                buffer.length += 4;
                                flushIfNeeded();
                        } else {
                                byte [] b = a4.allocate();
                                NetConvert.writeInt(v, b);

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
                        NetConvert.writeInt(v, b);
                        subOutput.writeArrayByte(b);
                        a4.free(b);
                }
                log.out();
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(8)) {
                                NetConvert.writeLong(v, buffer.data, buffer.length);
                                buffer.length += 8; 
                                flushIfNeeded();
                        } else {
                                byte [] b = a8.allocate();
                                NetConvert.writeLong(v, b);

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
                        NetConvert.writeLong(v, b);
                        subOutput.writeArrayByte(b);
                        a8.free(b);
                }
                log.out();
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(4)) {
                                NetConvert.writeFloat(v, buffer.data, buffer.length);
                                buffer.length += 4;
                                flushIfNeeded();
                        } else {
                                byte [] b = a4.allocate();
                                NetConvert.writeFloat(v, b);

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
                        NetConvert.writeFloat(v, b);
                        subOutput.writeArrayByte(b);
                        a4.free(b);
                }
                log.out();
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(8)) {
                                NetConvert.writeDouble(v, buffer.data, buffer.length);
                                buffer.length += 8; 
                                flushIfNeeded();
                        } else {
                                byte [] b = a8.allocate();
                                NetConvert.writeDouble(v, b);

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
                        NetConvert.writeDouble(v, b);
                        subOutput.writeArrayByte(b);
                        a8.free(b);
                }
                log.out();
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     v             The string v to write.
	 */
        public void writeString(String v) throws NetIbisException {
                log.in();
                final int l = v.length();
                char []   a = new char[v.length()];

                v.getChars(0, l-1, a, 0);
                writeInt(l);
                writeArraySliceChar(a, 0, a.length);
                log.out();
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws NetIbisException {
                log.in();
                subOutput.writeObject(v);                
                log.out();
        }


        public void writeArraySliceBoolean(boolean [] ub, int o, int l) throws NetIbisException {
                log.in();
                if (mtu > 0) {
                        if (ensureLength(l)) {
                                NetConvert.writeArraySliceBoolean(ub, o, l, buffer.data, buffer.length);
                                buffer.length += l;
                                flushIfNeeded();
                        } else {
                                while (l > 0) {
                                        if (buffer == null) {
                                                allocateBuffer();
                                        }

                                        int copyLength = Math.min(l, buffer.data.length - buffer.length);
                                        NetConvert.writeArraySliceBoolean(ub, o, copyLength, buffer.data, buffer.length);
                                        o += copyLength;
                                        l -= copyLength;
                                        buffer.length += copyLength;
                                        flushIfNeeded();
                                }
                        }
                } else {
                        if (l <= anThreshold) {
                                byte [] b = an.allocate();
                                NetConvert.writeArraySliceBoolean(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceBoolean(ub, o, l));
                        }
                }
                log.out();
        }

        public void writeArraySliceByte(byte [] ub, int o, int l) throws NetIbisException {
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
                        subOutput.writeArraySliceByte(ub, o, l);
                }
                
                log.out();
        }

        public void writeArraySliceChar(char [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceChar(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceChar(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceChar(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceChar(ub, o, l));
                        }
                }
                log.out();
        }

        public void writeArraySliceShort(short [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 2;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceShort(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceShort(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceShort(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceShort(ub, o, l));
                        }
                }
                log.out();
        }

        public void writeArraySliceInt(int [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 4;                
                
                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceInt(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceInt(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceInt(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceInt(ub, o, l));
                        }
                }
                log.out();
        }

        public void writeArraySliceLong(long [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceLong(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceLong(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceLong(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceLong(ub, o, l));
                        }
                
                }
                log.out();
        }

        public void writeArraySliceFloat(float [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 4;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceFloat(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceFloat(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceFloat(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceFloat(ub, o, l));
                        }
                }
                log.out();
        }

        public void writeArraySliceDouble(double [] ub, int o, int l) throws NetIbisException {
                log.in();
                final int f = 8;

                if (mtu > 0) {
                        if (ensureLength(f*(l+1) - 1)) {
                                NetConvert.writeArraySliceDouble(ub, o, l, buffer.data, buffer.length);
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
                                        
                                                NetConvert.writeArraySliceDouble(ub, o, copyLength, buffer.data, buffer.length);
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
                                NetConvert.writeArraySliceDouble(ub, o, l, b);
                                subOutput.writeArraySliceByte(b, 0, l*f);
                                an.free(b);
                        } else {
                                subOutput.writeArrayByte(NetConvert.writeArraySliceDouble(ub, o, l));
                        }
                }
                log.out();
        }	

        public void writeArraySliceObject(Object [] ub, int o, int l) throws NetIbisException {
                log.in();
                for (int i = 0; i < l; i++) {
                        writeObject(ub[o+i]);
                }
                log.out();
        }
}
