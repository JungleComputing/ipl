import ibis.group.*;

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

		int count = 100;
		int len = 100;

		System.out.println("size = " + size);
		System.out.println("rank = " + rank);

		if (args.length != 0) { 
			try { 				
				count = Integer.parseInt(args[0]);
			} catch (Exception e) { 
				System.out.println("arg must be an int");
				Group.exit();
			} 
		}

		if (rank == 0) { 
		    System.out.println("Creating group ...");
		    Group.create("TestGroup", myGroup.class, size);
		    System.out.println("Group created!");

		}

		System.out.println("Creating test ...");
		Test t = new Test();
		System.out.println("Joining group ...");
		Group.join("TestGroup", t);
		System.out.println("Joined group !!!");

		System.out.println("I am " + t.rank + " of " + size);

		if (rank == 0) { 		
			Object data = create(len);
			Object result = null;

			myGroup g = (myGroup) Group.lookup("TestGroup");
			try {
			    GroupMethod m = Group.findMethod(g, "java.lang.Object put_get(java.lang.Object)");
			    m.configure(new GroupInvocation(), new DiscardReply());
			} catch(NoSuchMethodException e) {
			    System.out.println("Method put_get not found");
			    Group.exit();
			    System.exit(1);
			} catch (ConfigurationException e2) {
			    System.out.println("Illegal configuration");
			    Group.exit();
			    System.exit(1);
			}

			for (int i=0;i<count;i++) { 
				result = g.put_get(data);
			}

			long time = System.currentTimeMillis();

			for (int i=0;i<count;i++) { 
				g.put_get(data);
			}

			time = System.currentTimeMillis() - time;

			System.out.println("Test took " + time + " ms. = " + ((1000.0*time)/count) + " micros/call");
		}

		else {
		    try {
			Thread.sleep(50000);
		    } catch(Exception e) {
		    }
		}
		System.out.println(rank + " doing exit");
		
		Group.exit();
	} 
} 
