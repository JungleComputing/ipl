import ibis.group.*;

public class Data extends GroupMember implements i_Data {
	
	byte [] data;	
	boolean gotData = false;
	int 	waiters = 0;

	Data()  { 
	} 
	
	public synchronized void storeL()  { 
		while (gotData) { 
			try { 
				waiters++;
				wait();
				waiters--;
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = true;
		if (waiters != 0) notifyAll();
// System.out.println("StoreL");
	} 

	synchronized void retrieveL()  { 

		while (!gotData) { 
			try { 
// System.out.println("retrieveL waits");
				waiters++;
				wait();
				waiters--;
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = false;
		if (waiters != 0) notifyAll();
// System.out.println("retrieveL done");
	} 

	public synchronized void storeT(byte [] data) { 

		while (gotData) { 
			try { 
				waiters++;
				wait();
				waiters--;
			} catch (Exception e) { 
				// ignore
			} 
		} 

		this.data = data;
		gotData = true;
		if (waiters != 0) notifyAll();
	} 

	synchronized byte [] retrieveT() {

		while (!gotData) { 
			try { 
				waiters++;
				wait();
				waiters--;
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = false;
		if (waiters != 0) notifyAll();
		return data;
	} 
}
