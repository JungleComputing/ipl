/*
 * Created on 21.01.2005
 */
package ibis.mpj;



/**
 * Request class for basic point-to-point communication.
 */
public class Request {
	
	
	protected transient Thread ibisMPJCommThread = null;
	protected transient IbisMPJComm ibisMPJComm = null; 
	
	private Status status = null;
	
	public Request() {
		
	}
	
	
	public void setIbisMPJComm(IbisMPJComm ibisMPJComm) {
		this.ibisMPJComm =  ibisMPJComm;
	}
	
	
	/**
	 * Blocks until the operation identified by the request is complete.
	 * After the call returns, the request object becomes inactive.
	 * @return status object
	 * @throws MPJException
	 */
	public Status Wait() throws MPJException {
		
		
		if (ibisMPJCommThread == null) {
		    return(null);
		}
			
		try {
		    //System.out.println("waiting");
			this.ibisMPJCommThread.join();
		    this.ibisMPJCommThread = null;
		}
		catch (Exception e) {
		    System.err.println(e.getMessage());
		    e.printStackTrace();
		}
		status = this.ibisMPJComm.getStatus();
		
		return(status);
		
	}
	
	/**
	 * Returns a status object if the operation identified by the request is complete, or a null reference otherwise.
	 * After the call, if the operation is complete (ie, if the return value of test is non-null),
	 * the request object becomes an inactive request.
	 *   
	 * @return status object
	 * @throws MPJException
	 */
	public Status test() throws MPJException {
		if (ibisMPJComm == null) {

			return(null);
		    
		}
		Status status;
		if (this.ibisMPJComm.isFinished()) {
			if (ibisMPJCommThread != null) {
				try {
					this.ibisMPJCommThread.join();
				}
				catch(InterruptedException e) {
					
				}
				this.ibisMPJCommThread = null;
			}
			
			return(this.ibisMPJComm.getStatus());
		}
		else {
			
			return(null);
		}
	}
	
	public void finalize() {
	}
	
	public boolean isVoid() throws MPJException {
		if (this.ibisMPJCommThread != null) {
			return(false);
		}
		else
			return(true);
	}
	
	/**
	 * Blocks until one of the operations associated with the active requests in the array has completed.
	 * The corresponding element of arrayOfRequests becomes inactive.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return status object
	 * @throws MPJException
	 */
	public static Status waitAny(Request[] arrayOfRequests) throws MPJException {
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		Status status = null;
		
		boolean check = false;
		while (!check) {
			for (int i=0; i < arrayOfRequests.length; i++) {
				if (arrayOfRequests[i] != null) {
					status = arrayOfRequests[i].test();
					if (status != null) {
						status.setIndex(i);
						check = true;
						break;
					}
				}
			}
		
		}
		return(status);
		
	}
	
	/**
	 * Tests for completion of either one or none of the operations associated with active requests.
	 * If some request completed, the index in arrayOfRequests of that request con be obtained from the 
	 * status object.
	 * If no request completed, testAny returns a null reference.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return status object or null reference
	 * @throws MPJException
	 */
	public static Status testAny(Request[] arrayOfRequests) throws MPJException {
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		Status status = null;
		for (int i=0; i < arrayOfRequests.length; i++) {
			if (arrayOfRequests[i] != null) {
				status = arrayOfRequests[i].test();
				if (status != null) {
					status.setIndex(i);
					break;
				}
			}
		}
		return(status);
		
	}
	
	/**
	 * Blocks until all of the operations associated with the active requests in the array have completed.
	 * The result array will be the same size as arrayOfRequests. On exit, requests become inactive. If the
	 * input value of arrayOfRequests contains any inactive requests, corresponding elements of the result
	 * array will contain null status references.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return array of status objects, or a null reference
	 * @throws MPJException
	 */
	public static Status[] waitAll(Request[] arrayOfRequests) throws MPJException {
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		Status[] status = new Status[arrayOfRequests.length];
		int tests = 0;
		
		
		for (int i=0; i < arrayOfRequests.length; i++) {
			status[i] = null;
			while (status[i] == null)
				status[i] = arrayOfRequests[i].test();
		}
		
		
		
		
		return(status);
		
	}
	
	
	/**
	 * Tests for completion of all of the operations associated with active requests.
	 * If all operations have completed, the exit values of the argument array and the result array
	 * are as for waitAll. If any operation has not completed, the result value is null and no element
	 * of the argument array is modified.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return array of status objects, or a null reference
	 * @throws MPJException
	 */
	public static Status[] testAll(Request[] arrayOfRequests) throws MPJException {
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		Status[] status = new Status[arrayOfRequests.length];
		for (int i=0; i < arrayOfRequests.length; i++) {
			if (arrayOfRequests[i] != null) {
				status[i] = arrayOfRequests[i].test();
			}
		}
		
		boolean check = false;
		for (int i=0; i < arrayOfRequests.length; i++) {
			if (status[i] != null) {
				check = true;
			}
		}
		
		if (!check) {
			status = null;
		}
			
		return(status);
	}

	
	
	/**
	 * Blocks until at least one of the operations associated with the active requests in the array has completed.
	 * The correspondig element in arrayOfRequests becomes inactive. If arrayOfRequests list contains no active
	 * requests, waitSome immediately returns a null reference.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return array of status objects, or a null reference
	 * @throws MPJException
	 */
	public static Status[] waitSome(Request[] arrayOfRequests) throws MPJException {
		
		System.out.println("waitsome");
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		
		Status[] status = new Status[arrayOfRequests.length];
		Status[] newStatus = null;
		Status[] newerStatus = null;
		boolean check = false;
		while(!check) {
			int count = 0;
			for (int i=0; i < status.length; i++) {
				if (arrayOfRequests [i] != null) {
				//	if (!arrayOfRequests[i].isVoid()) {
						status[i] = arrayOfRequests[i].test();
						if (status[i]!= null) {
							status[i].setIndex(i);
						}
						count++;
				//	}
				}
			}
			newStatus = new Status[count];
			
			int count2 = 0;
			for (int i=0; i<count; i++) {
				if (status[count] != null) {
					newStatus[count2] = status[count];
					count2++;
					check = true;
				}
			}
			
			if (check) {
				newerStatus = new Status[count2];
				for (int i=0; i < count2; i++) {
					newerStatus[i] = newStatus[i];
				}
			}
			
			if (count == 0) {
				//newerStatus = null;
				break;
			}
			
		}
		
		
		
		return(newerStatus);
	}
	
	
	/**
	 * Behaves like waitSome, except that it returns immediately.
	 * If no operation has completed, testSome returns an array of length zero and elements
	 * of arrayOfRequests are unchanged. Otherwise, arguments and return value are as for waitSome.
	 * 
	 * @param arrayOfRequests array of requests
	 * @return array of status objects, or a null reference
	 * @throws MPJException
	 */
	public static Status[] testSome(Request[] arrayOfRequests) throws MPJException {
		if ((arrayOfRequests == null) || (arrayOfRequests.length == 0)) {
			return(null);
		}
		
		Status[] status = new Status[arrayOfRequests.length];
		
		int count = 0;
		for (int i=0; i < arrayOfRequests.length; i++) {
			if (arrayOfRequests[i] != null) {
				status[count] = arrayOfRequests[i].test();
				if (status[count] != null) {
					status[count].setIndex(i);
					count++;
				}
			}
		}
		
		Status[] newStatus = new Status[count];
		for (int i = 0; i < count; i++) {
			newStatus[i] = status[i];
		}
		
		return(newStatus);
	}
}
