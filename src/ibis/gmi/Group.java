package ibis.group;

import java.net.InetAddress;

import java.util.Properties;
import java.util.Vector;

import ibis.ipl.*;
import ibis.util.Ticket;
import java.lang.reflect.Method;

public final class Group { 
	
	public final static boolean DEBUG = false;

	public final static byte
	        /* invocation modes */
		LOCAL       = 0,
		REMOTE      = 1,
		GROUP       = 2, 
		PERSONALIZE = 3;

	public final static byte
                /* result modes */
		DISCARD        = 0,
		RETURN         = 1,
		COMBINE        = 2;
	
	public static int _rank;
	protected static int _size;
	protected static String name;

	//	private static i_GroupRegistry _groupRegistry;
       
	private static Ibis ibis;
	private static IbisIdentifier localID;
	private static Registry ibisRegistry;
	
	private static PortType portType;

	private static ReceivePort receivePort;

	private static ReceivePort systemIn;
	private static SendPort    systemOut;

	private static ReceivePortIdentifier [] pool;

	// this must be public in order for generated classes to use it....
	public static SendPort [] unicast;
	public static SendPort multicast;		
       
	private static GroupCallHandler groupCallHandler;

	public static Ticket ticketMaster = null;
	protected static GroupRegistry registry;
	
	protected static Vector groups;
	protected static Vector skeletons;

	static { 
		try {
			ticketMaster = new Ticket();
			groups = new Vector();
			skeletons = new Vector();
			
			name = InetAddress.getLocalHost().getHostName();

			if (DEBUG) {
				System.out.println(name + ": init Group RTS");
			}

			

                        ibis         = Ibis.createIbis("ibis:" + name, "ibis.ipl.impl.tcp.TcpIbis", null);
//                        ibis         = Ibis.createIbis("ibis:" + name, "ibis.ipl.impl.panda.PandaIbis", null);
			localID      = ibis.identifier();
			ibisRegistry = ibis.registry();
//			ibis.start);

                        StaticProperties s = new StaticProperties();
			s.add("Serialization", "manta");

                        portType = ibis.createPortType("GMI", s);
                       
			groupCallHandler = new GroupCallHandler();

                        receivePort = portType.createReceivePort("GMI port on " + name, groupCallHandler);		       
			receivePort.enableConnections();

			IbisIdentifier i = (IbisIdentifier) ibisRegistry.elect("GMI Master", localID);

			if (localID.equals(i)) { 

				if (DEBUG) { 
					System.out.println(name + " I am master");
				}

				registry = new GroupRegistry();

				/* I am the master */				
				Properties p = System.getProperties();		

				_size = getIntProperty(p, "pool_total_hosts");
				_rank = 0;
				
				pool = new ReceivePortIdentifier[_size];
				pool[0] = receivePort.identifier();

				if (_size > 1) {

					systemIn  = portType.createReceivePort("GMI Master");
					systemIn.enableConnections();

					systemOut = portType.createSendPort("GMI Master");
					
					for (int j=1;j<_size;j++) { 
						
						ReadMessage r = systemIn.receive();
						ReceivePortIdentifier reply = (ReceivePortIdentifier) r.readObject();
						ReceivePortIdentifier id = (ReceivePortIdentifier) r.readObject();
						r.finish();

//						System.err.println("GOT MESSAGE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1");
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

				systemIn  = portType.createReceivePort("GMI Client " + name);
				systemIn.enableConnections();

				systemOut = portType.createSendPort("GMI Client " + name);

				ReceivePortIdentifier master = ibisRegistry.lookup("GMI Master");

				while (master == null) { 
					try { 
						Thread.sleep(1000);
					} catch (InterruptedException e) { 
						// ignore
					} 
					master = ibisRegistry.lookup("GMI Master");
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

			unicast = new SendPort[_size];
			
			for (int j=0;j<_size;j++) { 				
				unicast[j] = portType.createSendPort("Unicast on " + name + " to " + pool[j].name());

				if (DEBUG) { 
					System.out.println("Connecting unicast sendport " + unicast[j].identifier() + " to " + pool[j]);
				}

				unicast[j].connect(pool[j]);				

				if (DEBUG) { 
					System.out.println("Connecting unicast sendport " + unicast[j].identifier() + " done");
				}

			} 

			if (localID.equals(i)) { 
				for (int j=1;j<_size;j++) { 
					ReadMessage r = systemIn.receive();
					r.finish();
				}
				
				WriteMessage w = systemOut.newMessage(); 
				w.send();
				w.finish();						
			} else { 
				WriteMessage w = systemOut.newMessage(); 
				w.send();
				w.finish();	
				ReadMessage r = systemIn.receive();
				r.finish();
			} 

			multicast = portType.createSendPort("Multicast on " + name);
			
			for (int j=0;j<_size;j++) { 
				multicast.connect(pool[j]);
			}


			receivePort.enableUpcalls();

			if(DEBUG) {
				System.out.println(name + ": Group init");
			}

                } catch (Exception e) {
                        System.err.println(name + ": Could not init Group RTS " + e);
			e.printStackTrace();
                        System.exit(1);
                }
	}

	private static int getIntProperty(Properties p, String name) throws RuntimeException {

		String temp = p.getProperty(name);
		
		if (temp == null) { 
			throw new RuntimeException("Property " + name + " not found !");
		}
		
		return Integer.parseInt(temp);
	}

	protected static long getNewGroupObjectID(GroupSkeleton skel) { 
		
		synchronized (skeletons) {
			int next = skeletons.size();

			long id = _rank;
			id = id << 32;
			id = id | next;

			skeletons.add(next, skel);

			return id;
		}		
	} 

	protected static void registerGroupMember(int groupID, GroupSkeleton skeleton) { 
		/* this is wrong -> fix later */
		groups.add(groupID, skeleton);
	}  
	
	protected static GroupSkeleton getSkeleton(int skel) { 
		/* this is wrong -> fix later */
		return (GroupSkeleton) skeletons.get(skel);
	}  
	
	public static void create(String name, int size) throws RuntimeException {

		try { 
			if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + size + ") starting");

			int ticket = ticketMaster.get();

			WriteMessage w = unicast[0].newMessage();
			w.writeByte(GroupProtocol.REGISTRY);
			w.writeByte(GroupProtocol.CREATE_GROUP);
			w.writeInt(_rank);
			w.writeInt(ticket);
			w.writeObject(name);
			w.writeInt(size);
			w.send();
			w.finish();

			if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + size + ") waiting for reply on ticket(" + ticket +")");

			ReadMessage r = (ReadMessage) ticketMaster.collect(ticket);
			int result = r.readByte();			
			r.finish();

			if (result == GroupProtocol.CREATE_FAILED) { 
				throw new RuntimeException(_rank + " Group.create(" + name + ", " + size + ") Failed : Group allready exists!");  
			}

			if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + size + ") done");

		} catch (IbisIOException e) { 
			throw new RuntimeException(_rank + " Group.create(" + name + ", " + size + ") Failed : communication error !" + e.getMessage());  
		}

	}
       	
	public static void join(String name, ibis.group.GroupMember o) throws RuntimeException {

		try { 
			if (DEBUG) System.out.println(_rank + ": Group.join(" + name + ", " + o + ") starting");

			int groupnumber = 0;
			long [] memberIDs = null;
			boolean retry = true;
			int ticket;
			WriteMessage w;
			ReadMessage r;
			int result;
			
			while (retry) { 

				ticket = ticketMaster.get();
				
				w = unicast[0].newMessage();
				w.writeByte(GroupProtocol.REGISTRY);
				w.writeByte(GroupProtocol.JOIN_GROUP);
				w.writeInt(_rank);
				w.writeInt(ticket);
				w.writeObject(name);
				w.writeLong(o.myID);
				w.send();
				w.finish();

				if (DEBUG) System.out.println(_rank + ": Group.join(" + name + ") waiting for reply on ticket(" + ticket +")");
				
				r = (ReadMessage) ticketMaster.collect(ticket);
				result = r.readByte();			
				
				switch(result) { 
				case GroupProtocol.JOIN_UNKNOWN: 
					if (DEBUG) System.out.println(_rank + ": Group.join(" + name + ") group not found, retry");
					break;

				case GroupProtocol.JOIN_FULL:
					throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : Group full!");  
					
				case GroupProtocol.JOIN_OK:
					retry = false;
					groupnumber = r.readInt();
					memberIDs = (long []) r.readObject();
					break;					
				default:
					System.out.println(_rank + " Group.joinGroup(" + name + ") Failed : got illegal opcode");  									System.exit(1);
				} 
				
				r.finish();
			}
			
			if (DEBUG) System.out.println(_rank + ": Group.join(" + name + ") group(" + groupnumber + ") found !");
						
			o.init(groupnumber, memberIDs);			       

			// do a barrier to make sure all groupmembers are initialized 
			ticket = ticketMaster.get();
			
			w = unicast[0].newMessage();
			w.writeByte(GroupProtocol.REGISTRY);
			w.writeByte(GroupProtocol.BARRIER_GROUP);
			w.writeInt(_rank);
			w.writeInt(ticket);
			w.writeObject(name);
			w.send();
			w.finish();
				
			r = (ReadMessage) ticketMaster.collect(ticket);
			result = r.readByte();			
			r.finish();
			
			switch(result) { 
			case GroupProtocol.BARRIER_FAILED: 
				throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : Barrier failed!");  					
			case GroupProtocol.BARRIER_OK:
				break;					
			default:
				System.out.println(_rank + " Group.joinGroup(" + name + ") Failed : got illegal opcode");  					
				System.exit(1);
			} 
			
			if (DEBUG) System.out.println(_rank + ": Group.join(" + name + ", " + o + ") done");

		} catch (IbisIOException e) { 
			throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : communication error !" + e.getMessage());  
		} catch (ClassNotFoundException e1) { 
			throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : communication error !" + e1.getMessage());  
		}
	} 

	public static int rank() { 
		return _rank;
	}
	
	public static int size() { 
		return _size;
	}       

	public static void exit() { 

		try { 

			if (_rank == 0) { 
				
				if (DEBUG) { 
					System.out.println(name + " master doing exit");
				}
				
				if (_size > 1) {
					
					for (int j=1;j<_size;j++) { 						
						ReadMessage r = systemIn.receive();
						r.finish();
					}
					
					WriteMessage w = systemOut.newMessage(); 
					w.send();
					w.finish();
					
					systemOut.free();
					systemIn.free();
				}
				
			} else { 
				
				if (DEBUG) { 
					System.out.println(name + " client doing exit");
				}
				
				WriteMessage w = systemOut.newMessage();
				w.send();
				w.finish();				

				ReadMessage r = systemIn.receive();
				r.finish();
				systemIn.free();				
				systemOut.free();

			}
			
			for (int i=0;i<_size;i++) { 
				unicast[i].free();
			} 
			
			multicast.free();			
			receivePort.free();			
			ibis.end();
			
			System.out.println("Group exit done");

		} catch (Exception e) { 
			System.err.println("EEEEEK" + e);
		}		
	} 

	public static Method findMethod(Class c, String method, Class [] parameters) { 
	       
		Method temp = null;

		try { 
			temp = c.getDeclaredMethod(method, parameters);			
		} catch (Exception e) { 
			// ignore ... System.out.println("findMethod got " + e);
		}

		return temp;
	} 
	
	private static GroupMethod findMethod(GroupStub s, String method) { 

		for (int i=0;i<s.methods.length;i++) {
			if (s.methods[i].description.equals(method)) { 
				return s.methods[i];
			}
		}
		
		return null;
	} 
        /* ============================= setInvoke methods ========================================== */

	private static void setInvoke(GroupStub s, String method, int mode, int destination, Class personal_class, String personalize) {

		GroupMethod temp = findMethod(s, method);

		if (temp == null) { 
			System.out.println("Method " + method + " not found!");
			System.exit(1);
		}

		switch (mode) { 
		case Group.LOCAL:
	 		if (DEBUG) System.out.println("Setting mode of " + method + " to LOCAL");
		 	temp.invocationMode = Group.LOCAL;
			break;

 		case Group.REMOTE:
	 		if (DEBUG) System.out.println("Setting mode of " + method + " to REMOTE");

	 		if (destination >= 0 && destination < s.size) {
		 		temp.invocationMode = Group.REMOTE;
			 	temp.destinationMember = destination;
				
				long memberID = s.memberIDs[destination];
				temp.destinationRank = (int) ((memberID >> 32) & 0xFFFFFFFFL);
				temp.destinationSkeleton = (int) (memberID & 0xFFFFFFFFL);
 			} else { 
				System.out.println("Method " + method + " destination " + destination + " out of range");
				System.exit(1);
		 	}				
			break;

 		case Group.GROUP:
	 		if (DEBUG) System.out.println("Setting mode of " + method + " to GROUP");
		 	temp.invocationMode = Group.GROUP;
			break;

 		case Group.PERSONALIZE:
	 		if (DEBUG) System.out.println("Setting mode of " + method + " to PERSONAL");

			int p_func = 0;  //getPersonalizeMethod(personal_class, personalize, descriptor);  // should also check return type here !!
		 	
			if (p_func != 0) { 
			 	temp.invocationMode = Group.PERSONALIZE;
				// 				temp.personalization_method = p_func;
 			} else { 
				System.out.println("Method " + method + " personal not implemented yet");
				System.exit(1);
	 		} 
 			break;
 		}
	}

	public static void setInvoke(GroupMethods m, String method, int mode) {

		try { 
 			GroupStub s = (GroupStub) m;

			if (mode == Group.LOCAL || mode == Group.GROUP) { 
				setInvoke(s, method, mode, -1, null, null);
			} else { 
				// exception
			}

	 	} catch (ClassCastException e) { 
		 	// exception
			System.err.println("Group got exception " + e);
			System.exit(1);
		}  
	}

	public static void setInvoke(GroupMethods m, String method, int mode, int destination) {

		try { 
			GroupStub s = (GroupStub) m;

			// remote 
			if (mode == Group.REMOTE) {  
				setInvoke(s, method, mode, destination, null, null);
			} else {  
				// exception 
			} 			
		} catch (ClassCastException e) { 
			// exception
			System.err.println("Group got exception " + e);
			System.exit(1);
		}  
	}

	public static void setInvoke(GroupMethods m, 
				     String method, String desc, int mode, String classname, String personalize) {
		/*
		GroupStub s;
		Class p_class;

		try { 
			s = (GroupStub) m;

			p_class = Class.forName(classname);

		} catch (Exception e) { 
			// exception
		} 

		if (p_class == null) { 
			// exception 
		}

		// personalization
		if (mode == PERSONAL && personalize != null) { 
			s.setInvoke(method, desc, mode, -1, p_class, personalize);
		} else { 
			// exception
		}
		*/
	}

        /* ============================= setResult methods ========================================== */

	private static void setResult(GroupStub s, String method, int mode, Class combine_class, String combine) {

		GroupMethod temp = findMethod(s, method);

		if (temp == null) { 
			System.out.println("Method " + method + " not found!");
			System.exit(1);
		}

 		switch (mode) { 
		case Group.DISCARD:
			temp.resultMode = Group.DISCARD;
			break;	
		case Group.RETURN:
			temp.resultMode = Group.RETURN;
			break;	
		case Group.COMBINE:
			
			temp.combineMethod = findMethod(combine_class, combine, temp.combineParameters);

 			if (temp.combineMethod != null) { 
	 			temp.resultMode = Group.COMBINE;
				temp.combineMethodName = combine;
				temp.combineClass = combine_class;
 			} else { 
				System.out.print("Combine method \"" + temp.returnType.getName() + " " + combine + "(");

				for (int i=0;i<temp.combineParameters.length;i++) { 
					System.out.print(temp.combineParameters[i].getName());
					if (i<temp.combineParameters.length-1) { 
						System.out.print(", ");
					}
				} 
				System.out.println(")\" not found!");
				System.exit(1);
 			} 
	 		break;
 		}
	}


	public static void setResult(GroupMethods m, String method, int mode) {

 		try { 
			GroupStub s = (GroupStub) m;

			// discard or return
			if (mode == Group.DISCARD || mode == Group.RETURN) { 
				setResult(s, method, mode, null, null);
			} else { 
				// exception
			}
 		} catch (ClassCastException e) { 
 			// exception
			System.err.println("Group got exception " + e);
			System.exit(1);
 		} 
	}

	public static void setResult(GroupMethods m, String method, int mode, String classname, String combine) {

		try { 
			GroupStub s = (GroupStub) m;
			Class p_class = Class.forName(classname);

			// personalization
			if (mode == Group.COMBINE && combine != null) { 
				setResult(s, method, mode, p_class, combine);
			} else { 
				// exception
			}

		} catch (Exception e) { 
			// exception
			System.err.println("Group got exception " + e);
			System.exit(1);
		} 		
	}
}




