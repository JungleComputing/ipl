/* $Id$ */

package ibis.impl.net.ser;

import ibis.impl.net.NetBufferedOutputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedOutput;

import ibis.io.DataOutputStream;
import ibis.io.SerializationOutput;
import ibis.io.SerializationBase;

import java.io.IOException;

/**
 * The serialization driver output implementation.
 */
public final class SSerOutput extends NetSerializedOutput {

    public SSerOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
    }

    public SerializationOutput newSerializationOutputStream()
            throws IOException {
        DataOutputStream os = new DummyOutputStream();
        String ser = type.properties().find("Serialization");
        return SerializationBase.createSerializationOutput(ser, os);
    }

    /**
     * DataOutputStream used to output the result of the serialization to
     * the next driver. Actually does almost nothing, just passes along
     * the data.
     */
    private final class DummyOutputStream extends DataOutputStream {

        public void write(int b) throws IOException {
            subOutput.writeByte((byte) b);
        }

        public void write(byte[] data, int offset, int length)
                throws IOException {

            if (subBuffered != null) {
                subBuffered.writeBuffered(data, offset, length);
                return;
            }

            super.write(data, offset, length);
        }

        public void write(byte[] data) throws IOException {

            if (subBuffered != null) {
                subBuffered.writeBuffered(data, 0, data.length);
                return;
            }

            super.write(data);
        }

        public void flush() throws IOException {
	    if (subBuffered != null) {
		subBuffered.flushBuffer();
	    }
            /* nothing to flush here, no way to flush a
             netoutput */
        }

        public void close() throws IOException {
            // NOTHING
        }

        public long bytesWritten() {
            return subOutput.localPort().getCount();
        }

        public void resetBytesWritten() {
            subOutput.localPort().resetCount();
        }

        public void writeBoolean(boolean value) throws IOException {
            subOutput.writeBoolean(value);
        }

        public void writeByte(byte value) throws IOException {
            subOutput.writeByte(value);
        }

        public void writeChar(char value) throws IOException {
            subOutput.writeChar(value);
        }

        public void writeShort(short value) throws IOException {
            subOutput.writeShort(value);
        }

        public void writeInt(int value) throws IOException {
            subOutput.writeInt(value);
        }

        public void writeLong(long value) throws IOException {
            subOutput.writeLong(value);
        }

        public void writeFloat(float value) throws IOException {
            subOutput.writeFloat(value);
        }

        public void writeDouble(double value) throws IOException {
            subOutput.writeDouble(value);
        }

        public void writeArray(boolean[] a, int off, int len)
                throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(byte[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(short[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(char[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(int[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(long[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(float[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

        public void writeArray(double[] a, int off, int len) throws IOException {
            subOutput.writeArray(a, off, len);
        }

    }
}
