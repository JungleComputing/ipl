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


/**
 * implements a channelfactory using the tcp implementation of nio
 */
class TcpNioChannelFactory implements NioChannelFactory, NioProtocol {

    // Server socket Channel we listen for new connection on
    private ServerSocketChannel ssc;

    // Address ssc is bound to
    private InetSocketAddress address; 

    // list of ReceivePorts we listen for
    private ArrayList receivePorts;

    // connectioncache
    private NioConnectionCache cache;

    TcpNioChannelFactory() throws IOException {
	int port = IbisSocketFactory.allocLocalPort();

	Properties p = System.getProperties();

	String myIp = p.getProperty("ip_address");
	InetAddress inetAddress;

	// init server socket channel
	ssc = ServerSocketChannel.open();

	if(myIp != null) {
	    inetAddress = InetAddress.getByName(myIp);
	} else {
	    inetAddress = getPublicIP();
	}

	address = new InetSocketAddress(inetAddress, port);
	ssc.socket().bind(address);
	// just in case it binded to some other port
	inetAddress = ssc.socket().getInetAddress();
	port = ssc.socket().getLocalPort();
	address = new InetSocketAddress(inetAddress, port);

	receivePorts = new ArrayList();

	cache = new NioConnectionCache();

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
	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("Receiveport " + rp 
		    + " registered with factory");
	}
	receivePorts.add(rp);

	return address;
    }

    public synchronized void deRegister(NioReceivePort rp) throws IOException {
	NioReceivePort temp;

	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("Receiveport[" + rp
		    + "] DE-registers with factory");
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

	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("ChannelFactory connecting " + spi
		    + " to " + rpi);
	}

	if(timeoutMillis > 0) {
	    deadline = System.currentTimeMillis() + timeoutMillis;

	    channel.configureBlocking(false);
	    channel.connect(rpi.address);

	    Selector selector = Selector.open();
	    channel.register(selector, SelectionKey.OP_CONNECT);

	    if(selector.select(timeoutMillis) == 0) {
		//nothing selected, so we had a timeout
		throw new ConnectionTimedOutException("timed out while"
			+ " connecting socket to receiver");
	    }

	    if(!channel.finishConnect()) {
		throw new IbisError("finish connect failed while we made sure"
			+ " it would work");
	    }
	} else {
	    //do a blocking connect
	    channel.connect(rpi.address);
	    channel.configureBlocking(false);
	}

	channel.socket().setTcpNoDelay(true);

	// set up some temporary streams
	NioOutputStream out = new NioOutputStream(channel, 1000, false);
	NioInputStream in = new NioInputStream(channel, false, 10, false);
	ObjectOutputStream objOut = new ObjectOutputStream(out);
    

	// notify receiver we want to make a new connection
	// writing these in non-blocking mode, but it should fit in the
	// socket's buffer, so not a big problem.
	objOut.writeByte(NEW_CONNECTION);
	objOut.writeObject(spi);
	objOut.writeObject(rpi);
	objOut.flush();
	objOut.close();
	out.flush();
	out.close();

	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.err.println("ChannelFactory waiting for reply on connect");
	}

	//see what he thinks about it
	try {
	    reply = in.readByte(deadline);
	} catch (ReceiveTimedOutException e) {
	    try {
		channel.close();
	    } catch (IOException e2) {
		//IGNORE
	    }
	    throw new ConnectionTimedOutException("timeout while waiting for"
		    + " a reply from the receiveport");
	}

	if(reply == RECEIVER_DENIED) {
	    if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		System.err.println("ChannelFactory: Receiver " +
			"denied connection");
	    }
	    channel.close();
	    throw new ConnectionRefusedException("Could not connect");
	} else if (reply == NEW_CONNECTION) {
	    // we're going to use this channel as output
	    if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		System.err.println("ChannelFactory: Connection accepted"
			+ ", using new connection");
	    }

	    // remember we could use the input too.
	    cache.inputIsFree(rpi.ibis, channel);

	} else if (reply == EXISTING_CONNECTION) {
	    // we're going to use some other channel

	    if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		System.err.println("ChannelFactory: Connection accepted"
			+ ", using existing connection");
	    }
	    channel.close();

	    // get the free connection from the cache
	    channel = (SocketChannel) cache.getFreeOutput(rpi.ibis);

	    if(channel == null) {
		throw new IbisError("TcpNioConnectionCache corrupt");
	    }
	} else {
	    throw new IbisError("illigal opcode in ChannelFactory.connect()");
	}
	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.err.println("ChannelFactory made new connection from "
		    + spi + " to " + rpi);
	}

	in.close();
	return channel;
    }

    /**
     * Returns a write channel to the factory for recycling or destruction.
     */
    public void recycle(NioReceivePortIdentifier rpi,
	    Channel channel) throws IOException {
	cache.outputIsFree(rpi.ibis, channel);
    }

    /**
     * Returns a read channel to the factory for recycling or destruction.
     */
    public void recycle(NioSendPortIdentifier spi,
	    Channel channel) throws IOException {
	cache.inputIsFree(spi.ibis, channel);
    }

    /**
     * Handles incoming requests
     */
    private void handleRequest(SocketChannel channel) {
	int request;
	SocketChannel input = null;
	ObjectInputStream objIn;
	NioSendPortIdentifier spi = null;
	NioReceivePortIdentifier rpi;
	NioReceivePort rp = null;
	NioInputStream in = null;
	NioOutputStream out = null;

	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("ChannelFactory got new connection from "
		    + channel.socket().getInetAddress() + ":" 
		    + channel.socket().getPort());
	}

	try {
	    in = new NioInputStream(channel, false, 1000, false);
	    out = new NioOutputStream(channel, 10, false);
	    objIn = new ObjectInputStream(in);

	    request = objIn.readByte();

	    if(request != NEW_CONNECTION) {
	       if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		   System.err.println("Channelfactory received unknown opcode"
			   + " in request");
	       }
	       throw new IbisError("ChannelFactory: unknown opcode in request");
	    }

	    spi = (NioSendPortIdentifier) objIn.readObject();
	    rpi = (NioReceivePortIdentifier) objIn.readObject();
	    objIn.close();
	    in.close();

	    if(spi == null | rpi == null) {
		throw new IbisError("error: received null while"
			+ " expecting two identifiers");
	    }

	    rp = findReceivePort(rpi);

	    //find some free input in the cache
	    input = (SocketChannel) cache.getFreeInput(spi.ibis);

	    if(input == null) {
		//could not find free input, use current channel
		input = channel;
	    }

	    if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		System.err.println("ChannelFactory giving new connection" +
			" to receiveport");
	    }

	    //newConnection(...) registers this connection with the receiveport
	    //it returns false if the connection is not allowed.
	    if(rp == null | !rp.newConnection(spi, input)) {
		if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		    if(rp == null) {
			System.err.println("could not find receiveport");
		    } else {
			System.err.println("receiveport rejected connection");
		    }
		}
		out.writeByte(RECEIVER_DENIED);
		out.flush();
		out.reallyClose(); // closes the channel too.

		return;
	    }
	} catch (IOException e) {
	    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		System.err.println("EEK! TcpNioChannelFactory got an exception" 
		    + " on handling an incoming request, closing channel" + e);
	    }
	    try {
		channel.close();
		return;
	    } catch(IOException e2) {
		//IGNORE
	    }
	    return;

	} catch (ClassNotFoundException e3) {
	    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		System.err.println("EEK! TcpNioChannelFactory got an exception" 
		    + " on handling an incoming request, closing channel" + e3);
	    }
	    try {
		channel.close();
		return;
	    } catch(IOException e4) {
		//IGNORE
	    }
	    return;
	}


	//new try block here because the connection is now registerred with
	//the receiveport! If it failes now, we must un-register it
	try {

	    if(input == channel) {
		// could not find free input, use this (new!) connection
		
		if (DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		    System.err.println("accepted connection, using new"
			    + " channel");
		}

		out.writeByte(NEW_CONNECTION);
		out.flush();
		out.close();

		//remember we could use the output too
		cache.outputIsFree(spi.ibis, channel);

	    } else {
		// we have a "spare" input somewhere, use that instead of
		// current connection

		if (DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
		    System.err.println("accepted connection, using existing"
			    + " channel");
		}

		out.writeByte(EXISTING_CONNECTION);
		out.flush();
		out.reallyClose(); // close channel too
	    }

	} catch (IOException e4) {
	    if (DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		System.err.println("EEK! TcpNioChannelFactory got an exception"
		    + " on handling an incoming request, closing channel" + e4);
	    }
	    try {
		rp.lostConnection(spi, channel, e4);
		channel.close();
	    } catch(IOException e5) {
		//IGNORE
	    }
	    return;
	}

	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.err.println("Channelfactory set up new connection");
	}
    }

    /**
     * Accepts connections on the server socket channel
     */
    public void run() {
	SocketChannel channel = null;
	boolean exit = false;

	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("ChannelFactory running on " + ssc);
	}

	while(true) {
	    try {
		channel = ssc.accept();
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(true);
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
		throw new IbisError("Fatal: TcpNioChannelFactory could not do accept");
	    }

	    handleRequest(channel);
	}
    }
}

