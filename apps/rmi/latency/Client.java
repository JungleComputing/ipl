import java.rmi.*;

class Client { 

	public static final int COUNT = 10000;
	
	public static void main(String [] args) {

		if(args.length != 3) {
			System.err.println("usage: java Client <server hostname> <port> <name>");
			System.exit(1);
		}

		int port = 0;
		try {
		    port = Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
		    System.err.println("usage: java Client <server hostname> <port> <name>");
		    System.exit(1);
		}

		doClient(args[0], port, args[2]);
	}

	public static void doClient(String server, int port, String name) {
		try {
			MyServer s = null;
				
			String objname = "//" + server;
			if (port != 0) {
			    objname = objname + ":" + port;
			}
			objname = objname + "/" + name;

			do {
				try { 
					System.err.print(".");
					Thread.sleep(1000);
					s = (MyServer) Naming.lookup(objname);
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
		} catch (Exception e) { 
			System.out.println("OOPS");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} 
	} 
} 
