import java.io.IOException;
import ibis.ipl.IbisIOException;
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

	public void writeArray(boolean[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += len;
		buf.writeArray(a, off, len);
	}

	public void writeArray(byte[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(short[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 2*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(char[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 2*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(int[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 4*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(long[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 8*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(float[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 4*len;
		buf.writeArray(a, off, len);
	}
	
	public void writeArray(double[] a, int off, int len) throws ibis.ipl.IbisIOException { 
		this.len += 8*len;
		buf.writeArray(a, off, len);
	}
	
	public void flush() throws ibis.ipl.IbisIOException { 
	}

	public void finish() throws ibis.ipl.IbisIOException { 
	}

	public int bytesWritten() { 
		return len;
	} 

} 
