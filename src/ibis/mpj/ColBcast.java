/*
 * Created on 18.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: bcast.
 */
public class ColBcast {
	private final boolean DEBUG = false;
	
	private Object sendbuf = null;
	private int offset, count, root = 0;
	private Datatype datatype = null;
	private Intracomm comm = null;
	private int tag = 0;
	
	public ColBcast(Object sendbuf, int offset, int count, Datatype datatype, int root, Intracomm comm, int tag) {
		this.sendbuf = sendbuf;
		this.offset = offset;
		this.count = count;
		this.root = root;
		this.datatype = datatype;
		this.comm = comm;
		this.tag = tag;
	}
	
	// binomial tree (taken from CCJ)
	protected void call() throws MPJException {
		int rank_rel;
		int mask, sum;
	

		int rank = this.comm.rank();
		int size = this.comm.size();

		
		if (root < 0 || root >= size) {
			throw new MPJException("root rank " + root + " is invalid.");
		}

		if (size == 1) {
			return;
		}

		

		if (DEBUG) {
			System.out.println(rank + ": broadcast started, root = " +
			       root + " groupsize="+ size + " counter << = " +
			       this.tag);
		}

	

		rank_rel = rel_rank(rank, root, size);

		for (mask = 1; mask < size; mask *= 2);

		mask /= 2;
		sum = 0;

		try {
			while (mask > 0) {
				if (sum + (mask & rank_rel) == rank_rel) {
		

					if ((mask & rank_rel) != 0) {
						if(DEBUG) {
							System.out.println(rank + ": ColBcast: receive from "+ abs_rank(rank_rel - mask, root, size));
						}	

						this.comm.recv(this.sendbuf, this.offset, this.count, this.datatype, abs_rank(rank_rel - mask, root, size), this.tag);	
						
					} else if ((rank_rel + mask) < size) {
						if (DEBUG) {
							System.out.println(rank + ": ColBcast: send to " + abs_rank(rank_rel + mask, root, size));
						}
						this.comm.send(this.sendbuf, this.offset, this.count, this.datatype, abs_rank(rank_rel + mask, root, size), this.tag);
					}
				}

				sum += (mask & rank_rel);
				mask /= 2;
			}
		} catch (Exception e) {
			throw new MPJException(e.toString());
		}

		if (DEBUG) {
	    	System.out.println(rank + ": broadcast done.");
	    }
	    	
	
		
	}
	
	
	private int rel_rank(int ABS, int ROOT, int SIZE) {
		return ((SIZE + ABS - ROOT) % SIZE);
    }


    private int abs_rank(int REL, int ROOT, int SIZE) {
		return ((REL + ROOT) % SIZE);
    }

}
