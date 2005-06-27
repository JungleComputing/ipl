/*
 * Created on 19.06.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: allreduce.
 */
public class ColAllreduce {
	
	private Object sendbuf = null;
	private int sendoffset = 0;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int count = 0;
	private Datatype datatype = null;
	private Op op = null;
	private Intracomm comm = null;
	private int tag = 0;
	
	ColAllreduce(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset, int count, Datatype datatype, Op op, Intracomm comm, int tag) throws MPJException {
		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.count = count;
		this.datatype = datatype;
		this.op = op;
		this.comm = comm;
		this.tag = tag;
	}
	
	// taken from MPICH
	protected void call() throws MPJException {
		int pof2 = 1;
		while (pof2 <= this.comm.size()) pof2 <<= 1;
		pof2 >>=1;
		int rem = this.comm.size()-pof2;
		int newrank = this.comm.rank() -rem;
		int mask = 0x1;

		int rank = this.comm.rank();
		
		
		
		
		int tempoffset = 0;
		
		Object tempbuf = null;
		if (sendbuf instanceof byte[]) {
			tempbuf = new byte[((byte[])this.recvbuf).length];

		}
		else if (sendbuf instanceof char[]) {
			tempbuf = new char[((char[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof short[]) {
			tempbuf = new short[((short[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof boolean[]) {
			tempbuf = new boolean[((boolean[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof int[])  {
			tempbuf = new int[((int[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof long[]) {
			tempbuf = new long[((long[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof float[]) {
			tempbuf = new float[((float[])this.recvbuf).length];		

		}
		else if (sendbuf instanceof double[]) {
			tempbuf = new double[((double[])this.recvbuf).length];		

		}
		else {

			tempbuf = new Object[((Object[])this.recvbuf).length];		
			
			Object[] obj = (Object[])sendbuf;
				
		}

		System.arraycopy(this.sendbuf, this.sendoffset, this.recvbuf, this.recvoffset, this.count * this.datatype.extent());
		
		
		if (rank < 2*rem) {
			if (rank % 2 == 0) { /* even */
		    
				this.comm.send(recvbuf, recvoffset, count, datatype, rank+1, this.tag);
				/* temporarily set the rank to -1 so that this
		           process does not pariticipate in recursive
		           doubling */
				newrank = -1; 
			}
		    else { /* odd */
		    	this.comm.recv(tempbuf, tempoffset, count, datatype, rank-1, this.tag);
		    	/* do the reduction on received data. since the
		    	   ordering is right, it doesn't matter whether
		    	   the operation is commutative or not. */

		    	this.op.call(tempbuf, tempoffset, recvbuf, recvoffset, count, datatype);
		    	newrank = rank / 2;
		    }
		}
		else  /* rank >= 2*rem */
			newrank = rank - rem;

		
		if (newrank != -1) {
			while (mask < pof2) {
				int newdst = newrank ^ mask;
				/* find real rank of dest */
		    
				int dst = (newdst < rem) ? newdst*2 + 1 : newdst + rem;
		    
			
			
				/* Send the most current data, which is in recvbuf. Recv
				 into tmp_buf */ 
				if (this.comm.rank() < dst) {
					this.comm.send(recvbuf, recvoffset, count, datatype, dst, this.tag);
					this.comm.recv(tempbuf, tempoffset, count, datatype, dst, this.tag);
				}
				else {
					this.comm.recv(tempbuf, tempoffset, count, datatype, dst, this.tag);
					this.comm.send(recvbuf, recvoffset, count, datatype, dst, this.tag);
				}
				
				/* tempbuf contains data received in this step.
				   recvbuf contains data accumulated so far */

				if (op.isCommute()  || (dst < this.comm.rank())) {
					/* op is commutative OR the order is already right */
					op.call(tempbuf, tempoffset, recvbuf, recvoffset, count, datatype);
				}
				else {
					/* op is noncommutative and the order is not right */
					op.call(recvbuf, recvoffset, tempbuf, tempoffset, count, datatype);
					/* copy result back into recvbuf */
					comm.localcopy(tempbuf, tempoffset, count, datatype, recvbuf, recvoffset, count, datatype);
				}
				mask <<= 1;
		  
			}		
		}
	
	
	
		/* In the non-power-of-two case, all odd-numbered */
		if (rank < 2*rem) {
			if ((rank % 2) != 0)  /* odd */
				this.comm.send(recvbuf, recvoffset, count, datatype, rank-1, this.tag);
			else  /* even */
				this.comm.recv(recvbuf, recvoffset, count, datatype, rank+1, this.tag);
		}
	}
}
