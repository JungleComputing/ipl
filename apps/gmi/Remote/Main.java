import ibis.group.Group;
import ibis.group.GroupMember;

class Main { 

	public static Object create(int len) { 
		
		Data temp = null;

		for (int i=0;i<len;i++) { 
		        temp = new Data((len+0.8)/1.3, temp);
		} 

		return temp;
	} 

	public static void main(String [] args) { 

		int size = Group.size();
		int rank = Group.rank();

		int count = 1000;
		int len = 100;

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
			Group.setInvoke(g, "java.lang.Object put_get(java.lang.Object)", Group.REMOTE, size-1);

			Object data = create(len);
			Object result = null;

			for (int i=0;i<count;i++) { 
				result = g.put_get(data);
			}

			long time = System.currentTimeMillis();

			for (int i=0;i<count;i++) { 
				result = g.put_get(data);
			}

			time = System.currentTimeMillis() - time;

			System.out.println("Test took " + time + " ms. = " + ((1000.0*time)/count) + " micros/call");
		}

		System.out.println(rank + " doing exit");
		
		Group.exit();
	} 
} 
