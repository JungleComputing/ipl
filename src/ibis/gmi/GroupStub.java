package ibis.group;

// This is a base class for generated group stubs

public class GroupStub { 
	
	// all set by the RTS.
	//	protected Class myClass;
	protected int groupID;
	protected int rank;
	protected int size;
	protected long [] memberIDs;

	protected transient GroupSkeleton localSkeleton;

	protected GroupMethod [] methods;
             
	protected GroupStub(int numMethods) { 		
		methods = new GroupMethod[numMethods];	      
	} 
       
	protected void init(int groupID, long [] memberIDs, int rank) { 
		System.out.println("GroupStub.init() started");

 		this.groupID   = groupID;
	 	this.memberIDs = memberIDs;
		this.rank      = rank;
		this.size      = memberIDs.length; 

		int skel = (int) (memberIDs[rank] & 0xFFFFFFFFL);

		this.localSkeleton = Group.getSkeleton(skel);

		System.out.println("GroupStub.init() done");
	}             	
}


