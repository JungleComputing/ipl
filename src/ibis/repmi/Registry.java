package ibis.group;

import manta.ibis.*;

import java.util.Hashtable;

final class Registry {

	int number;
	
        public Registry() {
		number = 0;
        }
        
	private synchronized int getNumber() { 
		return number++;
	}

	public void handleMessage(ReadMessage r) { 

		try { 
			//int rank = r.readInt();	
//			int ticket = r.readInt();
			r.finish();
				

		} catch (IbisIOException e) {
			System.out.println(Group._rank + ": Error in GroupRegistry " + e);
			System.exit(1);
		}
	}        
}
