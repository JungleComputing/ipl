import javax.swing.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

strictfp class BarnesHut extends UnicastRemoteObject implements BodyManager {

    public static final boolean DEBUG = true;
    public static final int RMI_PORT = (int) (('B' << 8) | 'H');

    static final double START_TIME = 0.0;
    static final double DEFAULT_END_TIME = 10.0;
    static final double DEFAULT_DT = 0.025;
    static final double DEFAULT_THETA = 2.0;

    Body[] bodyArray;
    final int maxLeafBodies;

    final double DT;
    final double END_TIME;
    final double THETA;
    final int ITERATIONS;

    BarnesHut(int nBodies, int mlb) throws RemoteException {
	bodyArray = new Plummer().generate(nBodies);
	maxLeafBodies = mlb;

	//some magic copied from the RMI version...
	double scale = Math.pow( nBodies / 16384.0, -0.25 );
	DT = DEFAULT_DT * scale;
	END_TIME = DEFAULT_END_TIME * scale;
	THETA = DEFAULT_THETA / scale;

	ITERATIONS = (int)(((END_TIME - START_TIME) / DT) + 1.1);

    }

    void run(boolean printResult) {
	//???debug: generate only one body
	/*bodies = new ArrayList();
	  bodies.add(new Body(1.0, 1.0, 1.0, 0.0, 0.0, 0.0));
	  bodies.add(new Body(-1.0, -1.0, -1.0, 0.0, 0.0, 0.0));*/

	long time = runSim();
	System.out.println("application barnes took " +
			   (double)(time/1000.0) + " s");

	if (printResult) {
	    Body b;
	    int i;

	    System.out.print("application result: ");

	    Arrays.sort(bodyArray); //??? moet straks weg
	    //Collections.sort(bodies);

	    for (i = 0; i < bodyArray.length; i++) {
		b = bodyArray[i];
	    //for (i = 0; i < bodies.size(); i++) {
		//b = (Body)bodies.get(i);

		System.out.print(i + ": " + b.pos + ", " + b.vel + "; ");
	    }

	    System.out.println();
	}
    }	

    long runSim() {
	int i, j;
	BodyTreeNode btRoot;
	long start, end;

	LinkedList result;
	Iterator it;
	int[] bNumbers;
	Vec3[] accs;

	Body b;
	BodyManager rmiStub = null; //RMI stub to 'this'

	//BodyCanvas bc = visualize();

	/*try {
	    Registry reg = LocateRegistry.createRegistry(RMI_PORT);
	    reg.bind("BarnesHut.BodyManager", this);
	    rmiStub = (BodyManager) Naming.lookup("//localhost:" + RMI_PORT +
						  "/BarnesHut.BodyManager");
	} catch (Exception e) {
	    System.err.println("EEK! Error while making RMI stub:" + e);
	    System.exit(1);
	    }*/

	System.out.println("BarnesHut: doing " + ITERATIONS +
			   " iterations with " + bodyArray.length + " bodies");
	start = System.currentTimeMillis();
		
	for (j = 0; j < ITERATIONS; j++) {
	    //System.out.println("Starting iteration " + j);
	    //build tree
	    btRoot = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);

	    //compute centers of mass
	    btRoot.computeCentersOfMass();

	    //compute forces

	    //with recursive tree splitup & RMI:
	    //btRoot.barnes(btRoot, rmiStub);
	    //btRoot.sync();
	    
	    //recursive tree splitup that returns lists:
	    /*result = btRoot.barnes(btRoot);
	    btRoot.sync();
	    it = result.iterator();
	    while(it.hasNext()) {
		bNumbers = (int []) it.next();
		accs = (Vec3 []) it.next();
		for (i = 0; i < bNumbers.length; i++) {
		    bodyArray[bNumbers[i]].acc = accs[i];
		    bodyArray[bNumbers[i]].updated = true;
		}
		}*/
	    
	    /* this version spawns a job for each body
	       bodyArray is updated by BodyTreeNode */
	    btRoot.barnes(bodyArray);

	    //update bodies
	    for (i = 0; i < bodyArray.length; i++) {
		b = bodyArray[i];
		/*if (DEBUG && !b.updated) {
		    System.err.println("EEK! Body " + i + " wasn't updated!");
		    System.exit(1);
		    }*/
		b.computeNewPosition(j != 0, DT, b.acc);
		//if (DEBUG) b.updated = false;
	    }

	    //try {
	    //	Thread.sleep(400);
	    //} catch (InterruptedException e) {}
	    //System.out.print(".");
	    //bc.repaint();
	}

	end = System.currentTimeMillis();
	return end - start;
    }

    /*private BodyCanvas visualize() {
      JFrame.setDefaultLookAndFeelDecorated(true);

      //Create and set up the window.
      JFrame frame = new JFrame("Bodies");
      //frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      BodyCanvas bc = new BodyCanvas(500, 500, bodies);
      frame.getContentPane().add(bc);

      //Display the window.
      frame.pack();
      frame.setVisible(true);

      return bc;
      }*/

    void wait4key() {
	System.out.print("Press enter..");
	try {
	    System.in.read();
	} catch (Exception e) {
	    System.out.println("EEK: " + e);
	}
    }

    // ??? maybe optimize by using acc.x, acc.y and acc.z as parameters
    // instead of the Vec3 object (could also be done elsewhere, eliminating
    // the Vec3 object)
    /**
     * updates the acceleration fields of the specified bodies
     * @param bNumbers the numbers of the bodies
     * @param accs the corresponding accelerations
     */
    public void setAccs(int[] bNumbers, Vec3[] accs) throws RemoteException {
	int i;

	if (DEBUG && bNumbers.length != accs.length) {
	    throw new IllegalArgumentException("bNumbers.length!=accs.length");
	}

	for (i = 0; i < bNumbers.length; i++) {
	    bodyArray[bNumbers[i]].acc = accs[i];
	    if (DEBUG) {
		bodyArray[bNumbers[i]].updated = true;
	    }
	}
    }
    

    public static void main(String argv[]) {
	//arguments
	int nBodies = 0, mlb = 0;
	boolean printResult = false;

	int i;

	//parse arguments
	for (i = 0; i < argv.length; i++) {
	    //options
	    if (argv[i].equals("-test")) {
		printResult = true;

	    } else { //final arguments
		nBodies = Integer.parseInt(argv[i]);
		mlb = Integer.parseInt(argv[i+1]);
		break;
	    }
	}
	if (nBodies < 1) {
	    System.err.println("Invalid body count, generating 100 bodies...");
	    nBodies = 100;
	}

	try {
	    new BarnesHut(nBodies, mlb).run(printResult);
	} catch (RemoteException e) {
	    System.err.println("EEK! Couldn't initialize barneshut: " + e);
	    System.exit(1);
	}
    }
}
