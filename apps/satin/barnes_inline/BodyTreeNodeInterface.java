import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    LinkedList barnes( BodyTreeNode interactTree, int threshold);

    /* these ones should actually be static */
    LinkedList barnesTuple( byte[] jobWalk, String rootId, int threshold );
    double[] barnesBodyTuple( double pos_x, double pos_y, double pos_z,
			      String rootId );

}
