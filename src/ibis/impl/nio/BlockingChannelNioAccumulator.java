package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

final class BlockingChannelNioAccumulator extends NioAccumulator {

    private final NioSendPort port;

    public BlockingChannelNioAccumulator(NioSendPort port) {
	super();
	this.port = port;
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,  
	    NioReceivePortIdentifier peer) throws IOException {
	NioAccumulatorConnection result;

	if (DEBUG) {
	    Debug.enter("connections", this, "registering new connection");
	}

	SelectableChannel sChannel = (SelectableChannel) channel;

	sChannel.configureBlocking(true);

	result = new NioAccumulatorConnection(channel, peer);

	if (DEBUG) {
	    Debug.exit("connections", this, "registered new connection");
	}

	return result;
    }

    /**
     * Sends out a buffer to multiple channels.
     * Doesn't buffer anything
     */
    void doSend(SendBuffer buffer) throws IOException {
	if (DEBUG) {
	    Debug.enter("buffers", this, "sending a buffer");
	}
	for(int i = 0; i < nrOfConnections; i++) {
	    try {
		connections[i].sendDirectly(buffer);
	    } catch (IOException e) {
		//someting went wrong, close connection 
		connections[i].close();

		//inform the SendPort
		port.lostConnection(connections[i].peer, e);

		//remove connection
		nrOfConnections--;
		connections[i] = connections[nrOfConnections];
		connections[nrOfConnections] = null;
		i--;
	    }
	}
	if (DEBUG) {
	    Debug.exit("buffers", this, "done sending a buffer");
	}
    }

    void doFlush() throws IOException {
	//NOTHING
    }
}
