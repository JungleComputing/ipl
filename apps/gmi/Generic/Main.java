import ibis.group.Group;
import ibis.group.GroupMember;

class Main { 

	public static void foo(int rank1, int rank2, int size) { 
		//		System.out.println("foo(" + rank1 + ", " + rank2 + ", " + size + ")");
	}

	public static int add(int res1, int rank1, int res2, int rank2, int size) { 
		//		System.out.println("add : me(" + rank1 + ") = " + res1 + " other(" + rank2 + ") = " + res2 + " total = " + size + " result = " + (res1 + res2));
		return res1 + res2;
	}

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
			Group.create("TestGroup", size);
		}

		Test t = new Test();
		Group.join("TestGroup", t);

		System.out.println("I am " + t.rank + " of " + size);

		if (rank == 0) { 		
			myGroup g = (myGroup) t.createGroupStub();			
			/*
			//Group.setInvoke(g, "void put(int)", Group.REMOTE, size-1);
			//Group.setResult(g, "void put(int)", Group.RETURN);

			Group.setInvoke(g, "void put(int)", Group.GROUP);
			Group.setResult(g, "void put(int)", Group.COMBINE, "Main", "foo");

			for (int i=0;i<count;i++) { 
				g.put(i);
			}

			long time = System.currentTimeMillis();

			for (int i=0;i<count;i++) { 
				g.put(i);
			}

			time = System.currentTimeMillis() - time;
			*/

			Group.setInvoke(g, "int get()", Group.GROUP);
			Group.setResult(g, "int get()", Group.COMBINE, "Main", "add");

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
