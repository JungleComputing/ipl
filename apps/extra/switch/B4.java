class B4 extends B { 

    Thread waiter;

    B4() { 
	test = "synchronized method + try/catch blocks + interrupt";
    }

    synchronized void test(Thread t, boolean first, int count) throws Exception { 

	if (first) { 
	    // this one switches 'away' count times...
	    while (count > 0) { 				
		//System.err.println("A " + count);

		while (b) { 
		    waiter = t;
		    try { 
			wait();
		    } catch (InterruptedException e) { 
			// ignore;
		    } 
		} 
		count--;
		b = true;
		if (waiter != null) { 
		    waiter.interrupt();
		} 
	    } 

	    waiter = null;			
	} else { 	
	    // this one switches 'away' count times, but 
	    // does not wait for last one.
	    while (count > 0) { 
		//System.err.println("B " + count);

		while (!b) { 
		    waiter = t;
		    try { 
			wait();
		    } catch (InterruptedException e) { 
			// ignore;
		    } 
		} 
		b = false;
		count--;
		if (waiter != null) { 
		    waiter.interrupt();
		} 
	    } 
	}
    } 
} 



