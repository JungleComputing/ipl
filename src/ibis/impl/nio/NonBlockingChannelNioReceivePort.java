package ibis.impl.nio;

import java.io.IOException;

import java.util.Iterator;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ReadableByteChannel;

import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.Upcall;
import ibis.ipl.SendPortIdentifier;

final class NonBlockingChannelNioReceivePort extends NioReceivePort
					     implements Config {
    static final int INITIAL_ARRAY_SIZE = 8;

    private NonBlockingChannelNioDissipator[] connections;
    private int nrOfConnections = 0;

    private NonBlockingChannelNioDissipator[] pendingConnections;
    private int nrOfPendingConnections = 0;

    Selector selector;

    NonBlockingChannelNioReceivePort(NioIbis ibis, NioPortType type, 
	    String name, Upcall upcall, boolean connectionAdministration,
	    ReceivePortConnectUpcall connUpcall) throws IOException {
	super(ibis, type, name, upcall, connectionAdministration, connUpcall);

	connections = new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];
	pendingConnections =
			new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];

	selector = Selector.open();
    }

    synchronized void newConnection(NioSendPortIdentifier spi, Channel channel) 
						throws IOException {
	NonBlockingChannelNioDissipator dissipator;

	if (!((channel instanceof ReadableByteChannel)
	      && (channel instanceof SelectableChannel))) {
	    throw new IOException("wrong type of channel on"
		    + " creating connection");
	}

	SelectableChannel sh = (SelectableChannel) channel;
	dissipator = new NonBlockingChannelNioDissipator(spi, ident, 
					  (ReadableByteChannel) channel, type);

	if (nrOfConnections == connections.length) {
	    NonBlockingChannelNioDissipator[] newConnections;
	    newConnections = new NonBlockingChannelNioDissipator[
						    connections.length * 2];
	    for (int i = 0; i < connections.length; i++) {
		newConnections[i] = connections[i];
	    }
	    connections = newConnections;
	}
	connections[nrOfConnections] = dissipator;
	nrOfConnections++;

	if (nrOfPendingConnections == pendingConnections.length) {
	    NonBlockingChannelNioDissipator[] newPendingConnections;
	    newPendingConnections = new NonBlockingChannelNioDissipator[
					    pendingConnections.length * 2];

	    for (int i = 0; i < pendingConnections.length; i++) {
		newPendingConnections[i] = pendingConnections[i];
	    }
	    pendingConnections = newPendingConnections;
	}
	pendingConnections[nrOfPendingConnections] = dissipator;
	nrOfPendingConnections++;

	// wake up selector if needed
	selector.wakeup();

	notifyAll();
    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
	for (int i = 0; i < nrOfPendingConnections; i++) {
	    if(dissipator == pendingConnections[i]) {
		nrOfPendingConnections--;
		pendingConnections[i] = pendingConnections[
							nrOfPendingConnections];
		pendingConnections[nrOfPendingConnections] = null;
	    }
	}
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
		    selector.wakeup();
		    notifyAll();
		}
		return;
	    }
	}
    }

    synchronized void registerPendingConnections() throws IOException {
	SelectableChannel sh;

	for (int i = 0; i < nrOfPendingConnections; i++) {
	    sh = (SelectableChannel) pendingConnections[i].channel;
	    
	    sh.register(selector, SelectionKey.OP_READ, pendingConnections[i]);
	    pendingConnections[i] = null;
	}
	nrOfPendingConnections = 0;
    }

    NioDissipator getReadyDissipator(long deadline) throws IOException {
	boolean deadlinePassed = false;
	boolean firstTry = true;
	long time;
	Iterator keys;
	SelectionKey key;
	NonBlockingChannelNioDissipator dissipator = null;

	while(!deadlinePassed) {
	    synchronized(this) {
		registerPendingConnections();

		if(nrOfConnections == 0) {
		    if (exitOnNotConnected) {
			return null;
		    } else {
			if (deadline == -1) {
			    deadlinePassed = true;;
			    continue;
			} else if (deadline == 0) {
			    try {
				if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
				    System.err.println("wait()ing for a"
					    + " connection");
				}
				wait();
			    } catch (InterruptedException e) {
				//IGNORE
			    }
			    continue;
			} else {
			    time = System.currentTimeMillis();
			    if(time >= deadline) {
				deadlinePassed = true;
			    } else {
				try {
				    if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
					System.err.println("wait()ing for a"
						+ " connection");
				    }
				    wait();
				} catch (InterruptedException e) {
				}
				continue;
			    }
			}
		    }
		}

		/*
		if(firstTry && nrOfConnections == 1 && !type.manyToOne) {
		    //optimisticly do a single receive, to avoid
		    //the select statement below if possible
		    try {
			connections[0].readFromChannel();
		    } catch (IOException e) {
			errorOnRead(connections[0], e);
		    }
		    firstTry = false;
		}
		*/

		for (int i = 0; i < nrOfConnections; i++) {
		    try {
			if(connections[i].messageWaiting()) {
			    return connections[i];
			}
		    } catch (IOException e) {
			errorOnRead(connections[i], e);
			i--;
		    }
		}

	    }

	    if (deadline == -1) {
		selector.selectNow();
		deadlinePassed = true;
	    } else if (deadline == 0) {
		selector.select();
	    } else {
		time = System.currentTimeMillis();
		if (time >= deadline) {
		    deadlinePassed = true;
		} else {
		    selector.select(deadline - time);
		}
	    }

	    keys = selector.selectedKeys().iterator();

	    while(keys.hasNext()) {
		key = (SelectionKey) keys.next();
		dissipator = (NonBlockingChannelNioDissipator) key.attachment();

		try {
		    dissipator.readFromChannel();
		} catch (IOException e) {
		     errorOnRead(dissipator, e);
		}
	    }
	    selector.selectedKeys().clear();
	}
	throw new ReceiveTimedOutException("timeout while waiting"
					   + " for dissipator");
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
