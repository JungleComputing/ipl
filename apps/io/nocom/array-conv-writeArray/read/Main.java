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

			System.err.println("Main starting");
			
			StoreBuffer buf = new StoreBuffer();
			StoreOutputStream out = new StoreOutputStream(buf);
			StoreInputStream in = new StoreInputStream(buf);

			BufferedArrayOutputStream baos = new BufferedArrayOutputStream(out);
			BufferedArrayInputStream bais = new BufferedArrayInputStream(in);

			MantaOutputStream mout = new MantaOutputStream(baos);
			MantaInputStream min = new MantaInputStream(bais);
				
			// Create array
			int [] temp1 = new int[LEN/4];
			
			System.err.println("Writing int[" + (LEN/4) + "]");

			mout.writeArray(temp1, 0, (LEN/4));
			mout.flush();
			mout.reset();

			System.err.println("Wrote " + out.getAndReset() + " bytes");
			
			System.err.println("Reading int[" + (LEN/4) + "]");
			min.readArray(temp1, 0, (LEN/4));
			in.reset();
			buf.clear();

			System.err.println("Rewriting int[" + (LEN/4) + "]");

			mout.writeArray(temp1, 0, (LEN/4));
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

			System.err.println("Wrote " + bytes + " bytes");
			
			System.err.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp1, 0, (LEN/4));
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
				System.out.println();
				System.out.println("Read took " + time + " ms");
				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

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
			
			System.err.println("Writing long[" + (LEN/8) + "]");

			mout.writeArray(temp2, 0, (LEN/8));
			mout.flush();
			mout.reset();

			System.err.println("Wrote " + out.getAndReset() + " bytes");
			
			System.err.println("Reading long[" + (LEN/8) + "]");
			min.readArray(temp2, 0, (LEN/8));
			in.reset();
			buf.clear();

			System.err.println("Rewriting long[" + (LEN/8) + "]");

			mout.writeArray(temp2, 0, (LEN/8));
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

			System.err.println("Wrote " + bytes + " bytes");
			
			System.err.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp2, 0, (LEN/8));
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
				System.out.println();
				System.out.println("Read took " + time + " ms");
				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

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
			
			System.err.println("Writing double[" + (LEN/8) + "]");

			mout.writeArray(temp3, 0, (LEN/8));
			mout.flush();
			mout.reset();

			System.err.println("Wrote " + out.getAndReset() + " bytes");
			
			System.err.println("Reading double[" + (LEN/8) + "]");
			min.readArray(temp3, 0, (LEN/8));
			in.reset();
			buf.clear();

			System.err.println("Rewriting double[" + (LEN/8) + "]");

			mout.writeArray(temp3, 0, (LEN/8));
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

			System.err.println("Wrote " + bytes + " bytes");
			
			System.err.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readArray(temp3, 0, (LEN/8));
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;
				
				System.out.println();
				System.out.println("Read took " + time + " ms");
				System.out.println("Bytes read " + kb + " throughput = " + ktp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

			System.out.println("" + round(best_ktp));
			temp3 = null;


		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



