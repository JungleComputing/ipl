import java.io.IOException;
import ibis.io.ArrayOutputStream;
import ibis.io.IbisStreamFlags;

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

	public void doWriteArray(Object a, int off, int len, int type) throws IOException { 
	    switch(type){
	    case TYPE_BOOLEAN:
		this.len += len;
		buf.writeArray((boolean[]) a, off, len);
		break;
	    case TYPE_BYTE:
		this.len += len;
		buf.writeArray((byte[]) a, off, len);
		break;
	    case TYPE_SHORT:
		this.len += 2*len;
		buf.writeArray((short[]) a, off, len);
		break;
	    case TYPE_CHAR:
		this.len += 2*len;
		buf.writeArray((char[]) a, off, len);
		break;
	    case TYPE_INT:
		this.len += 4*len;
		buf.writeArray((int[]) a, off, len);
		break;
	    case TYPE_LONG:
		this.len += 8*len;
		buf.writeArray((long[]) a, off, len);
		break;
	    case TYPE_FLOAT:
		this.len += 4*len;
		buf.writeArray((float[]) a, off, len);
		break;
	    case TYPE_DOUBLE:
		this.len += 8*len;
		buf.writeArray((double[]) a, off, len);
	    }
	}
	
	public void doFlush() throws IOException { 
	}

	public boolean finished() {
	    return true;
	}

	public void finish() throws IOException { 
	    flush();
	}

	public void close() throws IOException { 
	    flush();
	}

	public int bytesWritten() { 
		return len;
	} 

	public void resetBytesWritten() {
		len = 0;
	}

} 
