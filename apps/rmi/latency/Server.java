import java.rmi.*;
import java.rmi.registry.*;

class Server { 

	public static void main(String [] args) {

		if(args.length != 3) {
			System.err.println("usage: java Server <server hostname> <port> <name>");
			System.exit(1);
		}

		int port = 0;
		try {
		    port = Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
		    System.err.println("usage: java Server <server hostname> <port> <name>");
		    System.exit(1);
		}

		doServer(args[0], port, args[2]);
	}

	public static void doServer(String server, int port, String name) {
		try {
			String objname = "//" + server;
			if (port != 0) {
			    Registry reg = LocateRegistry.createRegistry(port);
			    objname = objname + ":" + port;
			}
			objname = objname + "/" + name;

			System.out.println("creating new test");
				Test t = new Test();
				System.out.println("creating new test done");
				Naming.bind(objname, t);
				System.out.println("bind done");
				new Thread(t).start();
		} catch (Exception e) { 
			System.out.println("OOPS");
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} 
	} 
} 
