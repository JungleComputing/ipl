/* $Id$ */

package ibis.impl.net;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.IbisConfigurationException;

import java.io.EOFException;
import java.io.IOException;

/**
 * Provides an abstraction of a buffered network input.
 */
public abstract class NetBufferedInput extends NetInput implements
        NetBufferedInputSupport {

    private final static boolean DEBUG = false;

    /**
     * Make a copy if the data size is less than this threshold
     */
    protected int arrayThreshold = 0;

    /**
     * The current buffer.
     */
    private NetReceiveBuffer buffer = null;

    /**
     * The current memory block allocator.
     */
    private NetAllocator bufferAllocator = null;

    /**
     * The buffer offset of the payload area.
     */
    protected int dataOffset = 0;

    /**
     * The current buffer offset for extracting user data.
     */
    private int bufferOffset = 0;

    /**
     * Detect circular references in readByteBuffer default implementations.
     */
    private boolean circularCheck = false;

    /**
     * Don't set mtu or headerOffset if this is the same between messages
     */
    private Integer currentNum = null;

    /**
     * @param portType the {@link ibis.impl.net.NetPortType NetPortType}.
     * @param driver the driver of this poller.
     * @param context the context.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    protected NetBufferedInput(NetPortType portType, NetDriver driver,
            String context, NetInputUpcall inputUpcall) {
        super(portType, driver, context, inputUpcall);
    }

    public NetReceiveBuffer createReceiveBuffer(int length) {
        NetReceiveBuffer b = null;

        log.in();
        if (factory == null) {
            byte[] data = null;
            if (bufferAllocator == null) {
                data = new byte[length];
            } else {
                data = bufferAllocator.allocate();
            }
            b = new NetReceiveBuffer(data, length, bufferAllocator);
        } else {
            b = (NetReceiveBuffer) createBuffer(length);
        }
        log.out();

        return b;
    }

    /**
     * Optional method for zero-copy reception.
     * A default implementation that calls
     * {@link #receiveByteBuffer(int)}.
     * Note: at least one 'receiveByteBuffer' method must be implemented.
     *
     * @param buffer receive into this buffer. The buffer is read fully
     * 		from the current offset up to the buffer's length field.
     */
    protected void receiveByteBuffer(NetReceiveBuffer buffer)
            throws IOException {
        log.in();
        int offset = dataOffset;
        int length = buffer.length - offset;

        if (circularCheck) {
            throw new IbisConfigurationException("circular reference");
        }

        circularCheck = true;
        while (length > 0) {
            NetReceiveBuffer b = receiveByteBuffer(length);
            int copyLength = Math.max(length, b.length);
            System.arraycopy(b.data, 0, buffer.data, dataOffset, copyLength);
            offset += copyLength;
            length -= copyLength;
            b.free();
        }
        circularCheck = false;

        log.out();
    }

    /**
     * Optional method for static buffer reception.
     * A default implementation that calls
     * {@link #receiveByteBuffer(NetReceiveBuffer)}.
     * Note: at least one 'receiveByteBuffer' method must be implemented.
     *
     * @param expectedLength the amount of data to be received
     * @return a newly created {@link ibis.impl.net.NetReceiveBuffer} that
     * 		contains the received data
     */
    protected NetReceiveBuffer receiveByteBuffer(int expectedLength)
            throws IOException {
        log.in();
        NetReceiveBuffer b = null;

        if (circularCheck) {
            throw new IbisConfigurationException("circular reference");
        }

        circularCheck = true;
        if (mtu != 0) {
            b = createReceiveBuffer(mtu, 0);
        } else {
            //b = createReceiveBuffer(dataOffset + expectedLength, 0);
            int l = dataOffset + expectedLength;
            b = createReceiveBuffer(l, l);
        }

        receiveByteBuffer(b);
        circularCheck = false;

        log.out();

        return b;
    }

    public void initReceive(Integer num) throws IOException {
        log.in();
        if (mtu != 0) {
            if (bufferAllocator == null
                    || bufferAllocator.getBlockSize() != mtu) {
                bufferAllocator = new NetAllocator(mtu);
            }
        }

        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetReceiveBufferFactoryDefaultImpl(), bufferAllocator);
            dataOffset = getHeadersLength();
        } else if (num != currentNum) {
            currentNum = num;
            factory.setMaximumTransferUnit(
                    Math.max(mtu, factory.getMaximumTransferUnit()));
            dataOffset = getHeadersLength();
        }

        log.out();
    }

    private void pumpBuffer(NetReceiveBuffer buffer) throws IOException {
        log.in();
        receiveByteBuffer(buffer);
        buffer.free();
        log.out();
    }

    private void pumpBuffer(int length) throws IOException {
        log.in();

        if (buffer != null) {
            throw new Error("pumpBuffer but there IS already a buffer");
        }

        buffer = receiveByteBuffer(dataOffset + length);
        if (buffer == null) {
            throw new ConnectionClosedException("connection closed");
        }

        bufferOffset = dataOffset;
        if (DEBUG) {
            System.err.println(this + ": allocated new receive buffer "
                    + buffer + " size " + (dataOffset + length) + " data size "
                    + buffer.data.length + " dataOffset " + dataOffset);
        }
        log.out();
    }

    /**
     * Free the current buffer. Do nothing if there is no current buffer.
     */
    protected void freeBuffer() {
        log.in();
        if (buffer != null) {
            if (DEBUG) {
                System.err.println("Clear buffer, offset " + bufferOffset
                        + " length " + buffer.length);
            }
            buffer.free();
            buffer = null;
            bufferOffset = 0;
        }
        log.out();
    }

    public void doFinish() throws IOException {
        log.in();
        freeBuffer();
        log.out();
    }

    public NetReceiveBuffer readByteBuffer(int expectedLength)
            throws IOException {
        log.in();
        freeBuffer();
        NetReceiveBuffer b = receiveByteBuffer(expectedLength);
        log.out();

        return b;
    }

    public void readByteBuffer(NetReceiveBuffer b) throws IOException {
        log.in();
        freeBuffer();
        receiveByteBuffer(b);
        log.out();
    }

    public byte readByte() throws IOException {
        log.in();
        byte value = 0;
        if (DEBUG) {
            System.err.println(this + ": Wanna read one byte...");
            // Thread.dumpStack();
        }

        if (buffer == null) {
            pumpBuffer(1);
        }
        if (DEBUG) {
            System.err.println("POST-pump: readByte bufferOffset "
                    + bufferOffset + " dataOffset " + dataOffset
                    + " buffer.length " + buffer.length);
            // Thread.dumpStack();
        }

        value = buffer.data[bufferOffset++];

        if ((buffer.length - bufferOffset) == 0) {
            freeBuffer();
        }

        if (DEBUG) {
            System.err.println(this + ": Read one byte=" + value + " = '"
                    + (char) value + "'");
        }
        // log.disp("OUT value = ", value);
        log.disp("OUT");
        log.out();

        return value;
    }

    public boolean readBufferedSupported() {
        return true;
    }

    /**
     * Reads up to <code>length</code> bytes from the underlying input.
     *
     * @return the number of bytes actually read, or -1 in the case
     *         of end-of-file
     */
    public int readBuffered(byte[] data, int offset, int length)
            throws IOException {
        try {
            if (DEBUG) {
                System.err.println("PRE-pump:  readBuffered[" + offset + ":"
                        + length + "] bufferOffset " + bufferOffset
                        + " dataOffset " + dataOffset + " buffer.length "
                        + buffer.length);
            }
            if (buffer == null) {
                pumpBuffer(length);
            }
            if (DEBUG) {
                System.err.println("POST-pump: readBuffered[" + offset + ":"
                        + length + "] bufferOffset " + bufferOffset
                        + " dataOffset " + dataOffset + " buffer.length "
                        + buffer.length);
                // Thread.dumpStack();
            }

            length = Math.min(buffer.length - bufferOffset, length);
            if (DEBUG) {
                System.err.print(this + ": Read buffer[" + bufferOffset + ":"
                        + length + "] = (");
                int n = bufferOffset + Math.min(length, 32);
                for (int i = bufferOffset; i < n; i++) {
                    System.err.print("0x"
                            + Integer.toHexString(buffer.data[i] & 0xFF) + " ");
                }
                System.err.println(") buffer.length " + buffer.length
                        + " data[" + offset + ":" + length + "] data.length "
                        + data.length);
            }
            System.arraycopy(buffer.data, bufferOffset, data, offset, length);

            bufferOffset += length;
            if (bufferOffset == buffer.length) {
                freeBuffer();
            }

            return length;
        } catch (EOFException e) {
            return -1;
        }
    }

    public void readArray(byte[] userBuffer, int offset, int length)
            throws IOException {
        log.in();

        if (DEBUG) {
            System.err.println("Read byte array[" + offset + ":" + length
                    + "] dataOffset " + dataOffset + " length " + length
                    + " arrayThreshold " + arrayThreshold);
            // Thread.dumpStack();
        }

        if (length == 0) {
            return;
        }

        if (dataOffset != 0 || length <= arrayThreshold) {

            while (length > 0) {
                if (buffer == null) {
                    pumpBuffer(length);
                }

                int bufferLength = buffer.length - bufferOffset;
                int copyLength = Math.min(bufferLength, length);

                System.arraycopy(buffer.data, bufferOffset, userBuffer, offset,
                        copyLength);
                if (DEBUG) {
                    System.err.print(this + ": Read Array buffer["
                            + bufferOffset + ":" + copyLength + "] = (");
                    int n = bufferOffset + Math.min(copyLength, 32);
                    for (int i = bufferOffset; i < n; i++) {
                        System.err.print("0x"
                                + Integer.toHexString(buffer.data[i] & 0xFF)
                                + " ");
                    }
                    System.err.println(") buffer.length " + buffer.length
                            + " userBuffer[" + offset + ":" + copyLength
                            + "] userBuffer.length " + userBuffer.length);
                }

                bufferOffset += copyLength;
                bufferLength -= copyLength;
                offset += copyLength;
                length -= copyLength;

                if (bufferLength == 0) {
                    freeBuffer();
                }
            }

        } else {
            if (buffer != null) {
                freeBuffer();
            }

            // Here, the NetReceiveBuffer provides a view into a
            // pre-existing Buffer at a varying offset. For that,
            // we cannot use the BufferFactory.
            if (mtu != 0) {
                do {
                    int copyLength = Math.min(mtu, length);
                    pumpBuffer(new NetReceiveBuffer(userBuffer, offset,
                            copyLength));
                    offset += copyLength;
                    length -= copyLength;
                } while (length != 0);
            } else {
                pumpBuffer(new NetReceiveBuffer(userBuffer, offset, length));
            }
        }

        log.out();
    }

}
