final class StoreBuffer { 

	// This is the write part. It's slow, but we don't care (only read part matters)	
	byte [] byte_store = null;

	public void write(int b) { 
		byte [] temp = new byte[1];
		temp[0] = (byte)b;
		write(temp);
	} 

	public void write(byte[] a) { 
		write(a, 0, a.length);
	}

	public void write(byte[] a, int off, int len) { 
		if (byte_store == null) { 
			byte_store = new byte[len];
			System.arraycopy(a, off, byte_store, 0, len);
		} else { 
			byte [] temp = new byte[byte_store.length + len];
			System.arraycopy(byte_store, 0, temp, 0, byte_store.length);
			System.arraycopy(a, off, temp, byte_store.length, len);
			byte_store = temp;
		} 
	}

	public void clear() { 
		byte_store = null;
	} 
} 
