package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

final class NonBlockingChannelNioAccumulator extends NioAccumulator {

    private final NioSendPort port;
    private final Selector selector;

    public NonBlockingChannelNioAccumulator(NioSendPort port) 
	    throws IOException {
	super();
	this.port = port;

	selector = Selector.open();
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel, 
			   NioReceivePortIdentifier peer) throws IOException {
	NioAccumulatorConnection result;
	SelectableChannel sChannel = (SelectableChannel) channel;

	sChannel.configureBlocking(false);

	result = new NioAccumulatorConnection(channel, peer);
	result.key = sChannel.register(selector, 0);
	result.key.attach(result);

	return result;
    }

    /**
     * Sends out a buffer to multiple channels.
     * First adds buffer to pending buffers list,
     * then sends out as much data as possible
     */
    boolean doSend(SendBuffer buffer) throws IOException {
	SendBuffer copy;

	if (DEBUG) {
	    Debug.enter("channels", this, "doSend()");
	}

	if (nrOfConnections == 0) {
	    if (DEBUG) {
		Debug.exit("channels", this, "!not connected");
	    }
	    return true;
	} else if (nrOfConnections == 1) {
	    if (DEBUG) {
		Debug.message("channels", this, "sending to 1 connection");
	    }
	    try {
		if (!connections[0].addToSendList(buffer)) {
		    if (DEBUG) {
			Debug.message("channels", this, 
				"add failed, making room and trying again");
		    }
		    doFlush();
		    connections[0].addToSendList(buffer);
		}

		connections[0].send();
	    } catch (IOException e) {
		connections[0].close();

		port.lostConnection(connections[0].peer, e);

		connections[0] = null;
		nrOfConnections = 0;
	    }
	} else {
	    if (DEBUG) {
		Debug.message("channels", this, "sending to "
			+ nrOfConnections + " connections");
	    }

	    SendBuffer[] copies = SendBuffer.replicate(buffer, nrOfConnections);

	    for(int i = 0; i < nrOfConnections; i++) {
		try {
		    if (!connections[i].addToSendList(copies[i])) {
			doFlush(connections[i]);
			connections[i].addToSendList(copies[i]);
		    }
		    connections[i].send();
		} catch (IOException e) {
		    connections[i].close();

		    //inform the SendPort
		    port.lostConnection(connections[i].peer, e);

		    //remove connection
		    nrOfConnections--;
		    connections[i] = connections[nrOfConnections];
		    connections[nrOfConnections] = null;
		    SendBuffer.recycle(copies[nrOfConnections]);
		    i--;
		}
	    }
	}
	return false;
    }

    void doFlush() throws IOException {
	doFlush(null);
    }

    void doFlush(NioAccumulatorConnection connection) throws IOException {
	Iterator keys;
	int nrOfSendingConnections = 0;
	NioAccumulatorConnection selected;
	SelectionKey key;

	if (DEBUG) {
	    if(connection == null) {
		Debug.enter("channels", this, "doing a complete flush");
	    } else {
		Debug.enter("channels", this, "doing a flush of a single"
			+ " connection");
	    }
	}

	//first try to send out data one more time, and remember
	//which connections still have data left
	for (int i = 0; i < nrOfConnections; i++) {
	    try {
		if (!connections[i].send()) {
		    connections[i].key.interestOps(SelectionKey.OP_WRITE);
		    nrOfSendingConnections++;
		} else {
		    if(connections[i] == connection) {
			if (DEBUG) {
			    Debug.exit("channels", this,
				    "flush done for requested channel");
			}
			return;
		    }
		    connections[i].key.interestOps(0);
		}
	    } catch (IOException e) {
		connections[i].close();
		port.lostConnection(connections[i].peer, e);
		nrOfConnections--;
		connections[i] = connections[nrOfConnections];
		connections[nrOfConnections] = null;
		i--;
	    }
	}

	if (DEBUG) {
	    Debug.message("channels", this, "did one send for each connection"
		    + ", " + nrOfSendingConnections + " connections with data"
		    + " left");
	}

	//continually do a select and send data, until all data has been send
	while(nrOfSendingConnections > 0) {
	    selector.select();
	    if (DEBUG) {
		Debug.message("channels", this, "selected "
			+ selector.selectedKeys().size() + " channels");
	    }
	    keys = selector.selectedKeys().iterator();
	    while(keys.hasNext()) {
		key = (SelectionKey) keys.next();
		selected = (NioAccumulatorConnection) key.attachment();

		try {
		    if (selected.send()) {
			key.interestOps(0);
			nrOfSendingConnections--;
			if(selected == connection) {
			    if (DEBUG) {
				Debug.exit("channels", this,
					"done flushing given connection");
			    }
			    return;
			}
			if (DEBUG) {
			    Debug.message("channels", this,
				    "done flushing a connection, "
				    + nrOfSendingConnections + " left");
			}
		    }
		} catch (IOException e) {
		    nrOfSendingConnections--;
		    for (int i = 0; i < nrOfConnections; i++) {
			if (connection == connections[i]) {
			    connections[i].close();
			    port.lostConnection(connections[i].peer, e);
			    nrOfConnections--;
			    connections[i] = connections[nrOfConnections];
			    connections[nrOfConnections] = null;
			    break;
			}
		    }
		}
	    }
	    selector.selectedKeys().clear();
	}
	if (DEBUG) {
	    Debug.exit("channels", this, "flush done");
	}
    }
}
