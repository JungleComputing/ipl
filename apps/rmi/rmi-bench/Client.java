import ibis.rmi.*;
import ibis.rmi.server.*;
import java.net.*;
import ibis.rmi.registry.*;

public class Client {

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


    Client(String[] args, String server_name, Registry local) {

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

	start(local);
    }


    private void do_rmis(i_Server s, Object request, int n) throws RemoteException {
	Object reply;

	if (request == null) {
	    if (send_type == Datatype.TWO_INT) {
		for (int i = 0; i < n; i++) {
		    s.empty(i, n);
		}
	    } else {
		for (int i = 0; i < n; i++) {
		    s.empty();
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


    private void start(Registry reg) {

	i_Server s = null;
	Remote r = null;

	// System.out.println("Try to locate remote object server, server_name = " + server_name);

	for (int i = 0; i < 1000; i++) {
	    try {
		if (reg == null) {
		    r = Naming.lookup("//" + server_name + "/server");
		} else {
		    r = reg.lookup("server");
		} 
		s = (i_Server)r;
		break;
	    } catch (ibis.rmi.NotBoundException eR) {
		try {
		    System.out.println("Look up server object: sleep a while... " + eR);
		    Thread.sleep(1000);
		} catch (InterruptedException eI) {
		}
	    } catch (Exception e) {
		System.out.println("Exception: " + e);
		System.exit(33);
	    }
	}

	System.out.println("Located remote object " + r);

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
		}
	    }

	    Worker worker = null;

	    if (client_worker) {
		worker = new Worker("Client worker");
		worker.start();
	    }

	    if (warm_up > 0) {
		do_rmis(s, request, warm_up);

		System.out.println("Done " + warm_up + " warmup RMIs size " + size);
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
