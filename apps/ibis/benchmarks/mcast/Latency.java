import ibis.ipl.*;
import ibis.util.PoolInfo;

import java.util.Properties;
import java.io.IOException;

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

		if (args.length > 1) {
			upcall = args[1].equals("-u");
		}

		try {
			PoolInfo info = new PoolInfo();

			int rank = info.rank();
			int size = info.size();
			int remoteRank = (rank == 0 ? 1 : 0);

			ibis     = Ibis.createIbis("ibis:" + rank, "ibis.impl.tcp.TcpIbis", null);
			registry = ibis.registry();

			PortType t = ibis.createPortType("test type", null);

			ReceivePort rport = t.createReceivePort("receive port " + rank);
			SendPort sport = t.createSendPort("send port " + rank);

			rport.enableConnections();

			Latency lat = null;

			if (rank == 0) {
				sport.connect(rport.identifier());

				System.err.println(rank + "*******  connect to myself");

				for (int i=1;i<size;i++) {

					System.err.println(rank + "******* receive");

					ReadMessage r = rport.receive();
					ReceivePortIdentifier id = (ReceivePortIdentifier) r.readObject();
					r.finish();

					System.err.println(rank + "*******  connect to " + id);

					sport.connect(id);
				}

				System.err.println(rank + "*******  connect done ");

				WriteMessage w = sport.newMessage();
				w.writeInt(42);
				w.send();
				w.finish();

				sport.close();

			} else {
				ReceivePortIdentifier id = lookup("receive port 0");


				System.err.println(rank + "*******  connect to 0");
				
				sport.connect(id);

				System.err.println(rank + "*******  connect done");

				WriteMessage w = sport.newMessage();
				w.writeObject(rport.identifier());
				w.send();
				w.finish();

				sport.close();
			}

			ReadMessage r = rport.receive();
			int result = r.readInt();
			r.finish();

			System.out.println(rank + " got " + result);

			rport.close();
			ibis.end();

		} catch (IOException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		} catch (IbisException e) {
			System.out.println("Got exception " + e);
			System.out.println("StackTrace:");
			e.printStackTrace();
		}
	}
}
