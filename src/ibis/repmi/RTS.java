package ibis.repmi;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.util.PoolInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;

public final class RTS { 

	public final static boolean DEBUG = false;

	public static int _rank;
        protected static int _size;
        protected static String name;

	private static Ibis ibis;
        private static IbisIdentifier localID;
	
	private static ibis.ipl.Registry ibisRegistry;
        
        private static PortType portType;

        private static ReceivePort receivePort;

        private static ReceivePort systemIn;
        private static SendPort    systemOut;

        private static ReceivePortIdentifier [] pool;

        // this must be public in order for generated classes to use it....
//        public static SendPort [] unicast;
        public static SendPort multicast;               
       
        private static CallHandler ch;

//        protected static RepMIRegistry registry;        
        protected static Vector skeletons;
	
	// This is wrong when multiple machines create replicated objects */
	private static int next = 0;

	static { 
                try {
                        skeletons = new Vector();
                        
                        name = InetAddress.getLocalHost().getHostName();

                        if (DEBUG) {
                                System.out.println(name + ": init RepMI RTS");
                        }
                       
                        ibis         = Ibis.createIbis("ibis:" + name, "ibis.impl.tcp.TcpIbis", null);
//                        ibis         = Ibis.createIbis("ibis:" + name, "ibis.impl.panda.PandaIbis", null);
                        localID      = ibis.identifier();
                        ibisRegistry = ibis.registry();
//                      ibis.start);

                        portType = ibis.createPortType("RepMI",
						       StaticProperties.userProperties());
                       
                        ch = new CallHandler();

                        receivePort = portType.createReceivePort("RepMI port on " + name, ch);                     
                        receivePort.enableConnections();

                        IbisIdentifier i = (IbisIdentifier) ibisRegistry.elect("RepMI Master", localID);

                        if (localID.equals(i)) { 

                                if (DEBUG) { 
                                        System.out.println(name + " I am master");
                                }

//                                registry = new GroupRegistry();

                                /* I am the master */                           
				PoolInfo info = new PoolInfo();

                                _size = info.size();
                                _rank = 0;
                                
                                pool = new ReceivePortIdentifier[_size];
                                pool[0] = receivePort.identifier();

                                if (_size > 1) {

                                        systemIn  = portType.createReceivePort("RepMI Master");
                                        systemIn.enableConnections();

                                        systemOut = portType.createSendPort("RepMI Master");
                                        
                                        for (int j=1;j<_size;j++) { 
                                                
                                                ReadMessage r = systemIn.receive();
                                                ReceivePortIdentifier reply = (ReceivePortIdentifier) r.readObject();
                                                ReceivePortIdentifier id = (ReceivePortIdentifier) r.readObject();
                                                r.finish();

                                                systemOut.connect(reply);
                                                pool[j] = id;
                                        }
                                        
                                        WriteMessage w = systemOut.newMessage(); 
                                        w.writeObject(pool);
                                        w.send();
                                        w.finish();                                     
                                }

                        } else { 

                                if (DEBUG) { 
                                        System.out.println(name + " I am client");
                                }

                                systemIn  = portType.createReceivePort("RepMI Client " + name);
                                systemIn.enableConnections();

                                systemOut = portType.createSendPort("RepMI Client " + name);

                                ReceivePortIdentifier master = ibisRegistry.lookup("RepMI Master");

                                while (master == null) { 
                                        try { 
                                                Thread.sleep(1000);
                                        } catch (InterruptedException e) { 
                                                // ignore
                                        } 
                                        master = ibisRegistry.lookup("RepMI Master");
                                }
                                        
                                systemOut.connect(master);

                                WriteMessage w = systemOut.newMessage();
                                w.writeObject(systemIn.identifier());
                                w.writeObject(receivePort.identifier());
                                w.send();
                                w.finish();

                                ReadMessage r = systemIn.receive();
                                pool = (ReceivePortIdentifier []) r.readObject();
                                r.finish();
                                
                                _size = pool.length;
                                
                                for (int j=1;j<_size;j++) { 
                                        if (pool[j].equals(receivePort.identifier())) { 
                                                _rank = j;
                                                break;
                                        }
                                }
                        } 

                        multicast = portType.createSendPort("Multicast on " + name);
                        
                        for (int j=0;j<_size;j++) { 
                                multicast.connect(pool[j]);
                        }


                        receivePort.enableUpcalls();

                        if(DEBUG) {
                                System.out.println(name + ": RepMI_RTS init");
                        }

                } catch (Exception e) {
                        System.err.println(name + ": Could not init RepMI_RTS " + e);
                        e.printStackTrace();
                        System.exit(1);
                }
        }

	public static void init() { 
		// 
	} 

	protected static Skeleton findSkeleton(int skel) { 
                return (Skeleton) skeletons.get(skel);
        }  
	
	private static String get_skel_name(Class c) {
	    String class_name = c.getName();
	    Package pkg = c.getPackage();
	    String package_name = pkg != null ? pkg.getName() : null;
	    if (package_name == null || package_name.equals("")) {
		return "repmi_skeleton_" + class_name;
	    }
	    return package_name + ".repmi_skeleton_" +
		    class_name.substring(class_name.lastIndexOf('.') + 1);
	}

	private static String get_stub_name(Class c) {
	    String class_name = c.getName();
	    Package pkg = c.getPackage();
	    String package_name = pkg != null ? pkg.getName() : null;
	    if (package_name == null || package_name.equals("")) {
		return "repmi_stub_" + class_name;
	    }
	    return package_name + ".repmi_stub_" +
		    class_name.substring(class_name.lastIndexOf('.') + 1);
	}

	private static Skeleton newObject(String type) { 
		
		try { 
			Class c = Class.forName(type);
			ReplicatedObject o = (ReplicatedObject) c.newInstance();
			
			Class skel_c =  Class.forName(get_skel_name(c));
			Skeleton skel = (Skeleton) skel_c.newInstance();
			
			skel.init(o);
			
			return skel;

		} catch (Exception e) { 
			System.err.println("EEK: manta.repmi.RTS.newObject("+ type + ") Failed");
			e.printStackTrace();
			System.exit(0);
		}

		return null;
	} 

	protected static void newObject(ReadMessage m) throws IOException, ClassNotFoundException { 
		/* Invoked by callhandler */		

		int num = m.readInt();
		String type = (String) m.readObject();
		m.finish();
				
		Skeleton skel = newObject(type);
		
		synchronized (skeletons) { 
			skeletons.add(num, skel);
			skeletons.notifyAll(); /* needed to wake up waiting thread on sender */
		}
        } 

	public static Stub createReplica(String type) { 
		/* Invoked by user */

		try {
			int number;

			synchronized (skeletons) { 
				number = next++;
			}

			WriteMessage wm = multicast.newMessage();
			wm.writeByte(Protocol.NEW_OBJECT);
			wm.writeInt(number);
			wm.writeObject(type);
			wm.send();
			wm.finish();

			synchronized (skeletons) {
				while ((skeletons.size() <= number) || (skeletons.get(number) == null)) { 
					try { 
						skeletons.wait();
					} catch (InterruptedException e) { 
						// ignore
					} 
				}
			}
			
			Skeleton skel = (Skeleton) skeletons.get(number);	
			// this is not correct with package names !!
			Class stub_class = Class.forName(get_stub_name(Class.forName(type)));
			Stub stub = (Stub) stub_class.newInstance();
			stub.init(number, skel);
			return stub;
			
		} catch (Exception e) { 
			System.err.println("EEK: manta.repmi.RTS.createReplica("+ type + ") Failed");
			e.printStackTrace();
			System.exit(0);
		} 

		return null;			
        } 
}
