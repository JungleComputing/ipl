import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

class Throughput extends Thread { 

	int count = 1000;
	int transferSize = 0;
	int rank;
	int remoteRank;

	int windowSize = Integer.MAX_VALUE;

	ReceivePort rport;
	SendPort sport;

	byte[] data;

	public static void main(String [] args) { 
		new Throughput(args).start();
	}

	void send() throws IOException {
		int w = windowSize;

		System.err.println("count = " + count + " len = " + data.length);
		for(int i = 0; i< count; i++) {
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeArray(data);
			writeMessage.finish();

			if (--w == 0) {
				System.err.println("EEEEEEEEEEEK");
				ReadMessage readMessage = rport.receive();
				readMessage.readArray(data);
				readMessage.finish();
				w = windowSize;
			}
		}
	}


	void rcve() throws IOException {
		int w = windowSize;
		for(int i = 0; i< count; i++) {
			ReadMessage readMessage = rport.receive();
			readMessage.readArray(data);
			readMessage.finish();

			if (--w == 0) {
				System.err.println("EEEEEEEEEEEK");
				WriteMessage writeMessage = sport.newMessage();
				writeMessage.writeArray(data);
				writeMessage.finish();
				w = windowSize;
			}
		}
	}


	Throughput(String[] args) {
		/* parse the commandline */
		int options = 0;
		for (int i = 0; i < args.length; i++) {
		    if (false) {
		    } else if (args[i].equals("-window")) {
			windowSize = Integer.parseInt(args[++i]);
			if (windowSize <= 0) {
			    windowSize = Integer.MAX_VALUE;
			}
		    } else if (options == 0) {
			count = Integer.parseInt(args[i]);
			options++;
		    } else if (options == 1) {
			transferSize = Integer.parseInt(args[i]);
			options++;
		    }
		}

		if (options != 2) {
		    System.err.println("Throughput <count> <size>");
		    System.exit(11);
		}

		data = new byte[transferSize];
	}


	public void run() {
		Random rand = new Random();
		try {
			ReceivePortIdentifier ident;

			Ibis ibis = Ibis.createIbis(null, null);
			Registry r = ibis.registry();

			IbisIdentifier master = (IbisIdentifier)r.elect("throughput", ibis.identifier());

			if (master.equals(ibis.identifier())) {
				rank = 0;
				remoteRank = 1;
System.err.println(">>>>>>>> Righto, I'm the master");
			} else {
				rank = 1;
				remoteRank = 0;
System.err.println(">>>>>>>> Righto, I'm the slave");
			}

			PortType t = ibis.createPortType("test type", null);
			rport = t.createReceivePort("test port " + rank);
			rport.enableConnections();
			sport = t.createSendPort();

			do {
				ident = r.lookupReceivePort("test port " + remoteRank);
				if(ident == null) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {}
				}
			} while (ident == null);

			sport.connect(ident);

			if(rank == 0) {
				// warmup
				send();
				long time = System.currentTimeMillis();
				send();
				time = System.currentTimeMillis() - time;
				double speed = (time * 1000.0) / (double)count;
				double dataSent = ((double) transferSize * ( count + count / windowSize )) / (1024.0 * 1024.0);
				System.out.print("Latency: " + count + " calls took " + (time/1000.0) + " seconds, time/call = " + speed + " micros, ");
				System.out.println("Throughput: " + (dataSent / (time / 1000.0)) + " MByte/s");
			} else {			
				rcve();
				rcve();
			}

			/* free the send ports first */
			sport.close();
			rport.close();
			ibis.end();
			
			System.exit(0);

		} catch (IbisException e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();

		} catch (ClassNotFoundException e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();

		} catch (IOException e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	} 
} 
