package ibis.group;

import java.io.IOException;

import ibis.ipl.*;

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
				

		} catch (IOException e) {
			System.out.println(Group._rank + ": Error in GroupRegistry " + e);
			System.exit(1);
		}
	}        
}
