import ibis.ipl.*;

import java.io.IOException;

public class Main {
	static Ibis ibis;
	static Registry registry;

	public static final boolean DEBUG = false;
	public static final int LEN   = 1000;
	public static final int COUNT = 1000;

	public static ReceivePortIdentifier lookup(String name) throws IOException { 
		
		ReceivePortIdentifier temp = null;

		do {
			temp = registry.lookupReceivePort(name);

			if (temp == null) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// ignore
				}
			}
			
		} while (temp == null);
				
		return temp;
	} 

	public static void connect(SendPort s, ReceivePortIdentifier ident) {
		boolean success = false;
		do {
			try {
				s.connect(ident);
				success = true;
			} catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (Exception e2) {
					// ignore
				}
			}
		} while (!success);
	}

	public static void main(String args[]) {
			
		try {	
			long start, end; 

			Data temp = null;

			int len = LEN;
			int count = COUNT;
		
			boolean manta = false;
			int rank = 0, remoteRank = 1;

			for(int i=0; i<args.length; i++) {
				if(args[i].equals("-manta")) {
					manta = true;
				} 
			}
					
			ibis = Ibis.createIbis("ibis", "ibis.impl.tcp.TcpIbis", null);
			registry = ibis.registry();

			StaticProperties s = new StaticProperties();
			if (manta) { 
			    s.add("Serialization", "ibis");
			}
		
			PortType t = ibis.createPortType("test type", s);
			SendPort sport = t.createSendPort();		      
			ReceivePort rport;

			IbisIdentifier master = registry.elect("latency");

			if(master.equals(ibis.identifier())) {
				rank = 0;
				remoteRank = 1;
			} else {
				rank = 1;
				remoteRank = 0;
			}

			if (rank == 0) {
								
				System.err.println("Main starting");
				rport = t.createReceivePort("test port 0");
				rport.enableConnections();
				ReceivePortIdentifier ident = lookup("test port 1");
				connect(sport, ident);

				// Create list
				for (int i=0;i<len;i++) {
					temp = new Data(i, temp);
				}

				System.err.println("Writing list of " + len + " Data objects");

				// Warmup
				for (int i=0;i<count;i++) {
					WriteMessage wm = sport.newMessage();
					wm.writeObject(temp);					
					wm.finish();
					if (DEBUG) { 
						System.err.println("Warmup "+ i);
					}
				}

				ReadMessage rm = rport.receive();
				rm.finish();

				// Real test.
				start = System.currentTimeMillis();
				
				for (int i=0;i<count;i++) {
					WriteMessage wm = sport.newMessage();
					wm.writeObject(temp);					
					wm.finish();
					if (DEBUG) { 
						System.err.println("Test "+ i);
					}
				}

				rm = rport.receive();
				rm.finish();
				
				end = System.currentTimeMillis();
				
				System.err.println("Write took "
						   + (end-start) + " ms.  => "
						   + ((1000.0*(end-start))/count) + " us/call => "
						   + ((1000.0*(end-start))/(count*len)) + " us/object");
				
				System.err.println("Bytes written "
						   + (count*len*Data.OBJECT_SIZE)
						   + " throughput = "
						   + (((1000.0*(count*len*Data.OBJECT_SIZE))/(1024*1024))/(end-start))
						   + " MBytes/s");
							
				sport.close();
				rport.close();
				ibis.end();				
			} else {
				ReceivePortIdentifier ident = lookup("test port 0");
				connect(sport, ident);
				rport = t.createReceivePort("test port 1");
				rport.enableConnections();
				
				for (int j=0;j<2;j++) { 
					for (int i=0;i<count;i++) {
						ReadMessage rm = rport.receive();
						rm.readObject();
						rm.finish();
						if (DEBUG) { 
							System.err.println("Test "+ i);
						}
					}
					
					WriteMessage wm = sport.newMessage();
					wm.finish();
				}
			
				sport.close();
				rport.close();
				ibis.end();	
			}

		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



