/*
 * Created on 18.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: gatherv
 */
public class ColGatherV {
	private final boolean DEBUG = true;

	private Object sendbuf = null;
	private int sendoffset = 0;
	private int sendcount = 0;
	private Datatype sendtype = null;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int[] recvcount = null;
	private int[] displs = null;
	private Datatype recvtype = null;
	private int root = 0;
	private Intracomm comm = null;
	private int tag = 0;
	
	public ColGatherV(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
					  Object recvbuf, int recvoffset, int[] recvcount, int[] displs,
					  Datatype recvtype, int root, Intracomm comm, int tag) {
		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.sendcount = sendcount;
		this.sendtype = sendtype;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.recvcount = recvcount;
		this.displs = displs;
		this.recvtype = recvtype;
		this.root = root;
		this.comm = comm;
		this.tag = tag;
	}

	// flat tree
	protected void call() throws MPJException {
		int rank = this.comm.rank();
		int size = this.comm.size();

		if (root < 0 || root >= size) {
			throw new MPJException("root rank " + root + " is invalid.");
		}

		if (rank == root) {
			this.comm.localcopy2types(sendbuf, sendoffset, sendcount, sendtype,
					recvbuf, recvoffset + (displs[rank] * recvtype.extent()), recvcount[rank], recvtype);
			
			//this.comm.send(this.sendbuf, this.sendoffset, this.sendcount, this.sendtype, root, this.tag);
			
		
			for (int i=0; i < size; i++) {
				if (i != rank)
					this.comm.recv(recvbuf, this.recvoffset + displs[i] * recvtype.extent(), this.recvcount[i], this.recvtype, i, this.tag);
			}
	
		}
		else {
			this.comm.send(this.sendbuf, this.sendoffset, this.sendcount, this.sendtype, root, this.tag);
		}
	}
}
