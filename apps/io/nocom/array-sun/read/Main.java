import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int LEN   = 100*1024;
	public static final int COUNT = 1000;
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
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream mout = new ObjectOutputStream(baos);

			mout.reset();
			mout.flush();
			byte [] temp = baos.toByteArray();

			int mark = temp.length;

			// Create array
			int [] temp1 = new int[LEN/4];
			
			System.out.println("Reading int[" + (LEN/4) + "]");

			mout.writeObject(temp1);
			mout.flush();
			mout.reset();

			temp = baos.toByteArray();
			
			bytes = temp.length - mark;

			ByteArrayInputStream bais = new ByteArrayInputStream(temp);
			ObjectInputStream min = new ObjectInputStream(bais);
			bais.mark(bytes);

//			System.out.println("Wrote " + bytes + " bytes");
			
//			System.out.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readObject();
					bais.reset();
					bais.mark(bytes);
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

			// double [] 
			best_ktp = 0.0;
			best_time = 1000000;

			baos = new ByteArrayOutputStream();
			mout = new ObjectOutputStream(baos);

			mout.reset();
			mout.flush();
			temp = baos.toByteArray();

			mark = temp.length;

			// Create array
			double [] temp2 = new double[LEN/8];
			
			System.out.println("Reading double[" + (LEN/8) + "]");

			mout.writeObject(temp2);
			mout.flush();
			mout.reset();

			temp = baos.toByteArray();
			
			bytes = temp.length - mark;

			bais = new ByteArrayInputStream(temp);
			min = new ObjectInputStream(bais);
			bais.mark(bytes);

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readObject();
					bais.reset();
					bais.mark(bytes);
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

			// BYTE [] 
			best_ktp = 0.0;
			best_time = 1000000;

			baos = new ByteArrayOutputStream();
			mout = new ObjectOutputStream(baos);

			mout.reset();
			mout.flush();
			temp = baos.toByteArray();

			mark = temp.length;

			// Create array
			byte [] temp3 = new byte[LEN];
			
			System.out.println("Reading byte[" + (LEN) + "]");

			mout.writeObject(temp3);
			mout.flush();
			mout.reset();

			temp = baos.toByteArray();
			
			bytes = temp.length - mark;

			bais = new ByteArrayInputStream(temp);
			min = new ObjectInputStream(bais);
			bais.mark(bytes);

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readObject();
					bais.reset();
					bais.mark(bytes);
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



