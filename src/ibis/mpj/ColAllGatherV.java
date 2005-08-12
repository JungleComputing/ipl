/*
 * Created on 18.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: allgatherv.
 */
public class ColAllGatherV {
	
	private final boolean DEBUG = false;
	
	private Object sendbuf = null;
	private int sendoffset = 0;
	private int sendcount = 0;
	private Datatype sendtype = null;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int[] recvcount = null;
	private int[] displs = null;
	private Datatype recvtype = null;
	private Intracomm comm = null;
	private int tag = 0;
	
	
	public ColAllGatherV(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
	   Object recvbuf, int recvoffset, int[] recvcount, int[] displs, 
	   Datatype recvtype, Intracomm comm, int tag) {
		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.sendcount = sendcount;
		this.sendtype = sendtype;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.recvcount = recvcount;
		this.displs = displs;
		this.recvtype = recvtype;
		this.comm = comm;
		this.tag = tag;
		
	}
	
	// simple ring algorithm (taken from CCJ)
	protected void call() throws MPJException {
		int rank = this.comm.rank();
		int size = this.comm.size();
		
		this.comm.localcopy2types(sendbuf, sendoffset, sendcount, sendtype, 
							recvbuf, recvoffset + displs[rank] * recvtype.extent(), recvcount[rank], recvtype);

		
	    int left  = (size + rank - 1) % size;
	    int right = (rank + 1) % size;

	        

	    int j     = rank;
	    int jnext = left;
	    
	    for (int i = 1; i < size; i++) {
	    	Status stat = this.comm.sendrecv(recvbuf, recvoffset + displs[j] * recvtype.extent(), recvcount[j], recvtype, right, this.tag,
	    					   recvbuf, recvoffset + displs[jnext] * recvtype.extent(), recvcount[jnext], recvtype, left, this.tag);
	    	
	    	j = jnext;
			jnext = (size + jnext - 1) % size;

	    }

	
	}



  












}
