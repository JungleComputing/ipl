package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

final class ThreadNioAccumulatorConnection extends NioAccumulatorConnection 
					   implements Config {
    boolean sending = false;

    IOException error = null;

    SendReceiveThread sendReceiveThread;

    ThreadNioAccumulatorConnection(SendReceiveThread sendReceiveThread,
				   GatheringByteChannel channel,
				   NioReceivePortIdentifier peer) 
	    throws IOException {
	super(channel, peer);
	this.sendReceiveThread = sendReceiveThread;

	key = sendReceiveThread.register((SelectableChannel) channel, this);
    }

    /**
     * Adds given buffer to list of buffer which will be send out.
     * Make sure there is room!
     */
    synchronized void addToThreadSendList(SendBuffer buffer) 
							throws IOException {
	if (error != null) {
	    throw error;
	}
	while(full()) {
	    try {
		if (DEBUG) {
		    Debug.message("buffers", this, "waiting for the sendlist"
			    + " to have a free spot");
		}
		wait();
	    } catch (InterruptedException e) {
		//IGNORE
	    }
	}
	addToSendList(buffer);

	if(!sending) {
	    sendReceiveThread.enableWriting(key);
	    sending = true;
	}
    }

    synchronized void threadSend() {
	if(full()) {
	    notifyAll();
	}
	try {
	    if(send()) {
		//done sending
		key.interestOps(0);
		sending = false;
		notifyAll();
	    }
	} catch (IOException e) {
	    key.interestOps(0);
	    sending = false;
	    error = e;
	    notifyAll();
	}
    }

//    synchronized void waitUntilEmpty() throws IOException {
    synchronized void close() throws IOException {
	while (!empty()) {
	    if(error != null) {
		throw error;
	    }
	    if(!sending) {
		sendReceiveThread.enableWriting(key);
		sending = true;
	    }
	    try {
		wait();
	    } catch (InterruptedException e) {
		//IGNORE
	    }
	}
	channel.close();
    }
}
