/* $Id$ */

import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
	//public void computeCentersOfMass();

	LinkedList barnesNTC(BodyTreeNode interactTree, int threshold);
}
