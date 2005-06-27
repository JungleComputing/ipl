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
		if (this.comm.size() > 1) {
			for (int i = 0; i < this.comm.size(); i++) {
				byte[] sendbuf = {0};
				
				if (this.comm.rank() != i) {
					try {
						this.comm.send(sendbuf, 0, 1, MPJ.BYTE, i, this.tag);
					}
					catch (MPJException e) {
						throw e;
					}
				}
			}
			
			for (int i=0; i < this.comm.size(); i++) {
				byte[] recvbuf = {0};
				
				if (this.comm.rank() != i) {
					try {
						this.comm.recv(recvbuf, 0, 1, MPJ.BYTE, i, this.tag);
					}
					catch (MPJException e) {
						throw e;
					}
				}
			}
		}
	}
}
