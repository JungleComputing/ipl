import java.io.IOException;

import java.rmi.registry.Registry;

import ibis.util.PoolInfo;

class Main {

    public static final int DEFAULT_N = 180;
    public static final double BOUND = 0.001;

    static i_BroadcastObject connect(PoolInfo info, int cpu, int num) throws IOException { 		
	int i = 0;
	boolean done = false;
	i_BroadcastObject temp = (i_BroadcastObject) RMI_init.lookup("//" + info.hostName(num) + "/BCAST" + num);

	return temp;
    }		

    public static void main(String args[]) {

	try { 

	    int n = DEFAULT_N;
	    int piece, offset, counter;

	    long start, end;

	    switch (args.length) {
	    case 0:
		n = DEFAULT_N;
		break;
	    case 1:
		n = Integer.parseInt(args[0]);
		break;
	    default:
		System.err.println("Usage: LEQ <N>");
		System.exit(1);
	    }

	    offset = n;

	    PoolInfo info = PoolInfo.createPoolInfo();
	    int cpus = info.size();
	    int cpu = info.rank();

	    Registry reg = RMI_init.getRegistry(info.hostName(cpu));

	    int size = n / cpus;
	    int leftover = n % cpus;
	    offset = cpu*size;

	    if (cpu < leftover) { 
		offset += cpu;
		size++;
	    } else { 
		offset += leftover;
	    }

	    BroadcastObject b = new BroadcastObject(cpu, cpus);

	    i_Central central = null;

	    if (cpu == 0) { 
		Central c = new Central(b, n, cpus);
		RMI_init.bind("CENTRAL", c);
		System.out.println(cpu + " bound " + "CENTRAL");
		//central = c;
	    } //else { 
	    central = (i_Central) RMI_init.lookup("//" + info.hostName(0) + "/CENTRAL");
	    //			}

	    RMI_init.bind("BCAST" + cpu, b);
	    System.out.println(cpu + " bound " + "BCAST" + cpu);

	    int left = cpu*2+1;
	    int right = cpu*2+2;

	    i_BroadcastObject i_left = null, i_right = null;

	    if (left < cpus) { 
		i_left = connect(info, cpu, left);
	    } 

	    if (right < cpus) { 
		i_right = connect(info, cpu, right);
	    } 

	    b.connect(i_left, i_right);

	    DoubleVector x_val = new DoubleVector(cpu, cpus, n, 0.0, b, central);
	    new LEQ(cpu, cpus, x_val, offset, size, n).start();

	} catch (Exception e) { 
	    e.printStackTrace();
	} 

	System.exit(0);
    }

}
