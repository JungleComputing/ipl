import ibis.repmi.*;
import ibis.util.PoolInfo;

class Main { 

	public static void main(String [] args) { 	

		PoolInfo info = new PoolInfo();		

		if (info.rank() == 0) { 	
			myRep r = (myRep) RTS.createReplica("Test");
			r.foo();
			r.bar();		
		} else {
			RTS.init();
			// do nothing !
		} 
	} 
} 
