import ibis.util.PoolInfo;

import ibis.rmi.*;

final class Main { 

    public static boolean verbose = false;

    public static final double MB = (1024.0*1024.0);

    private static long one_way(myServer s, int count, Object data) throws Exception { 

	long start = System.currentTimeMillis();
		
	for (int i=0;i<count;i++) { 
	    s.one(data);
	} 
		
	long end = System.currentTimeMillis();			

	if (verbose) System.out.println("One way test took " + (end-start) + " milliseconds");

	return (end-start);
    } 

    private static long two_way(myServer s, int count, Object data) throws Exception { 

	long start = System.currentTimeMillis();
		
	for (int i=0;i<count;i++) { 
	    s.two(data);
	} 
		
	long end = System.currentTimeMillis();	

	if (verbose) System.out.println("Two way test took " + (end-start) + " milliseconds");
	return (end-start);
    } 

    private static long runTest(myServer s, int count, int retries, Object data, boolean one_way) throws Exception { 

	long best = 1000000;
	long temp;

	for (int i=0;i<retries;i++) { 
	    if (one_way) { 
		temp = one_way(s, count, data);
	    } else { 
		temp = two_way(s, count, data);
	    } 

	    if (temp < best) { 
		best = temp;
	    } 
	} 

	if (verbose) System.out.println("Best time : " + best + " milliseconds");

	return best;
    }

    private static double round(double v) { 
	return (Math.ceil(v*100.0)/100.0);
    } 

    private static double tp(int size, long time) { 		
	return round((1000.0*size / MB) / time);
    } 
	
    public static void main(String [] args) { 	

	try { 
	    boolean array = false, tree = false, list = false, dlist = false, oarray = false, one_way = true;
	    PoolInfo info = new PoolInfo();		
	    int i = 0;
	    int len = 1023;
	    int arraysize = 16*1024;
	    int count = 10000;
	    int retries = 10;
			
	    String server = info.hostName(1);
			
	    while (i < args.length) { 
		if (false) { 
		} else if (args[i].equals("-array")) { 
		    array = true;
		    i++;
		} else if (args[i].equals("-objectarray")) { 
		    oarray = true;
		    i++;
		} else if (args[i].equals("-list")) { 
		    list = true;
		    i++;
		} else if (args[i].equals("-dlist")) { 
		    dlist = true;
		    i++;
		} else if (args[i].equals("-tree")) { 
		    tree = true;
		    i++;
		} else if (args[i].equals("-twoway")) { 
		    one_way = false;
		    i++;
		} else if (args[i].equals("-verbose")) { 
		    verbose = true;
		    i++;
		} else if (args[i].equals("-len")) { 
		    len = Integer.parseInt(args[i+1]);
		    i += 2;
		} else if (args[i].equals("-arraysize")) { 
		    arraysize = Integer.parseInt(args[i+1]);
		    i += 2;
		} else if (args[i].equals("-count")) { 
		    count = Integer.parseInt(args[i+1]);
		    i += 2;
		} else if (args[i].equals("-retries")) { 
		    retries = Integer.parseInt(args[i+1]);
		    i += 2;
		} else if (args[i].equals("-server")) { 
		    server = args[i+1];
		    i += 2;
		} 
	    } 

	    if (info.rank() == 0) {
		myServer s = null;
		Object data;
		long time;

		do {
		    try { 
			Thread.sleep(1000);
			s = (myServer) Naming.lookup("//" + server + "/bla");
		    } catch (Exception e) { 
			// ignore.
		    } 
		} while (s == null);		    

		if (verbose) { 
		    System.out.println("client ok");
		}

		if (array) { 	
		    /*
		      data = new byte[arraysize];								
		      if (verbose) { 
		      System.out.println("starting byte[" + arraysize + "] test");
		      } 

		      time = runTest(s, count, retries, data, one_way);
		      System.out.println("byte[" + arraysize + "] = " + tp(arraysize*count, time) + " MBytes/sec.");

			
		      int alen = arraysize/4;
		      data = new int[alen];								
		      if (verbose) { 
		      System.out.println("starting int[" + alen + "] test");
		      } 

		      time = runTest(s, count, retries, data, one_way);
		      System.out.println("int[" + alen + "] = " + tp(arraysize*count, time) + " MBytes/sec.");

		      alen = arraysize/8;
		      data = new long[alen];				
		      if (verbose) { 
		      System.out.println("starting long[" + alen + "] test");
		      } 
				
		      time = runTest(s, count, retries, data, one_way);
		      System.out.println("long[" + alen + "] = " + tp(arraysize*count, time) + " MBytes/sec.");
		    */

		    int alen = arraysize/8;
		    data = new double[alen];				

		    if (verbose) { 
			System.out.println("starting double[" + alen + "] test");
		    } 
				
		    time = runTest(s, count, retries, data, one_way);
		    System.out.println("double[" + alen + "] = " + tp(arraysize*count, time) + " MBytes/sec.");
		} 

		if (list) { 
		    data = new List(len);					

		    if (verbose) { 
			System.out.println("starting list(" + len + ") test");
		    } 

		    time = runTest(s, count, retries, data, one_way);
		    System.out.println("list(" + len + ") = " + tp(len*count*List.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (dlist) { 
		    data = new DList(len);				

		    if (verbose) { 
			System.out.println("starting dlist(" + len + ") test");
		    } 

		    time = runTest(s, count, retries, data, one_way);
		    System.out.println("dlist(" + len + ") = " + tp(len*count*DList.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (tree) {  
		    data = new Tree(len);					

		    if (verbose) { 
			System.out.println("starting tree(" + len + ") test");
		    } 

		    time = runTest(s, count, retries, data, one_way);
		    System.out.println("tree(" + len + ") = " + tp(len*count*Tree.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (oarray) {  
		    data = null;
		    Data [] temp = new Data[len];
		    for (int j=0;j<len;j++) { 
			temp[j] = new Data();
		    } 

		    if (verbose) { 
			System.out.println("starting object array[" + len + "] test");
		    }
					
		    time = runTest(s, count, retries, temp, one_way);
		    System.out.println("object[" + len + "] = " + tp(len*count*Data.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (verbose) System.out.println("Done");
				
		s.done();
		System.exit(0);
	    } else {
		Test t = new Test();
		Naming.bind("bla", t);		    

		if (verbose) { 
		    System.out.println("server ok");
		}

		t.pleaseWait();

		Thread.sleep(3000);
		if (verbose) System.out.println("Done");
		System.exit(1);
	    } 
	} catch (Exception e) { 
	    System.out.println("OOPS" + e);
	} 
    } 
} 


