class T extends Thread { 

    B b;	
    int count;
    boolean first;

    public T(B b, int count, boolean first) { 
	this.b = b;	
	this.count = count;
	this.first = first;
    } 

    public void run() { 

	try { 
	    // warmup
	    b.test(this, first, count);

	    // test
	    long start = System.currentTimeMillis();
	    b.test(this, first, count);
	    long end = System.currentTimeMillis();

	    if (first) { 
		System.out.println((2*count) + " switches took " + (end-start) + " ms.");
		System.out.println(((1000.0*(end-start))/(2.0*count)) + " usec/switch");				 
	    }
	} catch (Exception e) { 
	    System.err.println("OOPS2" + e);
	} 
    }
} 
