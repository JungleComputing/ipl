package ibis.ipl.impl.net.s_ibis;

import ibis.io.ArrayInputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public final class SIbisInput extends NetSerializedInput {
        public SIbisInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
                super(pt, driver, context);
        }

        public SerializationInputStream newSerializationInputStream() throws NetIbisException {
                ArrayInputStream ais = new DummyInputStream();
		try {
		    return new IbisSerializationInputStream(ais);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }
                private final class DummyInputStream extends ArrayInputStream {
                public void readArray(boolean[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(byte[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(short[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(char[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(int[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(long[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(float[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public void readArray(double[] a, int off, int len) throws NetIbisException {
                        subInput.readArray(a, off, len);
                }

                public int available() throws NetIbisException {
                        return 0;
                }

		public void close() {
			/* Ignored. */
		}

        }
}
