import ibis.gmi.*;

public class Work extends GroupMember implements i_Work {

    int numVars, busy;

    boolean [] work;	
    Constraints constraints;

    int [] assignedProcessor;
    int [] workForProcessor;

    int [] working;

    final int IDLE    = 0,
    WORKING = 1, 
    BLOCKED = 2, 
    READY   = 3, 
    NEW     = 4, 
    ANY     = 5; 

    int slaves;

    int getWork = 0;

    Work(int vars, int slaves, int numVariables, 
	    int numValues, int numConnections, 
	    int numRelations, int numRelationPairs, int seed) { 

	Relation [] relations;

	// GenerateRelations.
	relations  = new Relation[numRelations];

	OrcaRandom random = new OrcaRandom(seed);

	for (int h=0;h<numRelations;h++) {			
	    relations[h] = new Relation(numValues, h, random, numRelationPairs, numValues);
	}

	// GenerateConstraints
	constraints = new Constraints(numVariables, numConnections, numRelations, relations, new OrcaRandom(seed));		

	numVars     = vars;
	this.slaves = slaves;
	busy        = 0;

	work = new boolean[vars];

	for (int i=0;i<vars;i++) {
	    work[i] = true;
	}		   

	assignedProcessor = new int[numVars];
	workForProcessor  = new int[slaves];
	working           = new int[slaves];

	// GenerateProcessor : Assign the work in a round robin fashion.
	for (int i=0;i<vars;i++) {
	    assignedProcessor[i] = i % slaves;
	    workForProcessor[i % slaves]++;
	}		 

	/*
	   int temp = vars / slaves;
	   int cur = 0;

	   for (int i=0;i<slaves;i++) {

	   for (int j=0;j<temp;j++) {				
	   assignedProcessor[cur] = i;
	   workForProcessor[i]++;
	   cur++;
	   }
	   }

	   for (int i=cur;i<vars;i++) {
	   assignedProcessor[i] = i % slaves;
	   workForProcessor[i % slaves]++;
	   }			      	       
	   */

	for (int i=0;i<slaves;i++) {
	    working[i] = IDLE;
	    System.err.println("Cpu " + i + " work " + workForProcessor[i]);
	}

	//		new Monitor(this).start();
    }

    public synchronized void vote(int var, boolean vote) {	       
	work[var] = vote;
    }

    public synchronized void announce(int var) {

	for (int i=0;i<numVars;i++) {
	    if (var != i && constraints.relation(var, i)) {
		work[i] = true;
	    }
	}	       		
	notifyAll();
    }

    public synchronized boolean newWork(int cpu) {

	working[cpu] = NEW;

	for (int i=0;i<numVars;i++) {
	    if (assignedProcessor[i] == cpu && work[i]) {
		return true;
	    }
	}	

	return false;
    }


    public synchronized void ready(int cpu) {		

	working[cpu] = READY;
	busy--;
	notifyAll();
    }

    /*
       public synchronized void ready(int cpu)  {		

       working[cpu] = READY;
       busy--;

       System.out.println(cpu + " ready.");

       while (busy != 0) {

       try {
       wait();
       } catch (InterruptedException e) {
       System.err.println(cpu + " got exception " + e);
       }
       }

       System.out.println(cpu + " Running.");

       notifyAll();
       }
       */

    public synchronized void printWorking() {

	StringBuffer temp = new StringBuffer("[");

	for (int i=0;i<slaves;i++) {
	    switch (working[i]) {
	    case IDLE:
		temp.append("I");
		break;
	    case WORKING:
		temp.append("W");
		break;
	    case BLOCKED:
		temp.append("B");
		break;
	    case READY:
		temp.append("R");
		break;
	    case NEW:
		temp.append("N");
		break;
	    case ANY:
		temp.append("A");
		break;
	    default:
		temp.append("E");
	    }
	}

	temp.append("] " + busy);
	System.out.println(temp.toString());
    }

    public synchronized boolean workFor(int cpu)  {

	while (true) {

	    //System.out.println("Starting workFor on " + cpu);

	    working[cpu] = IDLE;

	    if (newWork(cpu)) {
		//System.out.println("Done workFor (true) on " + cpu);
		working[cpu] = WORKING;
		busy++;
		return true;
	    }

	    if ((busy == 0) && !anyWork(cpu)) {
		//System.out.println("Done workFor (false) on " + cpu);
		return false;
	    }

	    //			System.out.println("No work for " + cpu);

	    try {
		working[cpu] = BLOCKED;
		wait();
	    } catch (InterruptedException e) {
		System.err.println(cpu + " got exception " + e);
	    }
	}	       					    			   
    }


    public synchronized boolean anyWork(int cpu) {

	working[cpu] = ANY;

	for (int i=0;i<numVars;i++) {
	    if (work[i]) {
		return true;
	    }
	}	

	return false;
    }

    public synchronized boolean [] getWork(int cpu)  {

	boolean [] temp = new boolean[numVars];

	getWork++;

	int w = 0;

	for (int i=0;i<numVars;i++) {
	    if (assignedProcessor[i] == cpu && work[i]) {
		temp[i] = true;
		work[i] = false;
		w++;
	    } else {
		temp[i] = false;
	    }
	}

	//		busy++;

	System.err.println("GetWork called " + getWork + " times. (" + w + ")");

	return temp;
    }

}









