/* $Id$ */

import java.util.ArrayList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    ArrayList barnesNTC(BodyTreeNode interactTree, int threshold);
}
