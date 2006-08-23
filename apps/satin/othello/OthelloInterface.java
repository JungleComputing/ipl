/* $Id$ */

public interface OthelloInterface extends ibis.satin.Spawnable {
    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
        short currChild, TranspositionTable tt) throws Done;
}
