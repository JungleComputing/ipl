package ibis.ipl.impl.net.s_ibis;

import ibis.io.ArrayOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The ID output implementation.
 */
public final class SIbisOutput extends NetSerializedOutput {
        public SIbisOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
	}
        public SerializationOutputStream newSerializationOutputStream() {
                ArrayOutputStream aos = new DummyOutputStream();
                return new IbisSerializationOutputStream(aos);
        }
        
        private final class DummyOutputStream extends ArrayOutputStream {

                private int byteCounter = 0;

                public void writeArray(boolean[] a, int off, int len) throws IbisIOException {
                        byteCounter += len;
                        subOutput.writeArraySliceBoolean(a, off, len);
                }

                public void writeArray(byte[] a, int off, int len) throws IbisIOException {
                        byteCounter += len;
                        subOutput.writeArraySliceByte(a, off, len);
                }
                       
                public void writeArray(short[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 2;
                        subOutput.writeArraySliceShort(a, off, len);
                }
                       
                public void writeArray(char[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 2;
                        subOutput.writeArraySliceChar(a, off, len);
                }
                       
                public void writeArray(int[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 4;

                        subOutput.writeArraySliceInt(a, off, len);
                }
                       
                public void writeArray(long[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 8;
                        subOutput.writeArraySliceLong(a, off, len);
                }
                       
                public void writeArray(float[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 4;
                        subOutput.writeArraySliceFloat(a, off, len);
                }
                       
                public void writeArray(double[] a, int off, int len) throws IbisIOException {
                        byteCounter += len * 8;
                        subOutput.writeArraySliceDouble(a, off, len);
                }

                public int bytesWritten() {
                        return byteCounter;
                }

                public void resetBytesWritten() {
                        byteCounter = 0;
                }

                public void flush() throws IbisIOException {
                        // Nothing
                }

                public void finish() throws IbisIOException {
                        // Nothing
                }

                public void close() throws IbisIOException {
                        // Nothing
                }
        }
}
