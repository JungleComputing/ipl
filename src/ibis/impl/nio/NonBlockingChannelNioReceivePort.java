package ibis.impl.nio;

import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

final class NonBlockingChannelNioReceivePort extends NioReceivePort
					     implements Config {
    static final int INITIAL_ARRAY_SIZE = 8;

    private NonBlockingChannelNioDissipator[] connections;
    private int nrOfConnections = 0;

    private NonBlockingChannelNioDissipator[] pendingConnections;
    private int nrOfPendingConnections = 0;

    private boolean closing = false;

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

	if (DEBUG) {
	    Debug.enter("connections", this, "registering new connection");
	}

	if (!((channel instanceof ReadableByteChannel)
	      && (channel instanceof SelectableChannel))) {
	    if (DEBUG) {
		Debug.exit("connections", this, "!wrong channel type");
	    }
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

	if(DEBUG) {
	    Debug.message("connections", this, "waking up selector");
	}

	// wake up selector if needed
	selector.wakeup();

	if(nrOfConnections == 1) {
	    notifyAll();
	}

	if (DEBUG) {
	    Debug.exit("connections", this, "registerred new connection");
	}
    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
	if(DEBUG) {
	    Debug.enter("connections", this, "!lost connection: " + cause);
	}

	for (int i = 0; i < nrOfPendingConnections; i++) {
	    if(dissipator == pendingConnections[i]) {
		nrOfPendingConnections--;
		pendingConnections[i] = pendingConnections[
							nrOfPendingConnections];
		pendingConnections[nrOfPendingConnections] = null;
		if(DEBUG) {
		    Debug.message("connections", this,
				  "lost connection removed from pending list");
		}
	    }
	}

	for (int i = 0; i < nrOfConnections; i++) {
	    if(dissipator == connections[i]) {
		try {
		    dissipator.reallyClose();
		} catch (IOException e) {
		    //IGNORE
		}
		connectionLost(dissipator, cause);
		nrOfConnections--;
		connections[i] = connections[nrOfConnections];
		connections[nrOfConnections] = null;

		if(nrOfConnections == 0) {
		    if (DEBUG) {
			Debug.message("connections", this,
				  "no more connections, waking up selector");
		    }
		    selector.wakeup();
		}
		if(DEBUG) {
		    Debug.exit("connections", this, "removed connection");
		}
		return;
	    }
	}
	if(DEBUG) {
	    Debug.exit("connections", this, "!lost connection not found");
	}
    }

    synchronized void registerPendingConnections() throws IOException {
	SelectableChannel sh;

	if (DEBUG && nrOfPendingConnections > 0) {
	    Debug.message("connections", this, "registerring " 
			  + nrOfPendingConnections + " connections");
	}

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

	if (DEBUG) {
	    Debug.enter("connections", this, "trying to find a dissipator"
		    + " with a message waiting");
	}

	while(!deadlinePassed) {
	    registerPendingConnections();

	    synchronized(this) {
		if(nrOfConnections == 0) {
		    if (closing) {
			if (DEBUG) {
			    Debug.exit("connections", this, "!exiting "
				    + "because we have no connections (as requested)");
			}
			throw new ConnectionClosedException();
		    } else {
			if (deadline == -1) {
			    deadlinePassed = true;;
			    continue;
			} else if (deadline == 0) {
			    try {
				if (DEBUG) {
				    Debug.message("connections", this,
					    " wait()ing for a connection");
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
				    if (DEBUG) {
					Debug.message("connections", this,
						"wait()ing for a connection");
				    }
				    wait();
				} catch (InterruptedException e) {
				}
				continue;
			    }
			}
		    }
		}

		if(firstTry && nrOfConnections == 1) {
		    //optimisticly do a single receive, to avoid
		    //the select statement below if possible
		    try {
			connections[0].readFromChannel();
		    } catch (IOException e) {
			errorOnRead(connections[0], e);
		    }
		    firstTry = false;
		}

		for (int i = 0; i < nrOfConnections; i++) {
		    try {
			if(connections[i].messageWaiting()) {
			    if (DEBUG) {
				Debug.exit("connections", this,
					"returning connection " + i);
			    }
			    return connections[i];
			}
		    } catch (IOException e) {
			errorOnRead(connections[i], e);
			i--;
		    }
		}
	    } // end of synchronized block

	    if (deadline == -1) {
		if (DEBUG) {
		    Debug.message("connections", this, "doing a selectNow");
		}
		try {
		    selector.selectNow();
		} catch (IOException e) {
		    //IGNORE
		}
		deadlinePassed = true;
	    } else if (deadline == 0) {
		if (DEBUG) {
		    Debug.message("connections", this, "doing a select() on "
			    + selector.keys().size() + " connections");
		}
		try {
		    selector.select();
		} catch (IOException e) {
		    Debug.message("connections", this, 
			    "!error on select: " + e);
		    //IGNORE
		}
	    } else {
		time = System.currentTimeMillis();
		if (time >= deadline) {
		    deadlinePassed = true;
		} else {
		    if (DEBUG) {
			Debug.message("connections", this, 
				"doing a select(timeout)");
		    }
		    try {
			selector.select(deadline - time);
		    } catch (IOException e) {
			Debug.message("connections", this, 
				"!error on select: " + e);
			//IGNORE
		    }
		}
	    }

	    if (DEBUG) {
		Debug.message("connections", this,
			"selected " + selector.selectedKeys().size() 
			+ " connections");
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
	} // end of while(!deadlinePassed)
	if (DEBUG) {
	    Debug.exit("connections", this, "!deadline passed");
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

    synchronized void closing() {
	closing = true;
    }

    synchronized void closeAllConnections() {
	for(int i = 0; i < nrOfConnections; i++) {
	    try {
		connections[i].reallyClose();
	    } catch (IOException e) {
		//IGNORE
	    }
	}
    }
}
