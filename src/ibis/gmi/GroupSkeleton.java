package ibis.group;

import ibis.ipl.ReadMessage;
import ibis.ipl.IbisException;

// This is a base class for generated group stubs

public abstract class GroupSkeleton { 
       	
	protected GroupMember destination;
	protected GroupMethod [] methods;
	protected GroupMessageQueue messageQ;

	public GroupSkeleton(int numMethods) { 
		methods = new GroupMethod[numMethods];	      
	} 

	public void init(GroupMember dest) { 
		destination = dest;
		messageQ = new GroupMessageQueue(dest.size);
	}

	public abstract void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException;	
}


