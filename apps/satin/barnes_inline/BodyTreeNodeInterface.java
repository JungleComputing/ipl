import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    LinkedList barnes( BodyTreeNode interactTree, int threshold);

    /* this one should actually be static */
    LinkedList barnesTuple( byte[] jobWalk, String rootId, int threshold );

}
