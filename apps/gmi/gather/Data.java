import ibis.group.*;

public class Data extends GroupMember implements i_Data {
	
	byte [] data;	
	boolean gotData = false;

	Data()  { 
	} 
	
	public synchronized void storeL()  { 
		while (gotData) { 
			try { 
				System.out.println("StoreL waits");
				wait();
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = true;
		notifyAll();
		System.out.println("StoreL done");
	} 

	synchronized void retrieveL()  { 

		while (!gotData) { 
			try { 
		System.out.println("retrieveL waits");
				wait();
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = false;
		notifyAll();
		System.out.println("retrieveL done");
	} 

	public synchronized void storeT(byte [] data) { 

		while (gotData) { 
			try { 
				wait();
			} catch (Exception e) { 
				// ignore
			} 
		} 

		this.data = data;
		gotData = true;
		notifyAll();
	} 

	synchronized byte [] retrieveT() {

		while (!gotData) { 
			try { 
				wait();
			} catch (Exception e) { 
				// ignore
			} 
		} 
		gotData = false;
		notifyAll();
		return data;
	} 
}
