package ibis.impl.nio;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.util.ArrayList;

abstract class NioReceivePort implements ReceivePort, Runnable, Config {

    protected NioPortType type;
    protected NioIbis ibis;
    private String name;
    NioReceivePortIdentifier ident;
    private Upcall upcall;
    private ReceivePortConnectUpcall connUpcall;

    private boolean connectionAdministration = false;
    private long count = 0;

    private boolean upcallsEnabled = false;
    private boolean connectionsEnabled = false;

    private ArrayList lostConnections = new ArrayList();
    private ArrayList newConnections = new ArrayList();

    private boolean upcallThreadRunning = false;

    private NioReadMessage m = null; // only used when upcalls are off

    protected volatile boolean exitOnNotConnected = false;

    /**
     * Fake readmessage used to indicate someone is trying to get a real
     * message. This makes sure only one thread is trying to receive a new
     * message.
     */
    private final NioReadMessage dummy;

    NioReceivePort(NioIbis ibis, NioPortType type, String name, Upcall upcall, 
	    boolean connectionAdministration, 
	    ReceivePortConnectUpcall connUpcall) throws IOException {

	this.type   = type;
	this.upcall = upcall;
	this.connUpcall = connUpcall;
	this.ibis = ibis;
	this.connectionAdministration = connectionAdministration;

	this.name = name;

	InetSocketAddress address = ibis.factory.register(this);

	ident = new NioReceivePortIdentifier(name, type.name(), 
			(NioIbisIdentifier) type.ibis.identifier(), address);

	ibis.nameServer.bind(name, ident);

	dummy = new NioReadMessage(null, null, -1);

	if(upcall != null) {
	    ThreadPool.createNew(this);
	}

    }

    /**
     * Sees if the user is ok with a new connection from "spi"
     */
    protected boolean connectionRequested(NioSendPortIdentifier spi,
					  Channel channel) {
	boolean allowed;

	synchronized(this) {
	    if(!connectionsEnabled) {
		return false;
	    }
	}

	if(connUpcall != null) {
	    if(!connUpcall.gotConnection(this, spi)) {
		return false;
	    }
	}

	try {
	    newConnection(spi, channel);
	} catch (IOException e) {
	    if(connUpcall != null) {
		connUpcall.lostConnection(this, spi, e);
	    }
	    return false;
	}

	synchronized(this) {
	    if(connectionAdministration) {
		newConnections.add(spi);
	    }
	}

	return true;
    }

    /**
     * Waits for someone to wake us up. waits:
     * - not at all if deadline == -1
     * - until System.getTimeMillis >= deadline if deadline > 0
     * - for(ever) if deadline == 0
     *
     * @returns true we (might have been) notified, or false if the 
     * deadline passed
     */
    private boolean waitForNotify(long deadline) {
	if(deadline == 0) {
	    try {
		wait();
	    } catch (InterruptedException e) {
		//IGNORE
	    }
	    return true;
	} else if (deadline == -1) {
	    return false; //deadline always passed
	}

	long time = System.currentTimeMillis();

	if(time >= deadline) {
	    return false;
	}

	try {
	    wait(deadline - time);
	} catch (InterruptedException e) {
	    //IGNORE
	}
	return true; //don't know if we have been notified, but could be...
    }

    /**
     * Called by the subclass to let us know a connection failed,
     * and we should report this to the user somehow
     */
    void connectionLost(NioDissipator dissipator, Exception cause)  {
	synchronized(this) {
	    if(connectionAdministration) {
		lostConnections.add(dissipator.peer);
	    }
	}

	if(connUpcall != null) {
	    connUpcall.lostConnection(this, dissipator.peer, cause);
	}
    }

    /**
     * gets a new message from the network. Will block until the deadline
     * has passed, or not at all if deadline = -1, or indefinitely if
     * deadline = 0
     *
     */
    private NioReadMessage getMessage(long deadline) throws IOException {
	NioDissipator dissipator;
	long time;
	NioReadMessage message;
	long sequencenr = -1;

	if (DEBUG) {
	    Debug.enter("messages", this, "trying to fetch message");
	}

	if(upcall == null) {
	    synchronized(this) {
		while(m != null) {
		    if(!waitForNotify(deadline)) {
			if (DEBUG) {
			    Debug.exit("messages", this, 
				"!timeout while waiting on previous message");
			}
			throw new ReceiveTimedOutException("previous message"
				+ " not finished yet");
		    }
		}
		m = dummy;  // reserve the global message so no-one will
			    // try to receive a message while we are too.
	    }
	}

	try {
	    dissipator  = getReadyDissipator(deadline);
	} catch (ReceiveTimedOutException e) {
	    if(upcall == null) {
		synchronized(this) {
		    m = null; //give up the lock
		}
	    }
	    if (DEBUG) {
		Debug.exit("messages", this, 
			"!timeout while waiting on dissipator with message");
	    }
	    throw e;
	}

	if(dissipator == null) {
	    synchronized(this) {
		if(exitOnNotConnected) {
		    if (DEBUG) {
			Debug.exit("messages", this, "!no (more) connections");
		    }
		    return null;
		} else {
		    throw new IbisError("getReadyDissipator returned null"); 
		}
	    }
	}

	if(type.sequenced) {
	    sequencenr = dissipator.sis.readLong();
	}

	message = new NioReadMessage(this, dissipator, sequencenr);

	if(upcall == null) {
	    synchronized(this) {
		m = message;
	    }
	}

	if (DEBUG) {
	    Debug.message("messages", this, "new message received (#"
			  + sequencenr + ")");
	}

	return message;
    }
    
    /**
     * called by the readMessage. Finishes message. Also wakes up everyone who 
     * was waiting for it
     */
    synchronized void finish(NioReadMessage m, long messageCount) 
	    throws IOException {

	if (DEBUG) {
	    Debug.message("messages", this, "finishing read message");
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

	//wake up everybody who was waiting for this message to finish
	notifyAll();

	m.isFinished = true;

	count += messageCount;

	if(upcall != null) {

	    //this finish was called from an upcall! Create a new thread to
	    //fetch the next message (this upcall might not exit for a while)
	    ThreadPool.createNew(this);
	}

	if (DEBUG) {
	    Debug.exit("messages", this, "finished read message, received "
		       + messageCount + " bytes");
	}
    }

    /**
     * the message ended on an error. Consider the connection to the SendPort
     * lost. Close it.
     */
    synchronized void finish(NioReadMessage m, Exception e) {
	if (DEBUG) {
	    Debug.message("messages", this, 
			  "!finishing read message with error");
	}

	//inform the subclass an error occured 
	errorOnRead(m.dissipator, e);

	if(upcall == null) {
	    if(this.m == m) {
		this.m = null;
	    }
	}

	//wake up everybody who was waiting for this message to finish
	notifyAll();

	m.isFinished = true;

	if(upcall != null) {

	    //this finish was called from an upcall! Create a new thread to
	    //fetch the next message (this upcall might not exit for a while)
	    ThreadPool.createNew(this);
	}

	if (DEBUG) {
	    Debug.exit("messages", this, "!finished read message with error");
	}
    }

    public ReadMessage receive() throws IOException { 
	return receive(0);
    }

    public ReadMessage receive(long timeoutMillis) 
							throws IOException {
	long deadline, time;
	ReadMessage m = null;

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

	return getMessage(deadline);
    }

    public ReadMessage poll() throws IOException {
	try {
	    return getMessage(-1);
	} catch (ReceiveTimedOutException e) {
	    //IGNORE
	}
	return null;
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
    }

    public synchronized void disableConnections() {
	connectionsEnabled = false;
    }

    public synchronized void enableUpcalls() {
	upcallsEnabled = true;
	notifyAll();
    }

    public synchronized void disableUpcalls() {
	upcallsEnabled = false;
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

    /**
     * Free resourced held by receiport AFTER waiting for all the connections
     * to close down
     */
    public synchronized void close() throws IOException {

	disableConnections();
	ibis.nameServer.unbind(ident.name);
	ibis.factory.deRegister(this);
	exitOnNotConnected = true;
	notifyAll();

	if (upcall == null) {
	    if (m != null) {
		throw new IOException("Message alive while doing close()");
	    }
	    if (getReadyDissipator(0) != null) {
		throw new IOException("Message received while doing close()");
	    }
	} else {
	    while (connectedTo().length > 0) {
		waitForNotify(0);
	    }
	}
	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("nio receiveport close() done");
	}
    }

    /**
     * Free resources geld by receiveport after waiting for all the connections
     * to close down, or the timeout to pass
     */
    public synchronized void forcedClose(long timeoutMillis) {
	long deadline = System.currentTimeMillis() + timeoutMillis;

	try {
	    disableConnections();
	    ibis.nameServer.unbind(ident.name);
	    ibis.factory.deRegister(this);
	    exitOnNotConnected = true;
	    notifyAll();

	    if(upcall == null) {
		if (m != null) {
		    throw new IOException("Message alive while doing"
					  + " forcedClose");
		}
		try {
		    if (getReadyDissipator(deadline) != null) {
			 throw new IOException("Message received while doing"
					       + " forcedClose()");
		    }
		} catch (ReceiveTimedOutException e) {
		    //close all remaining connections
		    closeAllConnections();
		    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
			System.err.println("nio receiveport closed");
		    }
		    return;
		}
	    } else {
		while(connectedTo().length > 0) {
		    if(!waitForNotify(deadline)) {
			//deadline passed, close all remaining connections
			if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
			    System.err.println("nio receiveport closed");
			}
			closeAllConnections();
		    }
		}
	    }
	} catch (IOException e) {
	    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		System.err.println("Exception caught while doing"
			+ " forcedClose(timeout) " + e);
	    }
	}
	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("nio receiveport closed");
	}
    }

    /**
     * close all connections. Don't wait for them to go away by themselves
     */
    public synchronized void forcedClose() throws IOException {
	disableConnections();
	ibis.nameServer.unbind(ident.name);
	ibis.factory.deRegister(this);
	exitOnNotConnected = true;
	notifyAll();
	closeAllConnections();
	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("nio receiveport closed");
	}
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
	return name;
    }

    public void run() {
	NioReadMessage m = null;

	Thread.currentThread().setName(this + " upcall thread");

	while(true) {
	    try {
		m = getMessage(0);
	    } catch (ReceiveTimedOutException e) {
		throw new IbisError("ReceiveTimedOutException caught while"
			+ " doing a getMessage(0)! : " + e);
	    } catch (IOException e) {
		if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		    System.err.println("eek! received error on getMessage(0)"
			+ e);
		}
		continue;
	    }

	    if(m == null) {
		synchronized(this) {
		    if(exitOnNotConnected) {
			if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
			    System.err.println("upcall thread exiting");
			}
			notifyAll();
			return;
		    }
		}
	    }

	    synchronized(this) {
		while(!upcallsEnabled) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			//IGNORE
		    }
		}
	    }

	    try {
		upcall.upcall(m);
	    } catch (IOException e) {
		errorOnRead(m.dissipator, e);
		synchronized(this) {
		    notifyAll();
		    m.isFinished = true;
		}
		continue;
	    }


	    synchronized(this) {
		if(m.isFinished) {
		    // a new thread was started to handle the next message,
		    // exit
		    return;
		}
	    }

	    //don't hold lock during calls into dissipator
	    long messageCount = m.dissipator.bytesRead();
	    m.dissipator.resetBytesRead();

	    synchronized(this) {
		//implicitly finish message
		notifyAll();
		m.isFinished = true;
	    }
	}
    }

    /**
     * A new connection has been established.
     */
    abstract void newConnection(NioSendPortIdentifier spi, 
				Channel channel) throws IOException;

    abstract void errorOnRead(NioDissipator dissipator, Exception cause);

    /**
     * Searches for a dissipator with a message waiting
     *
     * Will block until the deadline has passed, or not at all if 
     * deadline = -1, or indefinitely if deadline = 0
     *
     * @param deadline the deadline after which searching has failed
     *
     * @return a dissipator with a message waiting, or null if there are
     *	       no connections and "exitOnNotConnected" = true
     *
     * @throws ReceiveTimedOutException If no connections are ready after
     *         the deadline has passed
     */
    abstract NioDissipator getReadyDissipator(long deadline) throws IOException;

    /**
     * Generate an array of all connections to this receiveport
     */
    public abstract SendPortIdentifier[] connectedTo(); 

    /**
     * Forcibly close all open connections. Doesn't update administration or
     * generate upcalls.
     */
    abstract void closeAllConnections();

}
