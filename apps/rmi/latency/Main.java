import ibis.rmi.*;

class Main { 

	public static final int COUNT = 10000;
	
	public static void main(String [] args) { 	

		try {
			DasInfo info = new DasInfo();		
/*			String server = null;
 
			if (args.length > 0) { 
				server = args[0];
			} else { 
				server = info.getHost(1);
			}*/
			
			System.out.println("Starting process " + info.hostNumber() + " on " + info.hostName());

			if (info.hostNumber() == 0) {
				myServer s = null;
				
				do {
					try { 
						Thread.sleep(1000);
						s = (myServer) Naming.lookup("//bimbambom/bla");
					} catch (Exception e) { 
						// ignore.
					} 
				} while (s == null);		    
				
				s.foo();
System.out.println("Called foo");
				s.bar();	
System.out.println("Called bar");

				for (int j=0;j<10;j++) { 

					long start = System.currentTimeMillis();
					
					for (int i=0;i<COUNT;i++) { 
						s.foo();
						// System.out.println(i);
					} 
					
					long end = System.currentTimeMillis();	
					System.out.println("null latency (" + COUNT + ") = " + ((1000.0*(end-start))/(COUNT)) + " usec/call");
				}

/*				for (int j=1;j<10;j++) { 

					long start = System.currentTimeMillis();
					
					for (int i=0;i<j*COUNT;i++) { 
						s.foo();
					} 
					
					long end = System.currentTimeMillis();	
					System.out.println("null latency (" + (COUNT*j) + ") = " + ((1000.0*(end-start))/(j*COUNT)) + " usec/call");
				}*/
				// System.exit(0);

			} else {
				Test t = new Test();
				Naming.bind("//bimbambom/bla", t);
				Thread.sleep(100000);
			} 
		} catch (Exception e) { 
			System.out.println("OOPS");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} 
	} 
} 
