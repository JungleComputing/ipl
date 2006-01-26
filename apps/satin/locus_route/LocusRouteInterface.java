/* $Id$ */

import ibis.satin.Spawnable;
import java.util.LinkedList;

public interface LocusRouteInterface extends Spawnable {

    public LinkedList computeWires(LinkedList wires, CostArray costArray);
        
}
    
