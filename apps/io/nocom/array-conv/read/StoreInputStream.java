import java.io.IOException;
import java.io.InputStream;

/**
 *
 * Extends OutputStream with read of array of primitives and readSingleInt
 */

public class StoreInputStream extends InputStream {

	StoreBuffer buf;

	int byte_count = 0;

	public StoreInputStream(StoreBuffer buf) { 
		this.buf = buf;
	} 

	public void reset() { 
		byte_count = 0;
	} 
	
	public int read() { 
// System.err.println("Read one byte " + Integer.toHexString(buf.byte_store[byte_count]));
		return buf.byte_store[byte_count++];
	}

	public int read(byte[] b) { 
		return read(b, 0, b.length);
	}

		
	public int read(byte[] b, int off, int len) { 
		int left = buf.byte_store.length-byte_count;

// System.err.println("Read byte array[" + len + "] present " + left);
// for (int i = 0; i < (len < left ? len : left); i++) {
//     System.err.print(Integer.toHexString(buf.byte_store[byte_count + i] & 0xff) + " ");
// }
		if (len < left) { 
			System.arraycopy(buf.byte_store, byte_count, b, off, len);
			byte_count += len;
			return len;
		} else { 
			System.arraycopy(buf.byte_store, byte_count, b, off, left);
			byte_count += left;
			return left;
		} 
	} 

	public int available() throws IOException { 
		return 0;
	}
}

