package ibis.group;

import java.net.InetAddress;

import java.util.Properties;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;

import ibis.ipl.*;
import ibis.util.Ticket;
import ibis.util.PoolInfo;
import ibis.util.SpecialStack;
import java.lang.reflect.Method;

public final class Group { 
    
    public final static boolean DEBUG = true;

    // result opcodes
    public static final byte
	RESULT_VOID      = 0,
	RESULT_BOOLEAN   = 1,
	RESULT_BYTE      = 2,
	RESULT_SHORT     = 3,		
	RESULT_CHAR      = 4,
	RESULT_INT       = 5,
	RESULT_LONG      = 6,
	RESULT_FLOAT     = 7,
	RESULT_DOUBLE    = 8,
	RESULT_OBJECT    = 9,
 	RESULT_EXCEPTION = 10;

    public static int _rank;
    protected static int _size;
    protected static String name;

//    private static i_GroupRegistry _groupRegistry;
       
    private static Ibis ibis;
    private static IbisIdentifier localID;
    private static Registry ibisRegistry;
    
    private static PortType portType;

    private static ReceivePort receivePort;
    private static ReceivePort combinePort;

    private static ReceivePort systemIn;
    private static SendPort    systemOut;

    private static ReceivePortIdentifier [] pool;
    private static ReceivePortIdentifier [] combine_pool;

    // this must be public in order for generated classes to use it....
    public static SendPort [] unicast;
    public static SendPort [] combine_unicast;
//    public static SendPort multicast;		

    private static Hashtable multicastSendports;
       
    private static GroupCallHandler groupCallHandler;
    private static GroupCombineHandler groupCombineHandler;

    public static Ticket ticketMaster = null;
    protected static GroupRegistry registry;
    
    protected static Vector groups;
    protected static Vector skeletons;

    protected static Hashtable stubclasses;

    /* This handles the stubIDs */
    protected static SpecialStack stubIDStack;
    private static Object stubLock;

    static { 
	try {
	    ticketMaster = new Ticket();
	    groups = new Vector();
	    skeletons = new Vector();
	    stubclasses = new Hashtable();
	    multicastSendports = new Hashtable();
	    
	    name = InetAddress.getLocalHost().getHostName();

	    if (DEBUG) {
		System.out.println(name + ": init Group RTS");
	    }
	
	    ibis         = Ibis.createIbis("ibis:" + name, "ibis.ipl.impl.tcp.TcpIbis", null);
//	    ibis         = Ibis.createIbis("ibis:" + name, "ibis.ipl.impl.messagePassing.panda.PandaIbis", null);
	    localID      = ibis.identifier();
	    ibisRegistry = ibis.registry();

	    StaticProperties s = new StaticProperties();
	    s.add("Serialization", "ibis");

	    portType = ibis.createPortType("GMI", s);
	               
	    groupCallHandler = new GroupCallHandler();
	    groupCombineHandler = new GroupCombineHandler();

	    receivePort = portType.createReceivePort("GMI port on " + name, groupCallHandler);
	    receivePort.enableConnections();

	    combinePort = portType.createReceivePort("GMI combine port on " + name, groupCombineHandler);
	    combinePort.enableConnections();

	    IbisIdentifier i = (IbisIdentifier) ibisRegistry.elect("GMI Master", localID);

	    if (localID.equals(i)) {

		if (DEBUG) { 
		    System.out.println(name + " I am master");
		}

		registry = new GroupRegistry();

		/* I am the master */				
		PoolInfo info = new PoolInfo();

		_size = info.size();
		_rank = 0;

		pool = new ReceivePortIdentifier[_size];
		pool[0] = receivePort.identifier();

		combine_pool = new ReceivePortIdentifier[_size];
		combine_pool[0] = combinePort.identifier();

		if (_size > 1) {

		    systemIn  = portType.createReceivePort("GMI Master");
		    systemIn.enableConnections();

		    systemOut = portType.createSendPort("GMI Master");
		    
		    for (int j=1;j<_size;j++) { 
			ReadMessage r = systemIn.receive();
			ReceivePortIdentifier reply = (ReceivePortIdentifier) r.readObject();
			ReceivePortIdentifier id = (ReceivePortIdentifier) r.readObject();
			ReceivePortIdentifier combine_id = (ReceivePortIdentifier) r.readObject();
			r.finish();

//System.err.println("GOT MESSAGE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1");
			systemOut.connect(reply);
			pool[j] = id;
			combine_pool[j] = combine_id;
		    }

		    WriteMessage w = systemOut.newMessage(); 
		    w.writeObject(pool);
		    w.writeObject(combine_pool);
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
		w.writeObject(combinePort.identifier());
		w.send();
		w.finish();

		ReadMessage r = systemIn.receive();
		pool = (ReceivePortIdentifier []) r.readObject();
		combine_pool = (ReceivePortIdentifier []) r.readObject();
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
	    combine_unicast = new SendPort[_size];

	    for (int j=0;j<_size;j++) { 				
		unicast[j] = portType.createSendPort("Unicast on " + name + " to " + pool[j].name());
		combine_unicast[j] = portType.createSendPort("Unicast on " + name + " to " + combine_pool[j].name());

		if (DEBUG) { 
		    System.out.println("Connecting unicast sendport " + unicast[j].identifier() + " to " + pool[j]);
		}

		unicast[j].connect(pool[j]);				
		combine_unicast[j].connect(combine_pool[j]);				

		if (DEBUG) { 
		    System.out.println("Connecting unicast sendport " + unicast[j].identifier() + " done");
		}
	    } 

	    if (_size > 1) { 
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
	    }

//	    multicast = portType.createSendPort("Multicast on " + name);

//	    for (int j=0;j<_size;j++) { 
//		multicast.connect(pool[j]);
//	    }

	    receivePort.enableUpcalls();
	    combinePort.enableUpcalls();

	    stubLock = new Object();
	    stubIDStack = new SpecialStack(stubLock);

	    if(DEBUG) {
		System.out.println(name + ": Group init");
	    }

	} catch (Exception e) {
	    System.err.println(name + ": Could not init Group RTS " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    public static SendPort getMulticastSendport(String ID, int [] hosts) { 

	// Note: for efficiency the ranks in hosts should be sorted (low->high) !!!
	
	if (hosts.length == 1) { 
	    return unicast[hosts[0]];
	}
	System.out.println("Looking for multicast sendport " + ID);

	SendPort temp = (SendPort) multicastSendports.get(ID);

	if (temp == null) { 
	    // there is no multicast sendport to this combination of hosts yet.
	    // so create it and add it to the table.
	    System.out.println("Creating multicast sendport " + ID);

	    try { 
		temp = portType.createSendPort("Multicast on " + name + " to " + ID);
		
		for (int i=0;i<hosts.length;i++) { 
		    temp.connect(pool[hosts[i]]);
		}
	    } catch (IbisIOException e) { 
		 System.err.println(name + ": Could not create multicast group " + ID + " " + e);
		 e.printStackTrace();
		 System.exit(1);
	    } 

	    multicastSendports.put(ID, temp);
	}

	return temp;
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
//		if (Group.DEBUG) 
	System.out.println("Group.registerGroupMember(" + groupID + " " + 
		   skeleton.getClass().getName());
	
	groups.add(groupID, skeleton);
    }  
    
    protected static GroupSkeleton getSkeleton(int skel) { 
	/* this is wrong -> fix later */
	return (GroupSkeleton) skeletons.get(skel);
    }  
    
    public static void create(String name, Class type, int size) throws RuntimeException {

	try { 
	    if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + type + ", " + size + ") starting");

	    int ticket = ticketMaster.get();

	    WriteMessage w = unicast[0].newMessage();
	    w.writeByte(GroupProtocol.REGISTRY);
	    w.writeByte(GroupProtocol.CREATE_GROUP);
	    w.writeInt(_rank);
	    w.writeInt(ticket);
	    w.writeObject(name);
	    w.writeObject(type.getName());
	    w.writeInt(size);
	    w.send();
	    w.finish();

	    if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + size + ") waiting for reply on ticket(" + ticket +")");

	    ReadMessage r = (ReadMessage) ticketMaster.collect(ticket);
	    int result = r.readByte();			
	    r.finish();
	    synchronized(r) {
	        r.notify();
	    }

	    if (result == GroupProtocol.CREATE_FAILED) { 
		throw new RuntimeException(_rank + " Group.create(" + name + ", " + size + ") Failed : Group allready exists!");  
	    }

	    if (DEBUG) System.out.println(_rank + ": Group.create(" + name + ", " + size + ") done");

	} catch (IbisIOException e) { 
	    throw new RuntimeException(_rank + " Group.create(" + name + ", " + size + ") Failed : communication error !" + e.getMessage());  
	}

    }

    public static void join(String name, GroupMember o) throws RuntimeException {

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
		w.writeObject(o.groupInterfaces);
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

		case GroupProtocol.JOIN_WRONG_TYPE:
		    throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : Group member has wrong type!");  

		case GroupProtocol.JOIN_FULL:
		    throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : Group full!");  
		    
		case GroupProtocol.JOIN_OK:
		    retry = false;
		    groupnumber = r.readInt();
		    memberIDs = (long []) r.readObject();
		    break;
		default:
		    System.out.println(_rank + " Group.joinGroup(" + name + ") Failed : got illegal opcode");  		
		    System.exit(1);
		} 
		
		r.finish();
		synchronized(r) {
		    r.notify();
		}
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
	    synchronized(r) {
	        r.notify();
	    }
	    
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

	} catch (Exception e) { 
	    throw new RuntimeException(_rank + " Group.joinGroup(" + name + ") Failed : communication error !" + e.getMessage());  
	}
    } 
    
    public static GroupInterface lookup(String name) throws RuntimeException { 
    
	GroupStubData data = (GroupStubData) stubclasses.get(name);
    
	if (data == null) {

	    try { 		
		boolean done = false;

		while (!done) { 
		    /* this group is unknown -> go and ask the registry */
		    int ticket = ticketMaster.get();;
		    WriteMessage w = unicast[0].newMessage();
		    w.writeByte(GroupProtocol.REGISTRY);
		    w.writeByte(GroupProtocol.FIND_GROUP);
		    w.writeInt(_rank);
		    w.writeInt(ticket);
		    w.writeObject(name);
		    w.send();
		    w.finish();
		    
		    data = new GroupStubData();
		
		    ReadMessage r = (ReadMessage) ticketMaster.collect(ticket);
		    byte result = r.readByte();		
		    
		    switch (result) { 
		    case GroupProtocol.GROUP_UNKOWN:
			r.finish();
			synchronized(r) {
			    r.notify();
			}
			throw new RuntimeException(Group._rank + " Group.createGroupInterface(" + name + ") Failed : unknown group!");  	
			
		    case GroupProtocol.GROUP_NOT_READY: 
			System.err.println("Group " + name + " not ready yet -> going to sleep");
			r.finish();
			synchronized(r) {
			    r.notify();
			}
			try { 
			    Thread.sleep(100);
			} catch (Exception e) { 
			    // ignore
			}
			break;
			
		    case GroupProtocol.GROUP_OK:
			done = true;

			data.groupName = name;
			data.typeName  = (String) r.readObject();
			data.groupID   = r.readInt();
			data.memberIDs = (long []) r.readObject();
			r.finish();
			synchronized(r) {
			    r.notify();
			}
			
			try { 	
			    String classname = "";
			    String temp = data.typeName;
			    StringTokenizer s = new StringTokenizer(temp, ".");
			    int tokens = s.countTokens();
			    
			    if (tokens > 1) { 
				classname = s.nextToken();
				
				for (int i=1;i<tokens-1;i++) { 
				    classname += "." + s.nextToken();
				}
				classname += ".";
			    } 		
			    
			    classname += "group_stub_" + s.nextToken();
			    data.stubClass = Class.forName(classname); 
			} catch (Exception e) { 
			    throw new RuntimeException(Group._rank + " Group.createGroupInterface(" + name + ") Failed : unknown group!");  
			} 
		    } 
		} 

	    } catch (Exception e) { 
		throw new RuntimeException(Group._rank + " Group.createGroupInterface(" + name + ") Failed : " + e); 
	    } 

	    stubclasses.put(name, data);
	}
	    
	int num = 0;
	GroupInterface s = null;;

	num = stubIDStack.getPosition();
	s = data.newStub(num);
	stubIDStack.putData(num, s);
	return s;
    }


    public static CombinedInvocationInfo defineCombinedInvocation(CombinedInvocation ci, int groupID, String method, String name, int mode, int rank, int size) throws RuntimeException { 
	try { 		
	    int ticket = ticketMaster.get();;
	    WriteMessage w = unicast[0].newMessage();
	    w.writeByte(GroupProtocol.REGISTRY);
	    w.writeByte(GroupProtocol.DEFINE_COMBINED);
	    w.writeInt(groupID);
	    w.writeInt(_rank);
	    w.writeInt(ticket);
	    w.writeObject(name);
	    w.writeObject(method);
	    w.writeInt(rank);
	    w.writeInt(size);
	    w.writeInt(mode);
	    w.send();
	    w.finish();
		    
	    ReadMessage r = (ReadMessage) ticketMaster.collect(ticket);
	    byte result = r.readByte();		
		    
	    switch (result) { 
	    case GroupProtocol.COMBINED_FAILED: {
		String reason = (String) r.readObject();
		r.finish();
		throw new RuntimeException(reason);
		}
	    case GroupProtocol.COMBINED_OK: {
		CombinedInvocationInfo info = (CombinedInvocationInfo) r.readObject();
		r.finish();
		return info;
		}
	    default:
		throw new RuntimeException("Unexpected answer on DEFINE_COMBINED");
	    }
	} catch (Exception e) { 
	    throw new RuntimeException(Group._rank + " Group.createGroupInterface(" + name + ") Failed : " + e); 
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
		combine_unicast[i].free();
	    } 

	    Enumeration hash_elts = multicastSendports.elements();

	    while (hash_elts.hasMoreElements()) {
		SendPort p = (SendPort) (hash_elts.nextElement());
		p.free();
	    }
	    
	    //multicast.free();			
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
    
    public static GroupMethod findMethod(GroupInterface i, String desc) throws NoSuchMethodException { 
	GroupStub stub = (GroupStub) i;
	return stub.getMethod(desc);
    }
}
