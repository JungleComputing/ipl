import java.net.*;
import ibis.rmi.*;
import ibis.rmi.server.*;
import ibis.rmi.registry.*;
import java.io.IOException;

public class Server extends UnicastRemoteObject implements i_Server, Runnable {

    private Registry local = null;
    private boolean finished = false;
    private boolean server_worker = false;
    private Worker worker = null;


    public Server(String[] args, Registry local) throws ibis.rmi.RemoteException {
	super();

	System.err.println("Server object: constructor");

	int option = 0;
	for (int i = 0; i < args.length; i++) {
	    /* Eat away all the Client options. Just ignore them, the Server
	     * serves anything that comes along. */
	    if (false) {
	    } else if (args[i].equals("-one-way")) {
	    } else if (args[i].equals("-byte")) {
	    } else if (args[i].equals("-int")) {
	    } else if (args[i].equals("-int2")) {
	    } else if (args[i].equals("-int32")) {
	    } else if (args[i].equals("-float")) {
	    } else if (args[i].equals("-double")) {
	    } else if (args[i].equals("-tree")) {
	    } else if (args[i].equals("-cyc")) {
	    } else if (args[i].equals("-b")) {
	    } else if (args[i].equals("-inner")) {
	    } else if (args[i].equals("-switch")) {
	    } else if (args[i].equals("-warmup")) {
		i++;
	    } else if (args[i].equals("-registry")) {
		i++;
	    } else if (args[i].equals("-client-worker")) {
	    } else if (args[i].equals("-server-worker")) {
		server_worker = true;
	    } else if (option == 0) {
		option++;
	    } else if (option == 1) {
		option++;
	    } else {
		System.out.println("No such option: " + args[i]);
		Thread.dumpStack();
		System.exit(33);
	    }
	}

	this.local = local;
    }


    private static void fatal(String message) {
	System.err.println(message);
	System.exit(1);
    }


    private static void message(String message) {
	System.out.println(message);
    }


    public void empty() throws RemoteException {
    }


    public void emptyThreadSwitch() throws RemoteException {
	empty();
    }


    public void empty(int i0, int i1) throws RemoteException {
    }


    public Object empty(Object b) throws RemoteException {
	if (b instanceof B) {
	    System.out.println("B.j = " + ((B)b).j);
	    System.out.println("B.i = " + ((B)b).i);
	} else if (b instanceof WithInner[]) {
	    System.out.println("See a WithInner " + ((WithInner[])b)[0]);
	}
	return b;
    }


    public void one_way(Object b) throws RemoteException {
    }


    public void reset() throws RemoteException {
	// fs_stats_reset();
	System.gc();
	// javaSocketResetStats();
	// ibis.impl.messagePassing.Ibis.resetStats();
	if (server_worker) {
	    worker.reset();
	}
    }


    public void quit() throws ibis.rmi.RemoteException {
	// System.out.println("Server receives quit request");
	synchronized (this) {
	    finished = true;
	    notifyAll();
	}
    }


    private synchronized void waitForQuit() {
	while (! finished) {
	    try {
		wait();
	    } catch (Exception e) {
		System.err.println("waitForQuit woke up : " + e);
	    }
	}
    }


    public void run() {

	try {
	    System.out.println("Server: RMIenv done");
	    local.rebind("server", this);

	    // System.out.println("Server ready.");

	    if (server_worker) {
		worker = new Worker("Server worker");
		worker.start();
	    }

	    waitForQuit();

	    if (server_worker) {
		worker.quit();
	    }

	    Thread.sleep(500);
	    System.out.println("Server is gonna quit");

	    System.exit(0);
	} catch (Exception e) {
	    System.out.println("Exception in Server.main: " + e);
	}
    }

}
