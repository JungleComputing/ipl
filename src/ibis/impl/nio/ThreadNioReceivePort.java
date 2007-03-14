/* $Id: ThreadNioReceivePort.java 2944 2005-03-15 17:00:32Z ndrost $ */

package ibis.impl.nio;

import ibis.impl.Ibis;
import ibis.impl.SendPortIdentifier;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.Upcall;
import ibis.util.Queue;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;

final class ThreadNioReceivePort extends NioReceivePort {

    private ThreadNioDissipator current = null;

    private Queue readyDissipators;

    private boolean closing = false;

    ThreadNioReceivePort(Ibis ibis, NioPortType type, String name,
            Upcall upcall, boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) throws IOException {
        super(ibis, type, name, upcall, connectionAdministration, connUpcall);

        readyDissipators = new Queue();
    }

    synchronized void newConnection(SendPortIdentifier spi, Channel channel)
            throws IOException {

        if (!(channel instanceof ReadableByteChannel)) {
            throw new IOException("wrong channel type on creating connection");
        }

        addConnection(spi, new ThreadNioDissipator(
                ((NioIbis) ibis).sendReceiveThread(),
                (ReadableByteChannel) channel));
    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
        dissipator.info.close(cause);
    }

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

            dissipator = (ThreadNioDissipator)
                    readyDissipators.dequeue(deadline);

            if (dissipator == null) {
                synchronized (this) {
                    if (closing) {
                        if (connections.size() == 0) {
                            throw new ConnectionClosedException();
                        }
                    }
                }
                throw new ReceiveTimedOutException("deadline passed while"
                        + " selecting dissipator");
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

    synchronized void closing() {
        closing = true;
    }
}
