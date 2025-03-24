/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.impl.ReceivePortIdentifier;

final class ThreadNioAccumulatorConnection extends NioAccumulatorConnection {

    private static Logger logger = LoggerFactory.getLogger(ThreadNioAccumulatorConnection.class);

    boolean sending = false;

    IOException error = null;

    SendReceiveThread sendReceiveThread;

    ThreadNioAccumulatorConnection(NioSendPort port, SendReceiveThread sendReceiveThread, GatheringByteChannel channel, ReceivePortIdentifier peer)
            throws IOException {
        super(port, channel, peer);
        this.sendReceiveThread = sendReceiveThread;

        key = sendReceiveThread.register((SelectableChannel) channel, this);
    }

    /**
     * Adds given buffer to list of buffer which will be send out. Make sure there
     * is room!
     */
    synchronized void addToThreadSendList(SendBuffer buffer) throws IOException {
        if (error != null) {
            throw error;
        }
        while (full()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for the sendlist" + " to have a free spot");
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
    @Override
    public synchronized void closeConnection() {
        while (!empty()) {
            if (error != null) {
                // throw error;
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
