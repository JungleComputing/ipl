//import java.io.*;
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
    private Vec3 center;
    private double halfSize;

    /* these 4 variables are used during the force calculation */
    private double maxTheta;  //set during initialisation
    private Vec3 centerOfMass; //set during CoM computation
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
    private void initCenterSizeMaxtheta(Vec3 max, Vec3 min, double theta) {
	double size;

	center = new Vec3( (max.x+min.x) / 2.0, (max.y+min.y) / 2.0,
			   (max.z+min.z) / 2.0 );
	size = Math.max(max.x - min.x, max.y - min.y);
	size = Math.max(size, max.z - min.z);

	/* make size a little bigger to compensate for very small
	   floating point inaccuracy (value copied from splash2-barnes) */
	size *= 1.00002; 

	halfSize = size / 2.0;
	maxTheta = theta * theta * halfSize * halfSize;
    }

    //constructor to create an empty tree, used during tree contruction
    private BodyTreeNode(Vec3 center, double halfSize, double maxTheta) {
	//children = null and bodies = null by default
	this.center = new Vec3(center);
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
	Vec3 max, min;

	max = new Vec3();
	min = new Vec3();
	for (i = 0; i < bodyArray.length; i++) {
	    max.max(bodyArray[i].pos);
	    min.min(bodyArray[i].pos);
	}

	initCenterSizeMaxtheta(max, min, theta);

	for (i = 0; i < bodyArray.length; i++) {
	    try {
		addBody(bodyArray[i], maxLeafBodies);
	    } catch (Exception e) {
		print(System.err, 0);
		System.exit(1);
	    }
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

	center = new Vec3(original.center);
	halfSize = original.halfSize;

	maxTheta = original.maxTheta;
	centerOfMass = new Vec3(original.centerOfMass);
	totalMass = original.totalMass;

	//calculate if original can be cut off

	//first find the minimum (square) distance between job and centerOfMass
	if (centerOfMass.x > job.center.x + job.halfSize) {
	    dist1D = centerOfMass.x - (job.center.x + job.halfSize);
	    distsq = dist1D * dist1D;
	} else if (centerOfMass.x < job.center.x - job.halfSize) {
	    dist1D = (job.center.x - job.halfSize) - centerOfMass.x;
	    distsq = dist1D * dist1D;
	} else { //centerOfMass is in this dimension between the limits of job
	    distsq = 0.0;
	}
	if (centerOfMass.y > job.center.y + job.halfSize) {
	    dist1D = centerOfMass.y - (job.center.y + job.halfSize);
	    distsq += dist1D * dist1D;
	} else if (centerOfMass.y < job.center.y - job.halfSize) {
	    dist1D = (job.center.y - job.halfSize) - centerOfMass.y;
	    distsq += dist1D * dist1D;
	} //else add nothing

	if (centerOfMass.z > job.center.z + job.halfSize) {
	    dist1D = centerOfMass.z - (job.center.z + job.halfSize);
	    distsq += dist1D * dist1D;
	} else if (centerOfMass.z < job.center.z - job.halfSize) {
	    dist1D = (job.center.z - job.halfSize) - centerOfMass.z;
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
    public boolean outOfRange( Vec3 pos ) {
	if (Math.abs(pos.x - center.x) > halfSize ||
	    Math.abs(pos.y - center.y) > halfSize ||
	    Math.abs(pos.z - center.z) > halfSize) {

	    return true;
	} else {
	    return false;
	}
    }

    private void printOutOfRange(java.io.PrintStream out, Vec3 pos) {
	double xdiff = Math.abs(pos.x - center.x) - halfSize;
	double ydiff = Math.abs(pos.y - center.y) - halfSize;
	double zdiff = Math.abs(pos.z - center.z) - halfSize;
	if (xdiff > 0.0) out.println("x : " + xdiff);
	if (ydiff > 0.0) out.println("y : " + ydiff);
	if (zdiff > 0.0) out.println("z : " + zdiff);
    }


    private Vec3 computeChildCenter( int childIndex ) {
	Vec3 newCenter = new Vec3();
	double newHalfSize = halfSize / 2.0;

	if ( (childIndex & 1) != 0) { //lower bit: x dimension
	    newCenter.x = center.x + newHalfSize;
	} else {
	    newCenter.x = center.x - newHalfSize;
	}
	if ( (childIndex & 2) != 0) { //middle bit: y dimension
	    newCenter.y = center.y + newHalfSize;
	} else {
	    newCenter.y = center.y - newHalfSize;
	}
	if ( (childIndex & 4) != 0) { //upper bit: z dimension
	    newCenter.z = center.z + newHalfSize;
	} else {
	    newCenter.z = center.z - newHalfSize;
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

	    if (BarnesHut.ASSERTS && outOfRange(b.pos)) {
		System.err.println("EEK! Adding out-of-range body! " +
				   "Body position: "+b.pos+" id: "+b.number);
		System.err.println("     Center: " + center +
				   " halfSize: " + halfSize);
		printOutOfRange(System.err, b.pos);
		throw new IndexOutOfBoundsException("foo");
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
	Vec3 diff, newCenter;

	diff = new Vec3(b.pos);
	diff.sub(center);

	if (diff.x >= 0.0) child |= 1;
	if (diff.y >= 0.0) child |= 2;
	if (diff.z >= 0.0) child |= 4;

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
		for (int i = 0; i < bodyCount; i++) {
		    newBodies[i] = bodies[i];
		}
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

	centerOfMass = new Vec3();
	totalMass = 0.0;

	if (children == null) { //leaf node
	    
	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0) ) {
		System.err.println("computeCoM: Found empty leaf node!");
		return;
	    }

	    for (i = 0; i < bodies.length; i++) {
		centerOfMass.x += bodies[i].pos.x * bodies[i].mass;
		centerOfMass.y += bodies[i].pos.y * bodies[i].mass;
		centerOfMass.z += bodies[i].pos.z * bodies[i].mass;
		totalMass += bodies[i].mass;
	    }
	    centerOfMass.div(totalMass);

	} else { // cell node
	    // -> first process all children, then compute my center-of-mass

	    //??? maybe satinize this later, then the loop has to be split up
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    children[i].computeCentersOfMass();
		
		    centerOfMass.x +=
			children[i].centerOfMass.x * children[i].totalMass;
		    centerOfMass.y +=
			children[i].centerOfMass.y * children[i].totalMass;
		    centerOfMass.z +=
			children[i].centerOfMass.z * children[i].totalMass;
		    totalMass += children[i].totalMass;
		}
	    }
	    //??? then here comes a sync() and a loop to compute my CoM;
	    centerOfMass.div(totalMass);
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
    public Vec3 barnesBody( Vec3 pos ) {
	Vec3 diff;
	double dist, distsq, factor;
	int i;

	diff = new Vec3(centerOfMass);
	diff.sub(pos);

	distsq = diff.x * diff.x + diff.y * diff.y + diff.z * diff.z;

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

	    diff.mul(factor);

	    return diff;
	}
	/* else */
	
	Vec3 totalAcc = new Vec3();

	if (children == null) {
	    // Leaf node, compute interactions with all my bodies

	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0)) {
		System.err.println("EEK! invalid cutoff in " + center +
				   ".barnes(vec3)");
		System.err.println("My CoM = " + centerOfMass);
		System.exit(1);
	    }

	    for (i = 0; i < bodies.length; i++) {
		diff = new Vec3(bodies[i].pos);
		diff.sub(pos);

		distsq = diff.x * diff.x + diff.y * diff.y;
		distsq += diff.z * diff.z + SOFT_SQ;

		dist = Math.sqrt(distsq);
		factor = bodies[i].mass / (distsq * dist);

		diff.mul(factor);
		totalAcc.add(diff);
	    }
	} else { // Cell node
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    totalAcc.add( children[i].barnesBody( pos ) );
		}
	    }
	}
	return totalAcc;
    }

    //spawnable version of barnesBody(Vec3 pos)
    public Vec3 spawn_barnesBody (Vec3 pos) {
	return barnesBody(pos);
    }

    /**
     * Computes the acceleration that the bodies in 'this' give to 'b'
     * ( debug version of void barnesBody(pos) )
     */
    public Vec3 barnesBodyDbg( Vec3 pos, boolean debug ) {
	Vec3 diff;
	double dist, distsq, factor;
	int i;

	diff = new Vec3(centerOfMass);
	diff.sub(pos);

	distsq = diff.x * diff.x + diff.y * diff.y + diff.z * diff.z;

	if (debug) {
	    System.out.println();
	    System.out.println("Barnes: new level:");
	    System.out.println(" CoM pos = " + centerOfMass);
	    System.out.println(" pos = " + pos);
	    System.out.println(" diff = " + diff);
	    System.out.println(" distsq = " + distsq +
			       " maxTheta = " + maxTheta);
	}

	if (distsq >= maxTheta) {

	    /* The distance was large enough to use my centerOfMass instead
	       of iterating my children */
	    distsq += SOFT_SQ;
	    dist = Math.sqrt(distsq);
	    factor = totalMass / (distsq * dist);

	    diff.mul(factor);

	    if (debug) {
		System.out.println("  CoM interaction:");
		System.out.println("  added " + diff);
	    }

	    return diff;
	}

	// else

	Vec3 totalAcc = new Vec3();

	if (children == null) {
	    // Leaf node, compute interactions with all my bodies
	    if (BarnesHut.ASSERTS && (bodies == null || bodies.length == 0)) {
		System.err.println("EEK! invalid cutoff in " + center +
				   ".barnes(vec3)(debug version)");
		System.err.println("My CoM = " + centerOfMass);
		System.exit(1);
	    }

	    for (i = 0; i < bodies.length; i++) {
		diff = new Vec3(bodies[i].pos);
		diff.sub(pos);

		if (debug) {
		    System.out.println("  Interaction with " +
				       bodies[i].number);
		}

		distsq = diff.x * diff.x + diff.y * diff.y;
		distsq += diff.z * diff.z + SOFT_SQ;
		dist = Math.sqrt(distsq);
		factor = bodies[i].mass / (distsq * dist);

		diff.mul(factor);
		totalAcc.add(diff);

		if (debug) {
		    System.out.println("  distsq, dist, factor: " +
				       distsq + ", " + dist + ", " +
				       factor);
		    System.out.println("  added " + diff);
		}
	    }
	} else { // Cell node
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    totalAcc.add( children[i].barnesBodyDbg(pos, debug) );
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
	    Vec3[] accs = new Vec3[bodies.length];

	    for (i = 0; i < bodies.length; i++) {
		bodyNumbers[i] = bodies[i].number;
		accs[i] = interactTree.barnesBody(bodies[i].pos);
	    }
	    result = new LinkedList();
	    result.add(bodyNumbers);
	    result.add(accs);

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
    public LinkedList barnes( BodyTreeNode interactTree, int threshold ) {
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
			    new BodyTreeNode(interactTree, children[i]);

			//alternative: copy whole tree
			//BodyTreeNode necessaryTree = interactTree;

			/*System.err.println("Interacttree:");
			  interactTree.print(System.err, 0);
			  System.err.println("Job:");
			  children[i].print(System.err, 0);
			  System.err.println("Necessarytree:");
			  necessaryTree.print(System.err, 0); */
			res[i] = children[i].barnes(necessaryTree,threshold-1);
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
    public LinkedList barnes(byte[] jobWalk, String rootId, int threshold) {
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

		    res[i] = barnes(newJobWalk, rootId, threshold);
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
	Vec3[] allAccs, accs = null;

	it = suboptimal.iterator();	//calculate totalElements
	while(it.hasNext()) {
	    bodyNrs = (int[]) it.next();
	    it.next(); // skip accs
	    totalElements += bodyNrs.length;
	}
	
	if (totalElements == 0) return new LinkedList(); //nothing to optimize

	allBodyNrs = new int[totalElements];
	allAccs = new Vec3[totalElements];

	position = 0;
	it = suboptimal.iterator();
	while(it.hasNext()) {
	    bodyNrs = (int[]) it.next();
	    accs = (Vec3[]) it.next();

	    System.arraycopy(bodyNrs, 0, allBodyNrs, position, bodyNrs.length);
	    System.arraycopy(accs, 0, allAccs, position, accs.length);

	    position += bodyNrs.length;
	}

	optimal = new LinkedList();
	optimal.add(allBodyNrs);
	optimal.add(allAccs);

	return optimal;
    }

    /**
     * Computes the interactions between the bodies in 'bodies' and those
     * in 'this', by spawning a job for each body
     * The acc field in the body is set to the calculated interaction
     *
     * This version is actually *slower* than barnes(interactTree),
     * probably because barnes(interactTree) has better cache behaviour
     * due to better locality
     */
    public void barnes( Body[] bodies) {
	int i;

	Vec3[] accs = new Vec3[bodies.length];
	for (i = 0; i < bodies.length; i++) {
	    accs[i] = spawn_barnesBody(bodies[i].pos);
	}
	sync();
	for (i = 0; i < bodies.length; i++) {
	    bodies[i].acc = accs[i];
	    if (BarnesHut.ASSERTS) bodies[i].updated = true;
	}
    }

    //debug version of the method above
    public void barnesDbg( Body[] bodies, int iteration) {
	for (int i = 0; i < bodies.length; i++) {
	    bodies[i].acc = barnesBodyDbg(bodies[i].pos, i == 0);
	    //bodies[i].acc = barnesBodyDbg(bodies[i].pos, false);
	    if (BarnesHut.ASSERTS) bodies[i].updated = true;
	}
    }

    public void print(java.io.PrintStream out, int level) {
	int i, j;
	if (level == 0) {
	    out.println("halfSize = " + halfSize);
	    out.print("center");
	}

	out.print(" at " + center);

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
				    bodies[j].pos);
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
