package ibis.io;

import java.io.OutputStream;
import java.io.IOException;

import ibis.ipl.IbisIOException;

/**
 *
 * Extends OutputStream with write of array of primitives and writeSingleInt
 */

public final class BufferedArrayOutputStream extends ibis.io.ArrayOutputStream {
	
	public static final boolean DEBUG = false;

	private static final int BUF_SIZE = 8*1024;
	
	private OutputStream out;
	
	private byte [] buffer;
	private int index;
	
	private int bytes = 0;

	public int bytesWritten() { 
		int temp = bytes;
		bytes = 0;
		return temp;
	} 

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


	private final void flush(int incr, boolean always) throws IbisIOException {

//		System.out.println("flush(" + incr + " " + always + ") : " + " " + (index + incr >= BUF_SIZE) + " "  + (index) + ")");

		try {				
			if (always) { 
				bytes += index;

//				System.out.print("fflushing [");
//				for (int i=0;i<index;i++) { 
//					System.out.print(buffer[i] + ",");
//				}
//				System.out.println("] " + bytes);

				out.write(buffer, 0, index);
				index = 0;
				out.flush();	
			} else { 
				if (index + incr >= BUF_SIZE) { 
					bytes += index;

//					System.out.print("nflushing [");
//					for (int i=0;i<index;i++) { 
//						System.out.print(buffer[i] + ",");
//					}
//					System.out.println("] " + bytes);

					out.write(buffer, 0, index);
					index = 0;
				}
			} 
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

/*
	    if (always || index + incr >= BUF_SIZE) {
		try {			
			System.out.println("Writing " + index + " bytes");
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
*/
	}
/*
	private final void flush(int incr)
		throws IbisIOException {
	    flush(incr, false);
	}
*/
	
	public void writeArray(boolean[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(boolean[" + off + " ... " + (off+len) + "])");
		}			

		int size;

		do { 
			flush(0, false);

			size = min(BUF_SIZE-index, len);

			Conversion.boolean2byte(a, off, size, buffer, index);

			off   += size;
			index += size;
			len   -= size;

		} while (len != 0);
	}


	public void writeArray(byte[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.print("writeArray(byte[");
			for (int i=0;i<len;i++) { 
				System.out.print(a[off+i] + ",");
			}
			System.out.println("]");

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
		
	static int W = 0;

	public void writeArray(short[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.print("writeArray(short " + (W++) + " [");
			for (int i=0;i<len;i++) { 
				System.out.print(a[off+i] + ",");
			}
			System.out.println("]");
		}			

		int size;
		
		do { 
			flush(SIZEOF_SHORT, false);

			size = min((BUF_SIZE-index)/SIZEOF_SHORT, len);

//			System.out.println("Room to write " + size + " shorts");
			
			Conversion.short2byte(a, off, size, buffer, index);
			
			off   += size;
			len   -= size;
			index += size*SIZEOF_SHORT;

//			System.out.println("Len = " + len + " index = " + index);
			
		} while (len != 0);	
	}

	public void writeArray(char[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(char[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_CHAR, false);
			
			size = min((BUF_SIZE-index)/SIZEOF_CHAR, len);
			
			Conversion.char2byte(a, off, size, buffer, index);
			
			off   += size;
			len   -= size;
			index += size*SIZEOF_CHAR;
			
		} while (len != 0);
	}


	public void writeArray(int[] a, int off, int len) throws IbisIOException {
		
		if (DEBUG) { 
			System.out.println("writeArray(int[" + off + " ... " + (off+len) + "])");
//      		System.out.println("writeArray(int[");
//			for (int i=0;i<len;i++) { 
//				System.out.print(a[off+i] + ",");
//			}
//			System.out.println("]");
		}			

		int size;
		
		do { 
			flush(SIZEOF_INT, false);
			
			size = min((BUF_SIZE-index)/SIZEOF_INT, len);

//			System.out.println("Room to write " + size + " ints");
			
			Conversion.int2byte(a, off, size, buffer, index);
				
			off   += size;
			len   -= size;
			index += size*SIZEOF_INT;
			
//			System.out.println("Len = " + len + " index = " + index);

		} while (len != 0);	 
	}
	
	
	public void writeArray(long[] a, int off, int len) throws IbisIOException {

		if (DEBUG) { 
			System.out.println("writeArray(long[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			flush(SIZEOF_LONG, false);

			size = min((BUF_SIZE-index)/SIZEOF_LONG, len);
			
			Conversion.long2byte(a, off, size, buffer, index);
			
			off   += size;
			len   -= size;
			index += size*SIZEOF_LONG;
						
		} while (len != 0);  
	}
	
	
	public void writeArray(float[] a, int off, int len) throws IbisIOException {
		int size;
		
		do { 
			flush(SIZEOF_FLOAT, false);

			size = min((BUF_SIZE-index)/SIZEOF_FLOAT, len);
			
			Conversion.float2byte(a, off, size, buffer, index);
			
			off   += size;
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
			flush(SIZEOF_DOUBLE, false);
			
			size = min((BUF_SIZE-index)/SIZEOF_DOUBLE, len);

			Conversion.double2byte(a, off, size, buffer, index);
		   
			off   += size;
			len   -= size;
			index += size*SIZEOF_DOUBLE;
			
		} while (len != 0);	 		
	}


	public final void flush() throws IbisIOException {
	    flush(0, true);
	}


	public final void finish() throws IbisIOException {
	}
}
