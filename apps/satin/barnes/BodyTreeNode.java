import java.io.*;

/**
 * The tree of bodies is designed as follows:
 * A node has two modes:
 * - cell node, with zero or more children (children != null)
 * - a leaf node, with a body              (children == null)
 *   In this case the body field can still be null. This indicates an
 *   empty tree.
 */

/* TODO:
 * - boom-maak-fase optimaliseren door alleen bodies die van plek
 *   verwisselen te verplaatsen
 *   Let Op! Als deeltjes buiten de ruimte komen moet (eigenlijk) de hele
 *           boom opnieuw gebalanceerd worden.. 
 *           Alternatief: de root van te voren heel wijd maken (* 1.5 oid)
 *                        (hij is al ietsje wijder vanwege precisieproblemen)
 */

class BodyTreeNode implements Serializable {

	/* The part of space this node represents.
	   Leaf nodes also need this, to find out if a body has moved out of
	   the represented part */
	private Vec3 center;
	private double halfSize;

	private Vec3 centerOfMass;
	private double totalMass;

	//private BodyTreeNode parent;

	private BodyTreeNode children[];
	private Body body;

	//#ifdef DEBUG
	private static transient BodyTreeNode root; //VERY useful for debugging
	//#endif

	//constructor to create an empty tree
	public BodyTreeNode() {
		center = new Vec3(0.0, 0.0, 0.0);
		halfSize = 0.0;
		if (BarnesHut.DEBUG) root = this;
	}

	/**
	 * Generates a new tree with the specified bodies, with dimensions
	 * exactly large enough to contain all bodies
	 */
	public BodyTreeNode ( Body[] bodies ) {
		int i;
		Vec3 max, min;

		if (BarnesHut.DEBUG) root = this;

		if (bodies.length == 0) {
			center = new Vec3(0.0, 0.0, 0.0);
			halfSize = 0.0;
			return;
		}

		max = new Vec3(bodies[0].pos);
		min = new Vec3(bodies[0].pos);

		for (i = 1; i < bodies.length; i++) {
			max.max(bodies[i].pos);
			min.min(bodies[i].pos);
		}

		center = new Vec3( (max.x+min.x) / 2.0, (max.y+min.y) / 2.0,
						   (max.z+min.z) / 2.0 );
		halfSize = Math.max(max.x - min.x, max.y - min.y);
		halfSize = Math.max(halfSize, max.z - min.z) / 2.0;

		/* make halfSize a little bigger to compensate for very small
		   floating point inaccuracy */
		halfSize *= 1.000001;

		body = bodies[0]; //the first one is easy :-)
		for (i = 1; i < bodies.length; i++) {
			addBodyNoChecks(bodies[i]);
		}

	}

	//constructor to create a leaf node
	private BodyTreeNode(Vec3 center, double halfSize, Body b) {
		this.center = new Vec3(center);
		this.halfSize = halfSize;

		if (BarnesHut.DEBUG && outOfRange(b.pos)) {
			System.err.println("EEK! Trying to construct an incorrect " +
							   "BodyTreeNode leaf!");
			System.err.println("center = " + center);
			System.err.println("halfSize = " + halfSize);
			System.err.println("body is at: " + b.pos + 
							   ", differences that are out of range:");
			printOutOfRange(System.err, b.pos);
			System.exit(1);
		}

		//this.children is null by default
		this.body = b;
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
	public boolean addBody( Body b ) {
		if (outOfRange(b.pos)) {
			return false;
		} else {
			addBodyNoChecks(b);
			return true;
		}
	}

	private void addBodyNoChecks( Body b ) {
		if (children != null) { // cell node
			addBody2Cell( b );
		} else {
			if (body == null) { //empty tree
				body = b;
			} else {
				/* we are a body, and we'll have to convert ourselves
				   to a cell */
				children = new BodyTreeNode[8];
				addBody2Cell(body);
				addBody2Cell(b);
				body = null;
			}
		}
	}

	/**
	 * This method is used if this is a cell, to add a body to the appropiate
	 * child. It shouldn't touch the 'body' field.
	 */
	private void addBody2Cell( Body b ) {
		int index = 0;
		Vec3 diff, newCenter;

		diff = new Vec3(b.pos);
		diff.sub(center);

		if (diff.x >= 0) index |= 1;
		if (diff.y >= 0) index |= 2;
		if (diff.z >= 0) index |= 4;

		if (children[index] == null) {
			/* We could compute newCenter directly during the calculation of
			   childIndex, but with a large tree we'd have to do it at
			   every depth we pass while adding the node.. */
			newCenter = computeChildCenter(index);
			children[index] = new BodyTreeNode(newCenter, halfSize / 2.0, b);
		} else {
			children[index].addBodyNoChecks(b);
		}
	}

	public void print(PrintStream out) {
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
				out.println("body at: " + body.pos);
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
	}
}
