package ibis.impl.nio;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.io.NoSerializationOutputStream;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIOException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Replacer;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.util.DummyOutputStream;
import ibis.util.OutputStreamSplitter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;

final class NioSendPort implements SendPort, Config, NioProtocol {

    //class to keep track of a single connection
    private static class Connection {
	NioReceivePortIdentifier rpi;
	GatheringByteChannel channel;

	Connection(NioReceivePortIdentifier rpi,
		GatheringByteChannel channel) {
	    this.rpi = rpi;
	    this.channel = channel;
	}
    }


    private NioPortType type;
    private NioSendPortIdentifier ident;
    private boolean aMessageIsAlive = false;
    private NioIbis ibis;
    private NioChannelSplitter splitter;
    private NioOutputStream nos = null;
    private SerializationOutputStream out = null;
    private ArrayList receivers = new ArrayList();
    private NioWriteMessage message;
    private boolean connectionAdministration;
    private SendPortConnectUpcall connectUpcall;
    private ArrayList lostConnections = new ArrayList();
    private Replacer replacer = null;
    private long count = 0; //number of byte send since last resetCount();

    NioSendPort(NioIbis ibis, NioPortType type, String name,
	    boolean connectionAdministration, SendPortConnectUpcall cU)
							throws IOException {
	this.type = type;
	this.ibis = ibis;
	this.connectionAdministration = connectionAdministration;
	this.connectUpcall = cU;
	if(cU != null) connectionAdministration = true;

	ident = new NioSendPortIdentifier(name, type.name(), 
		(NioIbisIdentifier) type.ibis.identifier());

	splitter = new NioChannelSplitter(); 

    }

    public synchronized void connect(ReceivePortIdentifier receiver, long timeoutMillis) throws IOException {
	/* first check the types */
	if(!type.name().equals(receiver.type())) {
	    throw new PortMismatchException("Cannot connect ports of "
						    + "different PortTypes");
	}

	if(aMessageIsAlive) {
	    throw new IOException("A message was alive while adding a new "
							    + "connection");
	}

	if(timeoutMillis < 0) {
	    throw new IOException("NioSendport.connect(): timeout must be positive, or 0");
	}

	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("Sendport " + this + " '" +  ident.name +
		    "' connecting to " + receiver); 
	}

	NioReceivePortIdentifier rpi = (NioReceivePortIdentifier) receiver;

	// make the connection. Will throw an Exception if if failed
	Channel channel = ibis.factory.connect(this.ident, rpi,
							timeoutMillis);

	if(ASSERT) {
	    if(!(channel instanceof GatheringByteChannel)) {
		throw new IbisError("factory returned wrong type of channel");
	    }
	}

	// we have a new receiver, now add it to our tables.
	Connection connection = 
	    new Connection(rpi, (GatheringByteChannel) channel);

	//if this is not the first receiver, reset the output stream
	if (out != null) { 

	    out.writeByte(NEW_RECEIVER);

	    out.flush();
	    out.close();

	    out = null;
	}

	receivers.add(connection);
	splitter.add((GatheringByteChannel) connection.channel);

	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.err.println("Sendport '" + ident.name + "' connecting to " + receiver + " done"); 
	}
    }

    public void connect(ReceivePortIdentifier receiver) throws IOException {
	connect(receiver, 0);
    }

    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
	/* Niels: TODO! */
    }

    /**
     * Create a new nos/out pair.
     */
    private void newOutputStream() throws IOException {
	switch(type.serializationType) {
	    case NioPortType.SERIALIZATION_SUN:
		nos = new NioOutputStream(splitter);
		out = new SunSerializationOutputStream(nos);
		if (replacer != null) out.setReplacer(replacer);
		break;
	    case NioPortType.SERIALIZATION_NONE:
		nos = new NioOutputStream(splitter);
		out = nos;
		break;
	    case NioPortType.SERIALIZATION_IBIS:
	    case NioPortType.SERIALIZATION_DATA:
		out = new NioIbisSerializationOutputStream(splitter);
		if (replacer != null) out.setReplacer(replacer);
		break;
	    default:
		System.err.println("EEK, serialization type unknown");
		System.exit(1);
	}
    }

    public void setReplacer(Replacer r) {
	replacer = r;
	if (out != null) out.setReplacer(r);
    }

    public synchronized ibis.ipl.WriteMessage newMessage() throws IOException { 
	if(receivers.size() == 0) {
	    throw new IbisIOException("port is not connected");
	}

	while(aMessageIsAlive) {
	    try {
		wait();
	    } catch(InterruptedException e) {
		// Ignore.
	    }
	}
	aMessageIsAlive = true;

	if(out == null) {
	    //set up a stream to send data through
	    newOutputStream();

	    message = new NioWriteMessage(this, out);
	}

	out.writeByte(NEW_MESSAGE);
	return message;
    }

    synchronized long finish() {
	long messageCount = splitter.getCount();
	count += messageCount;
	splitter.resetCount();

	aMessageIsAlive = false;

	notifyAll();

	return messageCount;
    }

    public DynamicProperties properties() {
	return null;
    }

    public String name() {
	return ident.name;
    }

    public SendPortIdentifier identifier() {
	return ident;
    }

    public void close() throws IOException {
	if(aMessageIsAlive) {
	    throw new IOException("Trying to free a sendport port while a message is alive!");
	}

	if(ident == null) {
	    throw new IbisError("Port already freed");
	}

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println(type.ibis.name() + ": freeing sendport");
	}

	try {
	    if(out == null) {
		//create a new stream, just to say close :(
		newOutputStream();
	    }

	    out.writeByte(CLOSE_CONNECTION);
	    out.reset();
	    out.flush();
	    out.close();

	} catch (IOException e) {
	    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		System.err.println("Error in NioSendPort.free: " + e);
		e.printStackTrace();
	    }
	}

	for (int i=0; i<receivers.size(); i++) { 
	    Connection c = (Connection) receivers.get(i);
	    ibis.factory.recycle(c.rpi, c.channel);
	}

	receivers = null;
	splitter = null;
	out = null;
	ident = null;

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println(type.ibis.name() + ": freeing sendport DONE");
	}
    }

    public long getCount() {
	return count;
    }

    public void resetCount() {
	count = 0;
    }

    public synchronized ReceivePortIdentifier[] connectedTo() {
	ReceivePortIdentifier[] result = 
				new ReceivePortIdentifier[receivers.size()];
	for(int i = 0; i < result.length; i++) {
	    result[i] = ((Connection) receivers.get(i)).rpi;
	}

	return result;
    }

    // called by the writeMessage
    // the stream(s) has/have already been removed from the splitter.
    // we must remove it from our receivers table and inform the user.
    void lostConnections(NioSplitterException e) throws IOException {
	NioReceivePortIdentifier rpi = null;
	Channel channel;
	Exception cause;

	if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
	    System.out.println("sendport " + ident.name + " lost connection!");
	}

	if(!connectionAdministration) {
	    throw e;
	}

	for(int i = 0; i < e.count(); i++) {
	    channel = e.getChannel(i);
	    cause = e.getException(i);


	    synchronized (this) {
		for (int j= 0; j < receivers.size(); i++) { 
		    Connection c = (Connection) receivers.get(j);
		    if(c.channel == channel) {
			receivers.remove(c);
			rpi = c.rpi;
			break;
		    }
		}

		if(rpi == null) {
		    throw new IbisError("could not find connection in lostConnection");
		}
		if(connectUpcall == null) {
		    lostConnections.add(rpi);
		}

	    }

	    if(connectUpcall != null) {
		connectUpcall.lostConnection(this, rpi, cause);
	    }
	}
    }

    public synchronized ReceivePortIdentifier[] lostConnections() {
	return (ReceivePortIdentifier[]) lostConnections.toArray();
    }

}
