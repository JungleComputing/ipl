import java.util.LinkedList;

interface BodyTreeNodeInterface extends ibis.satin.Spawnable {
    //public void computeCentersOfMass();

    public void barnes( BodyTreeNode interactTree, BodyManager bm );

    public LinkedList barnes( BodyTreeNode interactTree );

    public Vec3 spawn_barnes( Vec3 pos );
}
