/* $Id$ */

class B { 

    boolean b = false;
    String test;

    B() { 
	test = "synchronized method + try/catch blocks";
    }

    synchronized void test(Thread t, boolean first, int count) throws Exception { 

	if (first) { 
	    // this one switches 'away' count times...
	    while (count > 0) { 				
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

	    // must wait for last one to come back		
	    while (b) { 
		try { 
		    wait();
		} catch (InterruptedException e) { 
		    // ignore;
		} 
	    } 
	} else { 	
	    // this one switches 'away' count times, but 
	    // does not wait for last one.
	    while (count > 0) { 
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
