package ibis.impl.nio;

import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.util.Queue;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;


final class ThreadNioReceivePort extends NioReceivePort
				 implements Config {
    static final int INITIAL_DISSIPATOR_SIZE = 8;

    private ThreadNioDissipator[] connections;
    private int nrOfConnections = 0;

    private ThreadNioDissipator current = null;

    private Queue readyDissipators = new Queue();

    ThreadNioReceivePort(NioIbis ibis, NioPortType type, 
	    String name, Upcall upcall, boolean connectionAdministration,
	    ReceivePortConnectUpcall connUpcall) throws IOException {
	super(ibis, type, name, upcall, connectionAdministration, connUpcall);

	connections = new ThreadNioDissipator[INITIAL_DISSIPATOR_SIZE];
    }

    synchronized void newConnection(NioSendPortIdentifier spi, Channel channel) 
						throws IOException {

	if (!((channel instanceof ReadableByteChannel)
	      && (channel instanceof SelectableChannel))) {
	    throw new IOException("wrong type of channel on"
		    + " creating connection");
	}

	if(nrOfConnections == 0) {
	    notifyAll();
	}

	if (nrOfConnections == connections.length) {
	    ThreadNioDissipator[] newConnections;
	    newConnections = new ThreadNioDissipator[connections.length * 2];
	    for (int i = 0; i < connections.length; i++) {
		newConnections[i] = connections[i];
	    }
	    connections = newConnections;
	}
	connections[nrOfConnections] = 
		new ThreadNioDissipator(ibis.sendReceiveThread(), this,
			    spi, ident, (ReadableByteChannel) channel, type);
	nrOfConnections++;

    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
	for (int i = 0; i < nrOfConnections; i++) {
	    if(dissipator == connections[i]) {
		try {
		    dissipator.close();
		} catch (IOException e) {
		    //IGNORE
		}
		connectionLost(dissipator, cause);
		nrOfConnections--;
		connections[i] = connections[nrOfConnections];
		connections[nrOfConnections] = null;
		if(nrOfConnections == 0) {
		    notifyAll();
		}
		return;
	    }
	}
    }

    NioDissipator getReadyDissipator(long deadline) throws IOException {
	ThreadNioDissipator dissipator;

	synchronized(this) {
	    if(current != null) {
		if(current.dataLeft()) {
		    readyDissipators.enqueue(current);
		}
		current = null;
	    }
	}


	//FIXME: if a connection doesn't close gracefully, we won't notice

	while(true) {

	    if(exitOnNotConnected) {
		synchronized(this) {
		    if (nrOfConnections == 0) {
			return null;
		    }
		}
	    }

	    dissipator = (ThreadNioDissipator) readyDissipators
							    .dequeue(deadline);

	    if (dissipator == null) {
		if(exitOnNotConnected) {
		    synchronized(this) {
			if (nrOfConnections == 0) {
			    return null;
			}
		    }
		}
		throw new ReceiveTimedOutException("deadline passed while"
			+ " selecting dissipator");
	    }

	    try {
		if(dissipator.messageWaiting()) {
		    synchronized(this) {
			current = dissipator;
			return dissipator;
		    }
		}
	    } catch (IOException e) {
		errorOnRead(dissipator, e);
	    }
	}
    }

    void addToReadyList(ThreadNioDissipator dissipator) {
	readyDissipators.enqueue(dissipator);
    }

    synchronized public SendPortIdentifier[] connectedTo() {
	SendPortIdentifier[] result = new SendPortIdentifier[nrOfConnections];
	for (int i = 0; i < nrOfConnections; i++) {
	    result[i] = connections[i].peer;
	}
	return result;
    }

    synchronized void closeAllConnections() {
	for(int i = 0; i < nrOfConnections; i++) {
	    try {
		connections[i].close();
	    } catch (IOException e) {
		//IGNORE
	    }
	}
    }
}
