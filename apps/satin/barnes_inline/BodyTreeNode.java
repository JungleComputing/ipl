import java.util.*;

/**
 * This oct tree is designed as follows:
 * A node has two modes:
 * - cell node, with zero to eight children (children != null)
 * - a leaf node                            (children == null)
 * The 'children' field must be used to distinguish between the two modes.
 *
 * When children == null, the body field can also still be null. This
 * indicates an empty tree (which could be the result of a cut off in the
 * necessary tree contruction).
 */

/* TODO:
 * - boom-maak-fase optimaliseren door alleen bodies die van plek
 *   verwisselen te verplaatsen
 *   Let Op! Als deeltjes buiten de ruimte komen moet (eigenlijk) de hele
 *           boom opnieuw gebalanceerd worden.. 
 *           Alternatief: de root van te voren heel wijd maken (* 1.5 oid)
 *                        (hij is al ietsje wijder vanwege precisieproblemen)
 */

final class BodyTreeNode extends ibis.satin.SatinObject
    implements BodyTreeNodeInterface, java.io.Serializable {

    BodyTreeNode children[];
    Body[] bodies;
    /* bodyCount is only used during (initial) tree building. After trimming,
       bodies.length must be used (also in the necessaryTree-constructor) */
    transient int bodyCount;

    /* The part of space this node represents.
       The fields are used during tree construction, and by the
       necessaryTree-constructor so they can not be made transient.
       In an alternative barneshut implementation, these fields could be
       used (in a leaf node) to find out if a body has moved out of the
       represented part */
    private double center_x, center_y, center_z;
    private double halfSize;

    /* these 4 variables are used during the force calculation */
    private double maxTheta;  //set during initialisation
    private double com_x, com_y, com_z; //set during CoM computation
    private double totalMass;  //set during CoM computation

    //usual potential softening value, copied from splash2-barnes
    static final double SOFT_SQ = 0.05 * 0.05;

    /**
     * creates a totally empty tree. Because the SatinTuple version of
     * barnes can't be static, you need to create some BodyTreeNode
     * object to call this method
     */
    public BodyTreeNode() { }

    /**
     * Initializes center, halfSize and maxTheta
     * @param max the maximum point the tree should represent
     * @param min the minimum point the tree should represent
     * @param theta the theta value used in the simulation
     */
    private void initCenterSizeMaxtheta
	(double max_x, double max_y, double max_z,
	 double min_x, double min_y, double min_z, double theta) {

	double size;

	center_x = (max_x + min_x) / 2.0;
	center_y = (max_y + min_y) / 2.0;
	center_z = (max_z + min_z) / 2.0;

	size = Math.max(max_x - min_x, max_y - min_y);
	size = Math.max(size, max_z - min_z);

	/* make size a little bigger to compensate for very small
	   floating point inaccuracy (value copied from splash2-barnes) */
	size *= 1.00002; 

	halfSize = size / 2.0;
	maxTheta = theta * theta * halfSize * halfSize;
    }

    //constructor to create an empty tree, used during tree contruction
    private BodyTreeNode(double[] center, double halfSize, double maxTheta) {
	//children = null and bodies = null by default
	this.center_x = center[0];
	this.center_y = center[1];
	this.center_z = center[2];
	this.halfSize = halfSize;
	this.maxTheta = maxTheta;
    }

    /**
     * Generates a new tree with dimensions exactly large enough to
     * contain all bodies.
     * @param bodyArray the bodies to add
     * @param maxLeafBodies the maximum number of bodies to put in a leaf node
     * @param theta The theta value used in the computation
     */
    public BodyTreeNode( Body[] bodyArray, int maxLeafBodies, double theta) {
	int i;
	double max_x = 0.0, max_y = 0.0, max_z = 0.0,
	    min_x = 0.0, min_y = 0.0, min_z = 0.0;
	

	for (i = 0; i < bodyArray.length; i++) {
	    max_x = Math.max(max_x, bodyArray[i].pos_x);
	    max_y = Math.max(max_y, bodyArray[i].pos_y);
	    max_z = Math.max(max_z, bodyArray[i].pos_z);
	    min_x = Math.min(min_x, bodyArray[i].pos_x);
	    min_y = Math.min(min_y, bodyArray[i].pos_y);
	    min_z = Math.min(min_z, bodyArray[i].pos_z);
	}

	initCenterSizeMaxtheta(max_x, max_y, max_z, min_x, min_y, min_z,theta);

	for (i = 0; i < bodyArray.length; i++) {
	    addBody(bodyArray[i], maxLeafBodies);
	}

	trim();
    }

    /**
     * Necessary Tree Constructor:
     * Creates a recursive copy of 'original', containing exactly the parts
     * that are needed to compute the interactions with the bodies in 'job'
     */
    private BodyTreeNode(BodyTreeNode original, BodyTreeNode job) {
	int i;
	double distsq, dist1D;

	center_x = original.center_x;
	center_y = original.center_y;
	center_z = original.center_z;
	halfSize = original.halfSize;

	maxTheta = original.maxTheta;
	com_x = original.com_x;
	com_y = original.com_y;
	com_z = original.com_z;
	totalMass = original.totalMass;

	//calculate if original can be cut off

	//first find the minimum (square) distance between job and centerOfMass
	if (com_x > job.center_x + job.halfSize) {
	    dist1D = com_x - (job.center_x + job.halfSize);
	    distsq = dist1D * dist1D;
	} else if (com_x < job.center_x - job.halfSize) {
	    dist1D = (job.center_x - job.halfSize) - com_x;
	    distsq = dist1D * dist1D;
	} else { //centerOfMass is in this dimension between the limits of job
	    distsq = 0.0;
	}
	if (com_y > job.center_y + job.halfSize) {
	    dist1D = com_y - (job.center_y + job.halfSize);
	    distsq += dist1D * dist1D;
	} else if (com_y < job.center_y - job.halfSize) {
	    dist1D = (job.center_y - job.halfSize) - com_y;
	    distsq += dist1D * dist1D;
	} //else add nothing

	if (com_z > job.center_z + job.halfSize) {
	    dist1D = com_z - (job.center_z + job.halfSize);
	    distsq += dist1D * dist1D;
	} else if (com_z < job.center_z - job.halfSize) {
	    dist1D = (job.center_z - job.halfSize) - com_z;
	    distsq += dist1D * dist1D;
	} //else add nothing

	if (distsq < maxTheta) {
	    //no cutoff possible, copy the necessary parts of original

	    if (original.children == null) {
		/* leaf node, only the 'bodies' reference has to be copied
		   ('bodyCount' is only used during tree contruction) */

		bodies = original.bodies;

	    } else {
		/* cell node, recursively create/copy necessary parts */

		children = new BodyTreeNode[8];
		for (i = 0; i < 8; i++) {
		    if (original.children[i] != null) {
			if (original.children[i] == job) {
			    //don't copy job, as it is fully necessary ;-)
			    children[i] = job;
			} else {
			    children[i] =
				new BodyTreeNode(original.children[i], job);
			}
		    }
		}
	    }
	}
	/* else the cutoff IS possible, don't copy the
	   children of / bodies in original */
    }

    /**
     * determines if the point indicated by pos is in or outside this node
     */
    public boolean outOfRange( double pos_x, double pos_y, double pos_z ) {
	if (Math.abs(pos_x - center_x) > halfSize ||
	    Math.abs(pos_y - center_y) > halfSize ||
	    Math.abs(pos_z - center_z) > halfSize) {

	    return true;
	} else {
	    return false;
	}
    }

    private void printOutOfRange(java.io.PrintStream out,
				 double pos_x, double pos_y, double pos_z ) {
	double xdiff = Math.abs(pos_x - center_x) - halfSize;
	double ydiff = Math.abs(pos_y - center_y) - halfSize;
	double zdiff = Math.abs(pos_z - center_z) - halfSize;
	if (xdiff > 0.0) out.println("x : " + xdiff);
	if (ydiff > 0.0) out.println("y : " + ydiff);
	if (zdiff > 0.0) out.println("z : " + zdiff);
    }


    private double[] computeChildCenter( int childIndex ) {
	double[] newCenter = new double[3];
	double newHalfSize = halfSize / 2.0;

	if ( (childIndex & 1) != 0) { //lower bit: x dimension
	    newCenter[0] = center_x + newHalfSize;
	} else {
	    newCenter[0] = center_x - newHalfSize;
	}
	if ( (childIndex & 2) != 0) { //middle bit: y dimension
	    newCenter[1] = center_y + newHalfSize;
	} else {
	    newCenter[1] = center_y - newHalfSize;
	}
	if ( (childIndex & 4) != 0) { //upper bit: z dimension
	    newCenter[2] = center_z + newHalfSize;
	} else {
	    newCenter[2] = center_z - newHalfSize;
	}

	return newCenter;
    }
	
    /**
     * Adds 'b' to 'this' or its children
     */
    private void addBody( Body b, int maxLeafBodies ) {
	int i;

	if (children != null) { // cell node

	    addBody2Cell(b, maxLeafBodies);

	} else { //leaf node

	    if (BarnesHut.ASSERTS && outOfRange(b.pos_x, b.pos_y, b.pos_z)) {

		System.err.println("EEK! Adding out-of-range body! " +
				   "Body position: " + b.pos_x + ", " +
				   b.pos_y + ", " + b.pos_z +" id: "+b.number);

		System.err.println("     Center: " + center_x + ", " +
				   center_y + ", " + center_z +
				   " halfSize: " + halfSize);
		printOutOfRange(System.err, b.pos_x, b.pos_y, b.pos_z);
		throw new IndexOutOfBoundsException("Out-of-range body!");
		//System.exit(1);
	    }

	    if (bodyCount < maxLeafBodies) { //we have room left
		if (bodyCount == 0) bodies = new Body[maxLeafBodies];
		bodies[bodyCount] = b;
		bodyCount++;
		totalMass += b.mass;
	    } else { //we'll have to convert ourselves to a cell
		children = new BodyTreeNode[8];
		addBody2Cell(b, maxLeafBodies);
		for (i = 0; i < bodyCount; i++) {
		    addBody2Cell(bodies[i], maxLeafBodies);
		}
		bodies = null;
		// totalMass is overwritten in the CoM computation
	    }
	}
    }

    /**
     * This method is used if 'this' is a cell, to add a body to the appropiate
     * child.
     */
    private void addBody2Cell( Body b, int maxLeafBodies ) {
	int child = 0;
	double[] newCenter;

	if (b.pos_x - center_x >= 0.0) child |= 1;
	if (b.pos_y - center_y >= 0.0) child |= 2;
	if (b.pos_z - center_z >= 0.0) child |= 4;

	if (children[child] == null) {
	    /* We could compute 'newCenter' directly during the calculation of
	       'child', but with a large tree we would do it at
	       every depth we pass while adding the node.. */
	    newCenter = computeChildCenter(child);
	    children[child] = new BodyTreeNode(newCenter, halfSize/2.0,
					       maxTheta / 4.0);
	    children[child].bodies = new Body[maxLeafBodies];
	    children[child].bodies[0] = b;
	    children[child].bodyCount = 1;
	} else {
	    children[child].addBody(b, maxLeafBodies);
	}
    }

    /**
     * Makes the tree smaller by:
     * - replacing the 'bodies' array in leaf nodes by one that is
     *   exactly large enough to hold all bodies (The array is initialised
     *   with 'maxLeafBodies' elements
     *
     * During force calculation, trimming has only been useful for the job
     * since the copy-constructor automatically trims original
     */
    private void trim() {
	if (children == null) {
	    //leaf node
	    if (bodies.length != bodyCount) {
		Body[] newBodies = new Body[bodyCount];
		System.arraycopy(bodies, 0, newBodies, 0, bodyCount);
		bodies = newBodies;
	    }
	} else {
	    //cell node, process all children
	    for (int i = 0; i < 8; i++) {
		if (children[i] != null) children[i].trim();
	    }
	}
    }

    public void computeCentersOfMass() {
	int i;

	com_x = com_y = com_z = 0.0;
	totalMass = 0.0;

	if (children == null) { //leaf node
	    
	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0) ) {
		System.err.println("computeCoM: Found empty leaf node!");
		return;
	    }

	    for (i = 0; i < bodies.length; i++) {
		com_x += bodies[i].pos_x * bodies[i].mass;
		com_y += bodies[i].pos_y * bodies[i].mass;
		com_z += bodies[i].pos_z * bodies[i].mass;
		totalMass += bodies[i].mass;
	    }

	    com_x /= totalMass;
	    com_y /= totalMass;
	    com_z /= totalMass;

	} else { // cell node
	    // -> first process all children, then compute my center-of-mass

	    //??? maybe satinize this later, then the loop has to be split up
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    children[i].computeCentersOfMass();
		
		    com_x += children[i].com_x * children[i].totalMass;
		    com_y += children[i].com_y * children[i].totalMass;
		    com_z += children[i].com_z * children[i].totalMass;
		    totalMass += children[i].totalMass;
		}
	    }
	    //??? then here comes a sync() and a loop to compute my CoM;
	    com_x /= totalMass;
	    com_y /= totalMass;
	    com_z /= totalMass;
	}
    }

    /**
     * Computes the acceleration which the bodies in 'this' give to
     * a body at position 'pos'
     * 'this' can be a tree which is the result of necessaryTree construction,
     * some parts may be cut off. We exploit this by first checking if
     * the tree below 'this' has been cut off. Then the distance calculation
     * is already done during necessarryTree construction
     */
    public double[] barnesBody( double pos_x, double pos_y, double pos_z ) {
	double diff_x, diff_y, diff_z;
	double[] totalAcc = new double[3];
	double dist, distsq, factor;
	int i;

	diff_x = com_x - pos_x;
	diff_y = com_y - pos_y;
	diff_z = com_z - pos_z;

	distsq = diff_x * diff_x + diff_y * diff_y + diff_z * diff_z;

	/* In the if-statement below we could only check if we are cut off
	   (children == null && bodies == null), but then the
	   'invalid cutoff' ASSERT statement doesn't make sense anymore, and:
	   The (square) distance computed here is *LARGER* than the
	   distance computed by the necessaryTree construction (which
	   uses the boundary of the job we're working on), so we can
	   still test if the distance is large enough to use my CoM */

	if (distsq >= maxTheta) {
	    distsq += SOFT_SQ;
	    dist = Math.sqrt(distsq);
	    factor = totalMass / (distsq * dist);

	    totalAcc[0] = diff_x * factor;
	    totalAcc[1] = diff_y * factor;
	    totalAcc[2] = diff_z * factor;

	    return totalAcc;
	}
	/* else */
	//totalAcc[[012]] = 0.0

	if (children == null) {
	    // Leaf node, compute interactions with all my bodies

	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0)) {
		System.err.println("EEK! invalid cutoff in barnes(vec3)");
		System.exit(1);
	    }

	    for (i = 0; i < bodies.length; i++) {
		diff_x = bodies[i].pos_x - pos_x;
		diff_y = bodies[i].pos_y - pos_y;
		diff_z = bodies[i].pos_z - pos_z;

		distsq = diff_x * diff_x + diff_y * diff_y;
		distsq += diff_z * diff_z + SOFT_SQ;

		dist = Math.sqrt(distsq);
		factor = bodies[i].mass / (distsq * dist);

		totalAcc[0] += diff_x * factor;
		totalAcc[1] += diff_y * factor;
		totalAcc[2] += diff_z * factor;
	    }
	} else { // Cell node
	    for (i = 0; i < 8; i++) {
		double[] childresult;
		if (children[i] != null) {
		    childresult = children[i].barnesBody(pos_x, pos_y, pos_z);
		    totalAcc[0] += childresult[0];
		    totalAcc[1] += childresult[1];
		    totalAcc[2] += childresult[2];
		}
	    }
	}
	return totalAcc;
    }

    /**
     * debug version of barnesBody(pos_x, pos_y, pos_z)
     */
    public double[] barnesBodyDbg( double pos_x, double pos_y, double pos_z,
				   boolean debug ) {
	double diff_x, diff_y, diff_z;
	double[] totalAcc = new double[3];
	double dist, distsq, factor;
	int i;

	diff_x = com_x - pos_x;
	diff_y = com_y - pos_y;
	diff_z = com_z - pos_z;

	distsq = diff_x * diff_x + diff_y * diff_y + diff_z * diff_z;

	if (debug) {
	    System.out.println();
	    System.out.println("Barnes: new level:");
	    System.out.println(" CoM pos = (" + com_x + ", " +
			       com_y + ", " + com_z + ")");
	    System.out.println(" pos = (" + pos_x + ", " + pos_y + ", " +
			       pos_z + ")");
	    System.out.println(" diff = (" + diff_x + ", " + diff_y + ", " +
			       diff_z + ")");
	    System.out.println(" distsq = " + distsq +
			       " maxTheta = " + maxTheta);
	}

	if (distsq >= maxTheta) {
	    /* The distance was large enough to use my centerOfMass instead
	       of iterating my children */
    	    distsq += SOFT_SQ;
	    dist = Math.sqrt(distsq);
	    factor = totalMass / (distsq * dist);

	    totalAcc[0] = diff_x * factor;
	    totalAcc[1] = diff_y * factor;
	    totalAcc[2] = diff_z * factor;

	    if (debug) {
		System.out.println("  CoM interaction:");
		System.out.println("  added (" + totalAcc[0] + ", " +
				   totalAcc[1] + ", " + totalAcc[2] + ")");
	    }
	    return totalAcc;
	}

	// else

	if (children == null) {
	    // Leaf node, compute interactions with all my bodies
	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0)) {
		System.err.println("EEK! invalid cutoff in " +
				   "barnes(vec3)(debug version)");
		System.exit(1);
	    }

	    for (i = 0; i < bodies.length; i++) {
		if (debug) {
		    System.out.println("  Interaction with " +
				       bodies[i].number);
		}

		diff_x = bodies[i].pos_x - pos_x;
		diff_y = bodies[i].pos_y - pos_y;
		diff_z = bodies[i].pos_z - pos_z;

		distsq = diff_x * diff_x + diff_y * diff_y;
		distsq += diff_z * diff_z + SOFT_SQ;

		dist = Math.sqrt(distsq);
		factor = bodies[i].mass / (distsq * dist);

		totalAcc[0] += diff_x * factor;
		totalAcc[1] += diff_y * factor;
		totalAcc[2] += diff_z * factor;

		if (debug) {
		    System.out.println("  distsq, dist, factor: " +
				       distsq + ", " + dist + ", " +
				       factor);
		    System.out.println("  added (" + diff_x * factor + ", " +
				       diff_y * factor + ", " +
				       diff_z * factor + ")");
		}
	    }
	} else { // Cell node
	    for (i = 0; i < 8; i++) {
		double[] childresult;
		if (children[i] != null) {
		    childresult =
			children[i].barnesBodyDbg(pos_x, pos_y, pos_z, debug);
		    totalAcc[0] += childresult[0];
		    totalAcc[1] += childresult[1];
		    totalAcc[2] += childresult[2];
		}
	    }
	}
	return totalAcc;
    }

    /**
     * computes the iteractions between [ the bodies in 'this' ]
     * and [ 'interactTree' ], by recursively splitting up 'this', and
     * calling interactTree.barnes(bodies[i].pos) for all the bodies
     * when 'this' is a leaf node.
     */
    public LinkedList barnesSequential(BodyTreeNode interactTree) {
	LinkedList result;
	int i;

	if (children == null) { //leaf node

	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0)) {
		System.err.println("barnes(interactTree): " +
				   "found empty leafnode!");
		return new LinkedList();
	    }
	    int [] bodyNumbers = new int[bodies.length];
	    double[] accs_x = new double[bodies.length];
	    double[] accs_y = new double[bodies.length];
	    double[] accs_z = new double[bodies.length];

	    double[] acc;

	    for (i = 0; i < bodies.length; i++) {
		bodyNumbers[i] = bodies[i].number;

		//??? debug code
		//acc = interactTree.barnesBodyDbg
		//    (bodies[i].pos_x, bodies[i].pos_y, bodies[i].pos_z,
		//     bodies[i].number == 0);

		acc = interactTree.barnesBody
		    (bodies[i].pos_x, bodies[i].pos_y, bodies[i].pos_z);

		accs_x[i] = acc[0];
		accs_y[i] = acc[1];
		accs_z[i] = acc[2];
	    }
	    result = new LinkedList();
	    result.add(bodyNumbers);
	    result.add(accs_x);
	    result.add(accs_y);
	    result.add(accs_z);

	} else { //cell node -> call children[].barnes()

	    LinkedList res[] = new LinkedList[8];
	    int lastValidChild = -1;

	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    res[i] = children[i].barnesSequential(interactTree);
		    lastValidChild = i;
		}
	    }

	    result = combineResults(res, lastValidChild);
	}
	return result;
    }
     
    /**
     * Does the same as barnesSequential, but spawnes itself until
     * a threshold is reached. Before a subjob is spawned, the necessary
     * tree for that subjob is created to be passed to the subjob.
     * @param threshold the recursion depth at which work shouldn't
     *                  be spawned anymore
     */
    public LinkedList barnesNTC( BodyTreeNode interactTree, int threshold ) {
	LinkedList result;
	int i;

	if (children == null) {
	    // leaf node, let barnesSequential handle this

	    // (using optimizeList isn't useful for leaf nodes)
	    result = barnesSequential(interactTree);

	} else { //cell node -> call children[].barnes()
	    LinkedList res[] = new LinkedList[8];
	    int lastValidChild = -1;

	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    if (threshold > 0) {
			//necessaryTree creation
			  BodyTreeNode necessaryTree =
			      children[i] == interactTree ? interactTree :
			      new BodyTreeNode(interactTree, children[i]);
			  res[i] = children[i].barnesNTC(necessaryTree,
  						       threshold-1);

			//alternative: copy whole tree
			  //res[i] = children[i].barnesNTC(interactTree,
				//		       threshold-1);
		    } else { //reached the threshold -> no spawn
			res[i] = children[i].barnesSequential(interactTree);
		    }
		    lastValidChild = i;
		}
	    }
	    if (threshold > 0) {
		sync();
		result = combineResults(res, lastValidChild);
	    } else {
		//this was a sequential job, optimize!
		result = optimizeList(combineResults(res, lastValidChild));
	    }
	}
	return result;
    }

    /**
     * This version also spawns itself until the threshold is reached.
     * It uses the satin tuplespace.
     * In this function, threshold isn't decremented each recursive call
     * because jobWalk.length indicates the recursion depth.
     * @param jobWalk an array describing the walk through the tree from
     *                the root to the job (null means: job == root)
     * @param threshold the recursion depth at which work shouldn't
     *                  be spawned anymore
     */
    public LinkedList barnesTuple(byte[] jobWalk, String rootId,
				   int threshold) {
	BodyTreeNode root, job;
	int i;
	LinkedList result, res[] = new LinkedList[8];
	int lastValidChild = -1;

	root = (BodyTreeNode) ibis.satin.SatinTupleSpace.get(rootId);

	//find job
	job = root;
	if (jobWalk != null) {
	    for (i = 0; i < jobWalk.length; i++) {
		job = job.children[jobWalk[i]];
	    }
	} else {
	    jobWalk = new byte[0];
	}
	
	if (job.children == null) { //job is a leaf node
	    // using optimizeList isn't useful for leaf nodes
	    return job.barnesSequential(root);
	}
	/* else */

	for (i = 0; i < 8; i++) {
	    if (job.children[i] != null) {
		if (jobWalk.length < threshold) {
		    //spawn new job
		    byte[] newJobWalk = new byte[jobWalk.length + 1];
		    System.arraycopy(jobWalk, 0, newJobWalk, 0,jobWalk.length);
		    newJobWalk[jobWalk.length] = (byte)i;

		    res[i] = barnesTuple(newJobWalk, rootId, threshold);
		} else {
		    res[i] = job.children[i].barnesSequential(root);
		}
		lastValidChild = i;
	    }
	}

	if (jobWalk.length < threshold) {
	    sync();
	    return combineResults(res, lastValidChild);
	} else {
	    //this was a sequential job, optimize!
	    return optimizeList(combineResults(res, lastValidChild));
	    //return combineResults(res, lastValidChild);
	}
    }

    /**
     * This version also spawns itself until the threshold is reached.
     * It uses the satin tuplespace.
     * In this function, threshold isn't decremented each recursive call
     * because jobWalk.length indicates the recursion depth.
     * @param jobWalk an array describing the walk through the tree from
     *                the root to the job (null means: job == root)
     * @param threshold the recursion depth at which work shouldn't
     *                  be spawned anymore
     */

    public LinkedList barnesTuple2(byte[] jobWalk, int threshold) {
	BodyTreeNode job;
	int i;
	LinkedList result, res[] = new LinkedList[8];
	int lastValidChild = -1;
	Integer integer;

	//find job
	job = BarnesHut.root;
	if (jobWalk != null) {
	    for (i = 0; i < jobWalk.length; i++) {
		job = job.children[jobWalk[i]];
	    }
	} else {
	    jobWalk = new byte[0];
	}
	
	if (job.children == null) { //job is a leaf node
	    // using optimizeList isn't useful for leaf nodes
	    return job.barnesSequential(BarnesHut.root);
	}
	/* else */

	for (i = 0; i < 8; i++) {
	    if (job.children[i] != null) {
		if (jobWalk.length < threshold) {
		    //spawn new job
		    byte[] newJobWalk = new byte[jobWalk.length + 1];
		    System.arraycopy(jobWalk, 0, newJobWalk, 0,jobWalk.length);
		    newJobWalk[jobWalk.length] = (byte)i;

		    res[i] = barnesTuple2(newJobWalk, threshold);
		} else {
		    res[i] = job.children[i].barnesSequential(BarnesHut.root);
		}
		lastValidChild = i;
	    }
	}

	if (jobWalk.length < threshold) {
	    sync();
	    return combineResults(res, lastValidChild);
	} else {
	    //this was a sequential job, optimize!
	    return optimizeList(combineResults(res, lastValidChild));
	    //return combineResults(res, lastValidChild);
	}
    }

    /**
     * adds all items in results[x] (x < lastValidIndex) to
     * results[lastValidIndex]
     * @return a reference to results[lastValidIndex], for convenience
     */
    private static LinkedList combineResults(LinkedList[] results,
					     int lastValidIndex) {

	if (BarnesHut.ASSERTS && lastValidIndex < 0) {
	    System.err.println("BodyTreeNode.combineResults: EEK! " +
			       "lvi < 0! All children are null in caller!");
	    System.exit(1);
	}

	for (int i = 0; i < lastValidIndex; i++) {
	    if (results[i] != null) {
		results[lastValidIndex].addAll(results[i]);
	    }
	}

	return results[lastValidIndex];
    }

    /**
     * @param suboptimal A list with (a lot of) bodyNumber and acc arrays
     * @return A list with only one bodyNumber and acc array, containing
     *         all the elements of these arrays in 'suboptimal'
     *         If all arrays in suboptimal are empty, an empty list is returned
     */
    private static LinkedList optimizeList(LinkedList suboptimal) {
	LinkedList optimal;
	Iterator it;
	int totalElements = 0, position;

	int[] allBodyNrs, bodyNrs = null;
	double[] allAccs_x, allAccs_y, allAccs_z;
	double[] accs_x = null, accs_y = null, accs_z = null;

	it = suboptimal.iterator();	//calculate totalElements
	while (it.hasNext()) {
	    bodyNrs = (int[]) it.next();
	    it.next(); // skip accs_x, accs_y, accs_z
	    it.next();
	    it.next();
	    totalElements += bodyNrs.length;
	}
	
	if (totalElements == 0) return new LinkedList(); //nothing to optimize

	allBodyNrs = new int[totalElements];
	allAccs_x = new double[totalElements];
	allAccs_y = new double[totalElements];
	allAccs_z = new double[totalElements];

	position = 0;
	it = suboptimal.iterator();
	while (it.hasNext()) {
	    bodyNrs = (int[]) it.next();
	    accs_x = (double[]) it.next();
	    accs_y = (double[]) it.next();
	    accs_z = (double[]) it.next();

	    System.arraycopy(bodyNrs, 0, allBodyNrs, position, bodyNrs.length);
	    System.arraycopy(accs_x, 0, allAccs_x, position, accs_x.length);
	    System.arraycopy(accs_y, 0, allAccs_y, position, accs_y.length);
	    System.arraycopy(accs_z, 0, allAccs_z, position, accs_z.length);

	    position += bodyNrs.length;
	}

	optimal = new LinkedList();
	optimal.add(allBodyNrs);
	optimal.add(allAccs_x);
	optimal.add(allAccs_y);
	optimal.add(allAccs_z);

	return optimal;
    }

    public void print(java.io.PrintStream out, int level) {
	int i, j;

	if (level == 0) {
	    out.println("halfSize = " + halfSize);
	    out.print("center");
	}

	out.println(" at " + center_x + ", " + center_y + ", " + center_z);

	out.print("CoM at " + com_x + ", " + com_y + ", " + com_z);

	if (children == null) {
	    out.print(": leaf, ");

	    if (bodies == null) {
		out.println("has been cut off");
	    } else {
		out.println(bodies.length + " bodies");

		for (j = 0; j < bodies.length; j++) {
		    for (i = 0; i < level + 1; i++) out.print(" ");
		    if (bodies[j] != null) {
			out.println("body #" + bodies[j].number + " at " +
				    bodies[j].pos_x + ", " + bodies[j].pos_y +
				    ", " + bodies[j].pos_z);
		    } else {
			out.println("body: null");
		    }
		}
	    }
	} else {
	    out.println();
	    for (j = 0; j < 8; j++) {
		if(children[j] != null) {
		    for (i = 0; i < level + 1; i++) out.print(" ");
		    out.print("child #" + j);
		    //the child will print its status + the newline
		    children[j].print(out, level + 1);
		}
	    }
	}
    }
}
