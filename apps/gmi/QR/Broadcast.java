import ibis.gmi.GroupMember;

class Broadcast extends GroupMember implements i_Broadcast { 

    double [] data;
    boolean empty = true;

    i_Broadcast group;

    Broadcast() { 
	super();
    }

    public void init(i_Broadcast group) { 
	this.group = group;
    } 

    public synchronized void broadcast_it(double [] data) { 		
	//		System.out.println("Received a broadcast");
	this.data = data;
	empty = false;
	notifyAll();
    }

    public synchronized double [] broadcast(double [] in, boolean send) { 

	if (send) { 
	    //			System.out.println("Doing a broadcast ...");
	    group.broadcast_it(in);
	} 

	while (empty) {
	    try {
		wait();
	    } catch (Exception e) {
		System.err.println("broadcast got exception " + e);
	    }
	}
	empty = true;
	return data;
    } 		
}

