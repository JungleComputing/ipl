import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class Reduce extends UnicastRemoteObject implements i_Reduce {

    PivotElt solution;
    int max, num;
    boolean done;

    Reduce(int max) throws RemoteException { 
	super();
	this.max = max;
	this.num = 0;	       		
	done = false;
    }

    private void do_reduce(PivotElt elt) {

	if (elt.cols < elt.max_cols) {
	    if (solution.cols < solution.max_cols) {
		if (elt.norm > solution.norm) {
		    solution.max_over_max_cols = elt.norm;
		    solution.index = elt.index;
		    solution.cols  = elt.cols;
		}
	    } else {
		solution.max_over_max_cols = elt.norm;
		solution.index = elt.index;
		solution.cols  = elt.cols;
	    }
	} else if (solution.cols >= solution.max_cols) {
	    solution.max_over_max_cols = 0.0;
	    if (elt.norm > solution.norm) {
		solution.index = elt.index;
	    }
	}

	if (elt.norm > solution.norm) {
	    solution.norm = elt.norm;
	}
    }

    public synchronized PivotElt reduce(PivotElt elt) throws RemoteException {

	// wait for previous reduce to return all results. 
	while (done) {
	    try {
		wait();
	    } catch (Exception e) {
		System.err.println("reduce got exception " + e);
	    }
	}

	if (num == 0) {
	    solution = elt;
	} else { 
	    do_reduce(elt);
	} 

	num++;

	if (num == max) { 
	    done = true;
	    notifyAll(); // wake up calls of this reduce.
	} else {
	    // wait for current reduce to finish.
	    while (!done) {
		try {
		    wait();
		} catch (Exception e) {
		    System.err.println("reduce got exception " + e);
		}
	    }		
	}

	num--;

	if (num == 0) { 
	    done = false;
	    notifyAll(); // wake up calls of next reduce.
	} 

	return solution;
    }
}
