final class StoreBuffer { 

	// This is the write part. It's slow, but we don't care (only read part matters)	
	boolean [] boolean_store = null;
	byte [] byte_store = null;
	short [] short_store = null;
	char [] char_store = null;
	int [] int_store = null;
	long [] long_store = null;
	float [] float_store = null;
	double [] double_store = null;
	
	public void writeArray(boolean[] a, int off, int len) { 
		if (boolean_store == null) { 
			boolean_store = new boolean[len];
			System.arraycopy(a, off, boolean_store, 0, len);
		} else { 
			boolean [] temp = new boolean[boolean_store.length + len];
			System.arraycopy(boolean_store, 0, temp, 0, boolean_store.length);
			System.arraycopy(a, off, temp, boolean_store.length, len);
			boolean_store = temp;
		} 
	}

	public void writeArray(byte[] a, int off, int len) { 
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
	
	public void writeArray(short[] a, int off, int len) { 
		if (short_store == null) { 
			short_store = new short[len];
			System.arraycopy(a, off, short_store, 0, len);
		} else { 
			short [] temp = new short[short_store.length + len];
			System.arraycopy(short_store, 0, temp, 0, short_store.length);
			System.arraycopy(a, off, temp, short_store.length, len);
			short_store = temp;
		} 
	}
	
	public void writeArray(char[] a, int off, int len) { 
		if (char_store == null) { 
			char_store = new char[len];
			System.arraycopy(a, off, char_store, 0, len);
		} else { 
			char [] temp = new char[char_store.length + len];
			System.arraycopy(char_store, 0, temp, 0, char_store.length);
			System.arraycopy(a, off, temp, char_store.length, len);
			char_store = temp;
		} 
	}
	
	public void writeArray(int[] a, int off, int len) { 
		if (int_store == null) { 
			int_store = new int[len];
			System.arraycopy(a, off, int_store, 0, len);
		} else { 
			int [] temp = new int[int_store.length + len];
			System.arraycopy(int_store, 0, temp, 0, int_store.length);
			System.arraycopy(a, off, temp, int_store.length, len);
			int_store = temp;
		} 
	}
	
	public void writeArray(long[] a, int off, int len) { 
		if (long_store == null) { 
			long_store = new long[len];
			System.arraycopy(a, off, long_store, 0, len);
		} else { 
			long [] temp = new long[long_store.length + len];
			System.arraycopy(long_store, 0, temp, 0, long_store.length);
			System.arraycopy(a, off, temp, long_store.length, len);
			long_store = temp;
		} 
	}
	
	public void writeArray(float[] a, int off, int len) { 
		if (float_store == null) { 
			float_store = new float[len];
			System.arraycopy(a, off, float_store, 0, len);
		} else { 
			float [] temp = new float[float_store.length + len];
			System.arraycopy(float_store, 0, temp, 0, float_store.length);
			System.arraycopy(a, off, temp, float_store.length, len);
			float_store = temp;
		} 
	}
	
	public void writeArray(double[] a, int off, int len) { 
		if (double_store == null) { 
			double_store = new double[len];
			System.arraycopy(a, off, double_store, 0, len);
		} else { 
			double [] temp = new double[double_store.length + len];
			System.arraycopy(double_store, 0, temp, 0, double_store.length);
			System.arraycopy(a, off, temp, double_store.length, len);
			double_store = temp;
		} 
	}

	public void clear() { 
		boolean_store = null;
		byte_store = null;
		short_store = null;
		char_store = null;
		int_store = null;
		long_store = null;
		float_store = null;
		double_store = null;
	} 
} 
