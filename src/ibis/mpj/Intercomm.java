/*
 * Created on 07.02.2005
 */
package ibis.mpj;

/**
 * Communicator for exchanging messages between two groups.
 */
public class Intercomm extends Comm {
	
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * @throws MPJException
	 */
	public Intercomm() throws MPJException {
		
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 */
	public Object clone() {
		return(null);
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * Size of remote group.
	 * 
	 * @return number of processes in remote group of this communicator
	 * @throws MPJException
	 */
	public int remoteSize() throws MPJException {
		return(0);
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * Return the remote group.
	 * 
	 * @return remote group of this communicator
	 * @throws MPJException
	 */
	public Group remoteGroup() throws MPJException {
		return(null);
		//return(this.group());
	}
	
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * Create an intra communicator from the union of the two group in the inter
	 * communicator.
	 * 
	 * @param high true if local group has higher ranks in combined group
	 * @return new inter communicator
	 * @throws MPJException
	 */
	public Intracomm merge(boolean high) throws MPJException {
		return(null);
	}
	
	

}
