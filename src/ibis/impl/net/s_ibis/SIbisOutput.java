package ibis.ipl.impl.net.s_ibis;

import ibis.io.ArrayOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.impl.net.*;

import java.io.IOException;

/**
 * The ID output implementation.
 */
public final class SIbisOutput extends NetSerializedOutput {

        public SIbisOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

        public SerializationOutputStream newSerializationOutputStream() throws IOException {
                ArrayOutputStream aos = new DummyOutputStream();
		return new IbisSerializationOutputStream(aos);
        }
        
        private final class DummyOutputStream extends ArrayOutputStream {

                private int byteCounter = 0;

                public void writeArray(boolean[] a, int off, int len) throws IOException {
                        byteCounter += len;
                        subOutput.writeArray(a, off, len);
                }

                public void writeArray(byte[] a, int off, int len) throws IOException {
                        byteCounter += len;
                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(short[] a, int off, int len) throws IOException {
                        byteCounter += len * 2;
                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(char[] a, int off, int len) throws IOException {
                        byteCounter += len * 2;
                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(int[] a, int off, int len) throws IOException {
                        byteCounter += len * 4;

                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(long[] a, int off, int len) throws IOException {
                        byteCounter += len * 8;
                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(float[] a, int off, int len) throws IOException {
                        byteCounter += len * 4;
                        subOutput.writeArray(a, off, len);
                }
                       
                public void writeArray(double[] a, int off, int len) throws IOException {
                        byteCounter += len * 8;
                        subOutput.writeArray(a, off, len);
                }

                public int bytesWritten() {
                        return byteCounter;
                }

                public void resetBytesWritten() {
                        byteCounter = 0;
                }

                public void flush() throws IOException {
                        // Nothing
                }

                public void finish() throws IOException {
                        // Nothing
                }

                public void close() throws IOException {
                        // Nothing
                }
        }
}
