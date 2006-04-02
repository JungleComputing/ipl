/* $Id: */

import ibis.satin.*;
import java.util.ArrayList;

public interface BarnesHutInterface extends Spawnable {
    
    public BodyUpdates doBarnesSO(byte[] nodeId, int iteration,
				    BodiesSO bodies);
    public BodyUpdates barnesNTC(BodyTreeNode me, BodyTreeNode tree,
            RunParameters params);
}
