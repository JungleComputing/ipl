/*
 * Created on 16.03.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: gather
 */
public class ColGather {
	private final boolean DEBUG = false;
	
	
	private Object sendbuf = null;
	private int sendoffset = 0;
	private int sendcount = 0;
	private Datatype sendtype =  null;
	private Object recvbuf = null;
	private int recvoffset = 0;
	private int recvcount = 0;
	private Datatype recvtype = null;
	private int root = 0;
	private Intracomm comm = null;
	private int tag = 0;
	
	protected ColGather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
					 Object recvbuf, int recvoffset, int recvcount, Datatype recvtype, 
					 int root, Intracomm comm, int tag) {

		this.sendbuf = sendbuf;
		this.sendoffset = sendoffset;
		this.sendcount = sendcount;
		this.sendtype =  sendtype;
		this.recvbuf = recvbuf;
		this.recvoffset = recvoffset;
		this.recvcount = recvcount;
		this.recvtype = recvtype;
		this.root = root;
		this.comm = comm;
		this.tag = tag;

	}

	
	// flat tree:
	
	public void call() throws MPJException {
		int rank = this.comm.rank();
		int size = this.comm.size();

		
		if (root < 0 || root >= size) {
			throw new MPJException("root rank " + root + " is invalid.");
		}

		if (rank == root) {
			this.comm.localcopy2types(sendbuf, sendoffset, sendcount, sendtype,
								recvbuf, recvoffset + (rank * this.recvcount * recvtype.extent()), recvcount, recvtype);
			
			//this.comm.send(this.sendbuf, this.sendoffset, this.sendcount, this.sendtype, root, this.tag);
			
			Object recv = null;
			int position = 0;
			for (int i=0; i < size; i++) {
				
				if (i != rank)
				  this.comm.recv(recvbuf, this.recvoffset + (i * this.recvcount * recvtype.extent()), this.recvcount, this.recvtype, i, this.tag);
			
			}
	
		}
		else {
			this.comm.send(this.sendbuf, this.sendoffset, this.sendcount, this.sendtype, root, this.tag);
		}
	
	}
	
	
	
	
	/* alternate algorithm: binomial tree*/
	
	/*
	public void execute() throws MPJException {

		int rank = this.comm.rank();
		int size = this.comm.size();

		if (root < 0 || root >= size) {
		    throw new MPJException("root rank " + root);
		}
		
		if (size == 1) {
			this.comm.localcopy(sendbuf, sendoffset, sendcount, sendtype, recvbuf, recvoffset + root * (recvtype.Extent() + 1), recvcount, recvtype);
			//System.arraycopy(sendbuf, sendoffset, recvbuf, root, recvcount);
			return;
		}
		
		
		
		if (DEBUG) {
		    System.out.println(rank + ": gather started. Root = "+ root + " groupsize = " + size + " counter<< = " + this.tag);
		}
		Object tmpBuf = null; 
		int tmpLength = size * recvcount * recvtype.Extent();
		if (recvbuf instanceof byte[]) {
			tmpBuf = new byte[tmpLength];
		}
		else if (recvbuf instanceof char[]) {
			tmpBuf = new char[tmpLength];		
		}
		else if (recvbuf instanceof short[]) {
			tmpBuf = new short[tmpLength];	
		}
		else if (recvbuf instanceof boolean[]) {
			tmpBuf = new boolean[tmpLength];		
		}
		else if (recvbuf instanceof int[])  {
			tmpBuf = new int[tmpLength];
		}
		else if (recvbuf instanceof long[]) {
			tmpBuf = new long[tmpLength];
		}
		else if (recvbuf instanceof float[]) {
			tmpBuf = new float[tmpLength];	
		}
		else if (recvbuf instanceof double[]) {
			tmpBuf = new double[tmpLength];	
		}
		else {
			tmpBuf = new Object[tmpLength];
		}
		
		int mask = 1;

		int relrank = (rank - root + size) % size;
		Object resultArray = null;

		while (mask < size) {
			if ((mask & relrank) == 0) {

				int peer = (relrank | mask);

			    if (peer < size) {
			    	peer = (peer + root) % size;

			    	if (DEBUG) {
			    		System.out.println("gather: "+ rank + " waiting for data from: " + peer);
			    	}

				
			    	this.comm.recv(tmpBuf, 0, size * recvcount, this.recvtype, peer, this.tag);
				
			    	if (DEBUG) {
			    		System.out.println(rank + ": ColMember.gather: got "+ "resultarray from " + peer);
			    	}
				
				
			    	// Merging :-)
			    	
			    		int resArLength = size * recvcount * recvtype.Extent();
			    		if (recvbuf instanceof byte[]) {
			    			if (resultArray == null) {
			    				resultArray = new byte[resArLength];
			    			}
			    	
			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((byte[])tmpBuf)[i] != 0) {
			    					((byte[])resultArray)[i] = ((byte[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof char[]) {
			    			if (resultArray == null) {
			    				resultArray = new char[resArLength];
			    			}

			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((char[])tmpBuf)[i] != 0) {
			    					((char[])resultArray)[i] = ((char[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof short[]) {
			    			if (resultArray == null) {
			    				resultArray = new short[resArLength];
			    			}

			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((short[])tmpBuf)[i] != 0) {
			    					((short[])resultArray)[i] = ((short[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof boolean[]) {
			    			if (resultArray == null) {
			    				resultArray = new boolean[resArLength];
			    			}

			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((boolean[])tmpBuf)[i] != false) {
			    					((boolean[])resultArray)[i] = ((boolean[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof int[])  {
			    			if (resultArray == null) {
			    				resultArray = new int[resArLength];
			    			}

			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((int[])tmpBuf)[i] != 0) {
			    					((int[])resultArray)[i] = ((int[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof long[]) {
			    			if (resultArray == null) {
			    				resultArray = new long[resArLength];
			    			}

			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((long[])tmpBuf)[i] != 0) {
			    					((long[])resultArray)[i] = ((long[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof float[]) {
			    			if (resultArray == null) {
			    				resultArray = new float[resArLength];
			    			}
			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((float[])tmpBuf)[i] != 0) {
			    					((float[])resultArray)[i] = ((float[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else if (recvbuf instanceof double[]) {
			    			if (resultArray == null) {
			    				resultArray = new double[resArLength];
			    			}
			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((double[])tmpBuf)[i] != 0) {
			    					((double[])resultArray)[i] = ((double[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
			    		else {
			    			if (resultArray == null) {
			    				resultArray = new Object[resArLength];
			    			}
						
			    			for (int i = 0; i < tmpLength; i++) {		
			    				if (((Object[])tmpBuf)[i] != null) {
			    					((Object[])resultArray)[i] = ((Object[])tmpBuf)[i];
			    					//System.arraycopy(tmpBuf, i, resultArray, i, sendcount);
						    		
			    				}
			    			}
			    		}
						
			    	
				
			    }

			} else { // send and terminate 

			    int peer = ((relrank & (~mask)) + root) % size;

			    if (DEBUG) {
				System.out.println("Gather: "+rank + " sending data to "+ peer);
			    }
			    
			    if (resultArray == null) {
			    	int resArLength = size * recvcount * (recvtype.Extent() + 1);
			    	if (recvbuf instanceof byte[]) {
			    		resultArray = new byte[resArLength];
					}
					else if (recvbuf instanceof char[]) {
						resultArray = new char[resArLength];		
					}
					else if (recvbuf instanceof short[]) {
						resultArray = new short[resArLength];	
					}
					else if (recvbuf instanceof boolean[]) {
						resultArray = new boolean[resArLength];		
					}
					else if (recvbuf instanceof int[])  {
						resultArray = new int[resArLength];
					}
					else if (recvbuf instanceof long[]) {
						resultArray = new long[resArLength];
					}
					else if (recvbuf instanceof float[]) {
						resultArray = new float[resArLength];	
					}
					else if (recvbuf instanceof double[]) {
						resultArray = new double[resArLength];	
					}
					else {
						resultArray = new Object[resArLength];
						
					}
			    }
			    
			    this.comm.localcopy(sendbuf, sendoffset, sendcount, sendtype, resultArray, rank * recvcount * recvtype.Extent(), recvcount, recvtype);
			    //System.arraycopy(sendbuf, 0, resultArray, rank, sendcount);
			    
			    this.comm.send(resultArray, 0, size*recvcount, this.recvtype, peer, this.tag);
			    
			    break;
			}

			mask <<= 1;
		}

		if (DEBUG) {
		    System.out.println(rank + ": gather done.");
		}

		


		if (rank != root) {
		    return;
		}

		// Add object of root to array.
		//System.arraycopy(sendbuf, sendoffset, resultArray, root, sendcount);
		this.comm.localcopy(sendbuf, sendoffset, sendcount, sendtype, resultArray, root * recvcount *  recvtype.Extent(), recvcount, recvtype);
		
		
		System.arraycopy(resultArray, 0, recvbuf, recvoffset, size * recvcount * recvtype.Extent());
		
		
	}
	*/
}
