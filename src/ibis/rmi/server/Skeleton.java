package ibis.rmi.server;

import ibis.ipl.*;
import ibis.rmi.RTS;
import java.util.Vector;

public abstract class Skeleton implements ibis.ipl.Upcall { 

	private ReceivePort receive;
	public Object destination;
	public Vector stubs;
	public String stubType;

	public Skeleton() {
		stubs = new Vector();
	}
	
	public void init(ReceivePort r, Object o) { 
		receive = r;
		destination = o;
	}    

	protected synchronized int addStub(ReceivePortIdentifier rpi) { 
		try { 
			SendPort s = RTS.createSendPort();
			s.connect(rpi);
			
			int id = stubs.size();
			stubs.add(id, s);
			
			return id;
		} catch (Exception e) { 
			System.out.println("OOPS " + e);
			e.printStackTrace();
			System.exit(1);			
		} 
		return -1; 
	}
	
	public ReceivePort receivePort() {
	    return receive;
	} 

	public abstract void upcall(ReadMessage m);
} 
