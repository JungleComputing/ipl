/* $Id$ */

import java.util.List;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    List barnesNTC(BodyTreeNode interactTree, int threshold);
}
