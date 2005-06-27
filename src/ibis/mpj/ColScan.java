/*
 * Created on 11.03.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: scan.
 */
public class ColScan {
	private final boolean DEBUG = false;
	
	private Object sendbuf = null;
	private int sendoffset = 0;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int count = 0;
	private Datatype datatype = null;
	private Op op = null;
	private Intracomm comm = null;
	private int tag = 0;
	
	
	public ColScan(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset,
			 int count, Datatype datatype, Op op, Intracomm comm, int tag) throws MPJException { 

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
	
	protected void call() throws MPJException {
		int size = this.comm.size();
		int rank = this.comm.rank();
	
		
		Object tempBuf = null;
		Object origin = null;
		
		if (rank == 0) {
			System.arraycopy(this.sendbuf, this.sendoffset, this.recvbuf, this.recvoffset, this.count * datatype.extent());
		}

		
		else {
		
		
			if (!op.isCommute()) {	
				if (sendbuf instanceof byte[]) {
					tempBuf = new byte[((byte[])this.recvbuf).length];
				}
				else if (sendbuf instanceof char[]) {
					tempBuf = new byte[((char[])this.recvbuf).length];
				}
				else if (sendbuf instanceof short[]) {
					tempBuf = new byte[((short[])this.recvbuf).length];
				}
				else if (sendbuf instanceof boolean[]) {
					tempBuf = new byte[((boolean[])this.recvbuf).length];
				}
				else if (sendbuf instanceof int[])  {
					tempBuf = new int[((int[])this.recvbuf).length];
				}
				else if (sendbuf instanceof long[]) {
					tempBuf = new byte[((long[])this.recvbuf).length];
				}
				else if (sendbuf instanceof float[]) {
					tempBuf = new byte[((float[])this.recvbuf).length];
				}
				else if (sendbuf instanceof double[]) {
					tempBuf = new byte[((double[])this.recvbuf).length];
				}
				else {
					tempBuf = new Object[((Object[])this.recvbuf).length];
				}  
				System.arraycopy(this.sendbuf, this.sendoffset, this.recvbuf, this.recvoffset, this.count * datatype.extent());

				this.comm.recv(tempBuf, recvoffset, count, datatype, rank - 1, tag);
			
				op.call(tempBuf, recvoffset, recvbuf, recvoffset, count, datatype);
			}
			else {
				this.comm.recv(recvbuf, recvoffset, count, datatype, rank - 1, tag);

				op.call(sendbuf, sendoffset, recvbuf, recvoffset, count, datatype);
			
			
			}
		}
		if (rank < (size - 1)) {
			this.comm.send(recvbuf, recvoffset, count, datatype, rank + 1, tag);
		}

	}
}
