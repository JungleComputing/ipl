package ibis.gmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Hashtable;

/**
 * The group registry keeps track of which groups there are, and deals with
 * joinGroup, findGroup, and createGroup requests. It also keeps track if
 * combined invocation structures.
 */
final class GroupRegistry implements GroupProtocol {

    /**
     * Hash table for the groups.
     */
    private Hashtable groups;

    /**
     * Current number of groups, used to hand out group identifications.
     */
    private int groupNumber;

    /**
     * Hash table for the combined invocations.
     */
    private Hashtable combinedInvocations;

    /**
     * Container class for the group information that the registry maintains.
     */
    static private final class GroupRegistryData { 
	/**
	 * The group interface through which this group is accessed.
	 */
	String type;

	/**
	 * The group identification.
	 */
	int groupNumber;

	/**
	 * The number of members in this group.
	 */
	int groupSize;

	/**
	 * Skeleton identifications of each group member.
	 */
	int [] memberSkels;

	/**
	 * Node identifications of each group member.
	 */
	int [] memberRanks;

	/**
	 * Tickets for the join-replies once the group is complete.
	 */
	int [] tickets;

	/**
	 * The number of group members that have joined this group so far.
	 */
	int joined;

	/**
	 * Constructor.
	 *
	 * @param groupNumber the number this group
	 * @param groupSize   the number of group members
	 * @param type        the group interface for this group
	 */
	GroupRegistryData(int groupNumber, int groupSize, String type) { 
	    this.groupNumber = groupNumber;
	    this.groupSize   = groupSize;
	    this.type        = type;

	    memberSkels = new int[groupSize];
	    memberRanks = new int[groupSize];
	    tickets   = new int[groupSize];

	    joined = 0;
	} 
    } 
    
    /**
     * Constructor.
     *
     * Allocates hash tables for groups and combined invocations.
     */
    public GroupRegistry() {
	combinedInvocations = new Hashtable();
	groups = new Hashtable();
	groupNumber = 0;
    }

    /**
     * A createGroup request was received from node "rank".
     * This method creates it and writes back the result.
     *
     * @param groupName the name of the group to be created
     * @param groupSize the number of members in the group
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @param type the group interface for this new group
     * @exception {@link java.io.IOException} on an IO error.
     */
    private synchronized void newGroup(String groupName, int groupSize, int rank, int ticket, String type) throws IOException {
           
	WriteMessage w;

	w = Group.unicast[rank].newMessage();
	w.writeByte(REGISTRY_REPLY);
	w.writeInt(ticket);

	if (groups.contains(groupName)) { 
	    w.writeByte(CREATE_FAILED);
	} else { 
	    groups.put(groupName, new GroupRegistryData(groupNumber++, groupSize, type));
	    w.writeByte(CREATE_OK);
	}

	w.send();
	w.finish();		
    } 

    /**
     * A joinGroup request was received from node "rank".
     * This method finds it and writes back the result.
     *
     * @param groupName the name of the group to be joined
     * @param memberSkel identification of the skeleton of the join requester
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @param interfaces the group interfaces that this requester implements
     * @exception {@link io.IOException} on an IO error.
     */
    private synchronized void joinGroup(String groupName, int memberSkel, int rank, int ticket, String [] interfaces) throws IOException { 	

	WriteMessage w;

	GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

	if (e == null) { 
	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);
	    w.writeByte(JOIN_UNKNOWN);
	    w.send();
	    w.finish();		

	} else if (e.joined == e.groupSize) {

	    w = Group.unicast[rank].newMessage();
	    w.writeByte(REGISTRY_REPLY);
	    w.writeInt(ticket);
	    w.writeByte(JOIN_FULL);
	    w.send();
	    w.finish();		

	} else {

	    boolean found = false;

	    for (int i=0;i<interfaces.length;i++) { 
		if (e.type.equals(interfaces[i])) { 
		    found = true;
		    break;
		}
	    }
	    
	    if (!found) {
		w = Group.unicast[rank].newMessage();
		w.writeByte(REGISTRY_REPLY);
		w.writeInt(ticket);
		w.writeByte(JOIN_WRONG_TYPE);
		w.send();
		w.finish();	
	    }
	    
	    e.memberSkels[e.joined] = memberSkel;
	    e.memberRanks[e.joined] = rank;
	    e.tickets[e.joined]   = ticket;
	    e.joined++;

	    if (e.joined == e.groupSize) { 
		for (int i=0;i<e.groupSize;i++) { 
		    
		    w = Group.unicast[e.memberRanks[i]].newMessage();
		    w.writeByte(REGISTRY_REPLY);
		    w.writeInt(e.tickets[i]);
		    w.writeByte(JOIN_OK);
		    w.writeInt(e.groupNumber);
		    w.writeObject(e.memberRanks);
		    w.writeObject(e.memberSkels);
		    w.send();
		    w.finish();		

		    e.tickets[i] = 0;
		} 
		e.tickets = null;
	    }			
	}
    }

    /**
     * A findGroup request was received from node "rank".
     * This method finds it and writes back the result.
     *
     * @param groupName the name of the group to be found
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @exception {@link java.io.IOException} on an IO error.
     */
    private synchronized void findGroup(String groupName, int rank, int ticket) throws IOException { 	

	WriteMessage w;

	GroupRegistryData e = (GroupRegistryData) groups.get(groupName);
	w = Group.unicast[rank].newMessage();
	w.writeByte(REGISTRY_REPLY);
	w.writeInt(ticket);

	if (e == null) { 
	    w.writeByte(GROUP_UNKNOWN);
	} else if (e.joined != e.groupSize) { 
	    w.writeByte(GROUP_NOT_READY);
	} else { 
	    w.writeByte(GROUP_OK);
	    w.writeObject(e.type);
	    w.writeInt(e.groupNumber);
	    w.writeObject(e.memberRanks);
	    w.writeObject(e.memberSkels);
	}
	w.send();
	w.finish();		
    }

    /**
     * Deals with a request for a combined invocation info structure. It waits until
     * all invokers have made such a request, and then writes back the
     * requested information.
     *
     * @param r the request message
     * @exception {@link java.io.IOException} on an IO error.
     */
    private void defineCombinedInvocation(ReadMessage r) throws IOException {
	String name;
	String method;
	int rank;
	int cpu;
	int ticket;
	int size;
	int mode;
	int groupID;
        
	groupID = r.readInt();
	cpu = r.readInt();
	ticket = r.readInt();
	try {
	    name = (String) r.readObject();
	    method = (String) r.readObject();
	} catch (ClassNotFoundException e) {
	    throw new StreamCorruptedException(e.toString());
	}
	rank = r.readInt();
	size = r.readInt();
	mode = r.readInt();
	r.finish();

	String id = name + "?" + method + "?" + groupID;
	CombinedInvocationInfo inf;

	WriteMessage w = Group.unicast[cpu].newMessage();
	w.writeByte(REGISTRY_REPLY);
	w.writeInt(ticket);

	synchronized(this) {
	    inf = (CombinedInvocationInfo) combinedInvocations.get(id);
	    if (inf == null) {
		inf = new CombinedInvocationInfo(groupID, method, name, mode, size);
		combinedInvocations.put(id, inf);
	    }

	    if (inf.mode != mode || inf.numInvokers != size) {
		w.writeByte(COMBINED_FAILED);
		w.writeObject("Inconsistent combined invocation");
		return;
	    }

	    if (inf.present == size) {
		w.writeByte(COMBINED_FAILED);
		w.writeObject("Combined invocation full");
		return;
	    }
	}

	inf.addAndWaitUntilFull(rank, cpu);

	w.writeByte(COMBINED_OK);
	w.writeObject(inf);
	w.send();
	w.finish();
    }

    /**
     * Reads a request from the message and deals with it.
     *
     * @param r the message
     */
    public void handleMessage(ReadMessage r) { 
	try { 

	    byte opcode;
	    
	    int rank;
	    String name;
	    String type;
	    String [] interfaces;
	    int size;
	    int ticket;
	    int number;
	    int memberSkel;
	    
	    opcode = r.readByte();
	    
	    switch (opcode) { 
	    case CREATE_GROUP:
		rank = r.readInt();		
		ticket = r.readInt();
		name = (String) r.readObject();
		type = (String) r.readObject();
		size = r.readInt();
		r.finish();
		
		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a CREATE_GROUP(" + name + ", " + type + ", " + size + ") from " + rank + " ticket(" + ticket +")");
		}
		
		newGroup(name, size, rank, ticket, type);				

		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": CREATE_GROUP(" + name + ", "  + type + ", " + size + ") from " + rank + " HANDLED");
		}
		break;
		
	    case JOIN_GROUP:
		rank = r.readInt();
		ticket = r.readInt();
		name = (String) r.readObject();
		interfaces = (String []) r.readObject();
		memberSkel = r.readInt();
		r.finish();
		
		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a JOIN_GROUP(" + name + ", " + interfaces + ") from " + rank);
		}
		
		joinGroup(name, memberSkel, rank, ticket, interfaces);
		break;		
		
	    case FIND_GROUP:
		rank = r.readInt();
		ticket = r.readInt();
		name = (String) r.readObject();
		r.finish();

		if (Group.DEBUG) { 
		    System.out.println(Group._rank + ": Got a FIND_GROUP(" + name + ")");
		}
		
		findGroup(name, rank, ticket);
		break;		

	    case DEFINE_COMBINED:
		defineCombinedInvocation(r);
		break;
	    }	       
		
	} catch (Exception e) {
	    /* TODO: is this a good way to deal with an exception? */
	    System.out.println(Group._rank + ": Error in GroupRegistry " + e);
	    System.exit(1);
	}
    }        
}
