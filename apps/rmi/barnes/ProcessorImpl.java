import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ibis.util.PoolInfo;

//import DasUtils.*;

class ThreadSyncer {
    private int counter = 0;
    private int dest;

    ThreadSyncer(int dest) {
	this.dest = dest;
    }

    synchronized void wakeup() {
	// System.err.println("WAKEUP");
	counter++;
	if(counter == dest) notifyAll();
    }

    synchronized void sync() {
	while(counter < dest) {
	    try {
		wait();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	counter = 0;
    }
}


strictfp
public class ProcessorImpl extends UnicastRemoteObject implements Processor {

    static final int Proc0ScanMaxRetries = 100;
    static final int Proc0ScanInterval = 500;
    static final int ProcRMIPort = 2222;

    public static final boolean VERBOSE = false;

    PoolInfo d;
    Procs p;

    Processor Processors[];
    SetEssentialThread[] setEssentialThreads;

    GlobalData g;

    ThreadSyncer threadSyncer;

    int ProcessorCount, myProc, SyncCounter=0;
    int minmaxCounter = 0;
    MinMax gminmax;

    int ExchangeDestination[];
    int ExchangeNumBodies[];

    Body ReceivedBodies[][];

    int teReceiveInt[];
    int teSendInt[];

    int ExchangeIntArrayOffset[];
    int ExchangeIntArraySize[];
    int ExchangeIntArrayReceived[][];
    int ExchangeIntArrayReceivedOffset[];
    int ExchangeIntArrayReceivedSize[];

    Body essBody[][];
    int essBodyCount[];
    int essBodyCountRecv[];
    int essBodyCountAll;
    Body essBodyRecv[][];

    // The following fields are needed for serialization 

    double essRecvBp[][];
    double essRecvCp[][];

    // until here

    CenterOfMass essCenterOfMass[][];
    int essCenterOfMassCount[];
    int essCenterOfMassCountRecv[];
    int essCenterOfMassCountAll;
    CenterOfMass essCenterOfMassRecv[][];

    // barrier profiling information

    public long barrierCallCount;
    public long barrierMilliCount;
    private long tBarrierEntry;
    private long tBarrierExit;

    public void HandleException( Exception e, String description ) {
	System.err.println("Exception caught (" + description + "): " +
		e.getMessage() );

	e.printStackTrace(System.err);
	System.exit( -1 );
    }


    private void initialize() throws RemoteException {
	if (VERBOSE) {
	    System.err.println("Start initialize");
	}
	SyncCounter = 0;

	teReceiveInt = new int[ ProcessorCount ];

	threadSyncer = new ThreadSyncer(ProcessorCount-1);
	if (VERBOSE) {
	    System.err.println(" initialize 1");
	}
	Processors = p.table((Processor) this, myProc);
	Processors[myProc] = this;
	if(g.gdThreads) {
	    setEssentialThreads = new SetEssentialThread[ProcessorCount];
	}
	if (VERBOSE) {
	    System.err.println(" initialize 2");
	}

	ExchangeDestination = new int[ g.gdMaxBodies ];

	ExchangeNumBodies = new int[ ProcessorCount ];

	ReceivedBodies = new Body[ProcessorCount][];

	ExchangeIntArrayOffset = new int[ ProcessorCount ];
	ExchangeIntArraySize = new int[ ProcessorCount ];
	ExchangeIntArrayReceived = new int[ ProcessorCount ][];
	ExchangeIntArrayReceivedOffset = new int[ ProcessorCount ];
	ExchangeIntArrayReceivedSize = new int[ ProcessorCount ];

	essBody = new Body[ ProcessorCount ][];
	for (int i=0; i<ProcessorCount; i++) {
	    essBody[i] = new Body[  g.gdMaxTempBodies ];
	}
	essBodyCount = new int[ ProcessorCount ];
	essBodyCountRecv = new int[ ProcessorCount ];
	essBodyRecv = new Body[ ProcessorCount ][];

	g.debugStr("maxTempCOFM   " + g.gdMaxTempCentersOfMass );
	g.debugStr("maxTempBodies " + g.gdMaxTempBodies );

	essCenterOfMass = new CenterOfMass[ ProcessorCount ][];
	for (int i=0; i<ProcessorCount; i++)
	    essCenterOfMass[i] = new CenterOfMass[ g.gdMaxTempCentersOfMass ];
	essCenterOfMassCount = new int[ ProcessorCount ];
	essCenterOfMassCountRecv = new int[ ProcessorCount ];
	essCenterOfMassRecv = new CenterOfMass[ ProcessorCount ][];
	if (VERBOSE) {
	    System.err.println(" initialize 3");
	}
	// serialization

	for (int i=0; i<ProcessorCount; i++ ) {
	    essBodyRecv[i] = new Body[g.gdMaxTempBodies]; 

	    for (int j=0;j<g.gdMaxTempBodies;j++)
		essBodyRecv[i][j] = new Body();
	}

	if (g.gdSerialize) {

	    essRecvBp = new double [ProcessorCount][];
	    essRecvCp = new double [ProcessorCount][];

	    for (int i=0; i<ProcessorCount; i++ ) {

		essCenterOfMassRecv[i] = new CenterOfMass[g.gdMaxTempCentersOfMass];

		for (int j=0;j<g.gdMaxTempCentersOfMass;j++)
		    essCenterOfMassRecv[i][j] = new CenterOfMass();
	    }
	}

	// until here

	barrierCallCount = 0;
	barrierMilliCount = 0;
	if (VERBOSE) {
	    System.err.println("end initialize");
	}
    }

    ProcessorImpl( GlobalData g, int numProcs, int myProc, Procs p )  throws RemoteException {
	super();
	this.g = g;
	this.d = null;
	this.p = p;
	this.ProcessorCount = numProcs;
	this.myProc = myProc;
	initialize();
    }

    ProcessorImpl( GlobalData g, PoolInfo d, Procs p )  throws RemoteException {
	super();
	if (VERBOSE) {
	    System.err.println("ProcessorImpl()");
	}
	this.g = g;
	this.d = d;
	this.p = p;
	this.ProcessorCount = d.size();
	this.myProc = d.rank();
	initialize();
    }

    public void cleanup() {
    }

    public void barrier()  throws RemoteException {

	//	 System.err.println("BARRIER");

	if (myProc==0) {

	    synchronized (this) { 

		if (SyncCounter<(ProcessorCount-1)) {

		    SyncCounter++;

		    try {
			wait();
		    } catch ( InterruptedException e ) {
			System.err.println( "barrier: Caught exception: " + e );
		    }

		} else {
		    SyncCounter = 0;
		    notifyAll();
		}
	    }
	} else {

	    // call barrier function on proc 0

	    tBarrierEntry = System.currentTimeMillis();
	    Processors[0].barrier();
	    tBarrierExit = System.currentTimeMillis();
	    barrierMilliCount += (tBarrierExit-tBarrierEntry);
	    barrierCallCount++;

	    if (g.gdIteration==0) {
		barrierCallCount = 0;
		barrierMilliCount = 0;
	    }
	}
    }


    public /*synchronized*/ void register() throws RemoteException {
	if(g.gdThreads) {

	    if (ProcessorCount > 1) barrier();

	    for (int i=0; i<ProcessorCount; i++) {
		setEssentialThreads[i] = new SetEssentialThread(Processors[i]);
	    }
	}
    }

    public void setBodyCount( int Count ) throws RemoteException {
	g.gdTotNumBodies = Count;
	g.gdNumBodies = 0;
    }


    public MinMax setMinMax(MinMax minmax) throws RemoteException {
	if (myProc == 0) {
	    synchronized (this) { 

		if (minmaxCounter == 0) {
		    gminmax = new MinMax();	/* Also initializes! */
		}
		if (gminmax.min.x > minmax.min.x) gminmax.min.x = minmax.min.x;
		if (gminmax.min.y > minmax.min.y) gminmax.min.y = minmax.min.y;
		if (gminmax.min.z > minmax.min.z) gminmax.min.z = minmax.min.z;
		if (gminmax.max.x < minmax.max.x) gminmax.max.x = minmax.max.x;
		if (gminmax.max.y < minmax.max.y) gminmax.max.y = minmax.max.y;
		if (gminmax.max.z < minmax.max.z) gminmax.max.z = minmax.max.z;

		if (minmaxCounter<(ProcessorCount-1)) {

		    minmaxCounter++;

		    try {
			wait();
		    } catch ( InterruptedException e ) {
			System.err.println( "setMinMax: Caught exception: " + e );
		    }

		} else {
		    minmaxCounter = 0;
		    notifyAll();
		}
		return gminmax;
	    }

	}
	else {
	    return Processors[0].setMinMax(minmax);
	}
    }

    public void setTotalExchangeInt( int Source, int Value ) throws RemoteException {
	this.teReceiveInt[ Source ] = Value;
    }

    public void TotalExchangeInt( int data[] ) {
	TotalExchangeInt( data, 0 );
    }

    public void TotalExchangeInt( int data[], int offset ) {

	int t;

	// reset the array

	for ( int i=0; i<ProcessorCount; i++ ) {
	    teReceiveInt[i] = -1;
	}

	try {

	    if (ProcessorCount > 1) barrier();

	    for ( int i=0; i<ProcessorCount; i++ ) {

		t = data[i+offset];

		//	g.debugStr("sending " + t + " to proc " + i );

		if (t>=0)
		    Processors[i].setTotalExchangeInt( myProc, t );
	    }

	    if (ProcessorCount > 1) barrier();

	    for ( int i=0; i<ProcessorCount; i++ ) {
		data[i+offset] = teReceiveInt[i];
	    }
	} catch( RemoteException r ) {
	    HandleException( r, "TotalExchangeInt");
	}
    }


    public void setExchangeBodies( int source, Body Bodies[] ) throws RemoteException {

	// System.out.println(myProc + ": { start  receive[" + (Bodies == null ? -1 : Bodies.length) + "] of exchange from " + source);
	ReceivedBodies[ source ] = Bodies;
	// System.out.println(myProc + ": } finish receive[" + (Bodies == null ? -1 : Bodies.length) + "] of exchange from " + source);
    }

    public void setExchangeDestination( int index, int dest ) {
	ExchangeDestination[index] = dest;
    }

    public void ExchangeBodies() throws RemoteException {
	ExchangeBodies( ExchangeDestination );
    }


    public void ExchangeBodies( int [] index ) throws RemoteException {

	int i, j, count;
	Body MovingBodies[];

	// Send all bodies with a destination other then myProc to their
	// processors, and receive all bodies.

	// Count all bodies

	for ( i=0; i<ProcessorCount; i++ )
	    ExchangeNumBodies[i] = 0;

	for ( i=0; i<g.gdNumBodies; i++ ) {
	    ExchangeNumBodies[ index[i] ]++;
	}

	/*
	   for ( i=0; i<g.gdNumBodies; i++ ) {
	   int blerk = index[i];
	   int t = ExchangeNumBodies[ blerk ];
	   t++;
	   ExchangeNumBodies[ blerk ] = t;
	   }
	   */
	if (ProcessorCount > 1) barrier(); // wait for all processors

	// Send all bodies to their destination

	for ( i=0; i<ProcessorCount; i++ ) {

	    /* Try to not have all processors send at the same time to the same
	     * recipient.
	     * This has the additional advantage that the well-known multiplexing
	     * bug in PandaIbis (caused by the fact that panda has only static
	     * multiplexing) is not triggered for #bodies >= 100000.
	     *							RFHH
	     */
	    int target = (myProc + i) % ProcessorCount;

	    if (ExchangeNumBodies[target]==0) {

		MovingBodies = null;

	    } else {

		MovingBodies = new Body[ ExchangeNumBodies[target] ];

		for ( j=0, count=0; j<g.gdNumBodies; j++ ) {

		    if (index[j]==target)
			MovingBodies[ count++ ] = g.gdBodies[j];

		}

	    }
	    if (target==myProc) {     
		setExchangeBodies( myProc, MovingBodies );
	    } else {
		// System.out.println(myProc + ": { start  exchange[" + (MovingBodies == null ? -1 : MovingBodies.length) + "] with " + target);
		Processors[target].setExchangeBodies( myProc, MovingBodies );
		// System.out.println(myProc + ": } finish exchange[" + (MovingBodies == null ? -1 : MovingBodies.length) + "] with " + target);
	    }
	}

	MovingBodies = null;

	if (ProcessorCount > 1) barrier(); // wait for all processors

	for ( i=0, count = 0; i<ProcessorCount; i++ ) {

	    if (ReceivedBodies[i]!=null) {

		for ( j=0; j<ReceivedBodies[i].length; j++ )
		    g.gdBodies[count++] = ReceivedBodies[i][j];
	    }
	}
	g.gdNumBodies = count;
    }



    public int[] getExchangeIntArray( int proc ) {
	return ExchangeIntArrayReceived[ proc ];
    }

    public int getExchangeIntArrayOffset( int proc ) {
	return ExchangeIntArrayReceivedOffset[ proc ];
    }

    public int getExchangeIntArraySize( int proc ) {
	return ExchangeIntArrayReceivedSize[ proc ];
    }


    public void resetExchangeIntArray() {
	for (int i=0; i<ProcessorCount; i++ ) {
	    ExchangeIntArrayOffset[i] = -1; 
	    ExchangeIntArraySize[i] = -1; 
	    ExchangeIntArrayReceived[i] = null;
	    ExchangeIntArrayReceivedOffset[i] = -1;
	    ExchangeIntArrayReceivedSize[i] = -1; 
	}
    }

    public void setExchangeIntArrayDest( int proc, int offset, int size ) {
	ExchangeIntArrayOffset[ proc ] = offset;
	ExchangeIntArraySize[ proc ] = size;
    }

    public void setExchangeIntArray( int source, int array[], int offset, int size ) {
	ExchangeIntArrayReceived[ source] = array;
	ExchangeIntArrayReceivedOffset[ source ] = offset;
	ExchangeIntArrayReceivedSize[ source ] = size; 
    }
    public void ExchangeIntArray( int [] array ) {

	try {
	    if (ProcessorCount > 1) barrier();

	    //      g.debugStr("the arraylength: " + array.length );

	    for (int i=0; i<ProcessorCount; i++ ) {
		if (ExchangeIntArrayOffset[i] != -1) {
		    if (i==myProc) {
			setExchangeIntArray( myProc, array,
				ExchangeIntArrayOffset[i],
				ExchangeIntArraySize[i] );
		    } else {
			Processors[i].setExchangeIntArray( myProc, array,
				ExchangeIntArrayOffset[i],
				ExchangeIntArraySize[i] );
		    }
		}
	    }
	    if (ProcessorCount > 1) barrier();

	} catch (Exception e) {
	    HandleException( e,"ExchangeIntArray" );
	}

    }


    public void broadcastBodies() throws RemoteException {

	// Set the total number of bodies

	for ( int i=0; i<ProcessorCount; i++ ) {
	    Processors[i].setBodyCount( g.gdTotNumBodies );
	}

	// Divide the bodies between all processors

	for ( int i=0; i<g.gdTotNumBodies; i++ ) {
	    setExchangeDestination( i, (i % ProcessorCount) ); 
	}  

	g.gdNumBodies = g.gdTotNumBodies;

	ExchangeBodies();
    }


    public void receiveBodies() throws RemoteException {
	g.gdNumBodies = 0;
	ExchangeBodies();
    }



    public void sendEssentialCenterOfMass( int dest, CenterOfMass c ) {
	if (essCenterOfMassCount[dest]<essCenterOfMass[dest].length) {
	    essCenterOfMass[ dest ][essCenterOfMassCount[ dest ]++] = c;
	} else
	    HandleException( null, "too many essential centers of mass for dest " + dest );
    }

    public void sendEssentialBody( int dest, Body b ) {

	if (essBodyCount[dest]<essBody[dest].length) {
	    essBody[dest][essBodyCount[ dest ]++] = b;
	} else
	    HandleException( null, "too many essential bodies for dest " + dest );
    }

    void resetExchangeEssential() {
	for (int i=0; i<ProcessorCount; i++) {
	    essBodyCount[i] = 0;
	    essCenterOfMassCount[i] = 0;
	}
    }


    public synchronized void setEssential( int Source, int bCount, SendBody [] b, int cCount, CenterOfMass [] c ) throws RemoteException  {

	// System.err.println(myProc + "Machine " + Source + " enters setEssential");
	// System.out.println(myProc + " ( enter setEssential[" + bCount + "] from " + Source);
	for (int i=0;i<bCount;i++) {
	    essBodyRecv[ Source ][i].bPos = b[i].bPos; 
	    essBodyRecv[ Source ][i].bMass = b[i].bMass;
	}
	essBodyCountRecv[ Source ] = bCount;
	essCenterOfMassRecv[ Source ] = c;
	essCenterOfMassCountRecv[ Source ] = cCount;

	// System.err.println(myProc + "Machine " + Source + " leaves setEssential");
	// System.out.println(myProc + " ) exit  setEssential[" + bCount + "] from " + Source);
	if (g.gdThreads) {
	    threadSyncer.wakeup();
	}
    }

    public synchronized void setEssential( int Source, int bCount, double [] bp, int cCount, double [] cp ) throws RemoteException {
	int i,p;

	essBodyCountRecv[ Source ] = bCount;
	//    essRecvBp[ Source ] = bp;

	for (i=0, p=0;i<bCount;i++, p+=4) {
	    essBodyRecv[ Source ][i].bPos.x = bp[p]; 
	    essBodyRecv[ Source ][i].bPos.y = bp[p+1]; 
	    essBodyRecv[ Source ][i].bPos.z = bp[p+2]; 
	    essBodyRecv[ Source ][i].bMass = bp[p+3]; 
	}

	essCenterOfMassCountRecv[ Source ] = cCount;
	//    essRecvBp[ Source ] = bp;

	for (i=0, p=0;i<cCount;i++, p+=7) {
	    essCenterOfMassRecv[ Source ][i].cofmMass = cp[p];
	    essCenterOfMassRecv[ Source ][i].cofmCenter_x = cp[p+1]; 
	    essCenterOfMassRecv[ Source ][i].cofmCenter_y = cp[p+2]; 
	    essCenterOfMassRecv[ Source ][i].cofmCenter_z = cp[p+3]; 
	    essCenterOfMassRecv[ Source ][i].cofmCenterOfMass_x = cp[p+4]; 
	    essCenterOfMassRecv[ Source ][i].cofmCenterOfMass_y = cp[p+5]; 
	    essCenterOfMassRecv[ Source ][i].cofmCenterOfMass_z = cp[p+6]; 
	}

	if (g.gdThreads) {
	    threadSyncer.wakeup();
	}
    }


    public void exchangeEssential() {

	int extraBods = 0; 
	int extraCOFM = 0;

	SendBody myBods[];
	CenterOfMass myCOFM[];

	try {

	    if (ProcessorCount > 1) barrier();

	    // Exchange all arrays...
	    // Calls to setEssential can be parellized by using threads...

	    for (int i=0; i<ProcessorCount; i++) {
		if (i!=myProc) {
		    if (g.gdSerialize) {
			double essBpTemp[] = new double[essBodyCount[i]*4];
			double essCpTemp[] = new double[essCenterOfMassCount[i]*7];
			for (int j = essBodyCount[i] - 1; j >= 0; j--) {
			    Body b = essBody[i][j];
			    int index = 4*j;
			    essBpTemp[index] = b.bPos.x;
			    essBpTemp[index+1] = b.bPos.y;
			    essBpTemp[index+2] = b.bPos.z;
			    essBpTemp[index+3] = b.bMass;
			}
			for (int j = essCenterOfMassCount[i]-1; j >= 0; j--) {
			    CenterOfMass c = essCenterOfMass[i][j];
			    int index = 7*j;
			    essCpTemp[index] = c.cofmMass;
			    essCpTemp[index+1] = c.cofmCenter_x;    
			    essCpTemp[index+2] = c.cofmCenter_y;    
			    essCpTemp[index+3] = c.cofmCenter_z;    
			    essCpTemp[index+4] = c.cofmCenterOfMass_x;    
			    essCpTemp[index+5] = c.cofmCenterOfMass_y;    
			    essCpTemp[index+6] = c.cofmCenterOfMass_z;    
			}
			if(g.gdThreads) {
			    setEssentialThreads[i].put(myProc, essBodyCount[i], essBpTemp, essCenterOfMassCount[i], essCpTemp);
			} else {
			    Processors[i].setEssential(myProc, essBodyCount[i], essBpTemp, essCenterOfMassCount[i], essCpTemp);
			}
		    } else {

			if (g.gdTrimArrays) {

			    myBods = new SendBody[ essBodyCount[i] ];
			    myCOFM = new CenterOfMass[ essCenterOfMassCount[i] ];

			    for (int j=0; j<essBodyCount[i]; j++) {
				myBods[j] = new SendBody();
				myBods[j].bPos = essBody[i][j].bPos;
				myBods[j].bMass = essBody[i][j].bMass;
			    }

			    for (int j=0; j<essCenterOfMassCount[i]; j++)
				myCOFM[j] = essCenterOfMass[i][j];

			    if(g.gdThreads) {
				// System.out.println(myProc + " doing setEssential[" + essBodyCount[i] + "] on " + i + " " + Processors[i]);
				setEssentialThreads[i].put(myProc, essBodyCount[i], myBods, essCenterOfMassCount[i], myCOFM );
			    } else {
				// System.err.println(myProc + " doing setEssential on " + i + " " + Processors[i]);

				Processors[i].setEssential( myProc, essBodyCount[i], myBods, essCenterOfMassCount[i], myCOFM );
			    }

			    myBods = null;
			    myCOFM = null;

			} else {
			    myBods = new SendBody[ g.gdMaxTempBodies ];
			    for (int j=0; j<g.gdMaxTempBodies; j++) {
				if (essBody[i][j] != null) {
				    myBods[j] = new SendBody();
				    myBods[j].bPos = essBody[i][j].bPos;
				    myBods[j].bMass = essBody[i][j].bMass;
				} else {
				    myBods[j] = null;
				}
			    }
			    extraBods += (g.gdMaxTempBodies - essBodyCount[i]);
			    extraCOFM += (g.gdMaxTempCentersOfMass - essCenterOfMassCount[i]);

			    if(g.gdThreads) {
				setEssentialThreads[i].put(myProc, essBodyCount[i], myBods, essCenterOfMassCount[i], essCenterOfMass[i] );
			    } else {
				Processors[i].setEssential(myProc, essBodyCount[i], myBods, essCenterOfMassCount[i], essCenterOfMass[i] );
			    }
			}
		    }
		}
	    }

	    // wait till everybody has sent his bodies...
	    if (g.gdThreads) {
		threadSyncer.sync();
	    }

	    if (ProcessorCount > 1) barrier();

	} catch (Exception e) {
	    HandleException( e, "ExchangeEssential" );
	}

	//    g.debugStr("Sent " + extraBods + " bodies and " + extraCOFM + " centers of mass overhead");
    }



    public Body[] getEssentialBodies( int source ) {
	return essBodyRecv[ source ];
    }


    public int getEssentialBodyCount( int source ) {
	return essBodyCountRecv[ source ];
    }


    public CenterOfMass[] getEssentialCenterOfMass( int source ) {
	return essCenterOfMassRecv[ source ];
    }


    public int getEssentialCenterOfMassCount( int source ) {
	return essCenterOfMassCountRecv[ source ];
    }

}

