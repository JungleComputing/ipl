import ibis.ipl.*;

import java.util.Properties;
import java.io.IOException;

class Sender {

	SendPort sport;
	ReceivePort rport;

	Sender(ReceivePort rport, SendPort sport) {
		this.rport = rport;
		this.sport = sport;
	}

	void send(int count) throws IOException {
		// warmup
		for(int i = 0; i< count; i++) {
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
			writeMessage.writeInt(2);
			writeMessage.writeInt(3);
			writeMessage.writeInt(4);
			writeMessage.writeInt(5);
			writeMessage.writeInt(6);
			writeMessage.send();
			writeMessage.finish();

			ReadMessage readMessage = rport.receive();
			byte b = readMessage.readByte();
			b = readMessage.readByte();
			readMessage.finish();
		}

		// test
		long time = System.currentTimeMillis();

		for(int i = 0; i< count; i++) {
			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
			writeMessage.writeInt(2);
			writeMessage.writeInt(3);
			writeMessage.writeInt(4);
			writeMessage.writeInt(5);
			writeMessage.writeInt(6);
			writeMessage.send();
			writeMessage.finish();

			ReadMessage readMessage = rport.receive();
			byte b = readMessage.readByte();
			b = readMessage.readByte();
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

	void receive(int count) throws IOException {
		for(int i = 0; i< count; i++) {
			ReadMessage readMessage = rport.receive();
			byte b = readMessage.readByte();
			b = readMessage.readByte();
			int p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			readMessage.finish();

			WriteMessage writeMessage = sport.newMessage();
			writeMessage.writeByte((byte)0);
			writeMessage.writeByte((byte)1);
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

		try {
			byte b = readMessage.readByte();
			b = readMessage.readByte();
			int p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			p = readMessage.readInt();
			readMessage.finish();

			WriteMessage writeMessage = sport.newMessage();
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

	public static ReceivePortIdentifier lookup(String name) throws IOException {

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

		boolean upcall = false;

		int count = Integer.parseInt(args[0]);
		int rank = 0, remoteRank = 1;

		if (args.length > 1) {
			upcall = args[1].equals("-u");
		}

		try {
			ibis     = Ibis.createIbis("ibis:" + rank, "ibis.impl.tcp.TcpIbis", null);
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

				rport = t.createReceivePort("test port 0");

				sport.connect(lookup("test port 1"));

				Sender sender = new Sender(rport, sport);
				sender.send(count);

			} else {

				sport.connect(lookup("test port 0"));

				if (upcall) {
					UpcallReceiver receiver = new UpcallReceiver(sport, 2*count);
					rport = t.createReceivePort("test port 1", receiver);
					receiver.finish();
				} else {
					rport = t.createReceivePort("test port 1");
					ExplicitReceiver receiver = new ExplicitReceiver(rport, sport);
					receiver.receive(2*count);
				}
			}

			/* free the send ports first */
                        sport.free();
                        rport.free();
			ibis.end();

                        System.exit(0);

		} catch (IOException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		} catch (IbisException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	}
}
