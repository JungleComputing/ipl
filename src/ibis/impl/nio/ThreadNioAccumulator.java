package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;


final class ThreadNioAccumulator extends NioAccumulator implements Config {
    static final int LOST_CONNECTION_SIZE = 8;

    NioSendPort port;
    SendReceiveThread thread;

    ThreadNioAccumulator(NioSendPort port, 
			 SendReceiveThread thread) {
	super();
	this.port = port;
	this.thread = thread;
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
	    NioReceivePortIdentifier peer) throws IOException {
	SelectableChannel sChannel = (SelectableChannel) channel;

	sChannel.configureBlocking(false);

	if (DEBUG) {
	    Debug.message("connections", this, "creating new"
		    + " ThreadNioAccumulatorConnection");
	}

	return new ThreadNioAccumulatorConnection(thread,
						  channel, peer);
    }

    void doSend(SendBuffer buffer) throws IOException {
	SendBuffer copy;
	
	if (DEBUG) {
	    Debug.enter("buffers", this, "doing send");
	}

	if (nrOfConnections == 0) {
	    if (DEBUG) {
		Debug.exit("buffers", this, "!no connections to send to");
		return; // don't do normal exit message
	    }
	    return;
	} else if (nrOfConnections == 1) {
	    if (DEBUG) {
		Debug.message("buffers", this, "sending to one(1) connection");
	    }
	    ThreadNioAccumulatorConnection connection;
	    connection = (ThreadNioAccumulatorConnection) connections[0];
	    try {
		connection.addToThreadSendList(buffer);
	    } catch (IOException e) {
		connection.close();
		port.lostConnection(connection.peer, e);
		connections[0] = null;
		nrOfConnections = 0;
		if (DEBUG) {
		    Debug.exit("buffers", this, "!(only) connection lost");
		    return; // don't do normal exit message
		}
	    }
	} else {
	    if (DEBUG) {
		Debug.message("buffers", this, "sending to " + nrOfConnections
			+ " connections");
	    }
	    for (int i = 0; i < nrOfConnections; i++) {
		ThreadNioAccumulatorConnection connection;
		connection = (ThreadNioAccumulatorConnection) connections[i];

		copy = SendBuffer.duplicate(buffer); 
		try {
		    connection.addToThreadSendList(copy);
		} catch (IOException e) {
		    if (DEBUG) {
			Debug.message("buffers", this, "connection lost");
		    }
		    connection.close();
		    port.lostConnection(connection.peer, e);
		    nrOfConnections--;
		    connections[i] = connections[nrOfConnections];
		    connections[nrOfConnections] = null;
		    i--;
		}
	    }
	}
	if (DEBUG) {
	    Debug.exit("buffers", this, "done send");
	}
    }

    void doFlush() throws IOException {
	if (DEBUG) {
	    Debug.enter("buffers", this, "doing flush");
	}
	for (int i = 0; i < nrOfConnections; i++) {
	    ((ThreadNioAccumulatorConnection) connections[i]).waitUntilEmpty();
	}
	if (DEBUG) {
	    Debug.exit("buffers", this, "done flush");
	}
    }
}
