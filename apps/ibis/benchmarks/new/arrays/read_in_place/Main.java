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

	public static void testBoolean(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/BOOLEAN_SIZE;		
		boolean [] temp = null;

		temp = new boolean[len];
		
		if (rank == 0) { 

			System.err.println("boolean[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*BOOLEAN_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*BOOLEAN_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testByte(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/BYTE_SIZE;		
		byte [] temp = null;

		temp = new byte[len];
		
		if (rank == 0) { 

			System.err.println("byte[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*BYTE_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*BYTE_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testChar(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/CHAR_SIZE;		
		char [] temp = null;

		temp = new char[len];
		
		if (rank == 0) { 

			System.err.println("char[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*CHAR_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*CHAR_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testShort(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/SHORT_SIZE;		
		short [] temp = null;

		temp = new short[len];
		
		if (rank == 0) { 

			System.err.println("short[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*SHORT_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*SHORT_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testInt(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/INT_SIZE;		
		int [] temp = null;

		temp = new int[len];
		
		if (rank == 0) { 

			System.err.println("int[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*INT_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*INT_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testLong(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/LONG_SIZE;		
		long [] temp = null;

		temp = new long[len];
		
		if (rank == 0) { 

			System.err.println("long[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*LONG_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*LONG_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testFloat(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/FLOAT_SIZE;		
		float [] temp = null;

		temp = new float[len];
		
		if (rank == 0) { 

			System.err.println("float[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*FLOAT_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*FLOAT_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
			
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

	public static void testDouble(int rank, int count, int bytes,  ReceivePort r, SendPort s) throws Exception { 

		// Create array
		long start, end;
		int len = bytes/DOUBLE_SIZE;		
		double [] temp = null;

		temp = new double[len];
		
		if (rank == 0) { 

			System.err.println("double[" + len + "]");
			
			// Warmup
			for (int i=0;i<count;i++) {
				WriteMessage wm = s.newMessage();
				wm.writeArray(temp);
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
				wm.writeArray(temp);
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
					   + (count*len*DOUBLE_SIZE)
					   + " throughput = "
					   + (((1000.0*(count*len*DOUBLE_SIZE))/(1024*1024))/(end-start))
					   + " MBytes/s");
		} else { 
			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			WriteMessage wm = s.newMessage();
			wm.finish();

			for (int i=0;i<count;i++) {
				ReadMessage rm = r.receive();
				rm.readArray(temp);
				rm.finish();
			}
				
			wm = s.newMessage();
			wm.finish();
			 
		} 
	} 

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
			boolean ibisSer = false;
			int rank = 0, remoteRank = 1;

			for(int i=0; i<args.length; i++) {
				if(args[i].equals("-ibis")) {
					ibisSer = true;
				} 
			}
					
			ibis = Ibis.createIbis("ibis" + Math.random(), "ibis.impl.tcp.TcpIbis", null);
			registry = ibis.registry();

			StaticProperties s = new StaticProperties();
			if (ibisSer) { 
			    s.add("Serialization", "ibis");
			}
		
			PortType t = ibis.createPortType("test type", s);
			SendPort sport = t.createSendPort();		      
			ReceivePort rport;

			IbisIdentifier master = registry.elect("throughput");

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
				
				testBoolean(0, COUNT, LEN, rport, sport);
				testByte(0, COUNT, LEN, rport, sport);
				testChar(0, COUNT, LEN, rport, sport);
				testShort(0, COUNT, LEN, rport, sport);
				testInt(0, COUNT, LEN, rport, sport);
				testLong(0, COUNT, LEN, rport, sport);
				testFloat(0, COUNT, LEN, rport, sport);
				testDouble(0, COUNT, LEN, rport, sport);
				
				sport.close();
				rport.close();
				ibis.end();				
			} else {
				ReceivePortIdentifier ident = lookup("test port 0");
				connect(sport, ident);
				rport = t.createReceivePort("test port 1");
				rport.enableConnections();
			
				testBoolean(1, COUNT, LEN, rport, sport);
				testByte(1, COUNT, LEN, rport, sport);
				testChar(1, COUNT, LEN, rport, sport);
				testShort(1, COUNT, LEN, rport, sport);
				testInt(1, COUNT, LEN, rport, sport);
				testLong(1, COUNT, LEN, rport, sport);
				testFloat(1, COUNT, LEN, rport, sport);
				testDouble(1, COUNT, LEN, rport, sport);
			
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



