package ibis.ipl.impl.stacking.p2p.viz;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PIdentifier;
import ibis.ipl.impl.stacking.p2p.util.P2PMessage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PVisualizer implements Runnable{

	private SendPort sendPort;
	private IbisIdentifier visualizerID;
	private Ibis baseIbis;
	private P2PIdentifier p2pID;

	private static final Logger logger = LoggerFactory
			.getLogger(P2PVisualizer.class);

	public P2PVisualizer(Ibis ibis, P2PIdentifier p2pID) {
		this.baseIbis = ibis;
		this.p2pID = p2pID;
	}

	public synchronized void sendNodeInfo(int type) {
		if (sendPort != null) {
			try {
				P2PMessage msg = new P2PMessage(null, type);
				WriteMessage writeMsg = sendPort.newMessage();
				writeMsg.writeObject(msg);
				writeMsg.writeObject(baseIbis.identifier());
				writeMsg.writeObject(p2pID);
				writeMsg.finish();
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	public synchronized void sendMessageUpdate(P2PIdentifier nextHop,
			String messageID) {
		if (sendPort != null) {
			try {
				P2PMessage msg = new P2PMessage(null,
						P2PMessage.MESSAGE_FORWARD);
				WriteMessage writeMsg = sendPort.newMessage();
				writeMsg.writeObject(msg);
				writeMsg.writeObject(p2pID);
				writeMsg.writeObject(messageID);
				writeMsg.writeObject(nextHop);
				writeMsg.finish();
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	public synchronized void sendAddMessage(String messageID) {
		if (sendPort != null) {
			try {
				P2PMessage msg = new P2PMessage(null, P2PMessage.MESSAGE_ADD);
				WriteMessage writeMsg = sendPort.newMessage();
				writeMsg.writeObject(msg);
				writeMsg.writeObject(baseIbis);
				writeMsg.writeObject(p2pID);
				writeMsg.writeObject(messageID);
				writeMsg.finish();
				logger.debug("Sent message add " + messageID);
			} catch (IOException ex) {
				// ignore, maybe GUI died
			}

		}
	}

	public synchronized void sendDeleteMessage(String messageID) {
		if (sendPort != null) {
			try {
				P2PMessage msg = new P2PMessage(null, P2PMessage.MESSAGE_DELETE);
				WriteMessage writeMsg = sendPort.newMessage();
				writeMsg.writeObject(msg);
				writeMsg.writeObject(baseIbis);
				writeMsg.writeObject(p2pID);
				writeMsg.writeObject(messageID);
				writeMsg.finish();

				logger.debug("Sent message delete " + messageID);
			} catch (IOException ex) {
				// ignore, maybe GUI died
			}
		}
	}

	public synchronized void setSendPort(SendPort sendPort) {
		logger.debug("Preparing to connect with GUI....");

		this.sendPort = sendPort;
		try {
			this.sendPort.connect(this.getVisualizerID(), P2PConfig.PORT_NAME);
		} catch (ConnectionFailedException ex) {
			ex.printStackTrace();
		}

		logger.debug("Sendport connected to: "
				+ sendPort.connectedTo()[0].ibisIdentifier());
	}

	public void end() throws IOException {
		if (sendPort != null) {
			sendPort.close();
		}
	}

	/**
	 * @param vizualizerID
	 *            the vizualizerID to set
	 */
	public void setVisualizerID(IbisIdentifier vizualizerID) {
		this.visualizerID = vizualizerID;
		logger.debug("Added logger " + vizualizerID.name());
	}

	/**
	 * @return the vizualizerID
	 */
	public IbisIdentifier getVisualizerID() {
		return visualizerID;
	}

	@Override
	public void run() {
		try {
			visualizerID = baseIbis.registry().getElectionResult(P2PConfig.ELECTION_GUI);
			setSendPort(baseIbis.createSendPort(P2PConfig.portType));
			sendNodeInfo(P2PMessage.NODE_JOIN);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
