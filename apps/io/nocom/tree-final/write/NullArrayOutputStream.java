import ibis.ipl.IbisIOException;
import ibis.io.ArrayOutputStream;

final class NullArrayOutputStream extends ArrayOutputStream { 
	
	int len = 0;

	public final int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 

	public final void writeArray(boolean[] a, int off, int len) throws IbisIOException { 
		this.len += len;
	}

	public final void writeArray(byte[] a, int off, int len) throws IbisIOException { 
		this.len += len;
	}
	
	public final void writeArray(short[] a, int off, int len) throws IbisIOException { 
		this.len += 2*len;
	}
	
	public final void writeArray(char[] a, int off, int len) throws IbisIOException { 
		this.len += 2*len;
	}
	
	public final void writeArray(int[] a, int off, int len) throws IbisIOException { 
		this.len += 4*len;
	}
	
	public final void writeArray(long[] a, int off, int len) throws IbisIOException { 
		this.len += 8*len;
	}
	
	public final void writeArray(float[] a, int off, int len) throws IbisIOException { 
		this.len += 4*len;
	}
	
	public final void writeArray(double[] a, int off, int len) throws IbisIOException { 
		this.len += 8*len;
	}
	
	public final void flush() throws IbisIOException { 
	}

	public final void finish() throws IbisIOException { 
	}

	public final int bytesWritten() {
	    return len;
	}
} 
