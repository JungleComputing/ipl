package ibis.io;

import java.io.OutputStream;
import java.io.IOException;

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

	public final void write(int b) throws IOException {
		throw new IOException("write(byte) has no meaning in typed stream");
	}
	
	public void writeArray(boolean[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.println("writeArray(boolean[" + off + " ... " + (off+len) + "])");
		}			

		int size;

		do { 
			if (index == BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}

			size = min(BUF_SIZE-index, len);

			Conversion.boolean2byte(a, off, size, buffer, index);

			index += size;
			len   -= size;

		} while (len != 0);
	}

	public void writeArray(byte[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.println("writeArray(byte[" + off + " ... " + (off+len) + "])");
		}			
	    
		int size;
		
		if (len > (BUF_SIZE-index)) { 

			if (index > 0) { 
				out.write(buffer, 0, index);
				index = 0;
			} 
			out.write(a, off, len);
		} else { 
			System.arraycopy(a, off, buffer, index, len);			
			index += len;
		} 
	}
		
	public void writeArray(short[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.print("writeArray(short[");
			for (int i=0;i<len;i++) { 
				System.out.print(a[off+i] + ",");
			}
			System.out.println("]");
		}			

		int size;
		
		do { 
			if (index+SIZEOF_SHORT >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}
			
			size = min((BUF_SIZE-index)/SIZEOF_SHORT, len);
			
			Conversion.short2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_SHORT;
			
		} while (len != 0);	
	}

	public void writeArray(char[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.println("writeArray(char[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			if (index+SIZEOF_CHAR >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}
			
			size = min((BUF_SIZE-index)/SIZEOF_CHAR, len);
			
			Conversion.char2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_CHAR;
			
		} while (len != 0);
	}


	public void writeArray(int[] a, int off, int len) throws IOException {
		
		if (DEBUG) { 
			System.out.println("writeArray(int[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			if (index+SIZEOF_INT >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}
			
			size = min((BUF_SIZE-index)/SIZEOF_INT, len);
			
			Conversion.int2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_INT;
			
		} while (len != 0);	 
	}
	
	
	public void writeArray(long[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.println("writeArray(long[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			if (index+SIZEOF_LONG >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}

			size = min((BUF_SIZE-index)/SIZEOF_LONG, len);
			
			Conversion.long2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_LONG;
						
		} while (len != 0);  
	}
	
	
	public void writeArray(float[] a, int off, int len) throws IOException {
		int size;
		
		do { 
			if (index+SIZEOF_FLOAT >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}

			size = min((BUF_SIZE-index)/SIZEOF_FLOAT, len);
			
			Conversion.float2byte(a, off, size, buffer, index);
			
			len   -= size;
			index += size*SIZEOF_FLOAT;
						
		} while (len != 0);  
	}


	public void writeArray(double[] a, int off, int len) throws IOException {

		if (DEBUG) { 
			System.out.println("writeArray(double[" + off + " ... " + (off+len) + "])");
		}			

		int size;
		
		do { 
			if (index+SIZEOF_DOUBLE >= BUF_SIZE) { 
				out.write(buffer, 0, index);
				index = 0;
			}
			
			size = min((BUF_SIZE-index)/SIZEOF_DOUBLE, len);

			Conversion.double2byte(a, off, size, buffer, index);
		   
			len   -= size;
			index += size*SIZEOF_DOUBLE;
			
		} while (len != 0);	 		
	}


	public void flush() throws IOException {
		out.write(buffer, 0, index);
		index = 0;
		out.flush();
	}
}
