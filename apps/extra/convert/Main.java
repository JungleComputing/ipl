class Bench { 
	
	private static final int LEN = 16*1024;
	private static final int COUNT = 10000;

	public static double round(double val) {                
                return (Math.ceil(val*100.0)/100.0);
        } 

	public static void main(String [] args) { 

		long start, end;
		int len;
		boolean ok = true;
		byte [] dst = new byte[LEN];

		// test boolean[]

		len = LEN;
		System.out.print("boolean[] ---> byte[] ");
		ok = true;
		boolean [] src_b = new boolean[len];

		for (int i=0;i<len;i++) { 
			src_b[i] = (i % 3 == 0);
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.boolean2byte(src_b, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.boolean2byte(src_b, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("boolean[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2boolean(dst, 0, src_b, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2boolean(dst, 0, src_b, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		for (int i=0;i<len;i++) { 
			if (src_b[i] != (i % 3 == 0)) { 
				ok = false;
			} 
		} 
		
		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_b = null;

		// test short[]
		len = LEN/2;
		System.out.print("short[] ---> byte[] ");
		ok = true;
		short [] src_s = new short[len];

		for (int i=0;i<len;i++) { 
			src_s[i] = (short) i;
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.short2byte(src_s, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.short2byte(src_s, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("short[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2short(dst, 0, src_s, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2short(dst, 0, src_s, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src_s[i] != (short) i) { 				
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_s = null;

		// test char[]
		len = LEN/2;
		System.out.print("char[] ---> byte[] ");
		char [] temp = { 'H', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!' };

		ok = true;
		char [] src_c = new char[len];

		for (int i=0;i<len;i++) { 
			src_c[i] = temp[(i % temp.length)];
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.char2byte(src_c, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.char2byte(src_c, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("char[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2char(dst, 0, src_c, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2char(dst, 0, src_c, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src_c[i] != temp[(i % temp.length)]) { 
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_c = null;
			
		// test int[]
		len = LEN/4;
		System.out.print("int[] ---> byte[] ");
		ok = true;
		int [] src1 = new int[len];

		for (int i=0;i<len;i++) { 
			src1[i] = (17 << 16) + i;
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.int2byte(src1, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.int2byte(src1, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("int[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2int(dst, 0, src1, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2int(dst, 0, src1, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src1[i] != (17 << 16) + i) { 
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src1 = null;
		
		// test long[]
		len = LEN/8;
		System.out.print("long[] ---> byte[] ");
		ok = true;
		long [] src_l = new long[len];
		
		long [] temp_l = new long[8];
		
		long temp_l_2 = (((long)1 << 7) << 56) | 
			(((long)1 << 6) << 48) |  
			(((long)1 << 5) << 40) |
			(((long)1 << 4) << 32) | 
			(((long)1 << 3) << 24) | 
			(((long)1 << 2) << 16) | 
			(((long)1 << 2) << 8)  | 
			((long)1); 
		
		for (int i=0;i<8;i++) { 
			temp_l[i] = temp_l_2 << i;
		} 

		for (int i=0;i<len;i++) { 
			src_l[i] = temp_l[i % 8];
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.long2byte(src_l, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.long2byte(src_l, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("long[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2long(dst, 0, src_l, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2long(dst, 0, src_l, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src_l[i] != temp_l[i % 8]) { 
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_l = null;

		// test float[]
		len = LEN/4;
		System.out.print("float[] ---> byte[] ");
		ok = true;
		float [] src_f = new float[len];

		for (int i=0;i<len;i++) { 
			src_f[i] = (float)(i/2.0);
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.float2byte(src_f, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.float2byte(src_f, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("float[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2float(dst, 0, src_f, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2float(dst, 0, src_f, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src_f[i] != (float) (i/2.0)) { 
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_f = null;
		
		// test double[]
		len = LEN/8;
		System.out.print("double[] ---> byte[] ");
		ok = true;
		double [] src_d = new double[len];

		for (int i=0;i<len;i++) { 
			src_d[i] = Math.PI + (i/2.5);
		} 

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.double2byte(src_d, 0, len, dst, 0);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.double2byte(src_d, 0, len, dst, 0);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");
		
		System.out.print("double[] <--- byte[] ");

		// warmup
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2double(dst, 0, src_d, 0, len);
		} 

		// test
		start = System.currentTimeMillis();
		
		for (int i=0;i<COUNT;i++) { 
			Conversion.byte2double(dst, 0, src_d, 0, len);
		} 

		end = System.currentTimeMillis();

		System.out.println(round(((1000.0*LEN*COUNT)/(1024*1024*(end-start)))) + " Mbytes/sec.");

		for (int i=0;i<len;i++) { 
			if (src_d[i] != Math.PI + (i/2.5)) { 
				ok = false;
			} 
		} 

		System.out.println("Verification " + (ok ? "ok" : "failed"));
		System.out.println();
		src_d = null;
	} 
} 
