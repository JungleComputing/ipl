import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class BroadcastObject extends UnicastRemoteObject implements i_BroadcastObject {

    private BroadcastSender leftSender, rightSender;
    private i_BroadcastObject [] receivers;
    private Object [] data;

    private int size;
    private int rank;

    public BroadcastObject(int rank, int size, int max_data) throws RemoteException {
	super();
	data  = new Object[max_data];
	this.size = size;
	this.rank = rank;

	leftSender  = new BroadcastSender();
	rightSender = new BroadcastSender();
	leftSender.start();
	rightSender.start();
    }

    public void connect(i_BroadcastObject [] receivers) {
	this.receivers = receivers;
    }

    private int rel_rank(int abs, int root, int size) {
	return ((size+abs-root)%size);
    }

    private int abs_rank(int rel, int root, int size) {
	return ((rel+root)%size);
    }

    public void send(int broadcast, Object o, int owner) throws RemoteException {

	int rel = rel_rank(rank, owner, size);
	int left, right;

	left = ((rel+1)*2)-1;
	right = left+1;

	if (left < size) { 
	    // send to left.
	    int dest = abs_rank(left, owner, size);
	    leftSender.put(receivers[dest], broadcast, o, owner);
	} 

	if (right < size) {
	    // send to right.
	    int dest = abs_rank(right, owner, size);
	    rightSender.put(receivers[dest], broadcast, o, owner);
	}		

	if (owner != rank) { 
	    synchronized(this) {
		data[broadcast] = o;		
		notifyAll();
	    }
	}
    }

    public synchronized Object receive(int broadcast) {

	Object temp;

	while (data[broadcast] == null) { 
	    try {
		wait();
	    } catch (Exception e) {
		System.err.println(rank + " BroadcastObject.receive got exception " + e);
		System.exit(1);
	    } 
	}

	temp = data[broadcast];
	data[broadcast] = null;

	return temp;
    }	             
}
