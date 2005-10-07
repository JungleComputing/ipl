
import ibis.satin.*;
import java.util.LinkedList;

public interface BarnesHutInterface extends Spawnable {
    
    public LinkedList computeForces(byte[] nodeId, int iteration,
				    int threshold, Bodies bodies);

    public LinkedList computeForcesNoSO(BodyTreeNode treeNode,
					BodyTreeNode interactTree,
					int threshold);

}
