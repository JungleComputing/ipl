/* $Id$ */

/*
 * Created on 07.04.2005
 */
package ibis.mpj;

/**
 * Communicator for virtal graph topologies.
 */
public class Graphcomm extends Intracomm {

    Graphcomm() throws MPJException {
    	// nothing here
    }

    public Object clone() {
        return(null);
    }

    // Topology Constructors

    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * 
     * Returns graph topology information.
     * The number of nodes and number of edges can be extracted from sizes of the index and edges arrays.
     * @return graphparms object
     * @throws MPJException
     */
    public GraphParms get() throws MPJException {
        return(null);
    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * 
     * Provides adjcency information for general graph topology.
     * The number of neighbours can be extracted from the size of the result.
     * 
     * @param rank rank of a process in the group of this communicator
     * @return array of ranks of neighbouring processes to one specified
     * @throws MPJException
     */
    public int[] neighbours(int rank) throws MPJException {
        return(null);
    }


    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * 
     * Comput an optimal placement.
     * The number of nodes is taken to be size of the index argument.
     * 
     * @param index node degrees
     * @param edges graph edges
     * @return reordered rank of calling process
     * @throws MPJException
     */
    public int map(int[] index, int[] edges) throws MPJException {
        return(0);
    }


}
