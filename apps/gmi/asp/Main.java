import ibis.group.*;

class Main {

    public static void main(String[] argv) {

	int nodes = Group.size();
	int rank  = Group.rank();
	
	try {	 
	    int n = 2000;
	    
	    boolean print_result    = false;
		
	    int option = 0;
	    	    
	    if (argv.length > 0) {
		n = Integer.parseInt(argv[0]);
	    }
	    
	    if (rank == 0) {
		Group.create("ASP", i_Asp.class, nodes);
	    } 	    
			    
	    Asp local = new Asp(n, rank, nodes, print_result);
	    Group.join("ASP", local);
	    i_Asp group = (i_Asp) Group.lookup("ASP");
	    
	    GroupMethod m = Group.findMethod(group, "void transfer(int[], int)");
	    m.configure(new GroupInvocation(), new DiscardReply());
	    
	    m = Group.findMethod(group, "void done()");
	    m.configure(new CombinedInvocation("ASP_DONE", rank, nodes, new MyCombiner(), new SingleInvocation(0)), new ReturnReply(0));
	
	    local.init(group);
	    local.start();
	
	} catch (Exception e) {
	    System.out.println("Oops " + rank + " " +  e);
	    e.printStackTrace();
	}
    }



}
