package ibis.ipl.impl.stacking.p2p.viz;

import java.util.ArrayList;

import com.touchgraph.graphlayout.Edge;
import com.touchgraph.graphlayout.Node;
import com.touchgraph.graphlayout.TGException;
import com.touchgraph.graphlayout.TGPanel;

public class P2PNode {
	private Node node, message;
	private Edge msgEdge, connector;
	private ArrayList<String> messages;
	private TGPanel panel;
	private String nodeID;
	private boolean isAdded;

	public P2PNode(String nodeID, TGPanel panel) {
		messages = new ArrayList<String>();
		setNode(new Node(nodeID));

		message = new Node("messages" + nodeID);
		msgEdge = new Edge(getNode(), message);

		this.panel = panel;
		this.setNodeID(nodeID);

	}

	public synchronized void add() {
		if (!isAdded) {
			try {
				this.panel.addNode(getNode());
				this.panel.addNode(message);
				this.panel.addEdge(msgEdge);
			} catch (TGException ex) {
				// FIXME: move catch somewhere else
				ex.printStackTrace();
			}
			updateMessages();
			isAdded = true;
		}
	}

	public void updateMessages() {
		String label = "";
		for (String message : messages) {
			label += " " + message;
		}
		message.setLabel(label);
	}

	public synchronized void remove() {
		panel.deleteNode(getNode());
		panel.deleteNode(message);
		panel.deleteEdge(msgEdge);
	}

	public synchronized void addMessage(String messageID) {
		messages.add(messageID);
	}

	public synchronized void removeMessage(String messageID) {
		messages.remove(messageID);
	}

	/**
	 * @param node
	 *            the node to set
	 */
	public void setNode(Node node) {
		this.node = node;
	}

	/**
	 * @return the node
	 */
	public Node getNode() {
		return node;
	}

	public void connect(P2PNode other) {
		if (other == null) {
			return;
		}

		if (connector != null) {
			this.panel.deleteEdge(connector);
		}
		connector = new Edge(node, other.getNode());
		this.panel.addEdge(connector);
	}

	public String toString() {
		return getNodeID();
	}

	/**
	 * @param nodeID
	 *            the nodeID to set
	 */
	public void setNodeID(String nodeID) {
		this.nodeID = nodeID;
	}

	/**
	 * @return the nodeID
	 */
	public String getNodeID() {
		return nodeID;
	}
}
