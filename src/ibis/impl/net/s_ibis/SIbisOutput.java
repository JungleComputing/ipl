/* $Id$ */

package ibis.impl.net.s_ibis;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedOutput;
import ibis.io.Accumulator;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationOutputStream;

import java.io.IOException;

/**
 * Output part of the s_ibis driver used for serialization. Uses the
 * IbisSerializationOutputStream to actually do the serialization.
 */
public final class SIbisOutput extends NetSerializedOutput {

    public SIbisOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
        requiresStreamReinit = false;
    }

    public SerializationOutputStream newSerializationOutputStream()
            throws IOException {
        Accumulator ia = new DummyAccumulator();

        return new IbisSerializationOutputStream(ia);
    }

    protected void handleEmptyMsg() throws IOException {
        // super.handleEmptyMsg();
        writeShort((short) 0xffff);
    }

    /**
     * Accumulator used to output the result of the serialization to
     * the next driver. Actually does almost nothing, just passes along
     * the data.
     */
    private final class DummyAccumulator extends Accumulator {

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
            // return subOutput.localPort().getCount();
            return subOutput.getCount();
        }

        public void resetBytesWritten() {
            // subOutput.localPort().resetCount();
            subOutput.resetCount();
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
