package ibis.ipl.impl.net.s_ibis;

import ibis.io.ArrayOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.impl.net.*;

/**
 * The ID output implementation.
 */
public final class SIbisOutput extends NetSerializedOutput {
        public SIbisOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
		super(pt, driver, up, context);
	}
        public SerializationOutputStream newSerializationOutputStream() throws NetIbisException {
                ArrayOutputStream aos = new DummyOutputStream();
		try {
		    return new IbisSerializationOutputStream(aos);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }
        
        private final class DummyOutputStream extends ArrayOutputStream {

                private int byteCounter = 0;

                public void writeArray(boolean[] a, int off, int len) throws NetIbisException {
                        byteCounter += len;
                        subOutput.writeArraySliceBoolean(a, off, len);
                }

                public void writeArray(byte[] a, int off, int len) throws NetIbisException {
                        byteCounter += len;
                        subOutput.writeArraySliceByte(a, off, len);
                }
                       
                public void writeArray(short[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 2;
                        subOutput.writeArraySliceShort(a, off, len);
                }
                       
                public void writeArray(char[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 2;
                        subOutput.writeArraySliceChar(a, off, len);
                }
                       
                public void writeArray(int[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 4;

                        subOutput.writeArraySliceInt(a, off, len);
                }
                       
                public void writeArray(long[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 8;
                        subOutput.writeArraySliceLong(a, off, len);
                }
                       
                public void writeArray(float[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 4;
                        subOutput.writeArraySliceFloat(a, off, len);
                }
                       
                public void writeArray(double[] a, int off, int len) throws NetIbisException {
                        byteCounter += len * 8;
                        subOutput.writeArraySliceDouble(a, off, len);
                }

                public int bytesWritten() {
                        return byteCounter;
                }

                public void resetBytesWritten() {
                        byteCounter = 0;
                }

                public void flush() throws NetIbisException {
                        // Nothing
                }

                public void finish() throws NetIbisException {
                        // Nothing
                }

                public void close() throws NetIbisException {
                        // Nothing
                }
        }
}
