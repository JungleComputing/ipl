/* $Id$ */

package ibis.impl.net.ser;

import ibis.impl.net.NetBufferedInputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedInput;
import ibis.io.DataInputStream;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;

import java.io.IOException;

/**
 * The serialization driver input implementation.
 */
public final class SSerInput extends NetSerializedInput {

    private NetBufferedInputSupport bufferInput;

    public SSerInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
    }

    public SerializationInput newSerializationInputStream()
            throws IOException {
        DataInputStream is = new DummyInputStream();
        String ser = type.properties().find("Serialization");
        return SerializationBase.createSerializationInput(ser, is);
    }

    public void setupConnection(NetConnection cnx) throws IOException {
        super.setupConnection(cnx);
        if (subInput instanceof NetBufferedInputSupport) {
            bufferInput = (NetBufferedInputSupport) subInput;
            if (!bufferInput.readBufferedSupported()) {
                bufferInput = null;
            }
        } else {
            bufferInput = null;
        }
    }

    /*
     * InputStream used to get the data from the driver below. Actually
     * does almost nothing, just passes the data up to the serialization.
     */
    private final class DummyInputStream extends DataInputStream {

        public int read() throws IOException {
            int result = subInput.readByte();
            return (result & 255);
        }

        public int read(byte[] data, int offset, int length) throws IOException {

            if (bufferInput != null) {
                // System.err.println("YES!!!");
                return bufferInput.readBuffered(data, offset, length);
            }
            // System.err.println("no..... :-(( subInput " + subInput);

            return super.read(data, offset, length);
        }

        public int read(byte[] data) throws IOException {

            if (bufferInput != null) {
                // System.err.println("YES!!!");
                return bufferInput.readBuffered(data, 0, data.length);
            }
            // System.err.println("no..... :-(( subInput " + subInput);

            return super.read(data);
        }

        public int available() throws IOException {
            return 0; // no way to tell if anything is available */
        }

        public void close() throws IOException {
            // NOTHING
        }

        /*
         * Since the NetInput interface has no way to get to this
         * information, we don't implement this here
         */
        public long bytesRead() {
            return 0;
        }

        public void resetBytesRead() {
            // NOTHING
        }

        public boolean readBoolean() throws IOException {
            return subInput.readBoolean();
        }

        public byte readByte() throws IOException {
            return subInput.readByte();
        }

        public char readChar() throws IOException {
            return subInput.readChar();
        }

        public short readShort() throws IOException {
            return subInput.readShort();
        }

        public int readInt() throws IOException {
            return subInput.readInt();
        }

        public long readLong() throws IOException {
            return subInput.readLong();
        }

        public float readFloat() throws IOException {
            return subInput.readFloat();
        }

        public double readDouble() throws IOException {
            return subInput.readDouble();
        }

        public void readArray(boolean[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(byte[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(short[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(char[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(int[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(long[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(float[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

        public void readArray(double[] a, int off, int len) throws IOException {
            subInput.readArray(a, off, len);
        }

    }
}
