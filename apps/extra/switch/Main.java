

class Main { 

    public static int COUNT = 100000;

    public static void main(String [] args) { 

	try { 
	    B b = null;
	    int which = 1;

	    if (args.length > 0) { 
		which = Integer.parseInt(args[0]);
	    } 

	    switch (which) { 
	    case 1: 
		b = new B();
		break;
	    case 2: 
		b = new B2();
		break;
	    case 3: 
		b = new B3();
		break;
	    case 4: 
		b = new B4();
		break;
	    } 

	    T t1 = new T(b, COUNT, false);
	    T t2 = new T(b, COUNT, true);

	    System.out.println(b.test);

	    t1.start();
	    t2.start();		
	} catch (Exception e) { 
	    System.out.println("OOPS" + e);
	} 
    } 
} 









