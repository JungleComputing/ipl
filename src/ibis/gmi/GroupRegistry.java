package ibis.group;

import ibis.ipl.*;

import java.util.Hashtable;

final class GroupRegistry implements GroupProtocol {

	Hashtable groups;
	int groupNumber;
	
        public GroupRegistry() {
		groups = new Hashtable();
		groupNumber = 0;
        }
        
	private synchronized void newGroup(String groupName, int groupSize, int rank, int ticket) throws IbisIOException {
	       
		WriteMessage w;

		w = Group.unicast[rank].newMessage();
		w.writeByte(REPLY);
		w.writeInt(ticket);

		if (groups.contains(groupName)) { 
			w.writeByte(CREATE_FAILED);
		} else { 
			groups.put(groupName, new GroupRegistryData(groupName, groupNumber++, groupSize));
			w.writeByte(CREATE_OK);
		}

		w.send();
		w.finish();		
	} 

	private synchronized void joinGroup(String groupName, long memberID, int rank, int ticket) throws IbisIOException { 	

		WriteMessage w;

		GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

		if (e == null) { 
			w = Group.unicast[rank].newMessage();
			w.writeByte(REPLY);
			w.writeInt(ticket);
			w.writeByte(JOIN_UNKNOWN);
			w.send();
			w.finish();		

		} else if (e.joined == e.groupSize) {

			w = Group.unicast[rank].newMessage();
			w.writeByte(REPLY);
			w.writeInt(ticket);
			w.writeByte(JOIN_FULL);
			w.send();
			w.finish();		

		} else {
			e.memberIDs[e.joined] = memberID;
			e.ranks[e.joined]     = rank;
			e.tickets[e.joined]   = ticket;
			e.joined++;

			if (e.joined == e.groupSize) { 
				for (int i=0;i<e.groupSize;i++) { 
					
					w = Group.unicast[e.ranks[i]].newMessage();
					w.writeByte(REPLY);
					w.writeInt(e.tickets[i]);
					w.writeByte(JOIN_OK);
					w.writeInt(e.groupNumber);
					w.writeObject(e.memberIDs);
					w.send();
					w.finish();		

					e.ranks[i]   = 0;
					e.tickets[i] = 0;
				} 
				e.ranks   = null;
				e.tickets = null;
			}				
		}
	}

	private synchronized void barrierGroup(String groupName, int rank, int ticket) throws IbisIOException { 	

		WriteMessage w;

		GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

		e.b_ranks[e.barrier]   = rank;
		e.b_tickets[e.barrier] = ticket;
		e.barrier++;

		if (e.barrier == e.groupSize) { 
			for (int i=0;i<e.groupSize;i++) { 
				
				w = Group.unicast[e.b_ranks[i]].newMessage();
				w.writeByte(REPLY);
				w.writeInt(e.b_tickets[i]);
				w.writeByte(BARRIER_OK);
				w.send();
				w.finish();		
				
				e.b_ranks[i]   = 0;
				e.b_tickets[i] = 0;
			} 						
			e.b_ranks   = null;
			e.b_tickets = null;
		}
	}

	public void handleMessage(ReadMessage r) { 

		try { 

			byte opcode;
			
			int rank;
			String name;
			int size;
			int ticket;
			int number;
			long memberID;
			
			opcode = r.readByte();
			
			switch (opcode) { 
			case CREATE_GROUP:
				rank = r.readInt();		
				ticket = r.readInt();
				name = (String) r.readObject();
				size = r.readInt();
				r.finish();
				
				if (Group.DEBUG) { 
					System.out.println(Group._rank + ": Got a CREATE_GROUP(" + name + ", " + size + ") from " + rank + " ticket(" + ticket +")");
				}
				
				newGroup(name, size, rank, ticket);				

				if (Group.DEBUG) { 
					System.out.println(Group._rank + ": CREATE_GROUP(" + name + ", " + size + ") from " + rank + " HANDLED");
				}
				break;
				
			case JOIN_GROUP:
				rank = r.readInt();
				ticket = r.readInt();
				name = (String) r.readObject();
				memberID = r.readLong();
				r.finish();
				
				if (Group.DEBUG) { 
					System.out.println(Group._rank + ": Got a JOIN_GROUP(" + name + ") from " + rank);
				}
				
				joinGroup(name, memberID, rank, ticket);
				break;		
				
			case BARRIER_GROUP:
				rank = r.readInt();
				ticket = r.readInt();
				name = (String) r.readObject();
				r.finish();
				
				if (Group.DEBUG) { 
					System.out.println(Group._rank + ": Got a BARRIER_GROUP(" + name + ")");
				}
				
				barrierGroup(name, rank, ticket);
				break;		
			}	       

		} catch (IbisIOException e) {
			System.out.println(Group._rank + ": Error in GroupRegistry " + e);
			System.exit(1);
		} catch (ClassNotFoundException e1) {
			System.out.println(Group._rank + ": Error in GroupRegistry " + e1);
			System.exit(1);
		}
	}        
}
