import ibis.group.Group;
import ibis.group.GroupMethod;
import ibis.group.CombineReply;
import ibis.group.GroupInvocation;
import ibis.group.BinaryCombiner;

class Adder extends BinaryCombiner {
    public int combine(int rank1, int result1, int rank2, int result2, int size) {
	return result1 + result2;
    }
}

class Main { 

	public static void main(String [] args) { 

		int size = Group.size();
		int rank = Group.rank();

		int count = 1000;

		if (args.length != 0) { 
			try { 				
				count = Integer.parseInt(args[0]);
			} catch (Exception e) { 
				System.out.println("arg must be an int");
				Group.exit();
			} 
		}

		if (rank == 0) { 
			Group.create("TestGroup", myGroup.class, size);
		}

		Test t = new Test();
		Group.join("TestGroup", t);

		System.out.println("I am " + t.rank + " of " + size);

		if (rank == 0) { 		
			myGroup g = (myGroup) Group.lookup("TestGroup");
			try {
			    GroupMethod m = Group.findMethod(g, "int get()");
			    m.configure(new GroupInvocation(), new CombineReply(new Adder()));
			} catch(Exception e) {
			    System.out.println("Caught exception: " + e);
			    System.exit(1);
			}


			int result = 0;

			for (int i=0;i<count;i++) { 
				result = g.get();
			}

			long time = System.currentTimeMillis();

			for (int i=0;i<count;i++) { 
				result = g.get();
			}

			time = System.currentTimeMillis() - time;

			System.out.println("Test took " + time + " ms. = " + ((1000.0*time)/count) + " micros/call (result = "+ result +")");
		}

		System.out.println(rank + " doing exit");
		
		Group.exit();
	} 
} 
