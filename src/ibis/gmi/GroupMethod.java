package ibis.group;

import  ibis.ipl.SendPort;

public class GroupMethod { 	

    public int invocation_mode;
    public int result_mode;
    public InvocationScheme inv;
    public ReplyScheme rep;

    public SendPort sendport;

    /* For InvocationScheme.I_SINGLE: */
    public int destinationSkeleton;

    String descriptor;
    GroupStub parent_stub;
    GroupSkeleton parent_skeleton;
    CombinedInvocationInfo info;

    public GroupMethod(GroupStub parent, String descriptor) { 
	this.descriptor = descriptor;
	this.parent_stub = parent;
    } 

    public GroupMethod(GroupSkeleton parent, String descriptor) { 
	this.descriptor = descriptor;
	this.parent_skeleton = parent;
    } 

    public void configure(InvocationScheme inv, ReplyScheme rep) throws ConfigurationException { 
	
	// all configurations are valid now ???

	switch (inv.mode) { 
	case InvocationScheme.I_SINGLE: 
	    SingleInvocation si = (SingleInvocation)inv;

	    if (parent_stub.size < si.destination || si.destination < 0) { 
		throw new ConfigurationException("Invalid configuration: destination groupmember ouf of range!");
	    } 

	    long memberID = parent_stub.memberIDs[si.destination];
	    destinationSkeleton = (int) (memberID & 0xFFFFFFFFL);
	    sendport = Group.unicast[(int) ((memberID >> 32) & 0xFFFFFFFFL)];
	
	    if (rep.mode == ReplyScheme.R_RETURN) { 
		if (si.destination != ((ReturnReply)rep).rank) { 
		    throw new ConfigurationException("Invalid configuration: invalid reply rank!");
		} 
	    } 


	    invocation_mode = inv.mode;
	    break;
	case InvocationScheme.I_GROUP: 
	    // extra checks ?? 
	    invocation_mode = inv.mode;			
	    sendport = Group.getMulticastSendport(parent_stub.multicastHostsID, parent_stub.multicastHosts);
	    break;
	case InvocationScheme.I_PERSONAL: 
	    invocation_mode = inv.mode;
	    break;
	case InvocationScheme.I_COMBINED_FLAT:
	case InvocationScheme.I_COMBINED_BINARY: {
	    // add more parameter checks here ?
	    CombinedInvocation ci = (CombinedInvocation)inv;

	    invocation_mode = inv.mode + ci.inv.mode;
	    info = Group.defineCombinedInvocation(ci, parent_stub.groupID, descriptor, ci.id, inv.mode, ci.rank, ci.size);
	    info.rank = ci.rank;
	    }
	    break;
	} 

	switch (rep.mode) { 
	case ReplyScheme.R_DISCARD:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_RETURN:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_FORWARD:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_COMBINE_BINARY:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_COMBINE_FLAT:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_PERSONALIZED_RETURN:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_PERSONALIZED_FORWARD:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_PERSONALIZED_COMBINE_BINARY:
	    result_mode = rep.mode;
	    break;
	case ReplyScheme.R_PERSONALIZED_COMBINE_FLAT:
	    result_mode = rep.mode;
	    break;
	}

	this.inv = inv;
	this.rep = rep;		
    } 
}
