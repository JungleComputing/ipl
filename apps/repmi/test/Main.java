import manta.repmi.*;

class Main { 

	public static void main(String [] args) { 	

		DasInfo info = new DasInfo();		

		if (info.hostNumber() == 0) { 	
			myRep r = (myRep) RTS.createReplica("Test");
			r.foo();
			r.bar();		
		} else {
			RTS.init();
			// do nothing !
		} 
	} 
} 
