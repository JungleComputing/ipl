class B2 extends B { 

    private boolean b = false;

    B2() { 
	test = "normal method, synchronized blocks +  try/catch blocks";
    } 

    void test(Thread t, boolean first, int count) throws Exception { 

	if (first) { 
	    // this one switches 'away' count times...
	    while (count > 0) { 				
		synchronized (this) { 
		    while (b) { 
			try { 
			    wait();
			} catch (InterruptedException e) { 
			    // ignore;
			} 
		    } 
		    count--;
		    b = true;
		    notifyAll();
		}
	    } 

	    // must wait for last one to come back		
	    while (b) { 
		synchronized (this) { 
		    try { 
			wait();
		    } catch (InterruptedException e) { 
			// ignore;
		    } 
		}
	    } 
	} else { 	
	    // this one switches 'away' count times, but 
	    // does not wait for last one.
	    while (count > 0) { 
		synchronized (this) { 
		    while (!b) { 
			try { 
			    wait();
			} catch (InterruptedException e) { 
			    // ignore;
			} 
		    } 
		    b = false;
		    count--;
		    notifyAll();				
		} 
	    }
	} 
    }
} 


