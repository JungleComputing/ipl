/*
 * Created on 18.02.2005
*/
package ibis.mpj;

/**
 * Implementation of the collective operation: barrier.
 */

public class ColBarrier {
	private Intracomm comm = null;
	private int tag = 0;
	public ColBarrier(Intracomm comm, int tag) {
		this.comm = comm;
		this.tag = tag;
	}
	
	
	
	// flat tree algorithm
	protected void call() throws MPJException {
		int size = this.comm.size();
		
		if (size > 1) {
			int rank = this.comm.rank();
			byte[] sendbuf = {};
			
			if (rank > 0) {
				this.comm.send(sendbuf, 0, 0, MPJ.BYTE, 0, this.tag);
				
				this.comm.recv(sendbuf, 0, 0, MPJ.BYTE, 0, this.tag);
			}
			else {
			
				for (int i = 1; i < this.comm.size(); i++) {
					this.comm.recv(sendbuf, 0, 0, MPJ.BYTE, i, this.tag);
				}
				
				for (int i=1; i < this.comm.size(); i++) {
					this.comm.send(sendbuf, 0, 0, MPJ.BYTE, i, this.tag);
				}
			}
		}
	}
}
