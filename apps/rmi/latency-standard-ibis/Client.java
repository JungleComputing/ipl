import ibis.rmi.*;

class Client { 

	public static final int COUNT = 10000;
	
	public static void main(String [] args) {

		if(args.length != 1) {
			System.err.println("usage: java Client <server hostname>");
			System.exit(1);
		}

		String server = args[0];

		try {
			MyServer s = null;
				
			do {
				try { 
					System.err.print(".");
					Thread.sleep(1000);
					s = (MyServer) Naming.lookup("//" + server + ":9911/bla");
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
		} catch (Exception e) { 
			System.out.println("OOPS");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} 
	} 
} 
