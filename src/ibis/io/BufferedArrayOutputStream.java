package ibis.io;

import java.io.OutputStream;
import java.io.IOException;

import ibis.ipl.IbisIOException;

/**
 *
 * Extends OutputStream with write of array of primitives and writeSingleInt
 */

public class BufferedArrayOutputStream extends ibis.io.ArrayOutputStream {
	
	public static final boolean DEBUG = false;

	private static final int BUF_SIZE = 32*1024;
	
	private OutputStream out;
	
	private byte [] buffer;
	private int index;
	
	public BufferedArrayOutputStream(OutputStream out) {
		this.out = out;
		buffer = new byte[BUF_SIZE];
	}

	private static final int min(int a, int b) {
		return (a > b) ? b : a;
	}

	public final void write(int b) throws IbisIOException {
		throw new IbisIOException("write(int b) has no meaning in typed stream");
	}


	private void flush(int incr, boolean always)
		throws IbisIOException {
	    if (always || index + incr >= BUF_SIZE) {
		try {
		    out.write(buffer, 0, index);
		} catch (IOException e) {
		    throw new IbisIOException(e);
		}
		index = 0;
	    }
	    if (always) {
		try {
		    out.flush();
		} catch (IOException e) {
		    throw new IbisIOException(e);
		}
	    }
	}


	private void flush(int incr)
		throws IbisIOException {
	    flush(incr, false);
	}

	
	public void writeArray(boolean[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(boolean[" + off + " ... " + (off+len) + "])");
		}			

		int size;

		do { 
			flush(0);

			size = min(BUF_SIZE-index, len);

			Conversion.boolean2byte(a, off, size, buffer, index);

			index += size;
			len   -= size;

		} while (len != 0);
	}


	public void writeArray(byte[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(byte[" + off + " ... " + (off+len) + "])");
		}			
	    
		int size;
		
		if (len > (BUF_SIZE-index)) { 

		    try {
			if (index > 0) { 
				out.write(buffer, 0, index);
				index = 0;
			} 
			out.write(a, off, len);
		    } catch (IOException e) {
			throw new IbisIOException(e);
		    }
		} else { 
			System.arraycopy(a, off, buffer, index, len);			
			index += len;
		} 
	}
		
	public void writeArray(short[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.print("writeArray(short[");
			for (int i=0;i<len;i++) { 
				System.out.print(a[off+i] + ",");
			}
			System.out.println("]");
		}			

		int size;
		
		do { 
			flush(SIZEOF_SHORT);

			size = min((BUF_SIZE-index)/SIZEOF_SHORT, len);
			
			Conversion.short2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_SHORT;
			
		} while (len != 0);	
	}

	public void writeArray(char[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(char[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_CHAR);
			
			size = min((BUF_SIZE-index)/SIZEOF_CHAR, len);
			
			Conversion.char2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_CHAR;
			
		} while (len != 0);
	}


	public void writeArray(int[] a, int off, int len) throws IbisIOException {
		
		if (DEBUG) { 
			System.out.println("writeArray(int[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_INT);
			
			size = min((BUF_SIZE-index)/SIZEOF_INT, len);
			
			Conversion.int2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_INT;
			
		} while (len != 0);	 
	}
	
	
	public void writeArray(long[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(long[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_LONG);

			size = min((BUF_SIZE-index)/SIZEOF_LONG, len);
			
			Conversion.long2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_LONG;
						
		} while (len != 0);  
	}
	
	
	public void writeArray(float[] a, int off, int len) throws IbisIOException {
		int size;
		
		do { 
			flush(SIZEOF_FLOAT);

			size = min((BUF_SIZE-index)/SIZEOF_FLOAT, len);
			
			Conversion.float2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_FLOAT;
						
		} while (len != 0);  
	}


	public void writeArray(double[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(double[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_DOUBLE);
			
			size = min((BUF_SIZE-index)/SIZEOF_DOUBLE, len);

			Conversion.double2byte(a, off, size, buffer, index);
		   
			len   -= size;
			index += size*SIZEOF_DOUBLE;
			
		} while (len != 0);	 		
	}


	public void flush() throws IbisIOException {
	    flush(0, true);
	}


	public final void finish() throws IbisIOException {
	}
}
