/* $Id: */

import ibis.satin.*;
import java.util.ArrayList;

public interface BarnesHutInterface extends Spawnable {
    
    public ArrayList doBarnesSO(byte[] nodeId, int iteration,
				    int threshold, BodiesSO bodies);
    public ArrayList barnesNTC(BodyTreeNode me, BodyTreeNode tree,
            int threshold, RunParameters params);
}
