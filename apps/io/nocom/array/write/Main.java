import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int LEN   = 100*1024;
	public static final int COUNT = 100;
	public static final int TESTS = 100;


	public static double round(double val) { 		
		return (Math.ceil(val*10.0)/10.0);
	} 

	public static void main(String args[]) {
		
		try {
			int bytes;
			long start, end;
			double best_ktp = 0.0;
			long best_time = 1000000;

			System.out.println("Main starting");

			NullArrayOutputStream naos = new NullArrayOutputStream();
			IbisSerializationOutputStream mout = new IbisSerializationOutputStream(naos);

			
			/*********************/

			System.out.print("Writing byte[" + LEN + "]\t");

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
			System.out.println("" + round(best_ktp));
			temp2 = null;
			best_time = 1000000;

			/***********************************/

			System.out.print("Writing int[" + (LEN/4) + "]\t");

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

			/*********************************/

			System.out.print("Writing double[" + (LEN/8) + "]\t");

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
