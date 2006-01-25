/* $Id$ */

import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
	//public void computeCentersOfMass();

	LinkedList barnesNTC(BodyTreeNode interactTree, int threshold);

	/* these ones should actually be static */
	LinkedList barnesTuple(byte[] jobWalk, String rootId, int threshold);

	LinkedList barnesTuple2(byte[] jobWalk, int threshold);

}
