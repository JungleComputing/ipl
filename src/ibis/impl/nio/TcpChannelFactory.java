package ibis.impl.nio;

import java.util.Properties;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.util.ArrayList;
import java.util.Enumeration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.NetworkInterface;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.AsynchronousCloseException;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.ReceiveTimedOutException;
import ibis.util.ThreadPool;
import ibis.ipl.IbisError;

import ibis.util.IbisSocketFactory;
import ibis.util.IPUtils;


/**
 * implements a channelfactory using the tcp implementation of nio
 */
class TcpChannelFactory implements ChannelFactory, Protocol {

    // Server socket Channel we listen for new connection on
    private ServerSocketChannel ssc;

    // Address ssc is bound to
    private InetSocketAddress address; 

    // list of ReceivePorts we listen for
    private ArrayList receivePorts;

    private static IbisSocketFactory socketFactory = 
	IbisSocketFactory.createFactory();

    TcpChannelFactory() throws IOException {
	int port = socketFactory.allocLocalPort();
	InetAddress localAddress = IPUtils.getLocalHostAddress();

	// init server socket channel
	ssc = ServerSocketChannel.open();

	address = new InetSocketAddress(localAddress, port);
	ssc.socket().bind(address);

	// just in case it binded to some other port
	localAddress = ssc.socket().getInetAddress();
	port = ssc.socket().getLocalPort();
	address = new InetSocketAddress(localAddress, port);

	receivePorts = new ArrayList();

	ThreadPool.createNew(this);
    }

    /**
     * Find the first public ip address for this node
     */
    private InetAddress getPublicIP() throws IOException {
	Enumeration interfaces, addresses;
	NetworkInterface networkInterface;
	InetAddress address;

	interfaces = NetworkInterface.getNetworkInterfaces();

	if (interfaces == null) {
	    throw new IOException("no network interfaces found");
	}

	while(interfaces.hasMoreElements()) {
	    networkInterface = 
		(NetworkInterface)interfaces.nextElement();

	    addresses = networkInterface.getInetAddresses();

	    while(addresses != null && addresses.hasMoreElements()) {
		address = (InetAddress)addresses.nextElement();

		if ( !(address.isAnyLocalAddress()
			    | address.isLoopbackAddress()
			    | address.isLinkLocalAddress() )) {
		    return address;
		}
	    }
	}
	throw new IOException("no local network address found");
    }

    /**
     * Register a receiveport with this factory, so it will listen for
     * connections to it from now on.
     *
     * @return the address of the socket we wil listen for connections on
     */
    public synchronized InetSocketAddress register(NioReceivePort rp) {
	if (DEBUG) {
	    Debug.message("connections", this, 
		   "Receiveport \"" + rp + "\" registered with factory");
	}

	receivePorts.add(rp);

	return address;
    }

    public synchronized void deRegister(NioReceivePort rp) throws IOException {
	NioReceivePort temp;

	if(DEBUG) {
	    Debug.message("connections", this, 
		  "Receiveport[" + rp + "] DE-registers with factory");
	}

	for(int i = 0; i < receivePorts.size(); i++) {
	    temp = (NioReceivePort) receivePorts.get(i);

	    if(temp == rp) {
		receivePorts.remove(i);
		return;
	    }
	}
	throw new IbisError("Receiveport " + rp + "tried to de-register "
		+ "without being registerred!" );
    }

    public void quit() throws IOException {
	//this will make the accept() throw an AsynchronusCloseException
	//or an ClosedChannelException and make the thread exit
	ssc.close();
    }

    /**
     * Finds the ReceivePort wich has the given identifier.
     *
     * @return the ReceivePort wich has the given identifier, or null
     * if not found.
     */
    private synchronized NioReceivePort findReceivePort(
	    NioReceivePortIdentifier rpi) {
	NioReceivePort temp;
	for(int i = 0; i < receivePorts.size(); i++) {
	    temp = (NioReceivePort) receivePorts.get(i);

	    if(rpi.equals(temp.ident)) {
		return temp;
	    }
	}
	return null;
    }

    /**
     * Tries to connect the sendport to the receiveport for the given
     * time. Returns the resulting channel.
     */
    public Channel connect(NioSendPortIdentifier spi, 
	    NioReceivePortIdentifier rpi, 
	    long timeoutMillis) throws IOException {
	int reply;

	SocketChannel channel = SocketChannel.open();
	long deadline = 0;
	long time;

	if (DEBUG) {
	    Debug.enter("connections", this, 
			"connecting \"" + spi + "\" to \"" + rpi + "\"");
	}

	if(timeoutMillis > 0) {
	    deadline = System.currentTimeMillis() + timeoutMillis;

	    channel.configureBlocking(false);
	    channel.connect(rpi.address);

	    Selector selector = Selector.open();
	    channel.register(selector, SelectionKey.OP_CONNECT);

	    if(selector.select(timeoutMillis) == 0) {
		//nothing selected, so we had a timeout

		if (DEBUG) {
		    Debug.exit("connections", this,
			"!timed out while connecting socket to receiver");
		}

		throw new ConnectionTimedOutException("timed out while"
			+ " connecting socket to receiver");
	    }

	    if(!channel.finishConnect()) {
		throw new IbisError("finish connect failed while we made sure"
			+ " it would work");
	    }

	    selector.close();
	    channel.configureBlocking(true);
	} else {
	    //do a blocking connect
	    channel.connect(rpi.address);
	}

	channel.socket().setTcpNoDelay(true);

	//write out rpi and spi
	ChannelAccumulator accumulator = new ChannelAccumulator(channel);
	accumulator.writeByte(CONNECTION_REQUEST);
	spi.writeTo(accumulator);
	rpi.writeTo(accumulator);
	accumulator.flush();

	if (DEBUG) { 
	    Debug.message("connections", this, "waiting for reply on connect");
	}

	if(timeoutMillis > 0) {
	    channel.configureBlocking(false);

	    Selector selector = Selector.open();
	    channel.register(selector, SelectionKey.OP_READ);

	    if(selector.select(timeoutMillis) == 0) {
		//nothing selected, so we had a timeout
		try {
		    channel.close();
		} catch (IOException e) {
		    //IGNORE
		}

		if (DEBUG) {
		    Debug.exit("connections", this,
			       "!timed out while for reply from receiver");
		}

		throw new ConnectionTimedOutException("timed out while"
			+ " waiting for reply from receiver");
	    }
	    selector.close();
	    channel.configureBlocking(true);
	}

	//see what he thinks about it
	ChannelDissipator dissipator = new ChannelDissipator(channel);
	reply = dissipator.readByte();
	dissipator.close();

	if(reply == CONNECTION_DENIED) {

	    if (DEBUG) {
		Debug.exit("connections", this,
			   "!Receiver denied connection");
	    }

	    channel.close();
	    throw new ConnectionRefusedException("Could not connect");
	} else if (reply == CONNECTION_ACCEPTED) {
	    if(DEBUG) {
		Debug.message("connections", this,
		   "Connection accepted, using new connection");
	    }
	} else {
	    if(DEBUG) {
		Debug.exit("connections", this,
		   "!illigal opcode in ChannelFactory.connect()");
	    }
	    throw new IbisError("illigal opcode in ChannelFactory.connect()");
	}

	if (DEBUG) {
	    Debug.exit("connections", this,
	    "made new connection from \"" + spi + "\" to \"" + rpi + "\"");
	}

	channel.configureBlocking(true);
	return channel;
    }

    /**
     * Handles incoming requests
     */
    private void handleRequest(SocketChannel channel) {
	byte request;
	NioSendPortIdentifier spi = null;
	NioReceivePortIdentifier rpi;
	NioReceivePort rp = null;
	ChannelDissipator dissipator = new ChannelDissipator(channel);
	ChannelAccumulator accumulator = new ChannelAccumulator(channel);

	if (DEBUG) {
	    Debug.enter("connections", this,
			"got new connection from " 
			+ channel.socket().getInetAddress() + ":" 
			+ channel.socket().getPort());
	}


	try {
	    request = dissipator.readByte();

	    if(request != CONNECTION_REQUEST) {
		if (DEBUG) {
		    Debug.exit("connections", this, 
			       "!received unknown request");
		}
		try {
		    dissipator.close();
		    accumulator.close();
		    channel.close();
		} catch (IOException e) {
		    //IGNORE
		}
		return;
	    }

	    spi = new NioSendPortIdentifier(dissipator);
	    rpi = new NioReceivePortIdentifier(dissipator);
	    dissipator.close();

	    rp = findReceivePort(rpi);

	    if(rp == null) {
		if (DEBUG) {
		    Debug.exit("connections", this, 
			   "!could not find receiveport, connection denied");
		}
		accumulator.writeByte(CONNECTION_DENIED);
		accumulator.flush();
		accumulator.close();
		channel.close();
		return;
	    }

	    if (DEBUG) {
		Debug.message("connections", this,
			"giving new connection to receiveport");
	    }

	    //register connection with receiveport
	    if(rp.connectionRequested(spi, channel)) {
		accumulator.writeByte(CONNECTION_ACCEPTED);
		accumulator.flush();
		accumulator.close();
	    } else {
		if (DEBUG) {
		    Debug.exit("connections", this,
			       "!receiveport rejected connection");
		}
		accumulator.writeByte(CONNECTION_DENIED);
		accumulator.flush();
		accumulator.close();
		channel.close();
		return;
	    }
	} catch (IOException e) {
	    if (DEBUG) {
		Debug.exit("connections", this, 
			   "!got an exception on handling an incoming request"
			   + ", closing channel" + e);
	    }
	    try {
		channel.close();
	    } catch(IOException e2) {
		//IGNORE
	    }
	    return;
	}

	if (DEBUG) {
	    Debug.exit("connections", this, "set up new connection");
	}
    }

    /**
     * Accepts connections on the server socket channel
     */
    public void run() {
	SocketChannel channel = null;
	boolean exit = false;

	Thread.currentThread().setName("ChannelFactory");

	if (DEBUG) {
	    Debug.resetLevel();
	    Debug.message("general", this, 
				 "ChannelFactory running on " + ssc);
	}

	while(true) {
	    try {
		channel = ssc.accept();
		channel.socket().setTcpNoDelay(true);
		channel.configureBlocking(true);
	    } catch (ClosedChannelException e) {
		// the channel was closed before we started the accept
		// OR while we were doing the accept
		// take the hint, and exit
		ssc = null;
		return;
	    } catch (Exception e3) {
		try {
		    ssc.close();
		    channel.close();
		} catch (IOException e4) {
		    //IGNORE
		}
		System.err.println("Fatal: TcpNioChannelFactory could not"
			+ " do accept");
		return;
	    }

	    handleRequest(channel);
	}
    }
}

