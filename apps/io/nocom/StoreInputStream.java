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

	public final void reset() { 
		byte_count = 0;
	} 
	
	public final int read() { 
		return buf.byte_store[byte_count++];
	}

	public final int read(byte[] b) { 
		return read(b, 0, b.length);
	}

		
	public final int read(byte[] b, int off, int len) { 
		int left = buf.byte_store.length-byte_count;

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

	public final int available() throws IOException { 
		return 0;
	}
}

