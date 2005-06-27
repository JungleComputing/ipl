/*
 * Created on 11.03.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: reduceScatter.
 */
public class ColReduceScatter {
	
	private Object sendbuf = null;
	private int sendoffset = 0;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int[] recvcounts = null;
	private Datatype datatype = null;
	private Op op = null;
	private Intracomm comm = null;
	private int tag = 0;
	
	public ColReduceScatter(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset, int[] recvcounts, 
			  		 Datatype datatype, Op op, Intracomm comm, int tag) throws MPJException {
		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.recvcounts = recvcounts;
		this.datatype = datatype;
		this.op = op;
		this.comm = comm;
		this.tag = tag;
		
	}

	protected void call() throws MPJException {
		int count = 0;
		
		for (int i = 0; i < this.comm.size(); i++) {
			count += recvcounts[i];
		}
		
		int[] displs = new int[this.comm.size()];
		displs[0] = 0;
		for (int i = 1; i < this.comm.size(); i++) {
			displs[i] = displs[i-1] + recvcounts[i];
		}
		
		
		this.comm.reduce(sendbuf, sendoffset, sendbuf, sendoffset, count, datatype, op, 0);
		this.comm.scatterv(sendbuf, sendoffset, recvcounts, displs, datatype, recvbuf, recvoffset, recvcounts[this.comm.rank()], datatype, 0);
	}

}
