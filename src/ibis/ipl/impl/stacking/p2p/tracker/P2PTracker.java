package ibis.ipl.impl.stacking.p2p.tracker;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.WriteMessage;
import ibis.ipl.SendPort;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PMessageHeader;
import ibis.ipl.impl.stacking.p2p.util.P2PNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PTracker implements MessageUpcall, RegistryEventHandler{

	private Vector<IbisIdentifier> joinedPeers = new Vector<IbisIdentifier>();

	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	Ibis myIbis;
	ReceivePort receiver;

	private static final Logger logger = LoggerFactory
			.getLogger(P2PTracker.class);

	public P2PTracker() {
		try {
			myIbis = IbisFactory.createIbis(ibisCapabilities, null, P2PConfig.portType);
			receiver = myIbis.createReceivePort(P2PConfig.portType, P2PConfig.TRACKER_PORT,
					this);

			// enable connections
			receiver.enableConnections();
			// enable upcalls
			receiver.enableMessageUpcalls();

			// perform an election, annouce to the others that I am the tracker
			myIbis.registry().elect(P2PConfig.ELECTION_TRACKER);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {

		P2PMessageHeader msg = (P2PMessageHeader) readMessage.readObject();

		switch (msg.getType()) {
		case P2PMessageHeader.REGISTER_IBIS:
			handleRegisterIbis(readMessage);
			break;
		case P2PMessageHeader.GET_IBISES:
			handleGetJoinedIbises(readMessage);
			break;
		}
	}
	
	private void handleGetJoinedIbises(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		IbisIdentifier source = (IbisIdentifier) readMessage.readObject();
		readMessage.finish();

		// connect to source
		SendPort sendPort = (SendPort) myIbis.createSendPort(P2PConfig.portType);
		sendPort.connect(source, P2PConfig.TRACKER_PORT);

		// select P2PConfig.NEARBY_REQUESTS random joined nodes
		int nearbyRequests = Math.min(joinedPeers.size(), P2PConfig.NEARBY_REQUESTS);
		ArrayList<IbisIdentifier> nearbyNodes = new ArrayList<IbisIdentifier>();
		
		BitSet set = new BitSet();
		set.clear();

		System.err.println("Nearby requests: " + nearbyRequests);
		
		Random random = new Random();
		int selected = 0;
		for (int i = 0; i < nearbyRequests; i++) {
			// generate random numbers between 0 and no. of ibises
			// until selected is not a not previously queried or my own id
			while (set.get(selected)) {
				selected = random.nextInt(nearbyRequests);
				
			}
			nearbyNodes.add(joinedPeers.elementAt(selected));
			set.set(selected);
		}
		
		// send back joined Ibises
		WriteMessage writeMessage = sendPort.newMessage();
		writeMessage.writeObject(nearbyNodes);
		writeMessage.finish();
		sendPort.close();
	}

	private void handleRegisterIbis(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		IbisIdentifier newIbis = (IbisIdentifier) readMessage.readObject();
		readMessage.finish();

		joinedPeers.add(newIbis);
		
		System.err.println("Registered node with IbisIdentifier " + newIbis.name() + " " + joinedPeers.size());
	}

	public static void main(String args[]) {
		P2PTracker tracker = new P2PTracker();
		tracker.run();
	}

	private synchronized void waitFor(long time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
			e.printStackTrace();		
		}
	}
	
	public void run() {
		while (true) {
			waitFor(5000);	
		}
	}

	@Override
	public void died(IbisIdentifier corpse) {
		joinedPeers.remove(corpse);
	}

	@Override
	public void electionResult(String electionName, IbisIdentifier winner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gotSignal(String signal, IbisIdentifier source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void joined(IbisIdentifier joinedIbis) {
	}

	@Override
	public void left(IbisIdentifier leftIbis) {
		joinedPeers.remove(leftIbis);
	}

	@Override
	public void poolClosed() {
	}

	@Override
	public void poolTerminated(IbisIdentifier source) {
		// TODO Auto-generated method stub
		
	}
}
