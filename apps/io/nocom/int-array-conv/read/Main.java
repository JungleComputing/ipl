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
	public static final int COUNT = 10000;
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
			StoreOutputStream out = new StoreOutputStream(buf);
			StoreInputStream in = new StoreInputStream(buf);

			BufferedArrayOutputStream baos = new BufferedArrayOutputStream(out);
			BufferedArrayInputStream bais = new BufferedArrayInputStream(in);

			MantaOutputStream mout = new MantaOutputStream(baos);
			MantaInputStream min = new MantaInputStream(bais);
				
			// Create array
			int [] temp1 = new int[4*1023];
			
			System.out.println("Reading int[" + (4*1023) + "]");

			mout.writeObject(temp1);
			mout.flush();
			mout.reset();

//			System.out.println("Wrote " + out.getAndReset() + " bytes");
			
//			System.out.println("Reading int[" + (LEN/4) + "]");
			min.readObject();
			in.reset();
			buf.clear();

//			System.out.println("Rewriting int[" + (LEN/4) + "]");

			mout.writeObject(temp1);
			mout.flush();
			mout.reset();

			bytes = out.getAndReset();

//			System.out.println("Wrote " + bytes + " bytes");
			
//			System.out.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readObject();
					in.reset();
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double kb = COUNT*(4*1023*4);
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
			
		} catch (Exception e) {
			System.out.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



