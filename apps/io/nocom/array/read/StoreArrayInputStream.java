import java.io.IOException;

/**
 *
 * Extends OutputStream with read of array of primitives and readSingleInt
 */

public class StoreArrayInputStream extends ibis.io.ArrayInputStream {

	StoreBuffer buf;

	int boolean_count = 0;
	int byte_count = 0;
	int short_count = 0;
	int char_count = 0;
	int int_count = 0;
	int long_count = 0;
	int float_count = 0;
	int double_count = 0;

	public StoreArrayInputStream(StoreBuffer buf) { 
		this.buf = buf;
	} 

	public void reset() { 
		boolean_count = 0;
		byte_count = 0;
		short_count = 0;
		char_count = 0;
		int_count = 0;
		long_count = 0;
		float_count = 0;
		double_count = 0;
	} 
	
	public void readArray(boolean[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.boolean_store, boolean_count, a, off, len);
		boolean_count += len;
	}
	
	public void readArray(byte[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.byte_store, byte_count, a, off, len);
		byte_count += len;
	}
	
	public void readArray(short[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.short_store, short_count, a, off, len);
		short_count += len;
	}
	
	public void readArray(char[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.char_store, char_count, a, off, len);
		char_count += len;
	}
	
	public void readArray(int[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.int_store, int_count, a, off, len);
		int_count += len;
	}
	
	public void readArray(long[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.long_store, long_count, a, off, len);
		long_count += len;
	}
	
	public void readArray(float[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.float_store, float_count, a, off, len);
		float_count += len;
	}

	public void readArray(double[] a, int off, int len) throws IOException { 
		System.arraycopy(buf.double_store, double_count, a, off, len);
		double_count += len;
	}
	
	public int available() throws IOException { 
		return 0;
	}
}

