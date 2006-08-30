/* $Id$ */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.io.IOException;

import ibis.util.TypedProperties;
import ibis.util.Timer;

public class Client {

    private final static boolean VARIANCE_TIMER = TypedProperties.booleanProperty("variance-timer", false);

    private int N = 10000;
    private int size = 0;
    private long t_start, t_stop, total;
    private String server_name;
    private boolean one_way = false;
    private int send_type = Datatype.BYTE;
    private int warm_up = -1;
    private boolean client_worker = false;

    // native void fs_stats_reset();
    // native void javaSocketResetStats();


    Client(String[] args, String server_name) {

	this.server_name = server_name;

	int		option = 0;
	for (int i = 0; i < args.length; i++) {
	    if (false) {
	    } else if (args[i].equals("-one-way")) {
		one_way = true;
	    } else if (args[i].equals("-byte")) {
		send_type = Datatype.BYTE;
	    } else if (args[i].equals("-int")) {
		send_type = Datatype.INT;
	    } else if (args[i].equals("-int2")) {
		send_type = Datatype.TWO_INT;
	    } else if (args[i].equals("-int32")) {
		send_type = Datatype.INT_32;
	    } else if (args[i].equals("-float")) {
		send_type = Datatype.FLOAT;
	    } else if (args[i].equals("-double")) {
		send_type = Datatype.DOUBLE;
	    } else if (args[i].equals("-tree")) {
		send_type = Datatype.TREE;
	    } else if (args[i].equals("-cyc")) {
		send_type = Datatype.CYCLIC;
	    } else if (args[i].equals("-b")) {
		send_type = Datatype.B;
	    } else if (args[i].equals("-inner")) {
		send_type = Datatype.INNER;
	    } else if (args[i].equals("-switch")) {
		send_type = Datatype.SWITCHER;
	    } else if (args[i].equals("-warmup")) {
		warm_up = Integer.parseInt(args[++i]);
	    } else if (args[i].equals("-registry")) {
		this.server_name = args[++i];
	    } else if (args[i].equals("-client-worker")) {
		client_worker = true;
	    } else if (args[i].equals("-server-worker")) {
	    } else if (option == 0) {
		N = Integer.parseInt(args[i]);
		option++;
	    } else if (option == 1) {
		size = Integer.parseInt(args[i]);
		option++;
	    } else {
		System.out.println("No such option: " + args[i]);
		Thread.dumpStack();
		System.exit(33);
	    }
	}

	if (warm_up == -1) {
	    warm_up = N;
	}

	client();
    }


    private void do_rmis(i_Server s, Object request, int n) throws RemoteException {
	Object reply;

	Timer t = null;
	Timer sum = null;
	if (VARIANCE_TIMER) {
	    t = Timer.createTimer("ibis.util.nativeCode.Rdtsc");
	    sum = Timer.createTimer("ibis.util.nativeCode.Rdtsc");
	}

	if (request == null) {
	    if (send_type == Datatype.TWO_INT) {
		for (int i = 0; i < n; i++) {
		    s.empty(i, n);
		}
	    } else if (send_type == Datatype.SWITCHER) {
		for (int i = 0; i < n; i++) {
		    s.emptyThreadSwitch();
		}
	    } else {
		for (int i = 0; i < n; i++) {
		    if (VARIANCE_TIMER) {
			t.reset();
			t.start();
		    }
		    s.empty();
		    if (VARIANCE_TIMER) {
			t.stop();
			double tNow = t.totalTimeVal();
			double tAv  = sum.averageTimeVal();
			if (i > 0 && tNow > 10 * tAv) {
			    System.err.println("Now see " + tNow + "; av " + tAv);
			} else {
			    sum.add(t);
			}
		    }
		    // System.err.print(".");
		}
	    }
	} else if (one_way) {
	    for (int i = 0; i < n; i++) {
		s.one_way(request);
	    }
	} else {
	    for (int i = 0; i < n; i++) {
		reply = s.empty(request);
	    }
	}
    }


    private void client() {

	i_Server s = null;

	// System.out.println("Try to locate remote object server, server_name = " + server_name);

	try {
	    s = (i_Server)RMI_init.lookup("//" + server_name + "/server");
	} catch (IOException e) {
	    System.err.println("Cannot resolve server name: " + e);
	}

	try {
	    Object request = null;

	    if (send_type == Datatype.INT_32) {
		size = 32 * 4;
	    }

	    if (size != 0 || send_type == Datatype.B) {
		switch (send_type) {
		case Datatype.BYTE:
		    request = new byte[size];
		    break;
		case Datatype.INT:
		    request = new int[(size + 3) / 4];
		    break;
		case Datatype.INT_32:
		    request = new Int32();
		    break;
		case Datatype.FLOAT:
		    request = new float[(size + 3) / 4];
		    break;
		case Datatype.DOUBLE:
		    request = new double[(size + 7) / 8];
		    break;
		case Datatype.TREE:
		    request = new DITree(size);
		    break;
		case Datatype.CYCLIC:
		    request = new Cyclic(size);
		    break;
		case Datatype.B:
		    request = new B(size);
		    break;
		case Datatype.INNER:
		    WithInner[] w = new WithInner[size];
		    for (int i = 0; i < size; i++) {
			w[i] = new WithInner();
		    }
		    request = w;
		    break;
		case Datatype.SWITCHER:
		    break;
		}
	    }

	    Worker worker = null;

	    if (client_worker) {
		worker = new Worker("Client worker");
		worker.start();
	    }

	    // System.out.println("Client starts");
	    // System.out.flush();

	    if (warm_up > 0) {
		do_rmis(s, request, warm_up);

		System.out.println("Done " + warm_up + " warmup RMIs size " + size);
		System.out.flush();
		s.reset();
		System.gc();
		// javaSocketResetStats();
		// fs_stats_reset();
		// ibis.impl.messagePassing.Ibis.resetStats();
		if (client_worker) {
		    worker.reset();
		}
	    }

	    t_start = System.currentTimeMillis();

	    do_rmis(s, request, N);

	    t_stop = System.currentTimeMillis();
	    total = t_stop - t_start;

	    System.out.print("RMI Bench: ");
	    System.out.print(total + " ms. for " + N + " RMIs of size " + size);
	    if (send_type == Datatype.TREE) {
		System.out.print(" trees; ");
	    } else if (send_type == Datatype.CYCLIC) {
		System.out.print(" Cyclics; ");
	    } else {
		System.out.print(" bytes; ");
	    }
	    System.out.println(((1000.0 * total)/N) + " us./RMI");

	    if (one_way) {
		switch (send_type) {
		case Datatype.BYTE:
		    System.out.print("Bytes");
		    if (System.getProperty("ObjectStream.SlowByteArray") != null) {
			System.out.print("/unopt");
		    } else {
			System.out.print("/opt");
		    }
		    break;
		case Datatype.INT:
		    System.out.print("Ints");
		    break;
		case Datatype.TWO_INT:
		    System.out.print("Pair of ints");
		    break;
		case Datatype.INT_32:
		    System.out.print("Object with 32 ints");
		    break;
		case Datatype.FLOAT:
		    System.out.print("Floats");
		    break;
		case Datatype.DOUBLE:
		    System.out.print("Doubles");
		    break;
		case Datatype.TREE:
		    System.out.print("Trees");
		    break;
		case Datatype.CYCLIC:
		    System.out.print("Cyclics");
		    break;
		case Datatype.INNER:
		    System.out.print("With Inner");
		    break;
		case Datatype.SWITCHER:
		    System.out.print("empty with thread switch");
		    break;
		}

		System.out.print(" " + N + " size " + size + " Throughput ");
		if (send_type == Datatype.TREE || send_type == Datatype.CYCLIC) {
		    System.out.print(((N * 1000.0) * size / (1048576.0 * total)) +
			    " Mnode/s; payload ");
		    System.out.println(N * (16 * 1000.0) * size / (1048576.0 * total) +
			    " MB/s");
		} else {
		    System.out.println(N * (size * 1000.0) / (1048576.0 * total) +
			    " MB/s");
		}
	    }

	    s.quit();

	    if (client_worker) {
		worker.quit();
	    }

	    System.exit(0);

	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Exception in Test: " + e);
	}     

    }

}
