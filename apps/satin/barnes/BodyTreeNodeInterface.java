import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    LinkedList barnes( BodyTreeNode interactTree, int threshold);

    /* this one should actually be static */
    LinkedList barnes( byte[] jobWalk, String rootId, int threshold );

    Vec3 spawn_barnesBody( Vec3 pos );
}
