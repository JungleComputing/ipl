import java.io.*;
import java.util.*;
import java.rmi.*;

/**
 * An oct tree is designed as follows:
 * A node has two modes:
 * - cell node, with zero to eight children (children != null)
 * - a leaf node                            (children == null)
 * The 'children' field must be used to distinguish between the two modes.
 *
 * A leaf node has a body (when children == null)
 * The body field can also still be null. This indicates an empty tree.
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
    int bodyCount;

    /* The part of space this node represents.
       The fields are only used during tree construction, so they can be made
       transient.
       In an alternative barneshut implementation, these fields could be
       used (in a leaf node) to find out if a body has moved out of the
       represented part */
    private transient Vec3 center;
    private transient double halfSize;

    /* these 4 variables are used during the force calculation */
    static final double SOFT_SQ = 0.00000000000625;
    private double maxTheta;  //set during initilisation
    private Vec3 centerOfMass; //set during CoM computation
    private double totalMass;  //set during CoM computation

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
	   floating point inaccuracy */
	size *= 1.000001;

	halfSize = size / 2.0;

	maxTheta = theta * theta * halfSize * halfSize;
    }

    //constructor to create an empty tree
    private BodyTreeNode(Vec3 center, double halfSize, double maxTheta) {
	//children = null and body = -1 by default
	this.center = new Vec3(center);
	this.halfSize = halfSize;
	this.maxTheta = maxTheta;
    }

    /**
     * Generates a new tree with the specified bodies, with dimensions
     * exactly large enough to contain all bodies
     */
    /*public BodyTreeNode ( List bodies ) {
	Vec3 max, min;
	double size;
	Iterator it;
	Body b;

	if (bodies.size() == 0) {
	    center = new Vec3();
	    halfSize = 0.0;
	    return;
	}

	max = new Vec3();
	min = new Vec3();
		
	it = bodies.iterator();
	while (it.hasNext()) {
	    b = (Body)it.next();
	    max.max(b.pos);
	    min.min(b.pos);
	}

	initCenterSize(max, min, theta);

	it = bodies.iterator();
	while (it.hasNext()) {
	    addBodyNoChecks((Body)it.next());
	}
	}*/

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
	    addBody(bodyArray[i], maxLeafBodies);
	}
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

    /**
     * print the amount that the point is out of range
     */
    private void printOutOfRange(PrintStream out, Vec3 pos) {
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
     * Adds 'body' to 'this' or its children
     */
    private void addBody( Body b, int maxLeafBodies ) {
	int i;

	if (children != null) { // cell node

	    addBody2Cell(b, maxLeafBodies);

	} else { //leaf node

	    if (BarnesHut.DEBUG && outOfRange(b.pos)) {
		System.err.println("EEK! Adding out-of-range body!");
		System.exit(1);
	    }

	    if (bodyCount < maxLeafBodies) { //we have room left
		if (bodyCount == 0) bodies = new Body[maxLeafBodies];
		bodies[bodyCount] = b;
		bodyCount++;
		totalMass += b.mass;
	    } else {
		/* we are a leaf, and we'll have to convert ourselves
		   to a cell */
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
     * child.    ??? stond er nog: It shouldn't touch the 'body' field.
     */
    private void addBody2Cell( Body b, int maxLeafBodies ) {
	int child = 0;
	Vec3 diff, newCenter;

	diff = new Vec3(b.pos);
	diff.sub(center);

	if (diff.x >= 0) child |= 1;
	if (diff.y >= 0) child |= 2;
	if (diff.z >= 0) child |= 4;

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

    /*public void print(PrintStream out) {
	out.println("root center: " + center + ", halfSize: " + halfSize);
	printRecursive(out, 0);
    }

    private void printSpaces(PrintStream out, int n) {
	for (int i = 0; i < n; i++) out.print(" ");
    }
			
    //prints the children or the body
    private void printRecursive(PrintStream out, int depth) {
	int i;

	if (children == null) {
	    //leaf node
	    printSpaces(out, depth);
	    if (body == null) {
				//empty tree
		out.println("empty tree");
	    } else {
		out.println("body at: " + body.pos + ", vel: " + body.vel);
	    }
	} else {
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    printSpaces(out, depth);
		    out.println(i + ": center: " + children[i].center +
				" halfSize: " + children[i].halfSize);
		    children[i].printRecursive(out, depth + 1);
		}
	    }
	}
	}*/

    public void computeCentersOfMass() {
	int i;

	centerOfMass = new Vec3();
	totalMass = 0.0;

	if (children == null) {
	    //leaf node
	    
	    if (BarnesHut.DEBUG && (bodyCount == 0 || bodies == null) ) {
		System.err.println("computeCoM: Found empty leaf node!");
		return;
	    }

	    for (i = 0; i < bodyCount; i++) {
		centerOfMass.x += bodies[i].pos.x * bodies[i].mass;
		centerOfMass.y += bodies[i].pos.y * bodies[i].mass;
		centerOfMass.z += bodies[i].pos.z * bodies[i].mass;
		totalMass += bodies[i].mass;
	    }
	    centerOfMass.div(totalMass);

	} else {
	    // cell node
	    // -> first process all children, then compute my center-of-mass

	    //??? maybe satinize this later, then the loop has to be split up
	    //!!! if it is parallelized, watch out! Body.mass is transient!
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
     */
    public Vec3 barnes( Vec3 pos ) {
	Vec3 diff;
	double dist, distsq, factor;
	int i;

	diff = new Vec3(centerOfMass);
	diff.sub(pos);

	distsq = diff.x * diff.x + diff.y * diff.y + diff.z * diff.z;

	if (distsq >= maxTheta) {

	    /* The distance was large enough to use my centerOfMass instead
	       of iterating my children */
	    distsq += SOFT_SQ;
	    dist = Math.sqrt(distsq);
	    factor = totalMass / (distsq * dist);

	    diff.mul(factor);

	    return diff;

	} else {
	    Vec3 totalAcc = new Vec3();

	    if (children == null) {
		// Leaf node, compute interactions with all my bodies
		for (i = 0; i < bodyCount; i++) {
		    diff = new Vec3(bodies[i].pos);
		    diff.sub(pos);

		    distsq = diff.x * diff.x + diff.y * diff.y;
		    distsq += diff.z * diff.z + SOFT_SQ;
		    dist = Math.sqrt(distsq);
		    factor = bodies[i].mass / (distsq * dist);

		    diff.mul(factor);

		    totalAcc.add(diff);
		}
	    } else {
		// Cell node
		for (i = 0; i < 8; i++) {
		    if (children[i] != null) {
			totalAcc.add( children[i].barnes( pos ) );
		    }
		}
	    }
 
	    return totalAcc;
	}
    }

    public Vec3 spawn_barnes (Vec3 pos) {
	return barnes(pos);
    }

    /**
     * computes the iteractions between [ the bodies in 'this' ]
     * and [ 'interactTree' ], by recursively splitting up 'interactTree', and
     * calling this.barnes(iT.bodies[i].pos) for all bodies in interactTree
     * when we have to process a leaf node.
     * @param bm (remote) reference to the object that manages the body-array
     */
    public void spawn_barnes( BodyTreeNode interactTree, BodyManager bm ) {
	int i;

	if (interactTree.children == null) {
	    if (BarnesHut.DEBUG && (interactTree.bodies == null || 
				    interactTree.bodyCount <= 0)) {
		System.err.println("BodyTreeNode.barnesTree: " +
				   "found empty leafnode!");
		return;
	    }
	    int [] bodyNumbers = new int[interactTree.bodyCount];
	    Vec3[] accs = new Vec3[interactTree.bodyCount];
	    for (i = 0; i < interactTree.bodyCount; i++) {
		bodyNumbers[i] = interactTree.bodies[i].number;
		accs[i] = barnes(interactTree.bodies[i].pos);
	    }
	    try {
		bm.setAccs(bodyNumbers, accs);
	    } catch (RemoteException e) {
		System.err.println("EEK! RMI call to update bodies failed!");
		System.exit(1);
	    }
	} else {
	    for (i = 0; i < 8; i++) {
		if (interactTree.children[i] != null) {
		    spawn_barnes(interactTree.children[i], bm);
		}
	    }
	    sync();
	}
    }

    /**
     * computes the iteractions between [ the bodies in 'this' ]
     * and [ 'interactTree' ], by recursively splitting up 'interactTree', and
     * calling this.barnes(iT.bodies[i].pos) for all bodies in interactTree
     * when we have to process a leaf node.
     * @param bm (remote) reference to the object that manages the body-array
     */
    public LinkedList spawn_barnes( BodyTreeNode interactTree ) {
	LinkedList result;
	int i;

	if (interactTree.children == null) {

	    if (BarnesHut.DEBUG && (interactTree.bodies == null || 
				    interactTree.bodyCount <= 0)) {
		System.err.println("BodyTreeNode.barnesTree: " +
				   "found empty leafnode!");
		return new LinkedList();
	    }
	    int [] bodyNumbers = new int[interactTree.bodyCount];
	    Vec3[] accs = new Vec3[interactTree.bodyCount];

	    for (i = 0; i < interactTree.bodyCount; i++) {
		bodyNumbers[i] = interactTree.bodies[i].number;
		accs[i] = barnes(interactTree.bodies[i].pos);

	    }
	    result = new LinkedList();
	    result.add(bodyNumbers);
	    result.add(accs);

	} else {

	    LinkedList childres[] = new LinkedList[8];
	    int lastValidChild = -1;

	    for (i = 0; i < 8; i++) {
		if (interactTree.children[i] != null) {
		    childres[i] = spawn_barnes(interactTree.children[i]);
		    lastValidChild = i;
		}
	    }
	    sync();

	    if (BarnesHut.DEBUG && lastValidChild < 0) {
		System.err.println("EEK! All children are null!");
		System.exit(1);
	    }
	    
	    result = childres[lastValidChild];
	    for (i = 0; i < lastValidChild; i++) {
		if (childres[i] != null) {
		    result.addAll(childres[i]);
		}
	    }
	}
	return result;
    }

    /**
     * Computes the interactions between the bodies in 'bodies' and those
     * in 'this', by spawning a job for each body
     * The acc field in the body is set to the calculated interaction
     */
    public void barnes( Body[] bodies ) {
	Vec3[] accs = new Vec3[bodies.length];
	int i;
	for (i = 0; i < bodies.length; i++) {
	    accs[i] = spawn_barnes(bodies[i].pos);
	}
	sync();
	for (i = 0; i < bodies.length; i++) {
	    bodies[i].acc = accs[i];
	}
    }
}
