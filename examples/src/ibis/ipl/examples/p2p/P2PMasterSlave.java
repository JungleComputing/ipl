package ibis.ipl.examples.p2p;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public class P2PMasterSlave {

	PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_EXPLICIT,
			PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_DOWNCALLS);

	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT);
	Ibis ibis;

	private void server(Ibis ibis, int nodes) throws IOException, ClassNotFoundException {
		System.out.println(ibis.identifier().name() + " I am the server!");

		ReceivePort receivePort = ibis.createReceivePort(portType, "server");
		receivePort.enableConnections();

		SendPort sendPort = ibis.createSendPort(portType);
		
		for (int i = 0; i < nodes; i++) {
			ReadMessage r = receivePort.receive();
			IbisIdentifier id = (IbisIdentifier) r.readObject();
			r.finish();

			sendPort.connect(id, "client");

			System.out.println("The server received:" + id + " " + i + " " + nodes);
		}
		
		String response = "Hello world from server!";
		
		// send a message to the server
		WriteMessage message = sendPort.newMessage();
		message.writeObject(response);
		message.finish();
		
		System.out.println("Server terminated!");
		
		sendPort.close();
		receivePort.close();
		ibis.end();
		
	}

	private void client(Ibis ibis, IbisIdentifier server) throws IOException, InterruptedException {
		ReceivePort receivePort = ibis.createReceivePort(portType, "client");
		receivePort.enableConnections();

		SendPort sendPort = ibis.createSendPort(portType);
		
		Thread.sleep(10000);
		
		//System.out.println("Connecting to server...");
		sendPort.connect(server, "server");
		//System.out.println("Connected to server.");
		
		long time = System.currentTimeMillis();
		
		// send a message to the server
		WriteMessage message = sendPort.newMessage();
		message.writeObject(ibis.identifier());
		message.finish();

		ReadMessage readMessage = receivePort.receive();
		String msg = readMessage.readString();
		readMessage.finish();
		time = System.currentTimeMillis() - time;
		
		System.out.println("RTT time:" + time);
		
		// close end port
		sendPort.close();
		receivePort.close();
		Thread.sleep(10000);
		ibis.end();
	}

	public void run(int nodes) throws IbisCreationFailedException, IOException {

		try {
			long time = System.currentTimeMillis();
			ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);
			time = System.currentTimeMillis() - time;
			
			System.out.println(time);
			
			IbisIdentifier server = ibis.registry().elect("Server");

			if (server.equals(ibis.identifier())) {
				server(ibis, nodes);
			} else {
				client(ibis, server);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			int nodes = Integer.parseInt(args[0]);
			new P2PMasterSlave().run(nodes - 1);
		} catch (Exception ex) {
			System.err.println();
			ex.printStackTrace();
		}
	}

}
