import ibis.ipl.*;

import java.util.Properties;
import manta.util.Ticket;

class Sender implements Upcall { 

	Ticket t;

	SendPort sport;

	Sender(SendPort sport) {
		this.sport = sport;
		t = new Ticket();
	} 
	
	void send(int count) throws IbisException {
		// warmup
		for(int i = 0; i< count; i++) {
			int ticket = t.get();

			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeInt(ticket);
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = (ReadMessage) t.collect(ticket);
			readMessage.readByte();
			readMessage.readByte();
			readMessage.finish();
		}

		// test
		long time = System.currentTimeMillis();

		for(int i = 0; i< count; i++) {
			int ticket = t.get();

			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeInt(ticket);
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.writeInt(0);
			writeMessage.send();
			writeMessage.finish();
			
			ReadMessage readMessage = (ReadMessage) t.collect(ticket);
			readMessage.readByte();
			readMessage.readByte();
			readMessage.finish();
		}

		time = System.currentTimeMillis() - time;

		double speed = (time * 1000.0) / (double)count;
		System.err.println("Latency: " + count + " calls took " + (time/1000.0) + " seconds, time/call = " + speed + " micros");
	}

	public void upcall(ReadMessage readMessage) { 

		try { 
			int ticket = readMessage.readInt();
			t.put(ticket, readMessage);			
		} catch (Exception e) { 			
			System.out.println("EEEEEK " + e);
			e.printStackTrace();
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

		try { 
			int ticket = readMessage.readInt();
			readMessage.readByte();
			readMessage.readByte();
			readMessage.readInt();
			readMessage.readInt();
			readMessage.readInt();
			readMessage.readInt();
			readMessage.finish();
			
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeInt(ticket);
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
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

		System.err.println("Finished");

	} 
} 


class Latency { 
	static Ibis ibis;
	static Registry registry;

	public static ReceivePortIdentifier lookup(String name) throws IbisException { 
		
		ReceivePortIdentifier temp = null;

		do {
			temp = registry.lookup(name);

			if (temp == null) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					// ignore
				}
			}
			
		} while (temp == null);
				
		return temp;
	} 

	public static void main(String [] args) { 
		/* Parse commandline. */

		if (args.length != 1) {
		    System.err.println("Usage: Latency <count>");
		    System.exit(33);
		}

		int count = Integer.parseInt(args[0]);
		int rank = 0, remoteRank = 1;
		
		try {
			ibis     = Ibis.createIbis("ibis:" + rank, "ibis.ipl.impl.panda.PandaIbis", null);
			registry = ibis.registry();

			StaticProperties s = new StaticProperties();
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

				Sender sender = new Sender(sport);
				rport = t.createReceivePort("test port 0", sender);
				sport.connect(lookup("test port 1"));		
				sender.send(count);
				
			} else { 

				sport.connect(lookup("test port 0"));

				UpcallReceiver receiver = new UpcallReceiver(sport, 2*count);
				rport = t.createReceivePort("test port 1", receiver);
				receiver.finish();
			} 

			/* free the send ports first */
                        sport.free();
                        rport.free();
			ibis.end();

                        System.exit(0);

		} catch (IbisException e) { 
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	} 
} 
