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

	public static void classInit() {
//		System.err.println("pre load");
		try {
			System.loadLibrary("conversion");
		} catch (Throwable e) {
			System.err.println("Could not load native library for data conversions, falling back to Java conversion ");
		}
//		System.err.println("post load");
	}
	
//	static {
//		classInit();
//	}

	static final int BOOLEAN2BYTE_THRESHOLD = 100;
	static final int CHAR2BYTE_THRESHOLD    = 50;
	static final int SHORT2BYTE_THRESHOLD   = 50;
	static final int INT2BYTE_THRESHOLD     = 25;
	static final int LONG2BYTE_THRESHOLD    = 13;
	static final int FLOAT2BYTE_THRESHOLD   = 25;
	static final int DOUBLE2BYTE_THRESHOLD  = 13;

	static final int BYTE2BOOLEAN_THRESHOLD = 100;
	static final int BYTE2SHORT_THRESHOLD   = 50;
	static final int BYTE2CHAR_THRESHOLD    = 50;
	static final int BYTE2INT_THRESHOLD     = 25;
	static final int BYTE2LONG_THRESHOLD    = 13;
	static final int BYTE2FLOAT_THRESHOLD   = 25;
	static final int BYTE2DOUBLE_THRESHOLD  = 13;

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

//		if (len < BOOLEAN2BYTE_THRESHOLD) { 
			for (int i=0;i<len;i++) { 			
				dst[off2+i] = (src[off+i] ? (byte)1 : (byte)0);
			} 
//		} else { 
//			n_boolean2byte(src, off, len, dst, off2);
//		} 
	}

	static final void byte2boolean(byte[] src, int index_src, boolean[] dst, int index_dst, int len) { 

//		if (len < BYTE2BOOLEAN_THRESHOLD) { 
			for (int i=0;i<len;i++) { 			
				dst[index_dst+i] = (src[i] == 1);
			}
//		} else { 			
//			n_byte2boolean(src, index_src, dst, index_dst, len);
//		} 
	} 

	static final void char2byte(char[] src, int off, int len, byte [] dst, int off2) {

//		if (len < CHAR2BYTE_THRESHOLD) { 
			char temp;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				temp = src[off+i];
				dst[count++] = (byte) (temp & 0xff);
				dst[count++] = (byte) ((temp >>> 8)  & 0xff);
			} 
//		} else { 
//			n_char2byte(src, off, len, dst, off2);
//		}
	}

	static final void byte2char(byte[] src, int index_src, char[] dst, int index_dst, int len) {
		
//		if (len < BYTE2CHAR_THRESHOLD) { 
 		        int temp;
			int count = index_src;

			for (int i=0;i<len;i++) { 			
				temp = (src[count+1] & 0xff);
				temp = temp << 8;
				temp |= (src[count] & 0xff);
				dst[index_dst+i] = (char) temp;
				count += 2;
			}
//		} else { 			
//			n_byte2char(src, index_src, dst, index_dst, len);
//		}
	}

	static final void short2byte(short[] src, int off, int len, byte [] dst, int off2) {

//		if (len < SHORT2BYTE_THRESHOLD) { 
			short v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = src[off+i];	
				dst[count++] = (byte)(0xff & (v >> 8));
				dst[count++] = (byte)(0xff & v);
			} 
//		} else { 
//			n_short2byte(src, off, len, dst, off2);
//		}
	}

	static final void byte2short(byte[] src, int index_src, short[] dst, int index_dst, int len) {

//		if (len < BYTE2SHORT_THRESHOLD) { 
			int count = index_src;
			for (int i=0;i<len;i++) { 			
				dst[index_dst+i] = (short)((src[count] << 8) | (src[count+1] & 0xff));
				count+=2;
			}
//		} else { 			
//			n_byte2short(src, index_src, dst, index_dst, len);
//		} 
	}

	static final void int2byte(int[] src, int off, int len, byte [] dst, int off2) {

//		if (len < INT2BYTE_THRESHOLD) { 
		
			int v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = src[off+i];

				dst[count++] = (byte)(0xff & (v >> 24));
				dst[count++] =(byte)(0xff  & (v >> 16));
				dst[count++] =(byte)(0xff  & (v >> 8));
				dst[count++] =(byte)(0xff  & v);
			} 

//		} else { 
//			n_int2byte(src, off, len, dst, off2);
//		}
	}

	static final void byte2int(byte[] src, int index_src, int[] dst, int index_dst, int len) {

//		if (len < BYTE2INT_THRESHOLD) { 
			int count = index_src;
			for (int i=0;i<len;i++) { 			
//				dst[index_dst+i] = (int)
//					((int)(src[count] & 0xff) | 
//					((int)(src[count+1] & 0xff) <<  8) |
//					((int)(src[count+2] & 0xff) << 16) |
//					((int)(src[count+3] & 0xff) << 24));

				 dst[index_dst+i] = (((src[count] & 0xff) << 24)   | 
						     ((src[count+1] & 0xff) << 16) |
						     ((src[count+2] & 0xff) << 8)  | 
						     (src[count+3] & 0xff));
				 count+=4;
			}
//		} else { 			
//			n_byte2int(src, index_src, dst, index_dst, len);
//		}
	}

	static final void long2byte(long[] src, int off, int len, byte [] dst, int off2) {

//		if (len < LONG2BYTE_THRESHOLD) { 
			long v;
			int count = off2;
			
			for (int i=0;i<len;i++) { 		
				v = src[off+i];

				dst[count++] = (byte)(0xff & (v >> 56));
				dst[count++] = (byte)(0xff & (v >> 48));
				dst[count++] = (byte)(0xff & (v >> 40));
				dst[count++] = (byte)(0xff & (v >> 32));
				dst[count++] = (byte)(0xff & (v >> 24));
				dst[count++] = (byte)(0xff & (v >> 16));
				dst[count++] = (byte)(0xff & (v >>  8));
				dst[count++] = (byte)(0xff & v);
			}

//		} else { 
//			n_long2byte(src, off, len, dst, off2);
//		}
	}

	static final void byte2long(byte[] src, int index_src, long[] dst, int index_dst, int len) { 		

//		if (len < BYTE2LONG_THRESHOLD) {

			int count = index_src;
			
			for (int i=0;i<len;i++) { 	
 				dst[index_dst+i] = (((long)(src[count] & 0xff) << 56) |
						    ((long)(src[count+1] & 0xff) << 48) |
						    ((long)(src[count+2] & 0xff) << 40) |
						    ((long)(src[count+3] & 0xff) << 32) |
						    ((long)(src[count+4] & 0xff) << 24) |
						    ((long)(src[count+5] & 0xff) << 16) |
						    ((long)(src[count+6] & 0xff) <<  8) |
						    ((long)(src[count+7] & 0xff)));
				count += 8;

			}
//		} else { 			
//			n_byte2long(src, index_src, dst, index_dst, len);
//		}
	} 

	static final void float2byte(float[] src, int off, int len, byte [] dst, int off2) {

//		if (len < FLOAT2BYTE_THRESHOLD) { 

			int v = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = Float.floatToIntBits(src[off+i]);
				dst[count++] = (byte)(0xff & (v >> 24));
				dst[count++] = (byte)(0xff & (v >> 16));
				dst[count++] = (byte)(0xff & (v >> 8));
				dst[count++] = (byte)(0xff & v);
			} 
//		} else { 
//			n_float2byte(src, off, len, dst, off2);
//		}			
	}

	static final void byte2float(byte[] src, int index_src, float[] dst, int index_dst, int len) { 

//		if (len < BYTE2FLOAT_THRESHOLD) {
			int temp = 0;
			int count = index_src;
			
			for (int i=0;i<len;i++) { 			

				temp = (((src[count] & 0xff) << 24) | 
					((src[count+1] & 0xff) << 16) |
					((src[count+2] & 0xff) << 8)  | 
					(src[count+3] & 0xff));
				dst[index_dst+i] = Float.intBitsToFloat(temp);
				count += 4;
			}
//		} else { 
//			n_byte2float(src, index_src, dst, index_dst, len);
//		}
	} 

//       private static native void doublesToBytes(double src[], int double_index, byte dest[], int byte_index, int num_doubles);

	static final void double2byte(double[] src, int off, int len, byte [] dst, int off2) {

//		if (len < DOUBLE2BYTE_THRESHOLD) { 
		        long v = 0;
			int temp2 = 0;
			int temp3 = 0;
			int count = off2;
			
			for (int i=0;i<len;i++) { 			
				v = Double.doubleToLongBits(src[off++]);

				dst[count++] = (byte)(0xff & (v >> 56));
				dst[count++] = (byte)(0xff & (v >> 48));
				dst[count++] = (byte)(0xff & (v >> 40));
				dst[count++] = (byte)(0xff & (v >> 32));
				dst[count++] = (byte)(0xff & (v >> 24));
				dst[count++] = (byte)(0xff & (v >> 16));
				dst[count++] = (byte)(0xff & (v >>  8));
				dst[count++] = (byte)(0xff & v);
			}

//		} else { 
//			n_double2byte(src, off, len, dst, off2);
//		}
	}

	static final void byte2double(byte[] src, int index_src, double[] dst, int index_dst, int len) { 

//		if (len < BYTE2DOUBLE_THRESHOLD) {
			
			long temp = 0;
			int count = index_src;
			
			for (int i=0;i<len;i++) { 			
				long x = (((long)(src[count] & 0xff) << 56) |
					  ((long)(src[count+1] & 0xff) << 48) |
					  ((long)(src[count+2] & 0xff) << 40) |
					  ((long)(src[count+3] & 0xff) << 32) |
					  ((long)(src[count+4] & 0xff) << 24) |
					  ((long)(src[count+5] & 0xff) << 16) |
					  ((long)(src[count+6] & 0xff) <<  8) |
					  ((long)(src[count+7] & 0xff)));

 				dst[index_dst+i] = Double.longBitsToDouble(x);
				count += 8;
			}
//		} else { 
//			n_byte2double(src, index_src, dst, index_dst, len);
//		}			
	} 

    public static void main(String[] arg) {
	System.loadLibrary("conversion");
    }

} 
