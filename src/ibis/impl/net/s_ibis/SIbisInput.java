package ibis.ipl.impl.net.s_ibis;

import ibis.io.ArrayInputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public final class SIbisInput extends NetSerializedInput {
        public SIbisInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
                super(pt, driver, up, context);
        }
        
        public SerializationInputStream newSerializationInputStream() throws IbisIOException {
                ArrayInputStream ais = new DummyInputStream();
		try {
		    return new IbisSerializationInputStream(ais);
		} catch(java.io.IOException e) {
		    throw new IbisIOException("got exception", e);
		}
        }
                private final class DummyInputStream extends ArrayInputStream {
                public void readArray(boolean[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceBoolean(a, off, len);
                }
                       
                public void readArray(byte[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceByte(a, off, len);
                }
                       
                public void readArray(short[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceShort(a, off, len);
                }
                       
                public void readArray(char[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceChar(a, off, len);
                }
                       
                public void readArray(int[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceInt(a, off, len);
                }
                       
                public void readArray(long[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceLong(a, off, len);
                }
                       
                public void readArray(float[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceFloat(a, off, len);
                }
                       
                public void readArray(double[] a, int off, int len) throws IbisIOException {
                        subInput.readArraySliceDouble(a, off, len);
                }
                
                public int available() throws IbisIOException {
                        return 0;
                }
                
        }
}
