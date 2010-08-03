package ibis.ipl.impl.stacking.p2p.endtoend;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.impl.stacking.p2p.P2PIbis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * remember sequence number + window per connection maintain separate queues per
 * connection discard multi-cast if end to end fault tolerance is on - several
 * problems: - what if allowed to send for some receivers and not for others -
 * how to advance window if maintain one queue - cannot maintain separate queues
 * - sequence numbers not necessarily in order for all connections
 * 
 * @author Delia
 * 
 */
public class P2PGoBackNSender extends Thread {

	private P2PIbis ibis;
	private Map<ReceivePortIdentifier, P2PSenderConnectionHandler> windows;

	private static final Logger logger = LoggerFactory
			.getLogger(P2PGoBackNSender.class);

	public P2PGoBackNSender(P2PIbis ibis) {
		windows = new HashMap<ReceivePortIdentifier, P2PSenderConnectionHandler>();
		this.ibis = ibis;
	}

	public synchronized void connect(ReceivePortIdentifier rid) {
		windows.put(rid, new P2PSenderConnectionHandler(ibis));
		//logger.debug("Connected to: " + rid);
	}

	/**
	 * add the message in the queue of each connection
	 * 
	 * @param message
	 */
	public synchronized void putMessage(P2PMessage message,
			Map<IbisIdentifier, ReceivePortIdentifier[]> connections) {
		Iterator it = connections.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			ReceivePortIdentifier[] receivers = (ReceivePortIdentifier[]) pairs
					.getValue();
			for (ReceivePortIdentifier receiver : receivers) {
				P2PSenderConnectionHandler window = windows.get(receiver);
				P2PMessage dupMessage = (P2PMessage) message.clone();
				dupMessage.setRid(receiver);
				
				window.putMessage(dupMessage);
			}
		}
	}

	public void sendNextMessage() throws IOException, ClassNotFoundException {
		Iterator it = windows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			P2PSenderConnectionHandler window = (P2PSenderConnectionHandler) pairs
					.getValue();
			window.sendNextMessage();
		}
	}

	public synchronized void processAck(ReceivePortIdentifier rid, int ackNum) {
		logger.debug("ack from " + rid);
		
		P2PSenderConnectionHandler window = windows.get(rid);
		window.processAck(ackNum);
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				//logger.debug("I am the sender thread!");
				sendNextMessage();
				Thread.sleep(50);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
}
