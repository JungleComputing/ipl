import ibis.rmi.*;
import ibis.rmi.registry.*;
import java.io.*;
import ibis.util.PoolInfo;

class Main {

	public static final int DEFAULT_N = 180;
	public static final double BOUND = 0.001;

	static i_BroadcastObject connect(PoolInfo info, int cpu, int num) { 		
		int i = 0;
		boolean done = false;
		i_BroadcastObject temp = null;

		while (!done && i < 10) {
			
			i++;
			done = true;
			
			try { 				
				Thread.sleep(cpu*500);	
				System.out.println(cpu + " connect to //" + info.hostName(num) + "/BCAST" + num);
				temp = (i_BroadcastObject) Naming.lookup("//" + info.hostName(num) + "/BCAST" + num);
			} catch (Exception e) {
				done = false;	
				try { 
					Thread.sleep(i*500);
				} catch (Exception e2) { 
				} 
			} 
		}
		
		if (!done) {
			System.out.println(cpu + " could not connect to //" + info.hostName(num) + "/BCAST" + num);
			System.exit(1);
		}				

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
			
			PoolInfo info = new PoolInfo();
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
				Naming.bind("CENTRAL", c);
				System.out.println(cpu + " bound " + "CENTRAL");
				//central = c;
			} //else { 
				int i = 0;
				boolean done = false;
				while (!done && i < 10) {
					
					i++;
					done = true;
					
					try { 				
						Thread.sleep(cpu*1000);	
						System.out.println(cpu + " connect to //" + info.hostName(0) + "/CENTRAL");
						central = (i_Central) Naming.lookup("//" + info.hostName(0) + "/CENTRAL");
					} catch (Exception e) {
						done = false;	
						try { 
							Thread.sleep((cpu+1)*1500);
						} catch (Exception e2) { 
						} 
					} 
				}
		
				if (!done) {
					System.out.println(cpu + " could not connect to //" + info.hostName(0) + "/CENTRAL");
					System.exit(1);
				}				
//			}

			Naming.bind("BCAST" + cpu, b);
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
