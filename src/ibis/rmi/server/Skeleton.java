package ibis.rmi.server;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

import ibis.rmi.RTS;

import java.util.Vector;

import java.io.IOException;

public abstract class Skeleton implements Upcall, ReceivePortConnectUpcall {

    private ReceivePort receive;
    public Object destination;
    public SendPort[] stubs;
    public String stubType;
    private int num_ports = 0;
    private int max_ports = 0;
    private int counter = 0;

    private static final int INCR = 16;

    public Skeleton() {
	stubs = new SendPort[INCR];
	max_ports = INCR;
    }

    public void init(ReceivePort r, Object o) { 
	receive = r;
	destination = o;
    }    

    public boolean gotConnection(SendPortIdentifier id) {
	counter++;
// System.out.println("Skeleton " + this + " got connection, id = " + id + ", counter = " + counter);
	return true;
    }

    public void lostConnection(SendPortIdentifier id) {
	counter--;
// System.out.println("Skeleton " + this + " lost connection, id = " + id + ", counter = " + counter);
	if (counter == 0) {
	    /* No more remote stubs alive now. We can remove the skeleton. */
//	    RTS.removeSkeleton(this);
//	    cleanup();
//	    Commented all this out, because stubs now set up connections lazily.
//	    This means that we cannot use the connection counter as a reference
//	    counter anymore (but then, it was not reliable anyway).
//	    We will have to find another mechanism to clean up skeletons.
//	    TODO!!!
	}
    }

    protected void finalize() {
	cleanup();
    }

    public synchronized int addStub(ReceivePortIdentifier rpi) { 
	try { 
	    int id = 0;
	    SendPort s = RTS.getSendPort(rpi);

	    for (int i = 0; i <= num_ports; i++) {
		if (stubs[i] == s) {
		    id = i;
		    break;
		}
	    }

	    if (id == 0) {
		id = ++num_ports;
		if (id >= max_ports) {
		    SendPort[] newports = new SendPort[max_ports+INCR];
		    for (int i = 0; i < max_ports; i++) newports[i] = stubs[i];
		    max_ports += INCR;
		    stubs = newports;
		}
		stubs[id] = s;
	    }

	    return id;
	} catch (Exception e) { 
	    System.out.println("OOPS " + e);
	    e.printStackTrace();
	    System.exit(1);			
	} 
	return -1; 
    }

    public ReceivePort receivePort() {
	return receive;
    } 

    private void cleanup() {
	destination = null;
	for (int i = 0; i < stubs.length; i++) {
	    if (stubs[i] != null) {
		try {
		    /* stubs[i].free(); */
		    stubs[i] = null;
		} catch(Exception e) {
		}
	    }
	}
/*
	if (receive != null) {
System.out.println("Freeing receiveport: " + receive);
	    try {
		receive.free();
	    } catch(Exception e) {
	    }
System.out.println("Receiveport freed: " + receive);
	}
*/
	receive = null;
    }

    public abstract void upcall(ReadMessage m) throws IOException;
} 
