import ibis.ipl.IbisIOException;
import ibis.io.ArrayOutputStream;

final class NullArrayOutputStream extends ArrayOutputStream { 
	
	int len = 0;

	public int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 

	public void writeArray(boolean[] a, int off, int len) throws IbisIOException { 
		this.len += len;
	}

	public void writeArray(byte[] a, int off, int len) throws IbisIOException { 
		this.len += len;
	}
	
	public void writeArray(short[] a, int off, int len) throws IbisIOException { 
		this.len += 2*len;
	}
	
	public void writeArray(char[] a, int off, int len) throws IbisIOException { 
		this.len += 2*len;
	}
	
	public void writeArray(int[] a, int off, int len) throws IbisIOException { 
		this.len += 4*len;
	}
	
	public void writeArray(long[] a, int off, int len) throws IbisIOException { 
		this.len += 8*len;
	}
	
	public void writeArray(float[] a, int off, int len) throws IbisIOException { 
		this.len += 4*len;
	}
	
	public void writeArray(double[] a, int off, int len) throws IbisIOException { 
		this.len += 8*len;
	}
	
	public void flush() throws IbisIOException { 
	}

	public void finish() throws IbisIOException { 
	}

	public int bytesWritten() { 
		return len;
	} 
} 
