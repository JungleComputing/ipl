package ibis.impl.nio;

import java.lang.InterruptedException;

import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;

import ibis.io.SerializationInputStream;
import ibis.io.SunSerializationInputStream;

import ibis.util.ThreadPool;

final class NioReceivePort implements ReceivePort, NioProtocol, 
							    Runnable, Config {

    //class to keep track of a single connection
    private static class Connection {
	SerializationInputStream in;
	NioSendPortIdentifier spi;
	NioInputStream nis;
	Channel channel;

	Connection(SerializationInputStream in,
		NioSendPortIdentifier spi,
		NioInputStream nis,
		Channel channel) {
	    this.in = in;
	    this.spi = spi;
	    this.nis = nis;
	    this.channel = channel;
	}
    }


    NioPortType type;
    private NioIbis ibis;
    private String name;
    NioReceivePortIdentifier ident;
    private Upcall upcall;
    private ReceivePortConnectUpcall connUpcall;

    //Connection[], list of all connections
    private ArrayList connections = new ArrayList();

    //SendPortIdentifier[], list of all lost connections(for user)
    private ArrayList lostConnections = new ArrayList();

    //SendPortIdentifier[], list of all new connections(for user)
    private ArrayList newConnections = new ArrayList();

    //Connection[], list of all connections which need to be added to
    //the selector
    private ArrayList pendingConnections = new ArrayList();

    private boolean connectionAdministration = false;
    private Selector selector;
    private long count;

    private boolean upcallsEnabled = false;
    private boolean connectionsEnabled = false;

    //set when free() is called.
    private boolean dying = false;

    /**
     * Lock used to keep two threads from getting a message at the same time
     * used in getMessage and finish(message)
     */
    private boolean locked = false;

    private NioReadMessage m = null; // only used when upcalls are off

    NioReceivePort(NioIbis ibis, NioPortType type, String name, Upcall upcall, 
	    boolean connectionAdministration, 
	    ReceivePortConnectUpcall connUpcall) throws IOException {

	this.type   = type;
	this.upcall = upcall;
	this.connUpcall = connUpcall;
	this.ibis = ibis;
	this.connectionAdministration = connectionAdministration;

	//FIXME : what if there are 2 anonymous ports?
	if(name == null) {
	    this.name = "anonymous";
	} else {
	    this.name = name;
	}

	InetSocketAddress address = ibis.factory.register(this);

	ident = new NioReceivePortIdentifier(name, type.name(), 
			(NioIbisIdentifier) type.ibis.identifier(), address);

	ibis.nameServer.bind(name, ident);

	selector = Selector.open();

	//start a thread to handle upcall's (if needed)
	//FIXME: does this thread eat cpu when no-one's connected?
	if(upcall != null) {
	    ThreadPool.createNew(this);
	}

    }

    private boolean connectionAllowed(NioSendPortIdentifier spi) {
	boolean allowed = false;

	synchronized(this) {
	    if(!connectionsEnabled) {
		return false;
	    }
	}

	if(connUpcall != null) {
	    return connUpcall.gotConnection(this, spi);
	} else {
	    return true;
	}
    }

    /**
     * Called by the factory to let us know a new connection has been
     * set up, returns false if we disagree.
     */
    boolean newConnection(NioSendPortIdentifier spi, 
	    Channel channel) throws IOException {
	SerializationInputStream in;
	NioInputStream nis;

	if(!connectionAllowed(spi)) {
	    return false;
	}

	if(!((channel instanceof ScatteringByteChannel)
	     && (channel instanceof SelectableChannel))) {
	     throw new IbisError("Wrong type of channel given by factory");
	}


	Connection connection = new Connection(null, spi, null, channel);

	synchronized(this) {
	    connections.add(connection);

	    if(connectionAdministration) {
		newConnections.add(spi);
	    }

	    pendingConnections.add(connection);

	    //wake up the selector. This wil make the reader 
	    //thread notice this connection
	    selector.wakeup();

	    //register the channel with the selector, and attach the connection

	}
	return true;
    }

    /**
     * creates a new SerializationInputStream.
     */
    private void newInputStream(Connection connection) throws IOException {
	long result;

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("creating new nis/in pair");
	}

	if(connection.in != null) {
	    throw new IbisError("tried to create a new inputstream, but we"
		    + " already have one!");
	}

	switch(type.serializationType) {
	    case NioPortType.SERIALIZATION_SUN:
		connection.nis = new NioInputStream(
				(ScatteringByteChannel) connection.channel);
		connection.in = new SunSerializationInputStream(connection.nis);
		break;
	    case NioPortType.SERIALIZATION_NONE:
		connection.nis = new NioInputStream(
				(ScatteringByteChannel) connection.channel);
		connection.in = connection.nis;
		break;
	    case NioPortType.SERIALIZATION_IBIS:
		connection.nis = null;
		connection.in = new NioIbisSerializationInputStream(
				(ScatteringByteChannel) connection.channel);
		break;
	    default:
		throw new IbisError("Unknown serializationtype in NioIbis");
	}
    }

    /**
     * Shuts down a connection. Called from getMessage() and the ChannelFactory.
     * Only updates connection information, does not close in, nis, or channel
     */
    void lostConnection(NioSendPortIdentifier spi, Channel channel,
					Exception cause) {
	Connection temp = null;

	synchronized(this) {
	    for(int i = 0;i < connections.size(); i++) {
		temp = (Connection) connections.get(i);
		if(spi == temp.spi && channel == temp.channel) {
		    connections.remove(i);
		    temp.channel = null; // make the connection invalid
		    if(connectionAdministration) {
			lostConnections.add(spi);
		    }
		    break;
		}
		temp = null;
	    }
	}

	if(temp == null) {
	   throw new IbisError("Tried to remove an non-registerred connection");
	}

	if(connUpcall != null) {
	    connUpcall.lostConnection(this, spi, cause);
	}
    }

    /**
     * Does a single select, or no select when there is an connection already
     * waiting
     */
    private Connection doSelect(long deadline) throws IOException{
	SelectionKey key;
	Iterator keys;
	long time;

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("doing select");
	}

	synchronized(this) {
	    //add pending connections to selector
	    if(!pendingConnections.isEmpty()) {
		Connection temp;
		for(int i = 0; i < pendingConnections.size(); i++) {
		    temp = (Connection) pendingConnections.get(i);

		    if(temp.channel != null) {
			if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
			    System.err.println("Adding connection to"
				    + " selector");
			}
			((SelectableChannel) temp.channel)
			    .register(selector, SelectionKey.OP_READ,temp);
		    }
		}
		pendingConnections.clear();
	    }

	    //look for an already selected channel
	    keys = selector.selectedKeys().iterator();
	    while(keys.hasNext()) {
		key = (SelectionKey) keys.next();
		if(!key.isValid()) {
		    selector.selectedKeys().remove(key);
		} else {
		    if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
			System.err.println("chose already selected channel");
		    }
		    return (Connection) key.attachment();
		}
	    }

	}

	//no selected channel found, do a new select
	if (deadline == 0) {
	    selector.select();
	} else if (deadline == -1) {
	    selector.selectNow();
	} else {
	    time = System.currentTimeMillis();
	    if(deadline <= time) {
		selector.selectNow();
	    } else {
		selector.select(deadline - time);
	    }
	}

	//look if there's one now... 
	keys = selector.selectedKeys().iterator();
	while(keys.hasNext()) {
	    key = (SelectionKey) keys.next();
	    if(!key.isValid()) {
		selector.selectedKeys().remove(key);
	    } else {
		if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
		    System.err.println("chose newly selected channel");
		}
		return (Connection) key.attachment();
	    }
	}

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("select failed to find anything");
	}
	return null;
    }

    /**
     * gets a new message from the network. Will block until the deadline
     * has passed, or not at all if deadline = -1, or indefinitely if
     * deadline = 0
     */
    private NioReadMessage getMessage(long deadline) throws IOException {
	Connection connection = null;
	byte command;
	boolean gotLock = false; // does THIS thread own the lock?
	long time;

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("Receiveport trying to fetch message");
	}

	while(true) {
	    synchronized(this) {
		if(connections.isEmpty() && dying) {
		    throw new IOException("no more receiveports left,"
			    + " and free/close was called for this port");
		}

		//try to obtain lock
		if(!gotLock) {
		    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
			System.err.println("getting lock");
		    }

		    while(m != null || locked) {
			//message alive, or somebody else has the lock
			if(deadline == 0) {
			    try {
				wait();
			    } catch (InterruptedException e) {
				//IGNORE
			    }
			} else if (deadline == -1) {
			    throw new ReceiveTimedOutException("message"
				    + " still alive while doing poll");
			} else {
			    time = System.currentTimeMillis();
			    if(time >= deadline) { 
				throw new ReceiveTimedOutException("message"
				    + " still alive after deadline passed");
			    } else {
				try { 
				    wait(deadline - time);
				} catch (InterruptedException e) {
				    //IGNORE
				}
			    }
			}
		    }
		    locked = true;  // set the (global) lock 
		    gotLock = true; // and remember we've got it

		    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
			System.err.println("got lock");
		    }
		}

	    }

	    connection = doSelect(deadline);
	    
	    if(connection == null) {  
		if(deadline == 0) {
		    //just try again
		    continue;
		} else if (deadline == -1) {
		    synchronized(this) {
			locked = false; // give up the lock
		    }
		    throw new ReceiveTimedOutException("no data available in"
			    + " a channel right now");
		} else {
		    time = System.currentTimeMillis();
		    if(time >= deadline) {
			synchronized(this) {
			    locked = false; //give up the lock
			}
			throw new ReceiveTimedOutException("timeout on"
				+ " selecting a channel");
		    } else {
			//try again
			continue;
		    }
		}
	    }

	    if(connection.in == null) {
		//create a stream to read from first
		newInputStream(connection);
	    }

	    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
		System.err.println("fetching command from the input stream");
	    }
	 
	    command = connection.in.readByte();

	    switch(command) {
		case NEW_RECEIVER:
		    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
			System.err.println("received NEW_RECEIVER, closing"
				+ " down input stream");
		    }
		    connection.in.close();
		    connection.in = null;
		    break;
		case NEW_MESSAGE:
		    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
			System.err.println("received NEW_MESSAGE");
		    }
		    //don't give up the lock, we will keep it until the
		    //finish() of this message
		    return new NioReadMessage(this, connection.in,
				connection.nis, connection.spi);
		case CLOSE_CONNECTION:
		    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
			System.err.println("received CLOSE_CONNECTION,"
				+ " closing down input stream and channel");
		    }
		    IOException e =
			new IOException("Sendport closed connection");
		    connection.in.close();
		    ibis.factory.recycle(connection.spi, connection.channel);
		    lostConnection(connection.spi, connection.channel, e);
		    break;
		default:
		    throw new IbisError("unknown opcode in command");
	    }
	}
    }

    /**
     * called by the readMessage. Finishes message. Also wakes up everyone who 
     * was waiting for it
     */
    synchronized long finish(NioReadMessage m) throws IOException {
	long messageCount;
	SelectionKey key;

	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("finishing message");
	}

	if(m.isFinished) {
	    throw new IOException("finish called twice on a message!");
	}

	if(upcall == null) {
	    if(this.m != m) {
		throw new IOException("finish called on non-current message");
	    }

	    //no (global)message alive
	    this.m = null;
	}

	locked = false; //give up lock so a new message can be fetched

	//wake up everybody who was waiting for this message to finish
	notifyAll();

	m.isFinished = true;

	//update count
	if(m.nis != null) {
	    messageCount = m.nis.getCount();
	    m.nis.resetCount();

	} else {
	    //if there's no nis, we have ibis serialization
	    messageCount = ((NioIbisSerializationInputStream) m.in).getCount();
	    ((NioIbisSerializationInputStream) m.in).resetCount();
	}
	
	count += messageCount;

	if(m.in.available() <= 0 && (m.nis == null || m.nis.available() <= 0)) {
	    //no more bytes waiting for this connection, remove it
	    //from the selected connections
	    if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
		System.err.println("removing connection from selected"
			+ " key set");
	    }

	    if(m.nis != null) {
		key = ((SelectableChannel) m.nis.channel).
		    keyFor(selector);
	    } else {
		NioIbisSerializationInputStream nisis =
		    (NioIbisSerializationInputStream) m.in;

		key = ((SelectableChannel) nisis.channel).
		    keyFor(selector);
	    }

	    selector.selectedKeys().remove(key);
	}


	if(upcall != null) {

	    //this finish was called from an upcall! Create a new thread to
	    //fetch the next message (this upcall might not exit for a while)
	    ThreadPool.createNew(this);
	}

	return count;
    }

    public ReadMessage receive() throws IOException { 
	return receive(0);
    }

    public ReadMessage receive(ReadMessage finishMe) 
							throws IOException { 
	throw new IbisError("receive(finishMe) not implemented in nio ibis");
    }

    public ReadMessage receive(long timeoutMillis) 
							throws IOException {
	long deadline, time;

	if(upcall != null) {
	    throw new IOException("explicit receive not allowed with upcalls");
	}

	if(timeoutMillis < 0) {
	    throw new IOException("timeout must be a non-negative number");
	} else if (timeoutMillis > 0) {
	    deadline = System.currentTimeMillis() + timeoutMillis;
	} else { // timeoutMillis == 0
	    deadline = 0;
	}

	m =  getMessage(deadline);

	return m;
    }

    public ReadMessage receive(ReadMessage finishMe, 
				    long timeoutMillis) throws IOException {
	throw new IbisError("receive(finishMe, timeout) not implemented in nio ibis");
    }

    public ReadMessage poll() throws IOException {
	try {
	    return getMessage(-1);
	} catch (ReceiveTimedOutException e) {
	    //no message available
	    return null;
	}
    }

    public ReadMessage poll(ReadMessage finishMe) 
	throws IOException {
	    throw new IbisError("poll(finishMe) not implemented in nio ibis");
	}

    public synchronized long getCount() {
	return count;
    }

    public synchronized void resetCount() {
	count = 0;
    }

    public DynamicProperties properties() {
	return null;
    }

    public ReceivePortIdentifier identifier() {
	return ident;
    }
    
    public String name() {
	return name;
    }

    public synchronized void enableConnections() {		
	connectionsEnabled = true;
	notifyAll(); // wake up any threads waiting.
    }

    public synchronized void disableConnections() {
	connectionsEnabled = false;
    }

    public synchronized void enableUpcalls() {
	upcallsEnabled = true;
    }

    public synchronized void disableUpcalls() {
	upcallsEnabled = false;
    }

    /**
     * frees all the resources used by this receiport AFTER waiting for all
     * the connections to this receiveport to close
     *
     * @return if the free succeeded or not (it may time-out)
     */
    private boolean free(long timeout) throws IOException {
	long deadline;

	if(timeout == 0) {
	    deadline = 0;
	} else {
	    deadline = System.currentTimeMillis() + timeout;
	}

	synchronized(this) {
	    if(this.m != null) {
		throw new IOException("free() called with message active");
	    }

	    dying = true; // make the getMessage functions throw an exception
			  // when connections.isEmpty()

	    if(upcall == null) {
		connectionsEnabled = false;
	    }
	}

	//wait for zero connections
	if(upcall == null) {
	    try {
		getMessage(deadline);
	    } catch (ReceiveTimedOutException e) {
		return false; // we didn't succeed
	    } catch (IOException e) {
		synchronized(this) {
		    if(!connections.isEmpty()) {
			throw new IOException("eek! error received while"
				+ " waiting for all connections to close");
		    }
		}
		ibis.nameServer.unbind(ident.name);
		ibis.factory.deRegister(this);
		return true;
	    }
	    throw new IOException("message received while waiting for all"
		    + " the connections to close");
	} else {
	    synchronized(this) {
		while(!connections.isEmpty()) {
		    if(deadline == 0) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    //IGNORE
			}
		    } else {
			long time = System.currentTimeMillis();  
			if(time >= deadline) {
			    return false;
			} else {
			    try {
				wait(deadline - time);
			    } catch (InterruptedException e) {
				//IGNORE
			    }
			}
		    }
		}
		ibis.nameServer.unbind(ident.name);
		ibis.factory.deRegister(this);
		return true;
	    }
	}
    }


    /**
     * Free resourced held by receiport AFTER waiting for all the connections
     * to close down
     */
    public void free() throws IOException {
	free(0);
    }


    /**
     * Free resources geld by receiveport after waiting for all the connections
     * to close down, or the timeout to pass
     */
    public void forcedClose(long timeoutMillis) {
	try {
	    if(!free(timeoutMillis)) {
		forcedClose();
	    }
	} catch (IOException e) {
	    System.err.println("EEK! received exception in forcedClose(t)");
	    //IGNORE
	}
    }


    /**
     * close all connections. Don't wait for them to go away by themselves
     */
    public void forcedClose() throws IOException {
	Connection temp;

	synchronized(this) { 
	    connectionsEnabled = false;
	    upcallsEnabled = false;

	    for (int i = 0; i < connections.size(); i++) {
		temp = (Connection) connections.get(i);
		try { 
		    IOException e =
			new IOException("Sendport closed connection");
		    temp.in.close();
		    lostConnection(temp.spi, temp.channel, e);
		} catch (IOException e) {
		    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
			System.err.println("EEK! received exception in forcedClose()");
		    }
		    //IGNORE
		}
	    }
	}
	ibis.nameServer.unbind(ident.name);
	ibis.factory.deRegister(this);
    }

    public synchronized SendPortIdentifier[] connectedTo() {
	SendPortIdentifier[] result =
	    new SendPortIdentifier[connections.size()];
	for(int i = 0; i < result.length; i++) {
	    result[i] = ((Connection) connections.get(i)).spi;
	}
	return result;
    }

    public synchronized SendPortIdentifier[] lostConnections() {
	SendPortIdentifier[] result;

	result = (SendPortIdentifier[]) lostConnections.toArray();
	lostConnections.clear();

	return result;
    }

    public synchronized SendPortIdentifier[] newConnections() {
	SendPortIdentifier[] result;

	result = (SendPortIdentifier[]) newConnections.toArray();
	newConnections.clear();

	return result;
    }

    public int hashCode() {
	return name.hashCode();
    }

    public boolean equals(Object obj) {
	if(obj instanceof NioReceivePort) {
	    NioReceivePort other = (NioReceivePort) obj;
	    return name.equals(other.name);
	} else if (obj instanceof String) {
	    String s = (String) obj;
	    return s.equals(name);
	} else {
	    return false;
	}
    }

    public String toString() {
	return (ibis + " " + name);
    }

    public void run() {
	NioReadMessage m = null;

	while(true) {

	    try {
		m = getMessage(0);
	    } catch (IOException e) {
		if(!dying) {
		    System.err.println("exception thrown while"
			    + " receiving message: " + e);
		    e.printStackTrace();
		}
	    }

	    synchronized(this) {

		if(dying) {
		    // time to go, exit
		    return;
		}

		if(m == null) {
		    continue;
		}

		while(!upcallsEnabled) {
		    try {
			wait(0);
		    } catch (InterruptedException e) {
			//IGNORE
		    }
		}
	    }

	    try {
		upcall.upcall(m);
	    } catch (IOException e) {
		//IGNORE
	    }

	    if(m.isFinished) {
		// a new thread was started to handle the next message,
		// exit
		return;
	    } else {
		//finish message
		try {
		    finish(m);
		} catch (IOException e) {
		    System.err.println("exception thrown while"
			    + " finishing message: " + e);
		    e.printStackTrace();
		}
	    }
	}
    }
}
