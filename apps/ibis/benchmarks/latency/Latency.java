import ibis.ipl.*;

import java.util.Properties;

import java.util.Random;

class Sender { 

	SendPort sport;
	ReceivePort rport;

	Sender(ReceivePort rport, SendPort sport) {
		this.rport = rport;
		this.sport = sport;
	} 
	
	void send(int count) throws IbisIOException {
		// warmup
		for(int i = 0; i< count; i++) {
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = rport.receive();
			readMessage.finish();
		}

		// test
		long time = System.currentTimeMillis();

		for(int i = 0; i< count; i++) {
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = rport.receive();
			readMessage.finish();
		}

		time = System.currentTimeMillis() - time;

		double speed = (time * 1000.0) / (double)count;
		System.err.println("Latency: " + count + " calls took " + (time/1000.0) + " seconds, time/call = " + speed + " micros");
	}
} 

class ExplicitReceiver { 

	SendPort sport;
	ReceivePort rport;

	ExplicitReceiver(ReceivePort rport, SendPort sport) {
		this.rport = rport;
		this.sport = sport;
	} 

	void receive(int count) throws IbisIOException { 		
		for(int i = 0; i< count; i++) {
			ReadMessage readMessage = rport.receive();
			readMessage.finish();
			
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();
		}
	} 
} 

class UpcallReceiver implements Upcall { 

	SendPort sport;

	int count = 0;
	int max;

	UpcallReceiver(SendPort sport, int max) {
		this.sport = sport;
		this.max = max;
	} 
	
	public void upcall(ReadMessage readMessage) { 

		//		System.out.println("Got readMessage!!");

		try { 
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
//				System.err.println("Jikes");
				wait();
			} catch (Exception e) { 
			} 
		}		

		System.err.println("Finished Receiver");

	} 
} 

class UpcallSender implements Upcall { 

	SendPort sport;

	int count, max;
	long time;

	UpcallSender(SendPort sport, int count) {
		this.sport = sport;
		this.count = 0;
		this.max   = count;
	} 

	public void start() { 
		try { 
			System.err.println("Starting " + count);
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();
		} catch (Exception e) { 			
			System.out.println("EEEEEK " + e);
			e.printStackTrace();
		}
	} 
	
	public void upcall(ReadMessage readMessage) { 

		try { 
			readMessage.finish();

			count++;

//			System.err.println("Sending " + count);
			
			if (count == max) { 
				time = System.currentTimeMillis();
			}
			
			if (count == 2*max) { 
				time = System.currentTimeMillis() - time;
				double speed = (time * 1000.0) / (double)max;
				System.err.println("Latency: " + max + " calls took " + (time/1000.0) + " seconds, time/call = " + speed + " micros");
				synchronized (this) { 
					notifyAll();
				}
				return;
			} 
			
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.send();
			writeMessage.finish();

		} catch (Exception e) { 			
			System.out.println("EEEEEK " + e);
			e.printStackTrace();
		}

	}

	synchronized void finish() { 
		while (count < 2*max) { 
			try { 
//				System.err.println("EEK");
				wait();
			} catch (Exception e) { 
			} 
		}		

		System.err.println("Finished Sender " + count + " " + max);
	} 
} 

class Latency { 

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

	public static ReceivePortIdentifier lookup(String name) throws IbisIOException { 
		
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

	static void usage() {
		System.out.println("Usage: Latency [-u] [-uu] [-manta] [-panda] [count]");
		System.exit(0);
	}

	public static void main(String [] args) { 
		boolean upcalls = false;
		boolean upcallsend = false;
		boolean panda = false;
		boolean manta = false;
		int count = -1;
		int rank = 0, remoteRank = 1;
		Random r = new Random();

		/* Parse commandline parameters. */
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-u")) { 
				upcalls = true;
			} else if(args[i].equals("-uu")) { 
				upcalls = true;
				upcallsend = true;
			} else if(args[i].equals("-panda")) {
				panda = true;
			} else if(args[i].equals("-manta")) {
				manta = true;
			} else {
				if(count == -1) {
					count = Integer.parseInt(args[i]);
				} else {
					usage();
				}
			}
		}

		if(count == -1) {
			count = 1000;
		}

		try {
			if(!panda) {
				ibis = Ibis.createIbis("ibis:" + r.nextInt(), "ibis.ipl.impl.tcp.TcpIbis", null);
			} else {
				ibis = Ibis.createIbis("ibis:" + r.nextInt(), "ibis.ipl.impl.messagePassing.panda.PandaIbis", null);
			}

			registry = ibis.registry();

			StaticProperties s = new StaticProperties();
			if (manta) { 
			    s.add("Serialization", "manta");
			}
			PortType t = ibis.createPortType("test type", s);

			SendPort sport = t.createSendPort();					      
			ReceivePort rport;
			Latency lat = null;

			IbisIdentifier master = (IbisIdentifier) registry.elect("latency", ibis.identifier());

			if(master.equals(ibis.identifier())) {
				rank = 0;
				remoteRank = 1;
			} else {
				rank = 1;
				remoteRank = 0;
			}

			if (rank == 0) { 
				if (!upcallsend) { 
					rport = t.createReceivePort("test port 0");
					rport.enableConnections();
					ReceivePortIdentifier ident = lookup("test port 1");
					connect(sport, ident);
					Sender sender = new Sender(rport, sport);
					sender.send(count);
				} else {
					UpcallSender sender = new UpcallSender(sport, count);
					rport = t.createReceivePort("test port 0", sender);
					rport.enableConnections();

					ReceivePortIdentifier ident = lookup("test port 1");
					connect(sport, ident);

					rport.enableUpcalls();

					sender.start();
					sender.finish();
				}
			} else { 
				ReceivePortIdentifier ident = lookup("test port 0");
				connect(sport, ident);

				if (upcalls) {
					UpcallReceiver receiver = new UpcallReceiver(sport, 2*count);
					rport = t.createReceivePort("test port 1", receiver);
					rport.enableConnections();
					rport.enableUpcalls();
					receiver.finish();
				} else { 
					rport = t.createReceivePort("test port 1");
					rport.enableConnections();
					ExplicitReceiver receiver = new ExplicitReceiver(rport, sport);
					receiver.receive(2*count);
				}
			}

			/* free the send ports first */
                        sport.free();
                        rport.free();
			ibis.end();

		} catch (IbisIOException e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	}
} 

