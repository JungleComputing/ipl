import java.io.InputStream;
import java.io.OutputStream;

import ibis.io.ArrayInputStream;
import ibis.io.ArrayOutputStream;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;
//import ibis.io.IbisSerializationTypedBufferOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int COUNT = 10000;
	public static final int TESTS = 10;


	public static double round(double val) { 		
		return (Math.ceil(val*100.0)/100.0);
	} 

	public static void main(String args[]) {
		
		try {
			int bytes;
			long start, end;
			double best_ktp = 0.0;
			long best_time = 1000000;

			System.out.println("Main starting");

			NullOutputStream naos = new NullOutputStream();
			BufferedArrayOutputStream baos = new BufferedArrayOutputStream(naos);
			IbisSerializationOutputStream mout = new IbisSerializationOutputStream(baos);
							
			System.out.println("Writing int[" + (4*1023) + "]");

			int [] temp1 = new int[4*1023];

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
				double kb = COUNT*(4*1023*4);
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				System.out.println("Write took " + time + " ms");
//				System.out.println("Payload bytes written " + kb + " throughput = " + ktp + " MBytes/s");
				System.out.println("Real bytes written per write : " + (bytes/COUNT));

				if (time < best_time) { 
					best_time = time;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("int [] : " + round(best_ktp));
			temp1 = null;

		} catch (Exception e) {
			System.out.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



