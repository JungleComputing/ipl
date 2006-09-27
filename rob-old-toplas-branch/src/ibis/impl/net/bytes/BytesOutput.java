/* $Id$ */

package ibis.impl.net.bytes;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.io.Conversion;
import ibis.util.Timer;

import java.io.IOException;

/**
 * The byte conversion output implementation.
 */
public final class BytesOutput extends NetOutput implements Settings,
        NetBufferedOutputSupport {

    private final static int splitThreshold = 8;

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
    private int anThreshold = 8 * 256;

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
     * The default conversion is *much* slower than the other default.
     * Select the other default, i.e. NIO.
     */
    Conversion conversion = Conversion.loadConversion(false);

    private static final boolean timerOn = false;

    private Timer writeArrayTimer;

    private Timer conversionTimer;
    {
        if (timerOn) {
            writeArrayTimer = Timer.createTimer();
            conversionTimer = Timer.createTimer();
        }
    }

    /**
     * Constructor.
     *
     * @param pt the properties of the output's
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the ID driver instance.
     */
    BytesOutput(NetPortType pt, NetDriver driver, String context) {
        super(pt, driver, context);
        an = new NetAllocator(anThreshold);
    }

    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
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
        if (maxMtu == 0) {
            System.err.println("You know that Bytes.maxMtu is " + maxMtu + "?");
        }
        /*
         if (_mtu == 0) {
         _mtu = maxMtu;
         }
         */

        if (mtu == 0 || mtu > _mtu) {
            mtu = _mtu;
        }

        if (mtu != 0) {
            if (factory == null) {
                factory = new NetBufferFactory(mtu,
                        new NetSendBufferFactoryDefaultImpl());
            } else {
                factory.setMaximumTransferUnit(mtu);
            }
        }

        log.disp("mtu is ", mtu);

        int _headersLength = subOutput.getHeadersLength();

        if (headerOffset < _headersLength) {
            headerOffset = _headersLength;
        }
        log.disp("headerOffset is ", headerOffset);
        log.out();
    }

    public long getCount() {
        if (subOutput != null) {
        	return subOutput.getCount();
        }
        return 0;
    }
    
    public void resetCount() {
        if (subOutput != null) {
            subOutput.resetCount();
        }
    }

    private void flush() throws IOException {
        log.in();
        if (buffer != null) {
            log.disp(buffer.length, "/", buffer.data.length);
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
            log.disp(buffer.length, "/", buffer.data.length, " ==> flushing");
            subOutput.writeByteBuffer(buffer);
            buffer = null;
        } else {
            if (buffer != null) {
                log.disp(buffer.length, "/", buffer.data.length);
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
            if (true) {
                throw new Error(this + ": discard buffer " + buffer
                		+ " length " + buffer.length);
            }
            buffer.free();
        }

        buffer = createSendBuffer();
        buffer.length = dataOffset;
        log.out();
    }

    private boolean ensureLength(int l) throws IOException {
        boolean ok = true;

        log.in();
        log.disp("param l = ", l);

        if (l > mtu - dataOffset) {
            log.disp("split mandatory");

            log.out();
            ok = false;

        } else if (buffer == null) {
            log.disp("no split needed but buffer allocation required");
            allocateBuffer();

        } else {
            final int availableLength = mtu - buffer.length;
            log.disp("availableLength = ", availableLength);

            if (l > availableLength) {
                if (l - availableLength > splitThreshold) {
                    log.disp("split required");
                    log.out();
                    ok = false;
                } else {
                    log.disp("split avoided, buffer allocation required");
                    flush();
                    allocateBuffer();
                }
            }
        }

        log.out();

        return ok;
    }

    public void initSend() throws IOException {
        log.in();
        dataOffset = getHeadersLength();
        subOutput.initSend();
        super.initSend();
        log.out();
    }

    /**
     * Block until the entire message has been sent and clean up the
     * message. Only after finish(), the data that was written
     * may be touched. Only one message is alive at one time for a
     * given sendport. This is done to prevent flow control problems.
     * When a message is alive and a new messages is requested, the
     * requester is blocked until the live message is finished.
     */
    public long finish() throws IOException {
        log.in();
        super.finish();
        flush();
        long l = subOutput.finish();
        log.out();
        return l;
    }

    public synchronized void close(Integer num) throws IOException {
        log.in();
        if (timerOn) {
            if (conversionTimer.nrTimes() > 0) {
                System.err.println(this + ": writeArrayTimer: total "
                        + writeArrayTimer.totalTime() + " ticks "
                        + writeArrayTimer.nrTimes() + " av "
                        + writeArrayTimer.averageTime());
            }
            if (conversionTimer.nrTimes() > 0) {
                System.err.println(this + ": conversionTimer: total "
                        + conversionTimer.totalTime() + " ticks "
                        + conversionTimer.nrTimes() + " av "
                        + conversionTimer.averageTime());
            }
        }
        if (subOutput != null) {
            subOutput.close(num);
        }
        log.out();
    }

    public void free() throws IOException {
        log.in();
        if (subOutput != null) {
            subOutput.free();
        }

        super.free();
        log.out();
    }

    /**
     * @return whether buffered writing is actually supported
     *
     * Implements NetBufferedOutputSupport
     */
    public boolean writeBufferedSupported() {
        return true;
    }

    /**
     * Sends the current buffer over the network.
     *
     * @throws IOException on error or if buffered writing is not supported
     *
     * Implements NetBufferedOutputSupport
     */
    public void flushBuffer() throws IOException {
        flush();
    }

    /**
     * Writes <code>length</code> bytes to the underlying output.
     *
     * @param data byte array that is filled
     * @param offset offset in <code>data</code>
     * @param length number of bytes to write.
     *
     * @throws IOException on error or if buffered writing is not supported
     *
     * Implements NetBufferedOutputSupport
     */
    public void writeBuffered(byte[] data, int offset, int length)
            throws IOException {
        writeArray(data, offset, length);
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

            buffer.data[buffer.length++] = conversion.boolean2byte(v);
            flushIfNeeded();
        } else {
            subOutput.writeByte(conversion.boolean2byte(v));
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
            if (buffer == null) {
                throw new Error("buffer race");
            }
            if (buffer.data == null) {
                throw new Error("buffer corrupted");
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
                conversion.char2byte(v, buffer.data, buffer.length);
                buffer.length += 2;
                flushIfNeeded();
            } else {
                byte[] b = a2.allocate();
                conversion.char2byte(v, b, 0);

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
            byte[] b = a2.allocate();
            conversion.char2byte(v, b, 0);
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
                conversion.short2byte(v, buffer.data, buffer.length);
                buffer.length += 2;
                flushIfNeeded();
            } else {
                System.err.println(this + ": Ugh... Short");
                byte[] b = a2.allocate();
                conversion.short2byte(v, b, 0);

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
            byte[] b = a2.allocate();
            conversion.short2byte(v, b, 0);
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
                conversion.int2byte(v, buffer.data, buffer.length);
                buffer.length += 4;
                flushIfNeeded();
            } else {
                System.err.println(this + ": Ugh... Int");
                byte[] b = a4.allocate();
                conversion.int2byte(v, b, 0);

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
            byte[] b = a4.allocate();
            conversion.int2byte(v, b, 0);
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
                conversion.long2byte(v, buffer.data, buffer.length);
                buffer.length += 8;
                flushIfNeeded();
            } else {
                System.err.println(this + ": Ugh... Long");
                byte[] b = a8.allocate();
                conversion.long2byte(v, b, 0);

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
            byte[] b = a8.allocate();
            conversion.long2byte(v, b, 0);
            subOutput.writeArray(b);
            a8.free(b);
        }
        log.out();
    }

    /**
     * Writes a float v to the message.
     *
     * @param v The float v to write.
     */
    public void writeFloat(float v) throws IOException {
        log.in();
        if (mtu > 0) {
            if (ensureLength(4)) {
                conversion.float2byte(v, buffer.data, buffer.length);
                buffer.length += 4;
                flushIfNeeded();
            } else {
                System.err.println(this + ": Ugh... Float");
                byte[] b = a4.allocate();
                conversion.float2byte(v, b, 0);

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
            byte[] b = a4.allocate();
            conversion.float2byte(v, b, 0);
            subOutput.writeArray(b);
            a4.free(b);
        }
        log.out();
    }

    /**
     * Writes a double v to the message.
     *
     * @param v The double v to write.
     */
    public void writeDouble(double v) throws IOException {
        log.in();
        if (mtu > 0) {
            if (ensureLength(8)) {
                conversion.double2byte(v, buffer.data, buffer.length);
                buffer.length += 8;
                flushIfNeeded();
            } else {
                System.err.println(this + ": Ugh... Double");
                byte[] b = a8.allocate();
                conversion.double2byte(v, b, 0);

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
            byte[] b = a8.allocate();
            conversion.double2byte(v, b, 0);
            subOutput.writeArray(b);
            a8.free(b);
        }
        log.out();
    }

    /**
     * Writes a Serializable string to the message.
     * Note: uses writeObject to send the string.
     *
     * @param v The string v to write.
     */
    public void writeString(String v) throws IOException {
        log.in();
        final int l = v.length();
        char[] a = new char[v.length()];

        v.getChars(0, l - 1, a, 0);
        writeInt(l);
        writeArray(a, 0, a.length);
        log.out();
    }

    /**
     * Writes a Serializable object to the message.
     *
     * @param v The object v to write.
     */
    public void writeObject(Object v) throws IOException {
        log.in();
        if (true) {
            throw new Error(this + ": writeObject is deprecated");
        }
        subOutput.writeObject(v);
        log.out();
    }

    public void writeArray(boolean[] ub, int o, int l) throws IOException {
        log.in();
        if (mtu <= 0) {
            if (l <= anThreshold) {
                byte[] b = an.allocate();
                conversion.boolean2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l);
                an.free(b);
            } else {
                byte[] b = new byte[l];
                conversion.boolean2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(l)) {
            conversion.boolean2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += l;
            flushIfNeeded();

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(l,
                        buffer.data.length - buffer.length);
                conversion.boolean2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(byte[] ub, int o, int l) throws IOException {
        log.in();
        if (mtu <= 0) {
            subOutput.writeArray(ub, o, l);

        } else if (ensureLength(l)) {
            System.arraycopy(ub, o, buffer.data, buffer.length, l);
            buffer.length += l;
            flushIfNeeded();

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(l,
                        buffer.data.length - buffer.length);
                System.arraycopy(ub, o, buffer.data, buffer.length, copyLength);
                o += copyLength;
                l -= copyLength;
                buffer.length += copyLength;
                flushIfNeeded();
            }
        }

        log.out();
    }

    public void writeArray(char[] ub, int o, int l) throws IOException {
        log.in();

        if (mtu <= 0) {
            if ((l * Conversion.CHAR_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                conversion.char2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l * Conversion.CHAR_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.CHAR_SIZE];
                conversion.char2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.CHAR_SIZE * (l + 1) - 1)) {
            conversion.char2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += Conversion.CHAR_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.CHAR_SIZE) {
            for (int i = 0; i < l; i++) {
                writeChar(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.CHAR_SIZE * l,
                        buffer.data.length - buffer.length)
                            / Conversion.CHAR_SIZE;

                if (copyLength == 0) {
                    flush();
                    continue;
                }

                conversion.char2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.CHAR_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(short[] ub, int o, int l) throws IOException {
        log.in();

        if (mtu <= 0) {
            if ((l * Conversion.SHORT_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                conversion.short2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l * Conversion.SHORT_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.SHORT_SIZE];
                conversion.short2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.SHORT_SIZE * (l + 1) - 1)) {
            conversion.short2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += Conversion.SHORT_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.SHORT_SIZE) {
            for (int i = 0; i < l; i++) {
                writeShort(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.SHORT_SIZE * l,
                        buffer.data.length - buffer.length)
                        / Conversion.SHORT_SIZE;
                log.disp("copyLength = ", copyLength);
                if (copyLength == 0) {
                    flush();
                    continue;
                }

                conversion.short2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.SHORT_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(int[] ub, int o, int l) throws IOException {
        log.in();

        if (mtu <= 0) {
            if ((l * Conversion.INT_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                conversion.int2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l * Conversion.INT_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.INT_SIZE];
                conversion.int2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.INT_SIZE * (l + 1) - 1)) {
            conversion.int2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += Conversion.INT_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.INT_SIZE) {
            for (int i = 0; i < l; i++) {
                writeInt(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.INT_SIZE * l,
                        buffer.data.length - buffer.length)
                        / Conversion.INT_SIZE;
                log.disp("copyLength = ", copyLength);

                if (copyLength == 0) {
                    flush();
                    continue;
                }

                conversion.int2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.INT_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(long[] ub, int o, int l) throws IOException {
        log.in();

        if (mtu <= 0) {
            if ((l * Conversion.LONG_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                conversion.long2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l * Conversion.LONG_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.LONG_SIZE];
                conversion.long2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.LONG_SIZE * (l + 1) - 1)) {
            conversion.long2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += Conversion.LONG_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.LONG_SIZE) {
            for (int i = 0; i < l; i++) {
                writeLong(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.LONG_SIZE * l,
                        buffer.data.length - buffer.length)
                        / Conversion.LONG_SIZE;

                if (copyLength == 0) {
                    flush();
                    continue;
                }

                conversion.long2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.LONG_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(float[] ub, int o, int l) throws IOException {
        log.in();

        if (mtu <= 0) {
            if ((l * Conversion.FLOAT_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                conversion.float2byte(ub, o, l, b, 0);
                subOutput.writeArray(b, 0, l * Conversion.FLOAT_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.FLOAT_SIZE];
                conversion.float2byte(ub, o, l, b, 0);
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.FLOAT_SIZE * (l + 1) - 1)) {
            conversion.float2byte(ub, o, l, buffer.data, buffer.length);
            buffer.length += Conversion.FLOAT_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.FLOAT_SIZE) {
            for (int i = 0; i < l; i++) {
                writeFloat(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.FLOAT_SIZE * l,
                        buffer.data.length - buffer.length)
                        / Conversion.FLOAT_SIZE;

                if (copyLength == 0) {
                    flush();
                    continue;
                }

                conversion.float2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.FLOAT_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        log.out();
    }

    public void writeArray(double[] ub, int o, int l) throws IOException {
        log.in();

        if (timerOn) {
            writeArrayTimer.start();
        }
        if (mtu <= 0) {
            if ((l * Conversion.DOUBLE_SIZE) <= anThreshold) {
                byte[] b = an.allocate();
                if (timerOn) {
                    conversionTimer.start();
                }
                conversion.double2byte(ub, o, l, b, 0);
                if (timerOn) {
                    conversionTimer.stop();
                }
                subOutput.writeArray(b, 0, l * Conversion.DOUBLE_SIZE);
                an.free(b);
            } else {
                byte[] b = new byte[l * Conversion.DOUBLE_SIZE];
                if (timerOn) {
                    conversionTimer.start();
                }
                conversion.double2byte(ub, o, l, b, 0);
                if (timerOn) {
                    conversionTimer.stop();
                }
                subOutput.writeArray(b);
            }

        } else if (ensureLength(Conversion.DOUBLE_SIZE * (l + 1) - 1)) {
            if (timerOn) {
                conversionTimer.start();
            }
            conversion.double2byte(ub, o, l, buffer.data, buffer.length);
            if (timerOn) {
                conversionTimer.stop();
            }
            buffer.length += Conversion.DOUBLE_SIZE * l;
            flushIfNeeded();

        } else if (mtu < Conversion.DOUBLE_SIZE) {
            for (int i = 0; i < l; i++) {
                writeDouble(ub[o + i]);
            }

        } else {
            while (l > 0) {
                if (buffer == null) {
                    allocateBuffer();
                }

                int copyLength = Math.min(Conversion.DOUBLE_SIZE * l,
                        buffer.data.length - buffer.length)
                        / Conversion.DOUBLE_SIZE;

                if (copyLength == 0) {
                    flush();
                    continue;
                }

                if (timerOn) {
                    conversionTimer.start();
                }
                conversion.double2byte(ub, o, copyLength, buffer.data,
                        buffer.length);
                if (timerOn) {
                    conversionTimer.stop();
                }
                o += copyLength;
                l -= copyLength;
                buffer.length += Conversion.DOUBLE_SIZE * copyLength;
                flushIfNeeded();
            }
        }
        if (timerOn) {
            writeArrayTimer.stop();
        }
        log.out();
    }

    public void writeArray(Object[] ub, int o, int l) throws IOException {
        log.in();
        if (true) {
            throw new Error(this + ": writeArray(Object[]) not supported");
        }
        for (int i = 0; i < l; i++) {
            writeObject(ub[o + i]);
        }
        log.out();
    }
}
