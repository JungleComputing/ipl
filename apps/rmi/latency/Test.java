import java.rmi.*;

public class Test extends java.rmi.server.UnicastRemoteObject implements myServer, Runnable { 

	int i;
	boolean finished = false;

	public Test() throws RemoteException { 
		super();
	} 

	public void foo() { 
//		System.out.println("foo");
//		i++;
	} 

	public synchronized void quit() {
	    finished = true;
	    notifyAll();
	}

	private synchronized void waitForQuit() {
	    while (! finished) {
		try {
		    wait();
		} catch(Exception e) {
		}
	    }
	}

	public void run() {
	    waitForQuit();
	    System.exit(0);
	}
} 
