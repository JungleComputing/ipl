package ibis.rmi;

import ibis.ipl.*;

import java.util.Hashtable;
import java.util.Properties;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.net.InetAddress;

final class RTS { 

	public final static boolean DEBUG = true;

	protected static Hashtable skeletons;
	protected static String hostname;
	protected static PortType portType;

	private static Ibis ibis;
        private static IbisIdentifier localID;
	private static ibis.ipl.Registry ibisRegistry;

	private static int stubcount = 0;

	static { 
                try {
                        skeletons = new Hashtable();

			hostname = InetAddress.getLocalHost().getHostName();

			System.out.println("hostname = " + hostname);

			InetAddress adres = InetAddress.getByName(hostname);
			adres = InetAddress.getByName(adres.getHostAddress());
			hostname = adres.getHostName();
			System.out.println("hostname = " + hostname);

                        if (DEBUG) {
                                System.out.println(hostname + ": init RMI RTS");
                        }
                       
                        ibis         = Ibis.createIbis("ibis:" + hostname, "ibis.ipl.impl.tcp.TcpIbis", null);
//                        ibis         = Ibis.createIbis("ibis:" + hostname, "ibis.ipl.impl.messagepassing.panda.PandaIbis", null);
                        localID      = ibis.identifier();
                        ibisRegistry = ibis.registry();
			
                        StaticProperties s = new StaticProperties();
                        s.add("Serialization", "manta");

                        portType = ibis.createPortType("RMI", s);
                       
//                        ch = new CallHandler();

//                        receivePort = portType.createReceivePort("RMI Registry port on " + hostname, null);                     
//                        receivePort.enableConnections();
//                        receivePort.enableUpcalls();

                        if(DEBUG) {
                                System.out.println(hostname + ": RMI RTS init done");
                        }

                } catch (Exception e) {
                        System.err.println(hostname + ": Could not init RMI RTS " + e);
                        e.printStackTrace();
                        System.exit(1);
                }
        }
	
	protected static synchronized int bind(String name, Remote o) throws Exception { 
	
		String url = "//" + RTS.hostname + "/" + name;
		
		if (DEBUG) { 
			System.out.println("Trying to bind object to " + url);
		}
		
		if (skeletons.containsKey(url)) { 
			return -1;
		}

		Class c = o.getClass();

		if (DEBUG) {
			System.out.println(hostname + ": creating skeleton of type rmi_skeleton_" + c.getName());
		}

		Class skel_c = Class.forName("rmi_skeleton_" + c.getName()); 
		Skeleton skel = (Skeleton) skel_c.newInstance();
		ReceivePort rec = portType.createReceivePort(url, skel);		
		
		skel.init(rec, o);			
		skeletons.put(url, skel);
		
		rec.enableConnections();
		rec.enableUpcalls();
		
		if (DEBUG) {
			System.out.println(hostname + ": skeleton of type rmi_skeleton_" + c.getName() + " bound to " + url);
		}

		return 0;
	} 

	protected static synchronized Remote lookup(String name)  throws NotBoundException, IbisException, IbisIOException, ClassNotFoundException { 

		Stub result;
		SendPort s = null;

		if (DEBUG) { 
			System.out.println(hostname + ": Trying to lookup object " + name);
		}

		ReceivePortIdentifier dest = ibisRegistry.lookup(name);
		
		if (dest == null) { 
			throw new NotBoundException(name + " not bound");
		} 

		if (DEBUG) { 
			System.out.println(hostname + ": Found skeleton " + name + " connecting");
		}

		s = portType.createSendPort();
		s.connect(dest);

		ReceivePort r = portType.createReceivePort(name + "_stub_" + stubcount++);
		r.enableConnections();

		if (DEBUG) { 
			System.out.println(hostname + ": Created receiveport for stub " + name + "_stub_" + (stubcount-1) + " -> id = " + r.identifier());
		}

		WriteMessage wm = s.newMessage();
		wm.writeInt(-1);
		wm.writeInt(0);
		wm.writeObject(r.identifier());
		wm.send();
		wm.finish();

		ReadMessage rm = r.receive();
		int stubID = rm.readInt();
		String stubType = (String) rm.readObject();
		rm.finish();
		
		try { 
			result = (Stub) Class.forName(stubType).newInstance();
		} catch (Exception e) { 
			s.free();	
			r.free();
			throw new IbisException("stub class " + stubType + " not found" + e);
		}
		
		result.init(s, r, stubID);			

		if (DEBUG) { 
			System.out.println(hostname + ": Created stub of type " + stubType);
		}
		return (Remote) result;
	} 
} 









