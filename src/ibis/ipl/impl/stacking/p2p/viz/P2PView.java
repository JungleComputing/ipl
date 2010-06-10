package ibis.ipl.impl.stacking.p2p.viz;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.p2p.P2PIbis;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PHashTools;
import ibis.ipl.impl.stacking.p2p.util.P2PIdentifier;
import ibis.ipl.impl.stacking.p2p.util.P2PMessage;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.touchgraph.graphlayout.Edge;
import com.touchgraph.graphlayout.GLPanel;
import com.touchgraph.graphlayout.Node;
import com.touchgraph.graphlayout.TGException;

public class P2PView extends GLPanel implements MessageUpcall, Runnable {
	public Map<String, P2PNode> nodes = Collections
			.synchronizedMap(new HashMap<String, P2PNode>());
	private boolean done = false;

	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_AUTO_UPCALLS,
			PortType.CONNECTION_MANY_TO_MANY);

	Ibis myIbis;
	ReceivePort receiver;
	ArrayList<Edge> edges = new ArrayList<Edge>();
	private Vector<P2PNode> deletedNodes = new Vector<P2PNode>();

	private static final Logger logger = LoggerFactory.getLogger(P2PView.class);

	public P2PView() {
		super(null, null);

		try {
			myIbis = IbisFactory.createIbis(ibisCapabilities, null, portType);
			receiver = myIbis.createReceivePort(portType, P2PConfig.PORT_NAME,
					this);

			// enable connections
			receiver.enableConnections();
			// enable upcalls
			receiver.enableMessageUpcalls();

			// perform an election
			myIbis.registry().elect(P2PConfig.ELECTION_GUI);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		new Thread(this).start();

	}

	public static void main(String[] args) {

		System.err.println("Starting P2PIbis Vizualization...");

		logger.debug("P2PVisualization started.");

		final Frame frame;

		final P2PView glPanel = new P2PView();

		frame = new Frame("P2PIbis Visualization");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					glPanel.done();
					frame.remove(glPanel);
					frame.dispose();

					System.exit(0); // otherwise it does'nt exit, there
					// is a non-deamon thread in touchgraph.
					// --Ceriel
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		frame.add("Center", glPanel);
		frame.setSize(800, 600);
		frame.setVisible(true);

	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		// receive updates from nodes
		P2PMessage msg = (P2PMessage) readMessage.readObject();

		logger.debug("Message of type " + msg.getType() + " received.");

		switch (msg.getType()) {
		case P2PMessage.NODE_JOIN:
			handleNodeJoin(readMessage);
			break;
		case P2PMessage.NODE_DEPARTURE:
			handleNodeDeparture(readMessage);
			break;
		case P2PMessage.MESSAGE_FORWARD:
			handleMessageUpdate(readMessage);
			break;
		case P2PMessage.MESSAGE_ADD:
			handleMessageAdd(readMessage);
			break;
		case P2PMessage.MESSAGE_DELETE:
			handleMessageDelete(readMessage);
			break;
		}
	}

	private void handleMessageDelete(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		IbisIdentifier ibisID = (IbisIdentifier) readMessage.readObject();
		P2PIdentifier p2pID = (P2PIdentifier) readMessage.readObject();
		String messageID = (String) readMessage.readObject();
		readMessage.finish();

		P2PNode node = nodes.get(ibisID.name() + " " + p2pID.getP2pID());
		node.removeMessage(messageID);

		logger.debug("Message delete at : " + node.getNodeID() + " "
				+ messageID);
	}

	private void handleMessageAdd(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		IbisIdentifier ibisID = (IbisIdentifier) readMessage.readObject();
		P2PIdentifier p2pID = (P2PIdentifier) readMessage.readObject();
		String messageID = (String) readMessage.readObject();
		readMessage.finish();

		P2PNode node = nodes.get(ibisID.name() + " " + p2pID.getP2pID());
		node.addMessage(messageID);

		logger.debug("Message add at : " + node.getNodeID() + " " + messageID);
	}

	private void handleMessageUpdate(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		P2PIdentifier source = (P2PIdentifier) readMessage.readObject();
		String messageID = readMessage.readString();
		P2PIdentifier dest = (P2PIdentifier) readMessage.readObject();

		P2PNode tgSource = nodes.get(source.getP2pID());
		P2PNode tgDest = nodes.get(dest.getP2pID());

		tgSource.removeMessage(messageID);
		tgDest.addMessage(messageID);
	}

	private void handleNodeDeparture(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		IbisIdentifier ibisID = (IbisIdentifier) readMessage.readObject();
		P2PIdentifier p2pID = (P2PIdentifier) readMessage.readObject();
		readMessage.finish();

		P2PNode node = nodes.get(ibisID.name() + " " + p2pID.getP2pID());
		if (node != null) {
			deletedNodes.add(node);
			logger.debug("Node departure received from: " + node.getNodeID());
		}
	}

	private void handleNodeJoin(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		IbisIdentifier ibisID = (IbisIdentifier) readMessage.readObject();
		P2PIdentifier p2pID = (P2PIdentifier) readMessage.readObject();
		readMessage.finish();

		P2PNode node = new P2PNode(ibisID.name() + " " + p2pID.getP2pID(),
				tgPanel);
		nodes.put(node.getNodeID(), node);

		logger.debug("Node join received from: " + node.getNodeID());
	}

	private synchronized void waitFor(long time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Override
	public void run() {
		while (!getDone()) {
			updateGraph();
			waitFor(5000);
		}
	}

	private void updateGraph() {
		// update nodes hashmap, remove deleted ones
		for (P2PNode node : deletedNodes) {
			node.remove();
			nodes.remove(node.getNodeID());
		}
		deletedNodes.clear();

		Iterator<Entry<String, P2PNode>> iterator = nodes.entrySet().iterator();
		P2PNode first = null, node = null, temp = null;
		while (iterator.hasNext()) {
			Map.Entry<String, P2PNode> pairs = iterator.next();

			node = pairs.getValue();
			node.add();
			node.updateMessages();
			node.connect(temp);

			System.out.println(node + " " + temp);
			temp = node;

			if (first == null) {
				first = node;
			}
		}
		if (first != null) {
			first.connect(node);
		}

		System.out.println(node + " " + first);
		tgPanel.repaint();
	}

	public synchronized void done() throws IOException {
		done = true;
		notifyAll();

		receiver.close();

		// End ibis.
		myIbis.end();
	}

	public synchronized boolean getDone() {
		return done;
	}
}
