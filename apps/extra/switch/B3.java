class B3 extends B { 
	
	B3() { 
		test = "synchronized method, no try/cath blocks";
	} 

	synchronized void test(Thread t, boolean first, int count) throws Exception { 

		if (first) { 
			// this one switches 'away' count times...
			while (count > 0) { 				
				while (b) { 
					wait();
				} 
				count--;
				b = true;
				notifyAll();			
			} 

			// must wait for last one to come back		
			while (b) { 
				wait();
			} 
		} else { 	
			// this one switches 'away' count times, but 
			// does not wait for last one.
			while (count > 0) { 
				while (!b) { 
					wait();
				}
				b = false;
				count--;
				notifyAll();				
			}
		} 
	}
} 


