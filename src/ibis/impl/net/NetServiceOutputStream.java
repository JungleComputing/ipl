package ibis.impl.net;

import ibis.io.Conversion;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Provide a multiplexed output sub-stream over the socket output stream.
 */
public class NetServiceOutputStream extends OutputStream {

    /**
     * Set to true once the stream is closed.
     *
     * The stream cannot be re-opened after having been closed (same semantics as a socket stream).
     */
    private boolean closed = false;

    /**
     * Store the packet identifier.
     */
    private int id = -1;

    /**
     * Indicate the length of the buffer.
     */
    private final int length = 65536;

    /**
     * Store the current bufferized bytes.
     */
    private byte[] buffer = new byte[length];

    /**
     * Store the current offset in the buffer.
     */
    private int offset = 0;

    /**
     * Permanent Conversion.INT_SIZE-byte buffer for 'buffer-length to bytes' conversion.
     */
    private byte[] intBuffer = new byte[Conversion.INT_SIZE];

    private OutputStream os;

    /**
     * Write a byte block to the sub-stream.
     *
     * Only this method actually access the sub-stream.
     *
     * @param b the byte block.
     * @param o the offset in the block of the first byte to write.
     * @param l the number of bytes to write.
     * @exception IOtion when the write operation to the {@link #os socket stream} fails.
     */
    private void writeBlock(byte[] b, int o, int l) throws IOException {
        synchronized (os) {
            Conversion.defaultConversion.int2byte(l, intBuffer, 0);
            os.write(id);
            os.write(intBuffer);
            os.write(b, o, l);
        }
    }

    /**
     * Construct an outgoing sub-stream.
     *
     * The {@link #id} value must be unique among this
     * service link outgoing sub-streams.
     *
     * @param id the sub-stream packets id.
     */
    public NetServiceOutputStream(int id, OutputStream os) {
        this.id = id;
        this.os = os;
    }

    /**
     * Flush the {@link #buffer} to the stream if the
     * current {@link #offset} is not <code>0</code>.
     *
     * @exception IOException when the flush operation fails.
     */
    private void doFlush() throws IOException {
        if (offset > 0) {
            writeBlock(buffer, 0, offset);
            os.flush();
            offset = 0;
        }
    }

    public void close() throws IOException {
        closed = true;
        doFlush();
    }

    public boolean closed() {
        return closed;
    }

    public void flush() throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
        doFlush();
    }

    public void write(byte[] buf) throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }

        write(buf, 0, buf.length);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }

        if (len <= length - offset) {
            System.arraycopy(buf, off, buffer, offset, len);
            offset += len;

            if (offset == length) {
                flush();
            }
        } else {
            doFlush();
            writeBlock(buf, off, len);
        }
    }

    public void write(int val) throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }

        buffer[offset++] = (byte) (val & 0xFF);

        if (offset == length) {
            doFlush();
        }
    }

}