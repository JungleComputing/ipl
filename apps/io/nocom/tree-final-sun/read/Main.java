import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Main {

	public static final boolean DEBUG = false;
	public static final int LEN   = (1024)-1;
	public static final int COUNT = 1000;
	public static final int TESTS = 10;
		
	public static double round(double val) { 		
		return (Math.ceil(val*10.0)/10.0);
	} 

	public static void main(String args[]) {
		
		try {
			DITree tree = null;
			long start, end;
			int bytes;
		
			double best_rtp = 0.0, best_ktp = 0.0;
			long best_time = 1000000;

			System.err.println("Main starting");
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream mout = new ObjectOutputStream(baos);

			mout.reset();
			mout.flush();
			byte [] temp = baos.toByteArray();

			int mark = temp.length;

			// Create tree
			tree = new DITree(LEN);
			
			System.err.println("Writing tree of " + LEN + " DITree objects");

			mout.writeObject(tree);
			mout.flush();
			mout.reset();

			temp = baos.toByteArray();			
			bytes = temp.length - mark;

			ByteArrayInputStream bais = new ByteArrayInputStream(temp);
			ObjectInputStream min = new ObjectInputStream(bais);
			bais.mark(bytes);

			System.err.println("Starting test");

			for (int j=0;j<TESTS;j++) { 

				start = System.currentTimeMillis();
				
				for (int i=0;i<COUNT;i++) {
					min.readObject();
					bais.reset();
					bais.mark(bytes);
				}
				
				end = System.currentTimeMillis();
				
				long time = end-start;
				double rb = COUNT*bytes;
				double kb = COUNT*LEN*DITree.KARMI_SIZE;

				double rtp = ((1000.0*rb)/(1024*1024))/time;
				double ktp = ((1000.0*kb)/(1024*1024))/time;

				System.out.println("Read took " + time + " ms.  => " + ((1000.0*time)/(COUNT*LEN)) + " us/object");
				System.out.println("Karmi bytes read " + kb + " throughput = " + ktp + " MBytes/s");
				System.out.println("Real bytes read " + rb + " throughput = " + rtp + " MBytes/s");

				if (time < best_time) { 
					best_time = time;
					best_rtp = rtp;
					best_ktp = ktp;
				}
			} 

//			System.out.println("Best result : " + best_rtp + " MBytes/sec (" + best_ktp + " MBytes/sec)");
			System.out.println("Total tp : " + round(best_rtp) + " User to :" + round(best_ktp));
		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



