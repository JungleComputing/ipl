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
	public static final int LEN   = 100 * 1024; // 1024*1024;
	public static final int COUNT = 100;
	public static final int TESTS = 10;


	public static double round(double val) { 		
		return (Math.ceil(val*10.0)/10.0);
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
							
			System.out.print("Write obj +cnv byte[" + (LEN) + "]\t");

			byte [] temp = new byte[(LEN)];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeObject(temp);
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

							
			System.out.print("Write obj +cnv int[" + (LEN/4) + "]\t");

			int [] temp1 = new int[(LEN/4)];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeObject(temp1);
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

			System.out.println("Writing byte[" + LEN + "]");

			byte [] temp2 = new byte[LEN];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeObject(temp2);
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
			System.out.println("byte [] : " + round(best_ktp));
			temp2 = null;
			best_time = 1000000;

			/***********************************/

			System.out.print("Write obj +cnv double[" + (LEN/8) + "]\t");

			double [] temp3 = new double[LEN/8];

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					mout.writeObject(temp3);
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



