import javax.swing.*;
import java.util.*;

strictfp class BarnesHut {

    public static final boolean DEBUG = true;

    static final double START_TIME = 0.0;
    static final double DEFAULT_END_TIME = 10.0;
    static final double DEFAULT_DT = 0.025;
    static final double DEFAULT_THETA = 2.0;

    List bodies;
    final double DT;
    final double END_TIME;
    final double THETA;
    final int ITERATIONS;

    BarnesHut(int nBodies) {
	Body[] bodyArray = new Plummer().generate(nBodies);
	bodies = new ArrayList(bodyArray.length);
	for (int i = 0; i < nBodies; i++) {
	    bodies.add(bodyArray[i]);
	}

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

	    Collections.sort(bodies);

	    System.out.print("application result: ");
	    for (i = 0; i < bodies.size(); i++) {
		b = (Body)bodies.get(i);
		System.out.print(i + ": " + b.pos + ", " + b.vel + "; ");
	    }
	    System.out.println();
	}
    }	

    long runSim() {
	int i, j;
	BodyTreeNode btRoot;
	CoMTreeNode comtRoot;
	long start, end;
	List accs;
	Iterator it;
	Body b;

	//BodyCanvas bc = visualize();

	System.out.println("BarnesHut: doing " + ITERATIONS +
			   " iterations with " + bodies.size() + " bodies");
	start = System.currentTimeMillis();
		
	for (j = 0; j < ITERATIONS; j++) {
	    //build tree
	    btRoot = new BodyTreeNode(bodies);

	    //compute centers of mass
	    comtRoot = new CoMTreeNode(btRoot, THETA);

	    //compute forces
	    bodies = btRoot.barnes(comtRoot);
	    btRoot.sync();
	
	    //update bodies
	    for (i = 0; i < bodies.size(); i++) {
		b = (Body)bodies.get(i);
		b.computeNewPosition(j != 0, DT, b.acc);
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

    public static void main(String argv[]) {
	//arguments
	int nBodies = 0;
	boolean printResult = false;

	int i;

	//parse arguments
	for (i = 0; i < argv.length; i++) {
	    //options
	    if (argv[i].equals("-test")) {
		printResult = true;

	    } else { //final argument
		nBodies = Integer.parseInt(argv[i]);
		break;
	    }
	}
	if (nBodies < 1) {
	    System.err.println("Invalid body count, generating 100 bodies...");
	    nBodies = 100;
	}

	new BarnesHut(nBodies).run(printResult);
    }
}
