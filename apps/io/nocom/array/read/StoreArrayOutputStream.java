import java.io.IOException;
import ibis.io.ArrayOutputStream;

final class StoreArrayOutputStream extends ArrayOutputStream { 
	
	int len = 0;
	StoreBuffer buf;

	public StoreArrayOutputStream(StoreBuffer buf) { 
		this.buf = buf;
	} 

	public int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 

	public void writeArray(boolean[] a, int off, int len) throws IOException { 
		this.len += len;
		buf.writeArray(a, off, len);
	}

	public void writeArray(byte[] a, int off, int len) throws IOException { 
		this.len += len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(short[] a, int off, int len) throws IOException { 
		this.len += 2*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(char[] a, int off, int len) throws IOException { 
		this.len += 2*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(int[] a, int off, int len) throws IOException { 
		this.len += 4*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(long[] a, int off, int len) throws IOException { 
		this.len += 8*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(float[] a, int off, int len) throws IOException { 
		this.len += 4*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(double[] a, int off, int len) throws IOException { 
		this.len += 8*len;
		buf.writeArray(a, off, len);
	}
	
	public void flush() throws IOException { 
	}

	public void finish() throws IOException { 
	}

	public void close() throws IOException { 
	}

	public int bytesWritten() { 
		return len;
	} 

	public void resetBytesWritten() {
		len = 0;
	}

} 
