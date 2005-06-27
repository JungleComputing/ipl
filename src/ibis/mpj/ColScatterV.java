/*
 * Created on 18.02.2005
*/
package ibis.mpj;

/**
 * Implementation of the collective operation: scatterv.
 */
public class ColScatterV {
	private Object sendbuf = null;
	private int sendoffset = 0;
	private int[] sendcount = null;
	private int[] displs = null;
	private Datatype sendtype = null;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int recvcount = 0;
	private Datatype recvtype = null;
	private int root = 0;
	private Intracomm comm = null;
	private int tag = 0;
	
	public ColScatterV(Object sendbuf, int sendoffset, int[] sendcount, int[] displs,
			  		   Datatype sendtype,	
					   Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
					   int root, Intracomm comm, int tag) {
		
		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.sendcount = sendcount;
		this.displs = displs;
		this.sendtype = sendtype;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.recvcount = recvcount;
		this.recvtype = recvtype;
		this.root = root;
		this.comm = comm;
		this.tag = tag;
	}

	
	//	flat tree algorithm,
	//	so we don't need to publish the sendcount[]-array
	//
	protected void call() throws MPJException {
		int rank = this.comm.rank();
		int size = this.comm.size();

		
		if (root < 0 || root >= size) {
			throw new MPJException("root rank " + root + " is invalid.");
		}
		
		if (rank == root) {
			int myPosition = 0;
			for (int i = 0; i < size; i++) {
				
				myPosition = this.sendoffset + this.displs[i] * sendtype.extent();
				
				this.comm.send(sendbuf, myPosition, sendcount[i], sendtype, i, this.tag);
					
			}
			
			this.comm.recv(recvbuf, recvoffset, recvcount, recvtype, root, this.tag);
		}
		else {
			this.comm.recv(recvbuf, recvoffset, recvcount, recvtype, root, this.tag);
		}
	
	}
}
