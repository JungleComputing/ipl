package ibis.ipl.impl.messagePassing;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import ibis.ipl.IbisIOException;
import ibis.ipl.IbisException;

class ElectionServer
	extends Thread
	implements ibis.ipl.Upcall {

    static private Hashtable elections;
    private boolean started = false;
    private boolean finished = false;

    final static boolean DEBUG = false;


    public void upcall(ibis.ipl.ReadMessage m) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	try {
	    int sender = m.readInt();
	    String name = (String)m.readObject();
	    Object o = m.readObject();
	    m.finish();

	    if (ElectionServer.DEBUG) {
		System.err.println(Thread.currentThread() + "ElectionServer receives election " + name + " contender " + o.toString());
	    }

	    Object e;
	    synchronized (elections) {
		while (! started) {
		    try {
			elections.wait();
		    } catch (InterruptedException ie) {
			// ignore
		    }
		}
		e = elections.get(name);
		if (e == null) {
		    elections.put(name, o);
		    e = o;
		}
	    }

	    if (ElectionServer.DEBUG) {
		System.err.println(Thread.currentThread() + "ElectionServer pronounces election " + name + " winner " + e.toString());
	    }
	    ibis.ipl.WriteMessage r = client_port[sender].newMessage();
	    r.writeObject(e);
	    r.send();
	    r.finish();
	    if (ElectionServer.DEBUG) {
		System.err.println(Thread.currentThread() + "ElectionServer election " + name + " done");
	    }
	} catch (ClassNotFoundException e) {
	    System.err.println(Thread.currentThread() + ": ElectionServer upcall exception " + e);
	    Thread.dumpStack();
	} catch (IbisIOException e) {
	    System.err.println(Thread.currentThread() + ": ElectionServer upcall exception " + e);
	    Thread.dumpStack();
	}
    }


    ibis.ipl.ReceivePort[] server_port;
    ibis.ipl.SendPort[] client_port;


    ElectionServer() throws IbisIOException {
	if (elections != null) {
	    throw new IbisIOException("Can have only one ElectionServer");
	}
	elections = new Hashtable();

	start();
    }


    void end() {
	synchronized (this) {
	    notifyAll();
	    finished = true;
	}
    }


    public void run() {
	int n = ibis.ipl.impl.messagePassing.Ibis.myIbis.nrCpus;

	try {
// System.err.println(Thread.currentThread() + "ElectionServer runs");
	    server_port = new ibis.ipl.ReceivePort[ibis.ipl.impl.messagePassing.Ibis.myIbis.nrCpus];
	    client_port = new ibis.ipl.SendPort[ibis.ipl.impl.messagePassing.Ibis.myIbis.nrCpus];

	    ibis.ipl.PortType type = ibis.ipl.impl.messagePassing.Ibis.myIbis.createPortType("++++ElectionPort++++",
							  new ibis.ipl.StaticProperties());

	    for (int i = 0; i < n; i++) {
// System.err.println(Thread.currentThread() + "ElectionServer will create ReceivePort " + i);
		server_port[i] = type.createReceivePort("++++ElectionServer-" +
							    i + "++++", 
							this);
		server_port[i].enableConnections();
		server_port[i].enableUpcalls();
// System.err.println(Thread.currentThread() + "ElectionServer will create SendPort " + i);
		client_port[i] = type.createSendPort();
	    }

	    for (int i = 0; i < n; i++) {
// System.err.println(Thread.currentThread() + "Now I'm gonna lookup ElectionClient receive port " + i);
		ibis.ipl.ReceivePortIdentifier rid = ibis.ipl.impl.messagePassing.Ibis.myIbis.registry().lookup("++++ElectionClient-" + i + "++++");
// System.err.println(Thread.currentThread() + "Now I'm gonna connect to ElectionClient receive port " + i + " RportID " + rid);
		client_port[i].connect(rid);
	    }

	    synchronized (elections) {
		started = true;
		elections.notifyAll();
	    }

// System.err.println(Thread.currentThread() + "ElectionServer up");
	} catch (IbisIOException e) {
	    System.err.println("ElectionServer meets exception " + e);
	} catch (IbisException e2) {
	    System.err.println("ElectionServer meets exception " + e2);
	}


	/* Await command to shut down */
	synchronized (this) {
	    while (! finished) {
		try {
		    wait();
		} catch (InterruptedException e) {
		    // Ignore
		}
	    }
	}

	for (int i = 0; i < n; i++) {
	    try {
		client_port[i].free();
	    } catch (IbisIOException e) {
		// Ignore
	    }
	}
	for (int i = 0; i < n; i++) {
	    if (DEBUG) {
		System.err.println("ElectionServer frees server port[" + i + "] = " + server_port[i]);
	    }
	    server_port[i].free();
	}
	if (DEBUG) {
	    System.err.println("ElectionServer has freed all server ports");
	}
    }

}
