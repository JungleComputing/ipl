import ibis.ipl.*;
import ibis.util.PoolInfo;

import java.io.IOException;

final class Receiver implements Upcall { 

	SendPort sport;
	ReceivePort rport;
	int count = 0;
	int max;
	boolean one;
	
	Receiver(ReceivePort rport, SendPort sport, int max, boolean one) {
		this.rport = rport;
		this.sport = sport;
		this.max = max;
		this.one = one;
	} 
	
	public void upcall(ReadMessage readMessage) { 

		try { 
			readMessage.readObject();
			readMessage.finish();
			
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();

			count++;

			if (count == max) { 
				synchronized (this) { 
					notifyAll();
				}
			}			
		} catch (Exception e) { 			
			System.out.println("EEEEEK " + e);
			e.printStackTrace();
		} 
	} 

	synchronized void finish() { 
		while (count < max) { 
			try { 
				wait();
			} catch (Exception e) { 
			} 
		}		
	} 

	public void receive() throws Exception { 
		if (one) {
			for(int i = 0; i< max; i++) {
				ReadMessage readMessage = rport.receive();
				readMessage.readObject();
				readMessage.finish();
				
				WriteMessage writeMessage = sport.newMessage();
				writeMessage.send();
				writeMessage.finish();
			}	
		} else {
			for(int i = 0; i< max; i++) {
				ReadMessage readMessage = rport.receive();
				Object temp = readMessage.readObject();
				readMessage.finish();
				
				WriteMessage writeMessage = sport.newMessage();
				writeMessage.writeObject(temp);
				writeMessage.send();
				writeMessage.finish();
			}	
		}
	} 
} 

final class Main { 

	public static boolean verbose = false;
	public static final double MB = (1024.0*1024.0);

	static Ibis ibis;
	static Registry registry;

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

	private static long one_way(ReceivePort rport, SendPort sport, int count, Object data) throws Exception { 

		long start = System.currentTimeMillis();
		
		for (int i=0;i<count;i++) { 
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeObject(data);
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = rport.receive();
			readMessage.finish();
		} 
		
		long end = System.currentTimeMillis();			

		if (verbose) System.out.println("One way test took " + (end-start) + " milliseconds");

		return (end-start);
	} 

	private static long two_way(ReceivePort rport, SendPort sport, int count, Object data) throws Exception { 

		long start = System.currentTimeMillis();
		
		for (int i=0;i<count;i++) { 
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeObject(data);
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = rport.receive();
			readMessage.readObject();
			readMessage.finish();
		} 
		
		long end = System.currentTimeMillis();	

		if (verbose) System.out.println("Two way test took " + (end-start) + " milliseconds");
		return (end-start);
	} 

	private static long runTest(ReceivePort rport, SendPort sport, 
				    int count, int retries, Object data, boolean one_way) throws Exception { 

		long best = 1000000;
		long temp;

		for (int i=0;i<retries;i++) { 
			if (one_way) { 
				temp = one_way(rport, sport, count, data);
			} else { 
				temp = two_way(rport, sport, count, data);
			} 

			if (temp < best) { 
				best = temp;
			} 
		} 

		if (verbose) System.out.println("Best time : " + best + " milliseconds");

		return best;
	}

	private static double round(double v) { 
		return (Math.ceil(v*100.0)/100.0);
	} 

	private static double tp(int size, long time) { 		
		return round((1000.0*size / MB) / time);
	} 
	
	public static void main(String [] args) { 	

		try { 
			boolean array = false, tree = false, list = false, dlist = false, oarray = false, one_way = true;
			PoolInfo info = new PoolInfo();		
			int i           = 0;
			int len         = 1023;
			int arraysize   = 16*1024;
			int count       = 10000;
			int retries     = 10;
			boolean upcalls = false;
			int rank = info.rank(); 
			int tests = 0;

			while (i < args.length) { 
				if (false) {
				} else if (args[i].equals("-upcalls")) { 
					upcalls = true;
					i++; 
				} else if (args[i].equals("-array")) { 
					array = true;
					i++;
					tests += 3;
				} else if (args[i].equals("-objectarray")) { 
					oarray = true;
					i++;
					tests++;
				} else if (args[i].equals("-list")) { 
					list = true;
					i++;
					tests++;
				} else if (args[i].equals("-dlist")) { 
					dlist = true;
					i++;
					tests++;
				} else if (args[i].equals("-tree")) { 
					tree = true;
					i++;
					tests++;
				} else if (args[i].equals("-twoway")) { 
					one_way = false;
					i++;
				} else if (args[i].equals("-verbose")) { 
					verbose = true;
					i++;
				} else if (args[i].equals("-len")) { 
					len = Integer.parseInt(args[i+1]);
					i += 2;
				} else if (args[i].equals("-arraysize")) { 
					arraysize = Integer.parseInt(args[i+1]);
					i += 2;
				} else if (args[i].equals("-count")) { 
					count = Integer.parseInt(args[i+1]);
					i += 2;
				} else if (args[i].equals("-retries")) { 
					retries = Integer.parseInt(args[i+1]);
					i += 2;
				} else {
					System.err.println("unknown option: " + args[i]);
					System.exit(1);
				}
			} 

			if (verbose) { 
				System.out.println("Creating ibis ...");
			}

			ibis = Ibis.createIbis(null, null);

			if (verbose) { 
				System.out.println("Ibis created; getting registry ...");
			}

			registry = ibis.registry();

			if (verbose) { 
				System.out.println("Got registry");
			}

			StaticProperties s = new StaticProperties();

			PortType t = ibis.createPortType("test type", s);			
			SendPort sport = t.createSendPort();					      
			ReceivePort rport;

			if (verbose) { 
				System.out.println("Got sendport");
			}

			if (rank == 0) {
				rport = t.createReceivePort("test port 0");
				rport.enableConnections();
				ReceivePortIdentifier ident = lookup("test port 1");
				connect(sport, ident);
			
				Object data;
				long time;

				if (verbose) { 
					System.out.println("client ok");
				}

				if (array) { 	
					data = new byte[arraysize];								
					if (verbose) { 
						System.out.println("starting byte[" + arraysize + "] test");
					} 

					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("byte[" + arraysize + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");

			
					int alen = arraysize/4;
					data = new int[alen];								
					if (verbose) { 
						System.out.println("starting int[" + alen + "] test");
					} 

					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("int[" + alen + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");

					alen = arraysize/8;
					data = new double[alen];				

					if (verbose) { 
						System.out.println("starting double[" + alen + "] test");
					} 
				
					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("double[" + alen + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");
				} 

				if (list) { 
					data = new List(len);					

					if (verbose) { 
						System.out.println("starting list(" + len + ") test");
					} 

					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("list(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*List.PAYLOAD, time) + " MBytes/sec.");
				} 

				if (dlist) { 
					data = new DList(len);				

					if (verbose) { 
						System.out.println("starting dlist(" + len + ") test");
					} 

					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("dlist(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*DList.PAYLOAD, time) + " MBytes/sec.");
				} 

				if (tree) {  
					data = new Tree(len);					

					if (verbose) { 
						System.out.println("starting tree(" + len + ") test");
					} 

					time = runTest(rport, sport, count, retries, data, one_way);
					System.out.println("tree(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*Tree.PAYLOAD, time) + " MBytes/sec.");
				} 

				if (oarray) {  
					data = null;
					Data [] temp = new Data[len];
					for (int j=0;j<len;j++) { 
						temp[j] = new Data();
					} 

					if (verbose) { 
						System.out.println("starting object array[" + len + "] test");
					}
					
					time = runTest(rport, sport, count, retries, temp, one_way);
					System.out.println("object[" + len + "] = " + tp((one_way ? len*count : 2*len*count)*Data.PAYLOAD, time) + " MBytes/sec.");
				} 

				if (verbose) System.out.println("Done");				

			} else { 

				ReceivePortIdentifier ident = lookup("test port 0");
				connect(sport, ident);

				if (upcalls) {
					Receiver receiver = new Receiver(null, sport, tests*retries*count, one_way);
					rport = t.createReceivePort("test port 1", receiver);
					rport.enableConnections();
					rport.enableUpcalls();
					receiver.finish();
				} else { 
					rport = t.createReceivePort("test port 1");
					rport.enableConnections();
					Receiver receiver = new Receiver(rport, sport, tests*retries*count, one_way);
					receiver.receive();
				}
			}

			/* free the send ports first */
                        sport.free();
                        rport.free();
			ibis.end();
		} catch (Exception e) { 
			System.out.println("OOPS " + e);
			e.printStackTrace();
		} 
	} 
}
