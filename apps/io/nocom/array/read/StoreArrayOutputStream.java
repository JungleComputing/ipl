import java.io.IOException;
import ibis.io.ArrayOutputStream;
import ibis.io.IbisStreamFlags;

final class StoreArrayOutputStream extends ArrayOutputStream { 
	
	long len = 0;
	StoreBuffer buf;

	public StoreArrayOutputStream(StoreBuffer buf) { 
		this.buf = buf;
	} 

	public int getAndReset() { 
		int temp = (int) len;
		len = 0;
		return temp;
	} 

	public void writeArray(boolean[] a, int off, int len) throws IOException { 
		this.len += len;
		buf.writeArray((boolean[]) a, off, len);
	}

	public void writeArray(byte[] a, int off, int len) throws IOException { 
		this.len += len;
		buf.writeArray((byte[]) a, off, len);
	}

	public void writeArray(short[] a, int off, int len) throws IOException { 
		this.len += 2*len;
		buf.writeArray((short[]) a, off, len);
	}

	public void writeArray(char[] a, int off, int len) throws IOException { 
		this.len += 2*len;
		buf.writeArray((char[]) a, off, len);
	}

	public void writeArray(int[] a, int off, int len) throws IOException { 
		this.len += 4*len;
		buf.writeArray((int[]) a, off, len);
	}

	public void writeArray(long[] a, int off, int len) throws IOException { 
		this.len += 8*len;
		buf.writeArray((long[]) a, off, len);
	}

	public void writeArray(float[] a, int off, int len) throws IOException { 
		this.len += 4*len;
		buf.writeArray((float[]) a, off, len);
	}

	public void writeArray(double[] a, int off, int len) throws IOException { 
		this.len += 8*len;
		buf.writeArray((double[]) a, off, len);
	}
	
	public void flush() throws IOException { 
	}

	public boolean finished() {
	    return true;
	}

	public void finish() throws IOException { 
	    flush();
	}

	public void close() throws IOException { 
	    flush();
	}

	public long bytesWritten() { 
		return len;
	} 

	public void resetBytesWritten() {
		len = 0;
	}

} 
