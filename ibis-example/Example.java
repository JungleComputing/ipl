import ibis.ipl.*;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This is basically the example RPC program described in the Ibis
 * Programmers manual. A server computes lengths of strings, and
 * a client supplies the server with strings.
 */
public class Example {

    /**
     * The port type for both send and receive ports.
     */
    PortType porttype;

    /**
     * Registry where receive ports are registered.
     */
    Registry rgstry;

    /**
     * When the client is done, it will send a null string, which is
     * a signal for the server to terminate. The finish field is set
     * to true when a null string is received.
     */
    boolean finish = false;

    boolean failure = false;

    /**
     * Upcall handler class for the server.
     */
    private class RpcUpcall implements Upcall {
	/**
	 */
	SendPort serverSender;

	RpcUpcall(SendPort p) {
	    serverSender = p;
	}

	public void upcall(ReadMessage m) throws IOException {
	    String s;
	    
	    try {
		s = (String) m.readObject();
	    } catch(ClassNotFoundException e) {
		s = null;
	    }
	    m.finish();

	    if (s == null) {
		synchronized(this) {
		    finish = true;
		    notifyAll();
		}
		return;
	    }
	    int len = s.length();
	    WriteMessage w = serverSender.newMessage();
	    w.writeInt(len);
	    w.finish();
	}
    }

    private void server() {
	try {
	    // Create a send port for sending answers
	    SendPort serverSender = porttype.createSendPort();
	    ReceivePortIdentifier client = rgstry.lookupReceivePort("client");
	    serverSender.connect(client);

	    // Create an upcall handler
	    RpcUpcall rpcUpcall = new RpcUpcall(serverSender);
	    ReceivePort serverReceiver =
		porttype.createReceivePort("server", rpcUpcall);
	    serverReceiver.enableConnections();
	    serverReceiver.enableUpcalls();

	    // Wait until finished
	    synchronized(rpcUpcall) {
		while (! finish) {
		    try {
			rpcUpcall.wait();
		    } catch(InterruptedException e) {
		    }
		}
	    }

	    // System.out.println("Server done");

	    // Close ports
	    serverSender.close();
	    serverReceiver.close();

	    // System.out.println("Server closed ports");

	} catch(Exception e) {
	    System.err.println("Server got exception: " + e);
	    failure = true;
	}
    }

    private void client() {
	try {
	    // Create a send port for sending requests
	    SendPort clientSender = porttype.createSendPort();

	    // Create a receive port for receiving answers
	    ReceivePort clientReceiver = porttype.createReceivePort("client");
	    clientReceiver.enableConnections();

	    // Connect send port
	    ReceivePortIdentifier srvr = rgstry.lookupReceivePort("server");
	    clientSender.connect(srvr);

	    FileReader f = new FileReader("Example.java");
	    BufferedReader bf = new BufferedReader(f);

	    // For every line in this file, compute its length by consulting
	    // the server.
	    String s;
	    do {
		s = bf.readLine();
		WriteMessage w = clientSender.newMessage();
		w.writeObject(s);
		w.finish();
		if (s != null) {
		    ReadMessage r = clientReceiver.receive();
		    int len = r.readInt();
		    r.finish();
		    // System.out.println(s + ": " + len);
		    if (len != s.length()) {
			System.err.println("String: \"" + s +
					   "\", expected length " + s.length() +
					   ", got length " + len);
			failure = true;
		    }
		}
	    } while (s != null);

	    // System.out.println("Client is done");

	    // Close ports
	    clientSender.close();
	    clientReceiver.close();

	    // System.out.println("Client closed ports");

	} catch(IOException e) {
	    System.err.println("Client got exception: " + e);
	    failure = true;
	}
    }

    private void run() {
	// Create properties for the Ibis to be created.
	StaticProperties props = new StaticProperties();
	props.add("communication",
		  "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt");
	props.add("serialization", "object");
	props.add("worldmodel", "closed");

	// Create an Ibis
	final Ibis ibis;
	try {
	    ibis = Ibis.createIbis(props, null);
	} catch(IbisException e) {
	    System.err.println("Could not create Ibis: " + e);
	    failure = true;
	    return;
	}

	// Install shutdown hook that terminates ibis
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    public void run() {
		try {
		    ibis.end();
		} catch (IOException e) {
		}
	    }
	});

	// Create properties for the port type
	StaticProperties portprops = new StaticProperties();
	portprops.add("communication",
		      "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt");
	portprops.add("serialization", "object");

	// Create the port type
	try {
	    porttype = ibis.createPortType("RPC port", portprops);
	} catch(Exception e) {
	    System.err.println("Could not create port type: " + e);
	    failure = true;
	    return;
	}

	// Elect a server
	IbisIdentifier me = ibis.identifier();
	rgstry = ibis.registry();
	IbisIdentifier server;
	try {
	    server = (IbisIdentifier) rgstry.elect("Server", me);
	} catch(Exception e) {
	    System.err.println("Could not elect: " + e);
	    failure = true;
	    return;
	}

	// Start either a server or a client.
	boolean i_am_server = server.equals(me);
	if (i_am_server) {
	    server();
	} else {
	    client();
	}
    }

    public static void main(String args[]) {
	Example ex = new Example();
	ex.run();
	if (ex.failure) {
	    System.exit(1);
	}
	else {
	    System.out.println("Test succeeded!");
	}
    }
}
