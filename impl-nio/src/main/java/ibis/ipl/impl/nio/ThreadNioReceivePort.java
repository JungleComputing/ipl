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
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.Queue;

final class ThreadNioReceivePort extends NioReceivePort {

    private ThreadNioDissipator current = null;

    private Queue readyDissipators;

    private boolean closing = false;

    ThreadNioReceivePort(Ibis ibis, PortType type, String name, MessageUpcall upcall, ReceivePortConnectUpcall connUpcall, Properties props)
            throws IOException {
        super(ibis, type, name, upcall, connUpcall, props);

        readyDissipators = new Queue();
    }

    @Override
    synchronized void newConnection(SendPortIdentifier spi, Channel channel) throws IOException {

        if (!(channel instanceof ReadableByteChannel)) {
            throw new IOException("wrong channel type on creating connection");
        }

        addConnection(spi, new ThreadNioDissipator(((NioIbis) ibis).sendReceiveThread(), (ReadableByteChannel) channel));
    }

    @Override
    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
        dissipator.info.close(cause);
    }

    @Override
    NioDissipator getReadyDissipator(long deadline) throws IOException {
        ThreadNioDissipator dissipator;

        synchronized (this) {
            if (current != null) {
                if (current.dataLeft()) {
                    readyDissipators.enqueue(current);
                }
                current = null;
            }
        }

        // FIXME: if a connection doesn't close gracefully, we won't notice

        while (true) {

            synchronized (this) {
                if (closing) {
                    if (connections.size() == 0) {
                        throw new ConnectionClosedException();

                    }
                }
            }

            dissipator = (ThreadNioDissipator) readyDissipators.dequeue(deadline);

            if (dissipator == null) {
                synchronized (this) {
                    if (closing) {
                        if (connections.size() == 0) {
                            throw new ConnectionClosedException();
                        }
                    }
                }
                throw new ReceiveTimedOutException("deadline passed while" + " selecting dissipator");
            }

            try {
                if (dissipator.messageWaiting()) {
                    synchronized (this) {
                        current = dissipator;
                        return dissipator;
                    }
                }
            } catch (IOException e) {
                if (dissipator != null) {
                    errorOnRead(dissipator, e);
                }
            }
        }
    }

    void addToReadyList(ThreadNioDissipator dissipator) {
        readyDissipators.enqueue(dissipator);
    }

    @Override
    synchronized void closing() {
        closing = true;
    }
}
