package ibis.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is an implementation of <code>ArrayOutputStream</code> (and thus also
 * of <code>IbisAccumulator</code>. It is built on top of an
 * <code>OutputStream</code>.
 */

public final class BufferedArrayOutputStream extends ArrayOutputStream {

    /**
     * Some debugging information is printed when this is set to
     * <code>true</code>.
     */
    public static final boolean DEBUG = false;

    /**
     * Size of the buffer in which output data is collected.
     */
    private static final int BUF_SIZE = IbisStreamFlags.BUFFER_SIZE;

    /**
     * The underlying <code>OutputStream</code>.
     */
    private OutputStream out;

    /**
     * The buffer in which output data is collected.
     */
    private byte [] buffer = new byte[BUF_SIZE];

    /**
     * Size of the buffer in which output data is collected.
     */
    private int index = 0;

    /**
     * Number of bytes written so far to the underlying layer.
     */
    private long bytes = 0;

    /**
     * Object used for conversion of primitive types to bytes
     */
    private Conversion conversion = null;

    /**
     * {@inheritDoc}
     */
    public long bytesWritten() { 
	return bytes;
    } 

    /**
     * {@inheritDoc}
     */
    public void resetBytesWritten() {
	bytes = 0;
    }

    /**
     * Constructor.
     * @param out	the underlying <code>OutputStream</code>
     */
    public BufferedArrayOutputStream(OutputStream out) {
	this.out = out;
	conversion = Conversion.loadConversion(false);
    }

    /**
     * Checks if there is space for <code>incr</code> more bytes and if not,
     * the buffer is written to the underlying <code>OutputStream</code>.
     *
     * @param incr		the space requested
     * @exception IOException	in case of trouble.
     */
    private final void flush(int incr) throws IOException {

//	System.err.println("flush(" + incr + ") : " + " " +
//		(index + incr >= BUF_SIZE) + " "  + (index) + ")");

	if (index + incr >= BUF_SIZE) { 
	    bytes += index;

//	    System.err.print("fflushing [");
//	    for (int i=0;i<index;i++) { 
//		System.err.print(buffer[i] + ",");
//	    }
//	    System.err.println("] " + bytes);

	    out.write(buffer, 0, index);
	    index = 0;
	}
    }

    /**
     * {@inheritDoc}
     */
    public final void writeArray(boolean[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(boolean[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(0);

	    int size = Math.min(BUF_SIZE-index, len);

	    conversion.boolean2byte(ref, off, size, buffer, index);

	    off   += size;
	    index += size;
	    len   -= size;

	} while (len != 0);
    }

    /**
     * {@inheritDoc}
     */
    public final void writeArray(byte[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(byte[" + off + " ... " +
		    (off+len) + "])");

	}			

	if (len > (BUF_SIZE-index)) { 

	    if (index > 0) { 
		out.write(buffer, 0, index);
		index = 0;
	    } 
	    if (len >= BUF_SIZE) {
		out.write(ref, off, len);
	    }
	    else {
		System.arraycopy(ref, off, buffer, 0, len);
		index = len;
	    }
	} else { 
	    System.arraycopy(ref, off, buffer, index, len);			
	    index += len;
	} 
    }

    public final void writeArray(char[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(char[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(SIZEOF_CHAR);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_CHAR, len);

	    conversion.char2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_CHAR;

	} while (len != 0);
    }

    public final void writeArray(short[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(short[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(SIZEOF_SHORT);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_SHORT, len);

//	    System.err.println("Room to write " + size + " shorts");

	    conversion.short2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_SHORT;

//	    System.err.println("Len = " + len + " index = " + index);

	} while (len != 0);	
    }

    public final void writeArray(int[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(int[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(SIZEOF_INT);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_INT, len);

//	    System.err.println("Room to write " + size + " ints");

	    conversion.int2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_INT;

//	    System.err.println("Len = " + len + " index = " + index);

	} while (len != 0);	 
    }

    public final void writeArray(long[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(long[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(SIZEOF_LONG);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_LONG, len);

	    conversion.long2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_LONG;

	} while (len != 0);  
    }

    public final void writeArray(float[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(float[" + off + " ... " +
		    (off+len) + "])");
	}			
	do { 
	    flush(SIZEOF_FLOAT);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_FLOAT, len);

	    conversion.float2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_FLOAT;

	} while (len != 0);  
    }

    public final void writeArray(double[] ref, int off, int len)
	    throws IOException {
	if (DEBUG) { 
	    System.err.println("writeArray(double[" + off + " ... " +
		    (off+len) + "])");
	}			

	do { 
	    flush(SIZEOF_DOUBLE);

	    int size = Math.min((BUF_SIZE-index)/SIZEOF_DOUBLE, len);

	    conversion.double2byte(ref, off, size, buffer, index);

	    off   += size;
	    len   -= size;
	    index += size*SIZEOF_DOUBLE;

	} while (len != 0);	 		
    }

    /**
     * {@inheritDoc}
     */
    public final void flush() throws IOException {
	flush(BUF_SIZE+1);	/* Forces flush */
	out.flush();
    }

    /**
     * {@inheritDoc}
     */
    public final void finish() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    public final boolean finished() {
	return true;
    }

    /**
     * {@inheritDoc}
     */
    public final void close() throws IOException {
	flush();
	out.close();
    }
}
