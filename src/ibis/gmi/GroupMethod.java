package ibis.group;

import ibis.ipl.*;
import java.lang.reflect.Method;

public final class GroupMethod { 

        public String name;
	public String description;

        public Class returnType;
        public Class [] parameters;
	public Class [] personalizeParameters;

	/* vvvvvvvvvvvvvvvvvvvvv Only used in stub vvvvvvvvvvvvvvvvvvvv */

        public byte invocationMode;
        public byte resultMode;        

	public SendPort sendport;

	/* For Group.REMOTE */
        public int destinationMember;
        public int destinationRank;
        public int destinationSkeleton;

	/* For Group.PERSONALIZE */
	public Personalizer personalizer;
	public Method personalizeMethod;

	/* For SELECT result */
        public int sourceMember;
        public int sourceRank;
        public int sourceSkeleton;

	/* For FORWARD result */
	public Forwarder resultForwarder;

	/* For COMBINE result */
        public BinaryCombiner binaryResultCombiner;
        public FlatCombiner flatResultCombiner;

	private GroupStub parent_stub;
	private GroupSkeleton parent_skeleton;
	
	public GroupMethod(GroupStub parent) {
		parent_stub = parent;
	}

	public GroupMethod(GroupSkeleton parent) {
		parent_skeleton = parent;
	}

//	public void localInvoke() { 
//		if (Group.DEBUG) System.out.println("Setting invocation of " + name + " to LOCAL");
//		invocationMode = Group.LOCAL;
//	}

	public void groupInvoke() { 
		if (Group.DEBUG) System.out.println("Setting invocation of " + name + " to GROUP");
		invocationMode = Group.GROUP;	
		sendport = Group.getMulticastSendport(parent_stub.multicastHostsID, parent_stub.multicastHosts);
	} 

	public void remoteInvoke(int destination) {
 		if (Group.DEBUG) System.out.println("Setting invocation of " + name + " to REMOTE");

		if (destination >= 0 && destination < parent_stub.size) {
			invocationMode = Group.REMOTE;
			destinationMember = destination;
			
			long memberID = parent_stub.memberIDs[destination];
			destinationRank = (int) ((memberID >> 32) & 0xFFFFFFFFL);
			destinationSkeleton = (int) (memberID & 0xFFFFFFFFL);

			sendport = Group.unicast[destinationRank];
		} else { 
			System.out.println("Method " + name + " destination " 
					   + destination + " out of range");
			System.exit(1);
		}			
	} 

	public void personalizedInvoke(Personalizer p) { 
//		if (Group.DEBUG) 
		System.out.println("Setting invocation of " + name + " to PERSONAL");
		
		Method temp = null;

		try { 
			temp = p.getClass().getDeclaredMethod("personalize", personalizeParameters);
		} catch (Exception e) { 
			throw new RuntimeException("Setting method " + name + "to personalizedInvoke failed " + e);
		} 
		
		invocationMode = Group.PERSONALIZE;
		personalizeMethod = temp;		
		personalizer = p;

		System.out.println("personalizeMethod = " + temp);
	} 

	public void discardResult() { 
		if (Group.DEBUG) System.out.println("Setting result of " + name + " to DISCARD");
		resultMode = Group.DISCARD;
	} 

	public void returnResult(int source) { 
		if (Group.DEBUG) System.out.println("Setting result of " + name + " to RETURN");

		resultMode = Group.RETURN;

		if (source >= 0 && source < parent_stub.size) {
			resultMode = Group.RETURN;
			sourceMember = source;
			
			long memberID = parent_stub.memberIDs[source];
			sourceRank = (int) ((memberID >> 32) & 0xFFFFFFFFL);
			sourceSkeleton = (int) (memberID & 0xFFFFFFFFL);
		} else { 
			System.out.println("Method " + name + " select result " 
					   + source + " out of range");
			System.exit(1);
		}	
	} 

	public void forwardResult(Forwarder f) { 
		if (Group.DEBUG) System.out.println("Setting result of " + name + " to FORWARD");
		resultMode = Group.FORWARD;
		resultForwarder = f;
	} 

	public void combineResult(BinaryCombiner c) { 
		if (Group.DEBUG) System.out.println("Setting result of " + name + " to BINARYCOMBINE using " + c.getClass().getName());
		resultMode = Group.BINARYCOMBINE;
		binaryResultCombiner = c;
	} 

	public void combineResult(FlatCombiner c) { 
		if (Group.DEBUG) System.out.println("Setting result of " + name + " to FLATCOMBINE using " + c.getClass().getName());
		resultMode = Group.FLATCOMBINE;
		flatResultCombiner = c;
	} 
}
