import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.ArrayInputStream;
import ibis.io.ArrayOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
//import ibis.io.MantaTypedBufferOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int LEN   = 16*1024;
	public static final int COUNT = 100;
	public static final int TESTS = 100;


	public static double round(double val) { 		
		return (Math.ceil(val*100.0)/100.0);
	} 

	public static void main(String args[]) {
		
		try {
			int bytes;
			long start, end;
			double best_ktp = 0.0;
			long best_time = 1000000;

			// System.out.println("Main starting");

			NullOutputStream naos = new NullOutputStream();
			BufferedArrayOutputStream baos = new BufferedArrayOutputStream(naos);
			MantaOutputStream mout = new MantaOutputStream(baos);
							
			System.out.print("Write arr +cnv byte[" + (LEN) + "]\t");

			byte [] temp = new byte[(LEN)];

			for (int x=0;x<(LEN);x++) { 
				temp[x] = (byte)x;
			} 

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeArray(temp, 0, LEN);
					mout.flush();
					mout.reset();
				}
				
				end = System.currentTimeMillis();

				bytes = naos.getAndReset();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				// System.out.println("Write took " + time + " ms");
//				System.out.println("Payload bytes written " + kb + " throughput = " + ktp + " MBytes/s");
//				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_ktp));
			temp = null;
			best_time = 1000000;
			
			/*********************/

							
			System.out.print("Write arr +cnv int[" + (LEN/4) + "]\t");

			int [] temp1 = new int[(LEN/4)];

			for (int x=0;x<(LEN/4);x++) { 
				temp1[x] = x;
			} 

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeArray(temp1, 0, LEN/4);
					mout.flush();
					mout.reset();
				}
				
				end = System.currentTimeMillis();

				bytes = naos.getAndReset();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				// System.out.println("Write took " + time + " ms");
//				System.out.println("Payload bytes written " + kb + " throughput = " + ktp + " MBytes/s");
//				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_ktp));
			temp1 = null;
			best_time = 1000000;
			
			/*********************/

			System.out.print("Write arr +cnv long[" + (LEN/8) + "]\t");

			long [] temp2 = new long[(LEN/8)];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeArray(temp2, 0, LEN/8);
					mout.flush();
					mout.reset();
				}
				
				end = System.currentTimeMillis();

				bytes = naos.getAndReset();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				// System.out.println("Write took " + time + " ms");
//				System.out.println("Write took " + time + " ms.  => " + ((1000.0*time)/(COUNT*LEN)) + " us/object");
//				System.out.println("Payload bytes written " + kb + " throughput = " + ktp + " MBytes/s");
//				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_ktp));
			temp2 = null;
			best_time = 1000000;

			/***********************************/

			System.out.print("Write arr +cnv double[" + (LEN/8) + "]\t");

			double [] temp3 = new double[LEN/8];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeArray(temp3, 0, LEN/8);
					mout.flush();
					mout.reset();
				}
				
				end = System.currentTimeMillis();

				bytes = naos.getAndReset();
				
				long time = end-start;
				double kb = COUNT*LEN;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				// System.out.println("Write took " + time + " ms");
//				System.out.println("Write took " + time + " ms.  => " + ((1000.0*time)/(COUNT*LEN)) + " us/object");
//				System.out.println("Payload bytes written " + kb + " throughput = " + ktp + " MBytes/s");
//				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_ktp));


		} catch (Exception e) {
			System.out.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



