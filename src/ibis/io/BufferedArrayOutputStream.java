package ibis.io;

import java.io.OutputStream;
import java.io.IOException;

/**
 * This is an implementation of <code>ArrayOutputStream</code> (and thus also
 * of <code>IbisAccumulator</code>. It is built on top of an <code>OutputStream</code>.
 */

public final class BufferedArrayOutputStream extends ArrayOutputStream {

    /**
     * Some debugging information is printed when this is set to <code>true</code>.
     */
    public static final boolean DEBUG = false;

    /**
     * Size of the buffer in which output data is collected.
     */
    private static final int BUF_SIZE = 8*1024;

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
    private int bytes = 0;

    /**
     * @inheritDoc
     */
    public int bytesWritten() { 
	return bytes;
    } 

    /**
     * @inheritDoc
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
    }

    /**
     * Checks if there is space for <code>incr</code> more bytes and if not,
     * the buffer is written to the underlying <code>OutputStream</code>.
     *
     * @param incr		the space requested
     * @exception IOException	in case of trouble.
     */
    private final void flush(int incr) throws IOException {

	//		System.err.println("flush(" + incr + ") : " + " " + (index + incr >= BUF_SIZE) + " "  + (index) + ")");

	if (index + incr >= BUF_SIZE) { 
	    bytes += index;

	    //			System.err.print("fflushing [");
	    //			for (int i=0;i<index;i++) { 
	    //				System.err.print(buffer[i] + ",");
	    //			}
	    //			System.err.println("] " + bytes);

	    out.write(buffer, 0, index);
	    index = 0;
	}
    }

    /**
     * @inheritDoc
     */
    public void doWriteArray(Object ref, int off, int len, int type)
	    throws IOException {
	int size;

	switch(type) {
	case TYPE_BOOLEAN:
	    if (DEBUG) { 
		System.err.println("writeArray(boolean[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(0);

		size = Math.min(BUF_SIZE-index, len);

		Conversion.boolean2byte((boolean[]) ref, off, size, buffer, index);

		off   += size;
		index += size;
		len   -= size;

	    } while (len != 0);
	    break;

	case TYPE_BYTE:
	    if (DEBUG) { 
		System.err.println("writeArray(byte[" + off + " ... " + (off+len) + "])");

	    }			

	    if (len > (BUF_SIZE-index)) { 

		if (index > 0) { 
		    out.write(buffer, 0, index);
		    index = 0;
		} 
		if (len >= BUF_SIZE) {
		    out.write((byte[]) ref, off, len);
		}
		else {
		    System.arraycopy((byte[]) ref, off, buffer, 0, len);
		    index = len;
		}
	    } else { 
		System.arraycopy((byte[]) ref, off, buffer, index, len);			
		index += len;
	    } 
	    break;

	case TYPE_CHAR:
	    if (DEBUG) { 
		System.err.println("writeArray(char[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(SIZEOF_CHAR);

		size = Math.min((BUF_SIZE-index)/SIZEOF_CHAR, len);

		Conversion.char2byte((char[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_CHAR;

	    } while (len != 0);
	    break;

	case TYPE_SHORT:
	    if (DEBUG) { 
		System.err.println("writeArray(short[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(SIZEOF_SHORT);

		size = Math.min((BUF_SIZE-index)/SIZEOF_SHORT, len);

		//			System.err.println("Room to write " + size + " shorts");

		Conversion.short2byte((short[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_SHORT;

		//			System.err.println("Len = " + len + " index = " + index);

	    } while (len != 0);	
	    break;

	case TYPE_INT:
	    if (DEBUG) { 
		System.err.println("writeArray(int[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(SIZEOF_INT);

		size = Math.min((BUF_SIZE-index)/SIZEOF_INT, len);

		//			System.err.println("Room to write " + size + " ints");

		Conversion.int2byte((int[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_INT;

		//			System.err.println("Len = " + len + " index = " + index);

	    } while (len != 0);	 
	    break;

	case TYPE_LONG:
	    if (DEBUG) { 
		System.err.println("writeArray(long[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(SIZEOF_LONG);

		size = Math.min((BUF_SIZE-index)/SIZEOF_LONG, len);

		Conversion.long2byte((long[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_LONG;

	    } while (len != 0);  
	    break;

	case TYPE_FLOAT:
	    do { 
		flush(SIZEOF_FLOAT);

		size = Math.min((BUF_SIZE-index)/SIZEOF_FLOAT, len);

		Conversion.float2byte((float[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_FLOAT;

	    } while (len != 0);  
	    break;

	case TYPE_DOUBLE:
	    if (DEBUG) { 
		System.err.println("writeArray(double[" + off + " ... " + (off+len) + "])");
	    }			

	    do { 
		flush(SIZEOF_DOUBLE);

		size = Math.min((BUF_SIZE-index)/SIZEOF_DOUBLE, len);

		Conversion.double2byte((double[]) ref, off, size, buffer, index);

		off   += size;
		len   -= size;
		index += size*SIZEOF_DOUBLE;

	    } while (len != 0);	 		
	    break;
	}
    }

    /**
     * @inheritDoc
     */
    public final void doFlush() throws IOException {
	if (DEBUG) {
	    System.out.println("Flushing ...");
	}
	flush(BUF_SIZE+1);	/* Forces flush */
	out.flush();
    }

    /**
     * @inheritDoc
     */
    public final void finish() throws IOException {
    }

    /**
     * @inheritDoc
     */
    public final boolean finished() {
	return true;
    }

    /**
     * @inheritDoc
     */
    public void close() throws IOException {
	flush();
	out.close();
    }
}
