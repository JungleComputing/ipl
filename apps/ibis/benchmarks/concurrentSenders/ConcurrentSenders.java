import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

interface Config {
	static final boolean DEBUG = false;
}


class Sender extends Thread implements Config { 
	int count, repeat;
	boolean ibisSer;
	Ibis ibis;
	PortType t;

	Sender(Ibis ibis, PortType t, int count, int repeat, boolean ibisSer) {
		this.ibis = ibis;
		this.t = t;
		this.count = count;
		this.repeat = repeat;
		this.ibisSer = ibisSer;
	} 
	
	public void run() {
		try {
			SendPort sport = t.createSendPort("send port");
			ReceivePort rport;

			ReceivePortIdentifier ident = ibis.registry().lookup("receive port");
			sport.connect(ident);
		
			for (int r=0;r<repeat;r++) { 

				long time = System.currentTimeMillis();

				for(int i = 0; i< count; i++) {
					WriteMessage writeMessage = sport.newMessage();
					if(DEBUG) {
						System.out.println("LAT: send message");
					}
					writeMessage.writeObject(new String("total world domination"));

					writeMessage.send();
					if(DEBUG) {
						System.out.println("LAT: finish message");
					}
					writeMessage.finish();
					if(DEBUG) {
						System.out.println("LAT: message done");
					}
				}

				time = System.currentTimeMillis() - time;

				double speed = (time * 1000.0) / (double)count;
				System.err.println("SENDER: " + count + " msgs took " + (time/1000.0) + 
						   " seconds, time/msg = " + speed + " micros");
			}

			System.err.println("sender done, freeing sport");
			sport.free();
			System.err.println("sender done, terminating ibis");
			ibis.end();
			System.err.println("sender done, exit");
		} catch (Exception e) {
			System.err.println("got exception: " + e);
			e.printStackTrace();
		}
	}
}


class Receiver implements Upcall { 
	int count;
	int repeat;
	boolean ibisSer;
	boolean done = false;
	boolean doFinish;
	Ibis ibis;
	PortType t;
	int msgs = 0;
	int senders;

	Receiver(Ibis ibis, PortType t, int count, int repeat, int senders, boolean ibisSer, boolean doFinish) {
		this.ibis = ibis;
		this.t = t;
		this.count = count;
		this.repeat = repeat;
		this.senders = senders;
		this.ibisSer = ibisSer;
		this.doFinish = doFinish;

		try {
			ReceivePort rport = t.createReceivePort("receive port", this);
			rport.enableConnections();

			long time = System.currentTimeMillis();
			rport.enableUpcalls();
			finish();
			time = System.currentTimeMillis() - time;
			double speed = (time * 1000.0) / (double)count;
			System.err.println("RECEIVEVER: " + count + " msgs took " 
					   + (time/1000.0) + " seconds");

			System.err.println("receiver done, freeing rport");
			rport.free();
			System.err.println("receiver done, terminating ibis");
			ibis.end();
			System.err.println("receiver exit");
		} catch (Exception e) {
			System.err.println("got exception: " + e);
			e.printStackTrace();
		}
	}
	
	public void upcall(ReadMessage readMessage) { 
		//		System.err.println("Got readMessage!!");

		try {
			readMessage.readObject();

			if(doFinish) {
				readMessage.finish();
			}

			synchronized(this) {
				msgs++;
				if(msgs == count * repeat * senders) {
					done = true;
					notifyAll();
				}
			}
		} catch (Exception e) { 			
			System.err.println("EEEEEK " + e);
			e.printStackTrace();
		} 
	} 

	synchronized void finish() { 
		while (!done) { 
			try { 
				wait();
			} catch (Exception e) { 
			} 
		}		

		System.err.println("Finished Receiver");
	} 
} 

class ConcurrentSenders implements Config { 

	static Ibis ibis;
	static Registry registry;

	static void usage() {
		System.out.println("Usage: ConcurrentReceives [-ibis] [-panda]");
		System.exit(0);
	}

	public static void main(String [] args) { 
		boolean panda = false;
		boolean ibisSer = false;
		boolean doFinish = false;
		int count = 10000;
		int repeat = 10;
		int rank = 0;
		int senders = 2;
		Random r = new Random();

		/* Parse commandline parameters. */
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-panda")) {
				panda = true;
			} else if(args[i].equals("-ibis")) {
				ibisSer = true;
			} else if(args[i].equals("-finish")) {
				doFinish = true;
			} else {
				usage();
			}
		}	

		try {
			if(!panda) {
				ibis = Ibis.createIbis("ibis:" + r.nextInt(), "ibis.impl.tcp.TcpIbis", null);
			} else {
				ibis = Ibis.createIbis("ibis:" + r.nextInt(), "ibis.impl.messagePassing.PandaIbis", null);
			}

			registry = ibis.registry();

			if(DEBUG) {
				System.out.println("LAT: pre elect");
			}
			IbisIdentifier master = (IbisIdentifier) registry.elect("latency", ibis.identifier());
			if(DEBUG) {
				System.out.println("LAT: post elect");
			}

			if(master.equals(ibis.identifier())) {
				if(DEBUG) {
					System.out.println("LAT: I am master");
				}
				rank = 0;
			} else {
				if(DEBUG) {
					System.out.println("LAT: I am slave");
				}
				rank = 1;
			}

			StaticProperties s = new StaticProperties();
			if (ibisSer) { 
				s.add("Serialization", "ibis");
			}
			PortType t = ibis.createPortType("test type", s);

			if (rank == 0) { 
				new Receiver(ibis, t, count, repeat, senders, ibisSer, doFinish);
			} else { 
				// start N senders
				for (int i=0; i<senders; i++) {
					new Sender(ibis, t, count, repeat, ibisSer).start();
				}
			}

		} catch (Exception e) { 
			System.err.println("Got exception " + e);
			System.err.println("StackTrace:");
			e.printStackTrace();
		}
	}
} 
