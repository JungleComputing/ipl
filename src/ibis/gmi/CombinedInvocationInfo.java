package ibis.group;

import java.io.Serializable;

public class CombinedInvocationInfo implements Serializable { 
    int id;
    int groupID;
    String method;
    String name;
    int mode;
    int size;
    int [] participating_cpus;

    transient int barrier_count = 0;
    transient int current_barrier = 0; 
    transient ParameterVector [] in;
    transient ParameterVector out;
    transient int present;
    transient int rank;

    CombinedInvocationInfo(int id, int groupID, String method, String name, int mode, int size) { 
	this.id = id;
	this.groupID = groupID;
	this.method = method;
	this.name = name;
	this.mode = mode;
	this.size = size;
	present = 0;
	participating_cpus = new int[size];			
	for (int i=0;i<size;i++) { 
	    participating_cpus[i] = -1;
	} 
    } 	

    synchronized void addAndWaitUntilFull(int rank, int cpu) { 
        if (participating_cpus[rank] != -1) { 
	    throw new RuntimeException("Jikes !! Combined invocation rank handed out twice !!!");
        } 
        present++;
        participating_cpus[rank] = cpu;
        if (present < size) {
	while (present < size) {
	    try {
		wait();
	    } catch(Exception e) {
		// ignore
	    }
	}
        }
        else {
	notifyAll();
        }
    } 

    synchronized void barrier() { 

	int my_barrier_number = current_barrier;

	barrier_count++;

	if (barrier_count < size) { 		
	    while (my_barrier_number == current_barrier) { 
		try { 
		    wait();
		} catch (Exception e) { 
		    // ignore
		} 
	    } 
	} else {
	    current_barrier++;
	    barrier_count = 0;
	    notifyAll();
	} 
    } 
} 





