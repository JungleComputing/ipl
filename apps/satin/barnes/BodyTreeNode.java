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
    int bodyIndex = -1; //index of the body at the main node

    /* The part of space this node represents.
       Leaf nodes might also need this in an alternative barneshut
       implementation, to find out if a body has moved out of
       the represented part
       The fields are only needed during tree construction, so they can be made
       transient.
    */
    transient Vec3 center;
    transient double halfSize;

    static final double SOFT_SQ = 0.00000000000625;

    /* these are used during the force calculation */
    private double maxTheta;  //set during initilisation
    private Vec3 centerOfMass; //set during CoM computation
    private double totalMass;

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
     * Generates a new tree with the specified bodies, with dimensions
     * exactly large enough to contain all bodies
     */
    public BodyTreeNode( Body[] bodyArray, double theta) {
	int i;
	Vec3 max, min;

	if (bodyArray.length == 0) {
	    center = new Vec3(0.0, 0.0, 0.0);
	    halfSize = 0.0;
	    return;
	}

	max = new Vec3();
	min = new Vec3();
		
	for (i = 0; i < bodyArray.length; i++) {
	    max.max(bodyArray[i].pos);
	    min.min(bodyArray[i].pos);
	}

	initCenterSizeMaxtheta(max, min, theta);

	for (i = 0; i < bodyArray.length; i++) {
	    addBodyNoChecks(bodyArray, i);
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
     * adds the body to the tree with this node as root, checking
     * if the body is in range
     * @return true: body is in range and is added
     *        false: body is out of range and is not added
     */
    /*public boolean addBody( Body b ) {
	if (outOfRange(b.pos)) {
	    return false;
	} else {
	    addBodyNoChecks(b);
	    return true;
	}
	}*/

    /**
     * Adds a body to 'this' or its children
     * @param bodyArray the array with all bodies, (used when splitting up)
     * @param index the index of the body in 'bodyArray' to add
     */
    private void addBodyNoChecks( Body[] bodyArray, int index ) {
	if (children != null) { // cell node
	    addBody2Cell(bodyArray, index);
	} else {
	    if (bodyIndex == -1) { //empty tree
		bodyIndex = index;
	    } else {
		/* we are a leaf, and we'll have to convert ourselves
		   to a cell */
		children = new BodyTreeNode[8];
		addBody2Cell(bodyArray, bodyIndex);
		addBody2Cell(bodyArray, index);
		bodyIndex = -1;
	    }
	}
    }

    /**
     * This method is used if 'this' is a cell, to add a body to the appropiate
     * child. It shouldn't touch the 'body' field.
     */
    private void addBody2Cell( Body[] bodyArray, int index ) {
	int child = 0;
	Vec3 diff, newCenter;

	diff = new Vec3(bodyArray[index].pos);
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
	    children[child].bodyIndex = index;
	} else {
	    children[child].addBodyNoChecks(bodyArray, index);
	}
    }

    public int bodyCount() {
	int i, bodies = 0;
	if (children == null) {
	    if (bodyIndex != -1) return 1; else return 0;
	} else {
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) bodies += children[i].bodyCount();
	    }
	}
	return bodies;
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

    public void computeCentersOfMass(Body[] bodies) {
	if (children == null) {
	    //leaf node
	    
	    if (BarnesHut.DEBUG && bodyIndex == -1) {
		System.err.println("computeCoM: Found empty leaf node!");
		return;
	    }
	    centerOfMass = new Vec3(bodies[bodyIndex].pos);
	    totalMass = bodies[bodyIndex].mass;
	} else {
	    /* cell node
	       -> first process all children, then compute my center-of-mass */
	    int i;

	    //??? maybe parallize this later, then the loop has to be split up
	    centerOfMass = new Vec3();
	    totalMass = 0.0;
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    children[i].computeCentersOfMass(bodies);
		
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

	if (children == null || distsq >= maxTheta) {

	    /* We are calculating a body <> body interaction, or the
	       distance was large enough to use 'tree' instead
	       of iterating its children */
	    distsq += SOFT_SQ;
	    dist = Math.sqrt(distsq);
	    factor = totalMass / (distsq * dist);

	    diff.mul(factor);

	    return diff;

	} else {

	    // We are processing a cell node and the distance was too small
	    Vec3[] accs = new Vec3[8];
	    Vec3 totalAcc = new Vec3();
			
	    for (i = 0; i < 8; i++) {
		if (children[i] != null) {
		    //accs[i] = children[i].barnes( b );

		    //??? satin semantiek hiervan??
		    totalAcc.add( children[i].barnes( pos ) );
		}
	    }
	    //sync();
			
	    /*for (i = 0; i < 8; i++) {
	      if (accs[i] != null) totalAcc.add(accs[i]);
	      }*/
	    return totalAcc;
	}
    }

    public Vec3 spawn_barnes (Vec3 pos) {
	return barnes(pos);
    }

    /**
     * computes the iteractions between [ the bodies in 'this' ]
     * and [ 'interactTree' ], by recursively splitting up 'interactTree', and
     * calling this.barnes(interactTree.centerOfMass) when we have to process
     * a leaf node. 'centerOfMass' in a leaf node is the body position in
     * that node
     * @param bm (remote) reference to the object that manages the body-array
     */
    public void barnes( BodyTreeNode interactTree, BodyManager bm ) {
	Vec3 acc;
	if (interactTree.children == null) {
	    if (BarnesHut.DEBUG && interactTree.bodyIndex == -1) {
		System.err.println("BodyTreeNode.barnesTree: " +
				   "found empty leafnode!");
		return;
	    }
	    acc = barnes(interactTree.centerOfMass);
	    /* the bodies aren't used in the force calculation
	       (the center-of-mass-fields in the leaf nodes are used instead)
	       the body could thus be updated now */
	    try {
		bm.setAcc(interactTree.bodyIndex, acc);
	    } catch (RemoteException e) {
		System.err.println("EEK! RMI call to update body failed!");
		System.exit(1);
	    }
	} else {
	    for (int i = 0; i < 8; i++) {
		if (interactTree.children[i] != null) {
		    barnes(interactTree.children[i], bm);
		}
	    }
	    sync();
	}
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
