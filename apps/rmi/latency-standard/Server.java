import java.rmi.*;
import java.rmi.registry.*;

class Server { 

	public static final int COUNT = 10000;
	
	public static void main(String [] args) {

		if(args.length != 1) {
			System.err.println("usage: java Server <server hostname>");
			System.exit(1);
		}

		String server = args[0];

		try {
			Registry reg = LocateRegistry.createRegistry(9911);

			System.out.println("creating new test");
				Test t = new Test();
				System.out.println("creating new test done");
				Naming.bind("//" + server + ":9911/bla", t);
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
