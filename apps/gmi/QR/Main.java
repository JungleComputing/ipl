import ibis.gmi.*;

class Main {

    static final int N = 2000;

    public static void main(String [] args) {

	try { 
	    Data data = null;

	    int n = N;
	    int m = -1;
	    int ncpus = Group.size();
	    int cpu = Group.rank();

	    // parse paremeters here.				
	    int options = 0;
	    for (int i = 0; i < args.length; i++) {
		if (false) {
		} else if (options == 0) {
		    n = Integer.parseInt(args[i]);
		    options++;
		} else if (options == 1) {
		    m = Integer.parseInt(args[i]);
		    options++;
		} else {
		    if (cpu == 0) {
			System.err.println("QR [N [M]]");
		    }
		    System.exit(33);
		}
	    }
	    if (m == -1) {
		m = n;
	    }

	    if (cpu == 0) {
		System.out.println("Starting QR of A[" + n + "][" + m + "] on " + ncpus + " cpus.");			
		Group.create("QR-Reduce", i_Reduce.class, ncpus);
		Group.create("QR-Broadcast", i_Broadcast.class, ncpus);
		Group.create("QR-Data", i_Data.class, 1);

		data = new Data(ncpus);
		Group.join("QR-Data", data);
	    } 

	    Reduce r = new Reduce();
	    Group.join("QR-Reduce", r);			
	    i_Reduce ir = (i_Reduce) Group.lookup("QR-Reduce");
	    GroupMethod m1 = Group.findMethod(ir, "void reduce_it(PivotElt)"); 
	    m1.configure(new CombinedInvocation("QR-REDUCE", cpu, ncpus, new MyCombiner(), new GroupInvocation()), new DiscardReply());		
	    r.init(ir);

	    System.out.println("Reduce ok");

	    Broadcast b = new Broadcast();
	    Group.join("QR-Broadcast", b);
	    System.out.println("Bcast ok1");
	    i_Broadcast ib = (i_Broadcast) Group.lookup("QR-Broadcast");
	    System.out.println("Bcast ok2");
	    m1 = Group.findMethod(ib, "void broadcast_it(double[])"); 
	    System.out.println("Bcast ok3 " + m1);
	    m1.configure(new GroupInvocation(), new DiscardReply());	
	    System.out.println("Bcast ok4");
	    b.init(ib);

	    System.out.println("Bcast ok5");

	    i_Data id = (i_Data) Group.lookup("QR-Data");
	    m1 = Group.findMethod(id, "void put(int, int)"); 
	    m1.configure(new SingleInvocation(0), new DiscardReply());		

	    System.out.println("data ok");

	    System.out.println("New QR_Worker on " + cpu);

	    QR_Pivot work = new QR_Pivot(cpu, ncpus, n, m, id, r, b);
	    work.qrfac();

	    if (cpu == 0) { 
		int    max    = data.max_time();
		double avg    = data.avg_time();
		double stddev = data.stddev_time();

		System.out.println("time : max = " + max + " avg = " + avg + " stddev = " + stddev);
	    }  
	    Group.exit();
	} catch (Exception e) {
	    System.out.println("Main got " + e);
	    e.printStackTrace();
	    System.exit(1);
	} 
    }
}


