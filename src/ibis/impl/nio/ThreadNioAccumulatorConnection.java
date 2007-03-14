/* $Id: ThreadNioAccumulatorConnection.java 2974 2005-04-29 15:30:11Z ceriel $ */

package ibis.impl.nio;

import ibis.impl.ReceivePortIdentifier;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class ThreadNioAccumulatorConnection extends NioAccumulatorConnection
        implements Config {

    private static Logger logger = Logger
            .getLogger(ThreadNioAccumulatorConnection.class);

    boolean sending = false;

    IOException error = null;

    SendReceiveThread sendReceiveThread;

    ThreadNioAccumulatorConnection(
            NioSendPort port, SendReceiveThread sendReceiveThread,
            GatheringByteChannel channel, ReceivePortIdentifier peer)
            throws IOException {
        super(port, channel, peer);
        this.sendReceiveThread = sendReceiveThread;

        key = sendReceiveThread.register((SelectableChannel) channel, this);
    }

    /**
     * Adds given buffer to list of buffer which will be send out. Make sure
     * there is room!
     */
    synchronized void addToThreadSendList(SendBuffer buffer) throws IOException {
        if (error != null) {
            throw error;
        }
        while (full()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for the sendlist"
                            + " to have a free spot");
                }
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        addToSendList(buffer);

        if (!sending) {
            sendReceiveThread.enableWriting(key);
            sending = true;
        }
    }

    synchronized void threadSend() {
        if (full()) {
            notifyAll();
        }
        try {
            if (send()) {
                // done sending
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

    // synchronized void waitUntilEmpty() throws IOException {
    public synchronized void closeConnection() throws IOException {
        while (!empty()) {
            if (error != null) {
                throw error;
            }
            if (!sending) {
                sendReceiveThread.enableWriting(key);
                sending = true;
            }
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        super.closeConnection();
    }
}
