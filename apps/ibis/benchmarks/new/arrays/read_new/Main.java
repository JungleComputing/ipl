import ibis.ipl.*;

import java.io.IOException;

public class Main {

	public static final boolean DEBUG = false;

	public static final int LEN   = 1024*1024;
	public static final int COUNT = 100;

	public static final int BOOLEAN_SIZE = 1;
	public static final int BYTE_SIZE    = 1;
	public static final int CHAR_SIZE    = 2;
	public static final int SHORT_SIZE   = 2;
	public static final int INT_SIZE     = 4;
	public static final int LONG_SIZE    = 8;
	public static final int FLOAT_SIZE   = 4;
	public static final int DOUBLE_SIZE  = 8;

	static Ibis ibis;
	static Registry registry;

	public static void test(int rank, int count, int bytes, Object temp, ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		
		if (rank == 0) { 

			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeObject(temp);
				wm.send();
				wm.finish();
				if (DEBUG) { 
					System.err.println("Warmup "+ i);
				}
			}
			
			ReadMessage rm = r.receive();
			rm.finish();
		
			// Real test.
			start = System.currentTimeMillis();
			
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeObject(temp);
				wm.send();
				wm.finish();
				if (DEBUG) { 
					System.err.println("Test "+ i);
				}
			}
			
			if (DEBUG) { 
				System.err.println("Test done");
			}

			rm = r.receive();
			rm.finish();
			
			end = System.currentTimeMillis();
			
			System.err.println("Write took "
					   + (end-start) + " ms.  => "
					   + ((1000.0*(end-start))/count) + " us/call => "
					   + ((1000.0*(end-start))/(count)) + " us/object");
			
			System.err.println("Bytes written "
					   + (count*bytes)
					   + " throughput = "
					   + (((1000.0*(count*bytes))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int j=0;j<2;j++) { 
				for (int i=0;i<count;i++) {
					ReadMessage rm = r.receive();
					rm.readObject();
					rm.finish();
				}
				
				WriteMessage wm = s.newMessage();
				wm.send();
				wm.finish();
			}
		} 
	} 

	public static ReceivePortIdentifier lookup(String name) throws IOException { 
		
		ReceivePortIdentifier temp = null;

		do {
			temp = registry.lookup(name);

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
			boolean manta = false;
			int rank = 0, remoteRank = 1;

			for(int i=0; i<args.length; i++) {
				if(args[i].equals("-manta")) {
					manta = true;
				} 
			}
					
			ibis = Ibis.createIbis("ibis", "ibis.ipl.impl.tcp.TcpIbis", null);
			registry = ibis.registry();

			StaticProperties s = new StaticProperties();
			if (manta) { 
			    s.add("Serialization", "ibis");
			}
		
			PortType t = ibis.createPortType("test type", s);
			SendPort sport = t.createSendPort();		      
			ReceivePort rport;

			IbisIdentifier master = (IbisIdentifier) registry.elect("latency", ibis.identifier());

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
								
				int len = LEN/BOOLEAN_SIZE;		
				boolean [] tempboolean = new boolean[len];				
				System.err.println("boolean[" + len + "]");
				test(0, COUNT, LEN, tempboolean, rport, sport);

				len = LEN/BYTE_SIZE;		
				byte [] tempbyte = new byte[len];				
				System.err.println("byte[" + len + "]");
				test(0, COUNT, LEN, tempbyte, rport, sport);

				len = LEN/CHAR_SIZE;		
				char [] tempchar = new char[len];				
				System.err.println("char[" + len + "]");
				test(0, COUNT, LEN, tempchar, rport, sport);

				len = LEN/SHORT_SIZE;		
				short [] tempshort = new short[len];				
				System.err.println("short[" + len + "]");
				test(0, COUNT, LEN, tempshort, rport, sport);
								
				len = LEN/INT_SIZE;		
				int [] tempint = new int[len];				
				System.err.println("int[" + len + "]");
				test(0, COUNT, LEN, tempint, rport, sport);
								
				len = LEN/LONG_SIZE;		
				long [] templong = new long[len];				
				System.err.println("long[" + len + "]");
				test(0, COUNT, LEN, templong, rport, sport);
								
				len = LEN/FLOAT_SIZE;		
				float [] tempfloat = new float[len];				
				System.err.println("float[" + len + "]");
				test(0, COUNT, LEN, tempfloat, rport, sport);
				
				len = LEN/DOUBLE_SIZE;		
				double [] tempdouble = new double[len];				
				System.err.println("double[" + len + "]");
				test(0, COUNT, LEN, tempdouble, rport, sport);

				sport.free();
				rport.free();
				ibis.end();				
			} else {
				ReceivePortIdentifier ident = lookup("test port 0");
				connect(sport, ident);
				rport = t.createReceivePort("test port 1");
				rport.enableConnections();
			
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
				test(1, COUNT, 0, null, rport, sport);
			
				sport.free();
				rport.free();
				ibis.end();	
			}

		} catch (Exception e) {
			System.err.println("Main got exception " + e);
			e.printStackTrace();
		}
	}
}



