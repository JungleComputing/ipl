package ibis.group;

import ibis.ipl.*;
import java.util.StringTokenizer;

public class GroupMember { 

	// These are always filled correctly.
        public int groupID;          // groupID
	protected long myID;          // memberID of this object

	public long [] memberIDs;    // memberIDs of all members
	public int  [] memberRanks;  // cpu ranks of all members

	private Class stubClass;

	public  int rank;           // rank of this object 
	public  int size;           // size of the group

	// These are used in the RTS.NEW_GROUP_OK
	//	private GroupMember nextLocalMember;        // points to next local member of this group

	// These are implemented in the RTS.
	//	private native void registerGroupMember(int groupID, GroupMember local);
	//	private native long getObjectID(GroupMember member);	
	//	private native int init_combine(int size);
	//	native void DebugMe(Object o, Object x);

	public GroupSkeleton skeleton;

	public GroupMember() { 		

		if (Group.DEBUG) System.out.println("GroupMember() starting");

		try { 
			String my_package = "";

			String temp = this.getClass().getName();
			StringTokenizer s = new StringTokenizer(temp, ".");
			
			int tokens = s.countTokens();
			
			for (int i=0;i<tokens-1;i++) { 
				my_package += s.nextToken() + ".";
			} 		
			
			String my_name = s.nextToken();

			if (Group.DEBUG) System.out.println("GroupMember() my type is " + my_package + my_name);

			String stub_name = my_package + "group_stub_" + my_name;

			if (Group.DEBUG) System.out.print("GroupMember() looking for " + stub_name);

			stubClass = Class.forName(stub_name);

			if (Group.DEBUG) System.out.println(" found");

			skeleton = (GroupSkeleton) Class.forName(my_package + "group_skeleton_" + my_name).newInstance();	
			myID = Group.getNewGroupObjectID(skeleton);
			
			if (Group.DEBUG) System.out.println("GroupMember() ID is " + myID);

		} catch (Exception e) { 
			if (Group.DEBUG) System.out.println(" not found");
		 	System.out.println("GroupMember could not init " + e);
			System.exit(1);
		}	

		if (Group.DEBUG) System.out.println("GroupMember() done");
	}

	public int getRank() { 
		return rank;
	} 

	public int getSize() { 
		return size;
	} 

	public GroupMethods getGroup() { 
		// returns the multicastStub.
		return null; //(GroupMethods) multicastStub;
	} 

	public GroupMethods getGroup(int wrapper_type, String method) { 
		// returns a stub of type "wraper_type" using method 
                // as the collective method.
		return null;
	} 

	public GroupMethods getGroupMember(int rank) { 
		// returns a unicast stub for member "number".
		//if (unicastStubs == null) return null;
                //return (GroupMethods) unicastStubs[rank];
		return null;
	}              
       
	public GroupStub createGroupStub() { 

		if (Group.DEBUG) System.out.println("GroupMember.createGroupStub() starting");

		GroupStub g = null;
	       
		try { 
			g = (GroupStub) stubClass.newInstance();		
		} catch (Exception e) { 
			System.out.println("GroupMember.createGroupStub() failed " + e);
			System.exit(1);
		} 

		g.init(groupID, memberIDs, rank);

		if (Group.DEBUG) System.out.println("GroupMember.createGroupStub() done");
		return g;
	} 

	protected void init(int groupNumber, long [] ids) { 			

		if (Group.DEBUG) System.out.println("GroupMember.init() starting");
		
		groupID    = groupNumber;
		memberIDs  = ids;
		size       = ids.length;

		memberRanks = new int[size];

		for (int i=0;i<size;i++) { 
			if (memberIDs[i] == myID) { 
				rank = i;
			} 
			memberRanks[i] = (int)((memberIDs[i] >> 32) & 0xFFFFFFFFL);
		}	       

		skeleton.init(this);
		Group.registerGroupMember(groupID, skeleton);	
		
		if (Group.DEBUG) { 
			System.out.println("GroupMember.init() rank = " + rank + " size = " + size);
			System.out.println("GroupMember.init() done");
		} 

		groupInit();
	}

	public void groupInit() { 
		/* overloaded by subtypes if required */
		System.out.println("GroupMember.groupInit()");
	} 
} 





