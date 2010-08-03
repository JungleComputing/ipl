package ibis.ipl.examples.p2p;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

/**
 * each node performs an election with its own id
 * associate nodes in pairs: Ibis 0 sends message to Ibis n, 
 * Ibis 1 sends message a message to Ibis n-1 and so on
 * measure RTTs 
 * @author delia
 *
 */
public class P2PPair {
	PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_EXPLICIT,
			PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_DOWNCALLS);

	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT);
	Ibis ibis;

	String idPrefix = "Ibis ";
	public void run(int N) {
		try {
			long time = System.currentTimeMillis();
			ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);
			time = System.currentTimeMillis() - time;
			
			ReceivePort receiver = ibis.createReceivePort(portType, "pair"); 
			receiver.enableConnections();
			
			// print join time
			System.out.println(time);
			
			Thread.sleep(60000);
			
			IbisIdentifier myself = ibis.registry().elect(ibis.identifier().name());
			
			// compute my pair
			int myNumber = Integer.valueOf(myself.name().substring(idPrefix.length()));
			int myPair = N - myNumber + 1;
			
			IbisIdentifier pair = ibis.registry().getElectionResult(idPrefix + myPair);
			
			SendPort sendPort = ibis.createSendPort(portType);
			sendPort.connect(pair, "pair");
			
			time = System.currentTimeMillis();
			
			WriteMessage writeMessage = sendPort.newMessage();
			writeMessage.writeString("Hello from " + myself.name());
			writeMessage.finish();
			
			ReadMessage readMessage = receiver.receive();
			String msg = readMessage.readString();
			readMessage.finish();
			
			SendPort replySendPort = ibis.createSendPort(portType);
			replySendPort.connect(pair, "pair");
			
			WriteMessage replyMessage = replySendPort.newMessage();
			replyMessage.writeString("Reply from " + myself.name());
			replyMessage.finish();
			
			readMessage = receiver.receive();
			msg = readMessage.readString();
			readMessage.finish();
			
			time = System.currentTimeMillis() - time;
			System.out.println(time);
			
			Thread.sleep(30000);
			
			replySendPort.close();
			sendPort.close();
			receiver.close();
			ibis.end();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int N = Integer.parseInt(args[0]);
		new P2PPair().run(N);
	}
}
