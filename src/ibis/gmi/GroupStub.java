package ibis.group;

import ibis.ipl.*;
import ibis.util.SpecialStack;
// This is a base class for generated group stubs

public class GroupStub implements GroupInterface { 

	// all set by the RTS.
	//	protected Class myClass;
	protected int groupID;
//	public int rank;     // remove this !!!
	protected int size;
	protected long [] memberIDs;
	protected int [] multicastHosts;
	protected String multicastHostsID;
	
//	protected transient GroupSkeleton localSkeleton; // remove this ????!!!

	// this is the stack of tickets, used for reply handling
	private final static int TICKET_SIZE = 16;

	protected int realStubID;
	protected int shiftedStubID;
	protected SpecialStack replyStack;

	protected GroupMethod [] methods;
             
	protected GroupStub(int numMethods) { 		
		methods = new GroupMethod[numMethods];	      
	} 
       
	protected void init(int groupID, long [] memberIDs, int stubID) { 
		System.out.println("GroupStub.init(" + stubID + ") started");

 		this.groupID   = groupID;
	 	this.memberIDs = memberIDs;
		//this.rank      = rank;
		this.size      = memberIDs.length; 

		//int skel = (int) (memberIDs[rank] & 0xFFFFFFFFL);
		//this.localSkeleton = Group.getSkeleton(skel);

		// Find all the ranks.
		multicastHosts = new int[memberIDs.length];
		
		for (int i=0;i<memberIDs.length;i++) { 
			multicastHosts[i] =  (int) ((memberIDs[i] >> 32) & 0xFFFFFFFFL);
		} 

		// sort them low...high (bubble sort)
		for (int i=0;i<multicastHosts.length-1;i++) { 
			for (int j=i+1;j<multicastHosts.length;j++) { 
				if (multicastHosts[i] > multicastHosts[j]) { 
					int temp = multicastHosts[i];
					multicastHosts[i] = multicastHosts[j];
					multicastHosts[j] = temp;
				} 
			}		
		}

		// create a multicast ID
		StringBuffer buf = new StringBuffer("");

		for (int i=0;i<multicastHosts.length;i++) { 
			buf.append(multicastHosts[i]);
			buf.append(".");				
		} 
		
		multicastHostsID = buf.toString();

		// init the ticketservice
		realStubID    = stubID;
		shiftedStubID = stubID << 16;
		replyStack = new SpecialStack(this);
		System.out.println("GroupStub.init(" + stubID + ") done");
	}             	

	private GroupMessage resultMessageCache = null;

	public GroupMessage getGroupMessage() { 
		
		if (resultMessageCache == null) { 
			return new GroupMessage();
		} else { 
			GroupMessage temp = resultMessageCache;
			resultMessageCache = temp.next;
			temp.next = null;
			return temp;
		}
	} 

	public void freeGroupMessage(GroupMessage m) { 		
		m.objectResult = m.exceptionResult = null;
		m.next = resultMessageCache;
		resultMessageCache = m;
	} 
	
	protected void handleResultMessage(ReadMessage r, int ticket, byte resultMode) throws IbisException, IbisIOException { 

		if (resultMode == Group.FORWARD) { 
			Forwarder f = (Forwarder) replyStack.peekData(ticket);
			f.receive(r);
		} else { 
			GroupMessage m = getGroupMessage();
			m.rank = r.readInt();
			byte result_type = r.readByte();

			switch (result_type) { 
			case Group.RESULT_VOID:				
				break;
			case Group.RESULT_BOOLEAN:
				try { 
					m.booleanResult = r.readBoolean();
				} catch (Exception e) {
					m.exceptionResult = e;
				}
				break;
			case Group.RESULT_BYTE:
				try {
					m.byteResult = r.readByte();
				} catch (Exception e) {
					m.exceptionResult = e;	
				}
				break;
			case Group.RESULT_SHORT:
				try {
					m.shortResult = r.readShort();
				} catch (Exception e) {
					m.exceptionResult = e;
				}
				break;
			case Group.RESULT_CHAR:
				try {
					m.charResult = r.readChar();
				} catch (Exception e) {
					m.exceptionResult = e;					
				}
				break;
			case Group.RESULT_INT:
				try {
					m.intResult = r.readInt();
				} catch (Exception e) {
					m.exceptionResult = e;					
				}
				break;
			case Group.RESULT_LONG:
				try {
					m.longResult = r.readLong();
				} catch (Exception e) {
					m.exceptionResult = e;					
				}
				break;
			case Group.RESULT_FLOAT:
				try {
					m.floatResult = r.readFloat();
				} catch (Exception e) {
					m.exceptionResult = e;					
				}
				break;
			case Group.RESULT_DOUBLE:
				try {
					m.doubleResult = r.readDouble();
				} catch (Exception e) {
					m.exceptionResult = e;
				}
				break;
			case Group.RESULT_OBJECT:
				try {
					m.objectResult = r.readObject();
				} catch (Exception e) {
					m.exceptionResult = e;
				}
				break;
			case Group.RESULT_EXCEPTION:
				try {
					m.exceptionResult = (Exception) r.readObject();
				} catch (Exception e) {
					m.exceptionResult = e;
				}
				break;
			}		
			replyStack.putData(ticket, m);
		} 
	} 
}












