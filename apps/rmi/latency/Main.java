import java.rmi.*;
import ibis.util.PoolInfo;

class Main { 

	public static final int COUNT = 10000;
	
	public static void main(String [] args) { 	

		try {
			PoolInfo info = PoolInfo.createPoolInfo();		
			
			System.out.println("Starting process " + info.rank() + " on " + info.hostName());

			if (info.rank() == 0) {
				myServer s = null;
				
				do {
					try { 
						System.err.print(".");
						Thread.sleep(1000);
						s = (myServer) Naming.lookup("//bimbambom/bla");
					} catch (Exception e) { 
						System.err.println("exception: " + e);
						// ignore.
					} 
				} while (s == null);
				
				for (int j=0;j<10;j++) { 

					long start = System.currentTimeMillis();
					
					for (int i=0;i<COUNT;i++) { 
						s.foo();
					} 
					
					long end = System.currentTimeMillis();	
					System.out.println("null latency (" + COUNT + ") = " + ((1000.0*(end-start))/(COUNT)) + " usec/call");
				}
				s.quit();
				System.exit(0);
			} else {
				System.out.println("creating new test");
				Test t = new Test();
				System.out.println("creating new test done");
				Naming.bind("//bimbambom/bla", t);
				System.out.println("bind done");
				new Thread(t).start();
			} 
		} catch (Exception e) { 
			System.out.println("OOPS");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} 
	} 
} 
