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
	public static final int LEN   = 1024*128-1;
	public static final int COUNT = 100;
	public static final int TESTS = 3;

	public static double round(double val) { 		
		return (Math.ceil(val*100.0)/100.0);
	} 

	public static void main(String args[]) {

		int len = LEN;
		int count = COUNT;

		int params = 0;
		for (int i = 0; i < args.length; i++) {
		    if (false) {
		    } else if (params == 0) {
			count = Integer.parseInt(args[i]);
			params++;
		    } else if (params == 1) {
			len = Integer.parseInt(args[i]);
			params++;
		    } else {
			System.err.println("Main {iterations {tree size}}");
			System.exit(33);
		    }
		}

		try {
			DITree temp = null;
			long start, end;
			long bytes;

			double best_rtp = 0.0, best_ktp = 0.0;
			long best_time = 1000000;

			System.err.println("Main starting");

			NullArrayOutputStream naos = new NullArrayOutputStream();
			IbisSerializationOutputStream mout = new IbisSerializationOutputStream(naos);
				
			// Create tree
			temp = new DITree(len);
			
			System.err.println("Writing tree of " + len + " DITree objects");
			// System.err.println("Writing tree " + temp);

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<count;i++) {
					mout.writeObject(temp);
					mout.flush();
					mout.reset();
//					System.gc();
				}
				end = System.currentTimeMillis();

				bytes = naos.getAndReset();
				
				long time = end-start;
				double rb = bytes;
				double kb = count*len*DITree.KARMI_SIZE;

				double rtp = ((1000.0*rb)/(1024*1024))/time;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				System.out.println("Write took " + time + " ms.  => " + ((1000.0*time)/(count*len)) + " us/object");
				System.out.println("Karmi bytes written " + kb + " throughput = " + ktp + " MBytes/s");
				System.out.println("Real bytes written " + rb + " throughput = " + rtp + " MBytes/s");
				mout.statistics();

				if (time < best_time) { 
					best_time = time;
					best_rtp = rtp;
					best_ktp = ktp;
				}
			} 

			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("" + round(best_rtp) + " " + round(best_ktp));

			mout.statistics();

		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



