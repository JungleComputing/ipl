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
	public static final int LEN   = 511;
	public static final int COUNT = 100000;
	public static final int TESTS = 10;

	public static double round(double val) { 		
		return (Math.ceil(val*100.0)/100.0);
	} 

	public static void main(String args[]) {
		
		try {
			List temp = null;
			long start, end;
			int bytes;

			double best_rtp = 0.0, best_ktp = 0.0;
			long best_time = 1000000;

			System.err.println("Main starting");

			NullArrayOutputStream naos = new NullArrayOutputStream();
			IbisSerializationOutputStream mout = new IbisSerializationOutputStream(naos);
				
			// Create tree
			temp = new List(LEN);
			
			System.err.println("Writing list of " + LEN + " objects");

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
				double rb = bytes;
				double kb = COUNT*LEN*List.KARMI_SIZE;

				double rtp = ((1000.0*rb)/(1024*1024))/time;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				System.out.println("Write took " + time + " ms.  => " + ((1000.0*time)/(COUNT*LEN)) + " us/object");
				System.out.println("Karmi bytes written " + kb + " throughput = " + ktp + " MBytes/s");
				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_rtp = rtp;
					best_ktp = ktp;
				}
			} 

			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_rtp) + " " + round(best_ktp));
		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



