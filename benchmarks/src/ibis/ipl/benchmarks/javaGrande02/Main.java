package ibis.ipl.benchmarks.javaGrande02;

/* $Id: Main.java 6720 2007-11-05 16:38:30Z ndrost $ */

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

final class Receiver implements MessageUpcall { 

    SendPort sport;
    ReceivePort rport;
    int count = 0;
    int max;
    boolean one;
    boolean stream;

    Receiver(ReceivePort rport, SendPort sport, int max, boolean one, boolean stream) {
	this.rport = rport;
	this.sport = sport;
	this.max = max;
	this.one = one;
	this.stream = stream;
    } 

    public void upcall(ReadMessage readMessage) { 

	try { 
	    readMessage.readObject();
	    readMessage.finish();

	    WriteMessage writeMessage = sport.newMessage();
	    writeMessage.finish();

	    count++;

	    if (count == max) { 
		synchronized (this) { 
		    notifyAll();
		}
	    }			
	} catch (Exception e) { 			
	    System.out.println("EEEEEK " + e);
	    e.printStackTrace();
	} 
    } 

    synchronized void finish() { 
	while (count < max) { 
	    try { 
		wait();
	    } catch (Exception e) { 
	    } 
	}		
    } 

    public void receive(int count) throws Exception { 
	if (one) {
	    //				System.out.println("expl. receive " + count);

	    for(int i = 0; i< count; i++) {
		ReadMessage readMessage = rport.receive();
		readMessage.readObject();
		readMessage.finish();

		if (!stream) { 
		    WriteMessage writeMessage = sport.newMessage();
		    writeMessage.send();
		    writeMessage.finish();
		} 
	    }	

	    if (stream) { 
		//				System.out.println("LAST SNED");
		WriteMessage writeMessage = sport.newMessage();
		writeMessage.finish();
	    } 
	} else {
	    for(int i = 0; i< count; i++) {
		ReadMessage readMessage = rport.receive();
		Object temp = readMessage.readObject();
		readMessage.finish();

		WriteMessage writeMessage = sport.newMessage();
		writeMessage.writeObject(temp);
		writeMessage.finish();
	    }	
	}
    } 
} 

final class Main { 

    public static boolean verbose = false;
    public static final double MB = (1024.0*1024.0);

    static Ibis ibis;
    static Registry registry;

    private static long one_way(ReceivePort rport, SendPort sport, int count, Object data, boolean stream) throws Exception { 

	//		System.out.println("one way " + count);

	long start = System.currentTimeMillis();

	for (int i=0;i<count;i++) { 
	    WriteMessage writeMessage = sport.newMessage();
	    writeMessage.writeObject(data);
	    writeMessage.finish();

	    if (!stream) { 
		ReadMessage readMessage = rport.receive();
		readMessage.finish();
	    }
	} 

	if (stream) { 
	    ReadMessage readMessage = rport.receive();
	    readMessage.finish();
	} 

	long end = System.currentTimeMillis();			

	if (verbose) System.out.println("One way test took " + (end-start) + " milliseconds");

	return (end-start);
    } 

    private static long two_way(ReceivePort rport, SendPort sport, int count, Object data) throws Exception { 

	long start = System.currentTimeMillis();

	for (int i=0;i<count;i++) { 
	    WriteMessage writeMessage = sport.newMessage();
	    writeMessage.writeObject(data);
	    writeMessage.finish();

	    ReadMessage readMessage = rport.receive();
	    readMessage.readObject();
	    readMessage.finish();
	} 

	long end = System.currentTimeMillis();	

	if (verbose) System.out.println("Two way test took " + (end-start) + " milliseconds");
	return (end-start);
    } 

    private static long runTest(ReceivePort rport, SendPort sport, 
	    int count, int retries, Object data, boolean one_way, boolean stream) throws Exception { 

	long best = 1000000;
	long temp;

	for (int i=0;i<retries;i++) { 
	    if (one_way) { 
		temp = one_way(rport, sport, count, data, stream);
	    } else { 
		temp = two_way(rport, sport, count, data);
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
	    int i           = 0;
	    int len         = 1023;
	    int arraysize   = 16*1024;
	    int count       = 10000;
	    int retries     = 10;
	    boolean upcalls = false;
	    boolean stream  = false;
	    boolean ibisSer = false;

	    int tests = 0;

	    while (i < args.length) { 
		if (false) {
		} else if (args[i].equals("-ibis")) { 
		    ibisSer = true;
		    i++; 
		} else if (args[i].equals("-upcalls")) { 
		    upcalls = true;
		    i++; 
		} else if (args[i].equals("-array")) { 
		    array = true;
		    i++;
		    tests += 4;
		} else if (args[i].equals("-objectarray")) { 
		    oarray = true;
		    i++;
		    tests++;
		} else if (args[i].equals("-list")) { 
		    list = true;
		    i++;
		    tests++;
		} else if (args[i].equals("-dlist")) { 
		    dlist = true;
		    i++;
		    tests++;
		} else if (args[i].equals("-tree")) { 
		    tree = true;
		    i++;
		    tests++;
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
		} else if (args[i].equals("-stream")) { 
		    stream = true;
		    i++;
		} else {
		    System.err.println("unknown option: " + args[i]);
		    System.exit(1);
		}
	    } 

	    IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.CLOSED_WORLD,
                    IbisCapabilities.ELECTIONS_STRICT);
            
            PortType t = new PortType(       
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.RECEIVE_AUTO_UPCALLS,
                    PortType.RECEIVE_EXPLICIT,
                    ibisSer ? PortType.SERIALIZATION_OBJECT_IBIS
                            : PortType.SERIALIZATION_OBJECT_SUN);
            
	    ibis = IbisFactory.createIbis(s, null, t);

	    if (verbose) { 
		System.out.println("Ibis created; getting registry ...");
	    }

	    registry = ibis.registry();

	    if (verbose) { 
		System.out.println("Got registry");
	    }

	    SendPort sport = ibis.createSendPort(t);					      
	    ReceivePort rport;

	    if (verbose) { 
		System.out.println("Got sendport");
	    }

            IbisIdentifier master = registry.elect("0");

	    if (master.equals(ibis.identifier())) {
		rport = ibis.createReceivePort(t, "test port");
		rport.enableConnections();
                IbisIdentifier other = registry.getElectionResult("1");
		sport.connect(other, "test port");

		Object data;
		long time;

		if (verbose) { 
		    System.out.println("client ok");
		}

		if (array) { 	
		    data = new byte[arraysize];								
		    if (verbose) { 
			System.out.println("starting byte[" + arraysize + "] test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("byte[" + arraysize + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");


		    int alen = arraysize/4;
		    data = new int[alen];								
		    if (verbose) { 
			System.out.println("starting int[" + alen + "] test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("int[" + alen + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");



		    alen = arraysize/8;
		    data = new long[alen];								
		    if (verbose) { 
			System.out.println("starting long[" + alen + "] test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("long[" + alen + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");

		    alen = arraysize/8;
		    data = new double[alen];				

		    if (verbose) { 
			System.out.println("starting double[" + alen + "] test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("double[" + alen + "] = " + tp((one_way ? arraysize*count : 2*arraysize*count), time) + " MBytes/sec.");
		} 

		if (list) { 
		    data = new List(len);					

		    if (verbose) { 
			System.out.println("starting list(" + len + ") test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("list(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*List.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (dlist) { 
		    data = new DList(len);				

		    if (verbose) { 
			System.out.println("starting dlist(" + len + ") test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("dlist(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*DList.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (tree) {  
		    data = new Tree(len);					

		    if (verbose) { 
			System.out.println("starting tree(" + len + ") test");
		    } 

		    time = runTest(rport, sport, count, retries, data, one_way, stream);
		    System.out.println("tree(" + len + ") = " + tp((one_way ? len*count : 2*len*count)*Tree.PAYLOAD, time) + " MBytes/sec.");
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

		    time = runTest(rport, sport, count, retries, temp, one_way, stream);
		    System.out.println("object[" + len + "] = " + tp((one_way ? len*count : 2*len*count)*Data.PAYLOAD, time) + " MBytes/sec.");
		} 

		if (verbose) System.out.println("Done");				

	    } else { 
                registry.elect("1");
                IbisIdentifier other = registry.getElectionResult("0");
		sport.connect(other, "test port");

		if (upcalls) {
		    Receiver receiver = new Receiver(null, sport, tests*retries*count, one_way, false);
		    rport = ibis.createReceivePort(t, "test port", receiver);
		    rport.enableConnections();
		    rport.enableMessageUpcalls();
		    receiver.finish();
		} else { 
		    rport = ibis.createReceivePort(t, "test port");
		    rport.enableConnections();
		    Receiver receiver = new Receiver(rport, sport, count, one_way, stream);

		    for (int r=0;r<tests*retries;r++) { 
			receiver.receive(count);
		    }
		}
	    }

	    /* free the send ports first */
	    sport.close();
	    rport.close();
	    ibis.end();
	} catch (Exception e) { 
	    System.out.println("OOPS " + e);
	    e.printStackTrace();
	} 
    } 
}
