package ibis.ipl.impl.stacking.p2p.tracker;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PMessageHeader;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PTrackerClient {
	private SendPort sendPort;
	private IbisIdentifier trackerID;
	private Ibis baseIbis;

	private static final Logger logger = LoggerFactory
			.getLogger(P2PTrackerClient.class);

	public P2PTrackerClient(Ibis ibis) throws IOException {
		this.baseIbis = ibis;
		initializeTracker();
		
		logger.debug("Tracker Client was initialized.");
	}

	public void sendNodeInfo() throws IOException {
		P2PMessageHeader msg = new P2PMessageHeader(null, P2PMessageHeader.REGISTER_IBIS);
		WriteMessage writeMsg = sendPort.newMessage();
		writeMsg.writeObject(msg);
		writeMsg.writeObject(baseIbis.identifier());
		writeMsg.finish();
	}

	/**
	 * initialize tracker
	 * @throws IOException
	 */
	private void initializeTracker() throws IOException {
		trackerID = baseIbis.registry().getElectionResult(
				P2PConfig.ELECTION_TRACKER);
		logger.debug("Tracker is located at " + trackerID.name() + " .");
		
		setSendPort(baseIbis.createSendPort(P2PConfig.portType));
	}

	/**
	 * @param sendPort
	 *            the sendPort to set
	 * @throws ConnectionFailedException
	 */
	public synchronized void setSendPort(SendPort sendPort)
			throws ConnectionFailedException {
		this.sendPort = sendPort;
		this.sendPort.connect(trackerID, P2PConfig.TRACKER_PORT);
	}

	/**
	 * get joined ibises from tracker
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<IbisIdentifier> getJoinedIbises() throws IOException,
			ClassNotFoundException {
		P2PMessageHeader msg = new P2PMessageHeader(null, P2PMessageHeader.GET_IBISES);
		WriteMessage writeMsg = sendPort.newMessage();
		writeMsg.writeObject(msg);
		writeMsg.writeObject(baseIbis.identifier());
		writeMsg.finish();

		logger.debug("Sent request for joined Ibises.");
		
		ReceivePort receiver = baseIbis.createReceivePort(P2PConfig.portType,
				P2PConfig.TRACKER_PORT);
		receiver.enableConnections();

		ReadMessage readMessage = receiver.receive();
		ArrayList<IbisIdentifier> joinedIbises = (ArrayList<IbisIdentifier>) readMessage
				.readObject();
		
		logger.debug("Received joined Ibises.");
		
		return joinedIbises;
	}
}
