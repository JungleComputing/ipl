import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.ArrayInputStream;
import ibis.io.ArrayOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int LEN   = 1024*1024;
	public static final int COUNT = 100;
	public static final int TESTS = 10;
		
	public static double round(double val) { 		
		return (Math.ceil(val*100.0)/100.0);
	} 

	public static void main(String args[]) {
		
		try {
			long start, end;
			int bytes;
		
			double best_ktp = 0.0;
			long best_time = 1000000;

			System.out.println("Main starting");
			
			StoreBuffer buf = new StoreBuffer();
			StoreArrayOutputStream out = new StoreArrayOutputStream(buf);
			StoreArrayInputStream in = new StoreArrayInputStream(buf);

			MantaOutputStream mout = new MantaOutputStream(out);
			MantaInputStream min = new MantaInputStream(in);
				
			// Create array
			int [] temp1 = new int[LEN/4];
			
			System.out.println("Writing int[" + (LEN/4) + "]");

			mout.writeArray(temp1, 0, LEN/4);
			mout.flush();
			mout.reset();

//			System.out.println("Wrote " + out.getAndReset() + " bytes");
			
//			System.out.println("Reading int[" + (LEN/4) + "]");
			min.readArray(temp1, 0, LEN/4);
			in.reset();
			buf.clear();

//			System.out.println("Rewriting int[" + (LEN/4) + "]");

			mout.writeArray(temp1, 0, LEN/4);
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

//			System.out.println("Wrote " + bytes + " bytes");
			
//			System.out.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp1, 0, LEN/4);
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
//				System.out.println();
//				System.out.println("Read took " + time + " ms");
//				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

			System.out.println("" + round(best_ktp));
			temp1 = null;
			in.reset();
			buf.clear();
			best_time= 1000000;
			/*********************************/

			// Create array
			long [] temp2 = new long[LEN/8];
			
			System.out.println("Writing long[" + (LEN/8) + "]");

			mout.writeArray(temp2, 0, LEN/8);
			mout.flush();
			mout.reset();

//			System.out.println("Wrote " + out.getAndReset() + " bytes");
			
//			System.out.println("Reading long[" + (LEN/8) + "]");
			min.readArray(temp2, 0, LEN/8);
			in.reset();
			buf.clear();

//			System.out.println("Rewriting long[" + (LEN/8) + "]");

			mout.writeArray(temp2, 0, LEN/8);
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

//			System.out.println("Wrote " + bytes + " bytes");
			
//			System.out.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp2, 0, LEN/8);
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
//				System.out.println();
//				System.out.println("Read took " + time + " ms");
//				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

			System.out.println("" + round(best_ktp));
			temp2 = null;
			in.reset();
			buf.clear();
			best_time= 1000000;
			/*********************************/

			// Create array
			double [] temp3 = new double[LEN/8];
			
			System.out.println("Writing double[" + (LEN/8) + "]");

			mout.writeArray(temp3, 0, LEN/8);
			mout.flush();
			mout.reset();

//			System.out.println("Wrote " + out.getAndReset() + " bytes");
			
//			System.out.println("Reading double[" + (LEN/8) + "]");
			min.readArray(temp3, 0, LEN/8);
			in.reset();
			buf.clear();

			//		System.out.println("Rewriting double[" + (LEN/8) + "]");

			mout.writeArray(temp3, 0, LEN/8);
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

//			System.out.println("Wrote " + bytes + " bytes");
			
//			System.out.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp3, 0, LEN/8);
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
//				System.out.println();
//				System.out.println("Read took " + time + " ms");
//				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

			System.out.println("" + round(best_ktp));
			temp3 = null;


		} catch (Exception e) {
			System.out.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



