import java.io.IOException;
import ibis.io.ArrayOutputStream;

final class NullArrayOutputStream extends ArrayOutputStream { 
	
	int len = 0;

	public int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 

	public void writeArray(boolean[] a, int off, int len) throws IOException { 
		this.len += len;
	}

	public void writeArray(byte[] a, int off, int len) throws IOException { 
		this.len += len;
	}
	
	public void writeArray(short[] a, int off, int len) throws IOException { 
		this.len += 2*len;
	}
	
	public void writeArray(char[] a, int off, int len) throws IOException { 
		this.len += 2*len;
	}
	
	public void writeArray(int[] a, int off, int len) throws IOException { 
		this.len += 4*len;
	}
	
	public void writeArray(long[] a, int off, int len) throws IOException { 
		this.len += 8*len;
	}
	
	public void writeArray(float[] a, int off, int len) throws IOException { 
		this.len += 4*len;
	}
	
	public void writeArray(double[] a, int off, int len) throws IOException { 
		this.len += 8*len;
	}
	
	public void flush() throws IOException { 
	}

	public void finish() throws IOException { 
	}

	public void close() throws IOException { 
	}

	public void resetBytesWritten() { 
		len = 0;
	}

	public int bytesWritten() { 
		return len;
	} 
} 
