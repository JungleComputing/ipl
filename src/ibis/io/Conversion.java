package ibis.io;

public final class Conversion { 

	/* NOTE: 
         *  
         * All conversion methods in this class have the precondition that the data actually 
         * fits in the destination buffer. The user should do the buffering himself.  
         * We may be able to rewrite this to use some callback mechanism. 
         * i.e., int2byte(src, off, len, dst, off, callback)
         *        
         * The callback function can then be used to flush the buffer (or get a new one). 
         * Not sure how expensive this is though ...
         *
         */

	private static boolean USE_NATIVE_CONVERSION = false;

	public static void classInit() {
//		System.err.println("pre load");
		if (USE_NATIVE_CONVERSION) {
		    try {
			System.loadLibrary("conversion");
		    } catch (Throwable e) {
			System.err.println("Could not load native library for data conversions, falling back to Java conversion ");
			USE_NATIVE_CONVERSION = false;
		    }
		}
//		System.err.println("post load");
	}
	
	static {
		classInit();
	}

	private static final native void n_boolean2byte(boolean[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_char2byte(char[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_short2byte(short[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_int2byte(int[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_long2byte(long[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_float2byte(float[] src, int off, int len, byte [] dst, int off2);
	private static final native void n_double2byte(double[] src, int off, int len, byte [] dst, int off2);

	private static final native void n_byte2boolean(byte[] src, int off2, boolean[] dst, int off, int len);
	private static final native void n_byte2short(byte[] src, int off2, short[] dst, int off, int len);
	private static final native void n_byte2char(byte[] src, int off2, char[] dst, int off, int len);
	private static final native void n_byte2int(byte[] src, int off2, int[] dst, int off, int len);
	private static final native void n_byte2long(byte[] src, int off2, long[] dst, int off, int len);
	private static final native void n_byte2float(byte[] src, int off2, float[] dst, int off, int len);
	private static final native void n_byte2double(byte[] src,  int off2, double[] dst, int off, int len);

	static final void boolean2byte(boolean[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
			for (int i=0;i<len;i++) { 			
				dst[off2+i] = (src[off+i] ? (byte)1 : (byte)0);
			} 
		} else { 
			n_boolean2byte(src, off, len, dst, off2);
		} 
	}

	static final void byte2boolean(byte[] src, int index_src, boolean[] dst, int index_dst, int len) { 

		if (! USE_NATIVE_CONVERSION) {
			for (int i=0;i<len;i++) { 			
				dst[index_dst+i] = (src[i] == 1);
			}
		} else { 			
			n_byte2boolean(src, index_src, dst, index_dst, len);
		} 
	} 

	static final void char2byte(char[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
			char temp;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				temp = src[off+i];
				dst[count+0] = (byte) (temp & 0xff);
				dst[count+1] = (byte) ((temp >>> 8)  & 0xff);
				count += 2;
			} 
		} else { 
			n_char2byte(src, off, len, dst, off2);
		}
	}

	static final void byte2char(byte[] src, int index_src, char[] dst, int index_dst, int len) {
		
		if (! USE_NATIVE_CONVERSION) {
 		        int temp;
			int count = index_src;

			for (int i=0;i<len;i++) { 			
				temp = (src[count+1] & 0xff);
				temp = temp << 8;
				temp |= (src[count] & 0xff);
				dst[index_dst+i] = (char) temp;
				count += 2;
			}
		} else { 			
			n_byte2char(src, index_src, dst, index_dst, len);
		}
	}

	static final void short2byte(short[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
			short v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = src[off+i];	
				dst[count+0] = (byte)(0xff & (v >> 8));
				dst[count+1] = (byte)(0xff & v);
				count += 2;
			} 
		} else { 
			n_short2byte(src, off, len, dst, off2);
		}
	}

	static final void byte2short(byte[] src, int index_src, short[] dst, int index_dst, int len) {

		if (! USE_NATIVE_CONVERSION) {
			int count = index_src;
			for (int i=0;i<len;i++) { 			
				dst[index_dst+i] = (short)((src[count] << 8) | (src[count+1] & 0xff));
				count+=2;
			}
		} else { 			
			n_byte2short(src, index_src, dst, index_dst, len);
		} 
	}

	static final void int2byte(int[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
		
			int v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = src[off+i];

				dst[count+0] = (byte)(0xff & (v >> 24));
				dst[count+1] =(byte)(0xff  & (v >> 16));
				dst[count+2] =(byte)(0xff  & (v >> 8));
				dst[count+3] =(byte)(0xff  & v);
				count += 4;
			} 

		} else { 
			n_int2byte(src, off, len, dst, off2);
		}
	}

	static final void byte2int(byte[] src, int index_src, int[] dst, int index_dst, int len) {

		if (! USE_NATIVE_CONVERSION) {
			int count = index_src;
			for (int i=0;i<len;i++) { 			
				 dst[index_dst+i] = (((src[count+3] & 0xff) <<  0) | 
						     ((src[count+2] & 0xff) <<  8) |
						     ((src[count+1] & 0xff) << 16) | 
						     ((src[count+0] & 0xff) << 24));
				 count += 4;
			}
		} else { 			
			n_byte2int(src, index_src, dst, index_dst, len);
		}
	}

	static final void long2byte(long[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
			long v;
			int count = off2;
			int end = off+len;
			
			for (int i=off;i<end;i++) { 		
				v = src[i];
				int v1 = (int)(v >> 32);
				int v2 = (int)(v);

				dst[count+0] = (byte)(0xff & (v1 >> 24));
				dst[count+1] = (byte)(0xff & (v1 >> 16));
				dst[count+2] = (byte)(0xff & (v1 >>  8));
				dst[count+3] = (byte)(0xff & (v1 >>  0));
				dst[count+4] = (byte)(0xff & (v2 >> 24));
				dst[count+5] = (byte)(0xff & (v2 >> 16));
				dst[count+6] = (byte)(0xff & (v2 >>  8));
				dst[count+7] = (byte)(0xff & (v2 >>  0));

				count += 8;
			}

		} else { 
			n_long2byte(src, off, len, dst, off2);
		}
	}

	static final void byte2long(byte[] src, int index_src, long[] dst, int index_dst, int len) { 		

		if (! USE_NATIVE_CONVERSION) {

			int count = index_src;
			int end = index_dst + len;
			
			for (int i=index_dst;i<end;i++) { 	
				int t1 = (((int)(src[count+0] & 0xff) << 24) |
					  ((int)(src[count+1] & 0xff) << 16) |
					  ((int)(src[count+2] & 0xff) <<  8) |
					  ((int)(src[count+3] & 0xff) <<  0));
				int t2 = (((int)(src[count+4] & 0xff) << 24) |
					  ((int)(src[count+5] & 0xff) << 16) |
					  ((int)(src[count+6] & 0xff) <<  8) |
					  ((int)(src[count+7] & 0xff) <<  0));
				dst[i] = ((((long) t1) << 32) | (((long) t2) & 0xffffffffL));
				count += 8;
			}
		} else { 			
			n_byte2long(src, index_src, dst, index_dst, len);
		}
	} 

	static final void float2byte(float[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {

			int v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = Float.floatToIntBits(src[off+i]);
				dst[count+0] = (byte)(0xff & (v >> 24));
				dst[count+1] = (byte)(0xff & (v >> 16));
				dst[count+2] = (byte)(0xff & (v >> 8));
				dst[count+3] = (byte)(0xff & v);
				count += 4;
			} 
		} else { 
			n_float2byte(src, off, len, dst, off2);
		}			
	}

	static final void byte2float(byte[] src, int index_src, float[] dst, int index_dst, int len) { 

		if (! USE_NATIVE_CONVERSION) {
			int temp = 0;
			int count = index_src;
			
			for (int i=0;i<len;i++) { 			

				temp = (((src[count+0] & 0xff) << 24) | 
					((src[count+1] & 0xff) << 16) |
					((src[count+2] & 0xff) <<  8) | 
					((src[count+3] & 0xff) <<  0));
				dst[index_dst+i] = Float.intBitsToFloat(temp);
				count += 4;
			}
		} else { 
			n_byte2float(src, index_src, dst, index_dst, len);
		}
	} 


	static final void double2byte(double[] src, int off, int len, byte [] dst, int off2) {

		if (! USE_NATIVE_CONVERSION) {
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				long v = Double.doubleToLongBits(src[off++]);
				int v1 = (int) (v >> 32);
				int v2 = (int) (v);

				dst[count+0] = (byte)(0xff & (v1 >> 24));
				dst[count+1] = (byte)(0xff & (v1 >> 16));
				dst[count+2] = (byte)(0xff & (v1 >>  8));
				dst[count+3] = (byte)(0xff & (v1 >>  0));
				dst[count+4] = (byte)(0xff & (v2 >> 24));
				dst[count+5] = (byte)(0xff & (v2 >> 16));
				dst[count+6] = (byte)(0xff & (v2 >>  8));
				dst[count+7] = (byte)(0xff & (v2 >>  0));
				count += 8;
			}

		} else { 
			n_double2byte(src, off, len, dst, off2);
		}
	}

	static final void byte2double(byte[] src, int index_src, double[] dst, int index_dst, int len) { 

		if (! USE_NATIVE_CONVERSION) {
			
			int count = index_src;
			int end = index_dst + len;
			
			for (int i=index_dst;i<end;i++) { 			
				int t1 = (((int)(src[count+0] & 0xff) << 24) |
					  ((int)(src[count+1] & 0xff) << 16) |
					  ((int)(src[count+2] & 0xff) <<  8) |
					  ((int)(src[count+3] & 0xff) <<  0));
				int t2 = (((int)(src[count+4] & 0xff) << 24) |
					  ((int)(src[count+5] & 0xff) << 16) |
					  ((int)(src[count+6] & 0xff) <<  8) |
					  ((int)(src[count+7] & 0xff) <<  0));

 				dst[i] = Double.longBitsToDouble((((long) t1) << 32) | (((long) t2) & 0xffffffffL));
				count += 8;
			}
		} else { 
			n_byte2double(src, index_src, dst, index_dst, len);
		}			
	} 

    public static void main(String[] arg) {
	System.loadLibrary("conversion");
    }
} 
