/*
 * Created on 07.04.2005
 */
package ibis.mpj;

/**
 * Communicator for virtual cartesian topologies.
 */
public class Cartcomm extends Intracomm{
	
	Cartcomm() throws MPJException {
		// does nothing
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return(null);
	}
	
	
	// Topology constructors 
	
	/**
	 * 	
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Select a balanced distribution of processes per coordinate direction.
	 * Number of dimensions ist the size of dims. Note that dims is an inout parameter.
	 * 
	 * @param nnodes number of nodes in a grid
	 * @param dims array of Cartesian dimensions
	 * @throws MPJException 
	 */
	static public void dimsCreate(int nnodes, int[] dims) throws MPJException {
		// not implemented
	}
	
	
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Returns Cartesian topology information.
	 * The number of dimensions can be obtained from the size of (eg) the dims array.
	 * 
	 * @return cartparms object
	 * @throws MPJException
	 */
	public CartParms get() throws MPJException {
		return(null);
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Translate logical process coordinates to process rank.
	 * 
	 * @param coords Cartesian coordinates of a process
	 * @return rank of the specified process
	 * @throws MPJException
	 */
	public int rank(int[] coords) throws MPJException {
		return(0);
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Translate process rank to logical proces coordinates.
	 * 
	 * @param rank rank of a process
	 * @return Cartesian coordinates of the specified process
	 * @throws MPJException
	 */
	public int[] coords(int rank) throws MPJException {
		return(null);
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Compute source and destination ranks for "shift" communication. 
	 * 
	 * @param direction coordinate dimension of shift
	 * @param disp displacement
	 * @return shiftparms object
	 * @throws MPJException
	 */
	public ShiftParms shift(int direction, int disp) throws MPJException {
		return(null);
	}
	

	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Partition Cartesian communicator into subgroups of lower dimension.
	 * 
	 * @param remainDims by dimension, true if dimension is to be kept, false otherwise
	 * @return communicator containing subgrid including this process
	 * @throws MPJException
	 */
	public Cartcomm sub(boolean[] remainDims) throws MPJException {
		return(null);
	}
	
	
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Comput an optimal placement.
	 * The number of dimensions is taken to be size of the dims argument.
	 * 
	 * @param dims the number of processes in each dimension
	 * @param periods true if grid is periodic, false if not, in each dimension
	 * @return reordered rank of calling process
	 * @throws MPJException
	 */
	public int map(int[] dims, boolean[] periods) throws MPJException {
		return(0);
	}
	
	
}
