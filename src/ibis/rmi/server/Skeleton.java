package ibis.rmi.server;

import ibis.ipl.*;
import ibis.rmi.RTS;
import java.util.Vector;

public abstract class Skeleton implements ibis.ipl.Upcall { 

	private ReceivePort receive;
	public Object destination;
	public SendPort[] stubs;
	public String stubType;
	private int num_ports = 0;
	private int max_ports = 0;

	private static final int INCR = 16;

	public Skeleton() {
		stubs = new SendPort[INCR];
		max_ports = INCR;
	}
	
	public void init(ReceivePort r, Object o) { 
		receive = r;
		destination = o;
	}    

	protected void finalize() {
	    for (int i = 0; i < stubs.length; i++) {
		if (stubs[i] != null) {
		    try {
			stubs[i].free();
		    } catch(Exception e) {
		    }
		}
	    }
	    receive.free();
	}

	protected synchronized int addStub(ReceivePortIdentifier rpi) { 
		try { 
			SendPort s = RTS.createSendPort();
			s.connect(rpi);
			
			int id = ++num_ports;
			if (id >= max_ports) {
				SendPort[] newports = new SendPort[max_ports+INCR];
				for (int i = 0; i < max_ports; i++) newports[i] = stubs[i];
				max_ports += INCR;
				stubs = newports;
			}
			stubs[id] = s;
			
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
