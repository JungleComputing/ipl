package ibis.gmi;

import ibis.ipl.IbisException;
import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.util.Ticket;

import java.io.IOException;

/**
 * The {@link GroupStub} class provides a base class for generated stubs. A GroupStub
 * provides an interface through which group methods can be called.
 */

public class GroupStub implements GroupInterface, GroupProtocol { 

    /**
     * The identification of the target group.
     */
    protected int groupID;

    /**
     * The number of members in the target group.
     */
    protected int targetGroupSize;

    /**
     * The member ranks. When indexed with the rank within group "groupID",
     * this gives the global rank.
     */
    protected int [] memberRanks;

    /**
     * The member skeleton identifications.
     */
    protected int [] memberSkels;

    /**
     * Like {@link #memberRanks}, but sorted. Together with
     * {@link #multicastHostsID} used to create multicast ports.
     * Note that this information is also available in {@link GroupMember},
     * but not all stubs are group members!
     */
    protected int [] multicastHosts;

    /**
     * A unique string, identifying a multicast port. See also
     * {@link #multicastHosts}.
     */
    protected String multicastHostsID;
    
    /* Identification of this stub. */
    private int realStubID;

    /**
     * Stub identification, but shifted, to make room for a ticket.
     * Needs to be protected (not private) so that generated stubs can
     * access it.
     */
    protected int shiftedStubID;

    /**
     * Where the replies are pushed, identified by tickets.
     */
    protected Ticket replyStack;

    /**
     * A list of the methods in this group.
     */
    protected GroupMethod [] methods;

    /** Invocation count for combined invocations. These are handled in the stubs,
     * not in the skeletons, because invokers don't have to be members of the
     * group.
     */
    private int invocation_count = 0;

    /**
     * True when the current cpu has started a combined invocation.
     */
    private boolean started = false;

    private int waiters = 0;


    /**
     * Constructor. Create the {@link #methods} array, which is to be filled
     * in by the compiler-generated constructor.
     *
     * @param numMethods the number of methods
     */
    protected GroupStub(int numMethods) { 		
	methods = new GroupMethod[numMethods];	      
    } 
       
    /**
     * Initializes the stub, once everything is known.
     *
     * @param groupID the identification of the group
     * @param memberRanks the ranks of the group members
     * @param memberSkels the skeleton identifications of the group skeletons
     * @param stubID the identification of this stub
     */
    protected void init(int groupID, int memberRanks[], int [] memberSkels, int stubID) { 
	if (Group.DEBUG) System.out.println("GroupStub.init(" + stubID + ") started");

	this.groupID   = groupID;
	this.memberRanks = memberRanks;
     	this.memberSkels = memberSkels;
	this.targetGroupSize = memberSkels.length; 

	// Find all the ranks.
	multicastHosts = new int[memberRanks.length];
	
	for (int i=0;i<memberRanks.length;i++) { 
	    multicastHosts[i] =  memberRanks[i];
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
	replyStack = new Ticket();
	if (Group.DEBUG) System.out.println("GroupStub.init(" + stubID + ") done, multicastHostsID = " + multicastHostsID);
    }             	

    /**
     * Find the group method described by the parameter "desc".
     *
     * @param desc the description of the group method. Format example: "void set(int)"
     * @return A {@link GroupMethod} object, or null when not found.
     */
    protected GroupMethod getMethod(String desc) {
        for (int i=0;i<methods.length;i++) {
	    if (desc.equals(methods[i].descriptor)) {
		return methods[i];
	    }
        }
        return null;
    }

    /**
     * Linked list of (cached) free result message structures.
     */
    private GroupMessage resultMessageCache = null;

    /**
     * Gets a group message from the cache, or allocates a new one.
     */
    private synchronized GroupMessage getGroupMessage() { 
	GroupMessage temp = resultMessageCache;

	if (temp == null) { 
	    return new GroupMessage();
	}

	resultMessageCache = temp.next;
	temp.next = null;
	return temp;
    } 

    /**
     * Returns a group message to the group message cache.
     *
     * @param m the group message to be returned
     */
    protected synchronized void freeGroupMessage(GroupMessage m) { 		
	m.objectResult = m.exceptionResult = null;
	m.next = resultMessageCache;
	resultMessageCache = m;
    } 

    /**
     * Deals with a result message. It either must be forwarded, or be placed
     * on the replystack, with its ticket.
     *
     * @param r the result message
     * @param ticket the ticket number of this reply
     * @param resultMode the reply handling scheme
     */
    protected final void handleResultMessage(ReadMessage r, int ticket, byte resultMode) throws IbisException, IOException { 

	if (resultMode == ReplyScheme.R_FORWARD) { 
	    Forwarder f = (Forwarder) replyStack.peek(ticket);
	    synchronized(this) {
		f.receive(r);
	    }
	} else { 
	    GroupMessage m = getGroupMessage();
	    m.rank = r.readInt();
	    byte result_type = r.readByte();

	    switch (result_type) { 
	    case RESULT_VOID:				
		break;
	    case RESULT_BOOLEAN:
		try { 
		    m.booleanResult = r.readBoolean();
		} catch (Exception e) {
		    m.exceptionResult = e;
		}
		break;
	    case RESULT_BYTE:
		try {
		    m.byteResult = r.readByte();
		} catch (Exception e) {
		    m.exceptionResult = e;	
		}
		break;
	    case RESULT_SHORT:
		try {
		    m.shortResult = r.readShort();
		} catch (Exception e) {
		    m.exceptionResult = e;
		}
		break;
	    case RESULT_CHAR:
		try {
		    m.charResult = r.readChar();
		} catch (Exception e) {
		    m.exceptionResult = e;					
		}
		break;
	    case RESULT_INT:
		try {
		    m.intResult = r.readInt();
		} catch (Exception e) {
		    m.exceptionResult = e;					
		}
		break;
	    case RESULT_LONG:
		try {
		    m.longResult = r.readLong();
		} catch (Exception e) {
		    m.exceptionResult = e;					
		}
		break;
	    case RESULT_FLOAT:
		try {
		    m.floatResult = r.readFloat();
		} catch (Exception e) {
		    m.exceptionResult = e;					
		}
		break;
	    case RESULT_DOUBLE:
		try {
		    m.doubleResult = r.readDouble();
		} catch (Exception e) {
		    m.exceptionResult = e;
		}
		break;
	    case RESULT_OBJECT:
		try {
		    m.objectResult = r.readObject();
		} catch (Exception e) {
		    m.exceptionResult = e;
		}
		break;
	    case RESULT_EXCEPTION:
		try {
		    m.exceptionResult = (Exception) r.readObject();
		} catch (Exception e) {
		    m.exceptionResult = e;
		}
		break;
	    }		
	    replyStack.put(ticket, m);
	} 
	r.finish();
    } 

    /* Fill a writeMessage and send it out. */
    private final void do_message(WriteMessage w, GroupMethod m, ReplyPersonalizer personalizer, int dest, ParameterVector v) throws IOException {
	w.writeByte(INVOCATION);
	w.writeInt(dest);
	w.writeByte((byte) m.invocation_mode);
	w.writeByte((byte) m.result_mode);
	w.writeInt(m.index);
	if (m.info != null) {
	    w.writeObject(m.info);
	}
	if (personalizer != null) {
	    w.writeObject(personalizer);
	    if (m.result_mode == ReplyScheme.R_PERSONALIZED_COMBINE_BINOMIAL) {
		w.writeObject(((CombineReply)(((PersonalizeReply)(m.rep)).rs)).binomialCombiner);
	    }
	} else if (m.result_mode == ReplyScheme.R_COMBINE_BINOMIAL) {
	    w.writeObject(((CombineReply)(m.rep)).binomialCombiner);
	}
	v.writeParameters(w);
	w.send();
	w.finish();
    }

    /**
     * When all parameters have been combined, actually invokes the group method.
     */
    private final void do_invoke(GroupMethod m, CombinedInvocation inv, ReplyPersonalizer personalizer) throws IOException {
	switch (inv.inv.mode) {
	case InvocationScheme.I_SINGLE:
	    {
		if (Group.DEBUG) System.out.println("Single invoke");
		WriteMessage w = m.sendport.newMessage();
		do_message(w, m, personalizer, m.destinationSkeleton, m.info.out);
	    }
	    break;
	case InvocationScheme.I_GROUP:
	    {
		if (Group.DEBUG) System.out.println("Group invoke");
		WriteMessage w = m.sendport.newMessage();
// System.out.println("Sendport = " + m.sendport);
		do_message(w, m, personalizer, groupID, m.info.out);
	    }
	    break;
	case InvocationScheme.I_PERSONAL:
	    {
		if (Group.DEBUG) System.out.println("Personalized invoke");
		ParameterVector [] personal = new ParameterVector[targetGroupSize];
		for (int i = 0; i < targetGroupSize; i++) {
		    personal[i] = m.info.out.getVector();
		}
		try {
		    ((PersonalizedInvocation)(inv.inv)).p.personalize(m.info.out, personal);
		} catch (Exception e) {
		    throw new RuntimeException("exception in personalize: " + e);
		}
		for (int i = 0; i < targetGroupSize; i++) {
		    if (! personal[i].done) {
			throw new RuntimeException("Parameters for member " + i + " not completed.");
		    }
		}
		for (int i = 0; i < targetGroupSize; i++) {
		    WriteMessage w = Group.unicast[memberRanks[i]].newMessage();
		    do_message(w, m, personalizer, memberSkels[i], personal[i]);
		}
	    }
	}
    }

    /**
     * Invokes a group method using the flat-combining invocation scheme.
     * This method blocks until a result is available, and returns it, unless,
     * of course, the result is discarded, in which case it still blocks if this
     * node actually has to take care of the invocation.
     * Currently, the node with rank number 0 in the "group" of invokers
     * deals with that. It collects the parameters of the other invokers,
     * combines them, and does the invocation.
     *
     * @param params the parameters for this invocation (yet to be combined)
     * @param m the group method to be invoked
     * @return The {@link GroupMessage} that holds the result of the combined
     * invocation, or null if the result is discarded or forwarded.
     *
     * TODO: Deal with exceptions.
     *
     */
    protected final GroupMessage flatCombineInvoke(ParameterVector params, GroupMethod m) throws IOException {

	CombinedInvocation inv = (CombinedInvocation) m.inv;
	CombinedInvocationInfo info = m.info;
	int ticket = 0;
	ReplyPersonalizer personalizer = null;
	ReplyScheme rep = m.rep;

	int result_mode = m.result_mode;
	if (result_mode >= ReplyScheme.R_PERSONALIZED) {
	    result_mode -= ReplyScheme.R_PERSONALIZED;
	    personalizer = ((PersonalizeReply)rep).rp;
	    rep = ((PersonalizeReply)rep).rs;
	}

	if (info.myInvokerRank == 0) {
	    synchronized(this) {
		if (info.in == null) {
		    info.in = new ParameterVector[info.numInvokers];
		}
		info.in[0] = params;
		info.out = params.getVector();
		invocation_count = 1;
		started = true;
		if (waiters != 0) {
		    notifyAll();
		}

		while (invocation_count < info.numInvokers) {
		    try {
			wait();
		    } catch(Exception e) {
		    }
		}

		started = false;
	    }

	    inv.flatCombiner.combine(info.in, info.out);

	    switch(result_mode) {
	    case ReplyScheme.R_COMBINE_BINOMIAL:
	    case ReplyScheme.R_COMBINE_FLAT:
	    case ReplyScheme.R_FORWARD:
	    case ReplyScheme.R_RETURN:
		ticket = replyStack.get();
		info.stubids_tickets[0] = shiftedStubID | ticket;
	    }
	    do_invoke(m, inv, personalizer);
	}
	else {
	    WriteMessage w = Group.unicast[info.participating_cpus[0]].newMessage();

	    w.writeByte(INVOCATION_FLATCOMBINE);
	    w.writeInt(realStubID);
	    w.writeInt(m.index);
	    w.writeInt(info.myInvokerRank);

	    switch(result_mode) {
	    case ReplyScheme.R_COMBINE_BINOMIAL:
	    case ReplyScheme.R_COMBINE_FLAT:
	    case ReplyScheme.R_FORWARD:
	    case ReplyScheme.R_RETURN:
		ticket = replyStack.get();
		w.writeInt(ticket);
	    }
	    params.writeParameters(w);
	    w.send();
	    w.finish();
	}

	switch(result_mode) {
	case ReplyScheme.R_DISCARD:
	    return null;
	case ReplyScheme.R_FORWARD:
	    synchronized(this) {
		((ForwardReply) rep).f.startReceiving(this, targetGroupSize, ticket);
	    }
	    return null;
	default:
	    return (GroupMessage) replyStack.collect(ticket);
	}
    }

    /**
     * Invokes a group method using the binomial-combining invocation scheme.
     * This method blocks until a result is available, and returns it, unless,
     * of course, the result is discarded, in which case it still blocks if this
     * node actually has to take care of the invocation, or combine parameters
     * of other nodes.
     * Currently, the node with rank number 0 in the "group" of invokers
     * deals with the invocation.
     *
     * @param params the parameters for this invocation (yet to be combined)
     * @param m the group method to be invoked
     * @return The {@link GroupMessage} that holds the result of the combined invocation.
     *
     * TODO: Deal with exceptions.
     *
     */
    protected final GroupMessage binCombineInvoke(ParameterVector params, GroupMethod m) throws IOException {
	CombinedInvocation inv = (CombinedInvocation) m.inv;
	CombinedInvocationInfo info = m.info;
	ReplyPersonalizer personalizer = null;
	ReplyScheme rep = m.rep;

	int peer;
	int lroot = 0;
	int mask = 1;
	int size = info.numInvokers;
	int rank = info.myInvokerRank;
	int relrank = (rank - lroot + size) % size;		
	int ticket = 0;

	int result_mode = m.result_mode;
	if (result_mode >= ReplyScheme.R_PERSONALIZED) {
	    result_mode -= ReplyScheme.R_PERSONALIZED;
	    personalizer = ((PersonalizeReply)rep).rp;
	    rep = ((PersonalizeReply)rep).rs;
	}

	synchronized(this) {
	    if (info.in == null) {
		info.in = new ParameterVector[size];
	    }
	    for (int i = 0; i < size; i++) {
		info.in[i] = null;
		info.stubids_tickets[i] = -1;
	    }
	    switch(result_mode) {
	    case ReplyScheme.R_COMBINE_BINOMIAL:
	    case ReplyScheme.R_COMBINE_FLAT:
	    case ReplyScheme.R_FORWARD:
	    case ReplyScheme.R_RETURN:
		ticket = replyStack.get();
		break;
	    }
	    info.stubids_tickets[info.myInvokerRank] = ticket | shiftedStubID;
// System.out.println("Starting binomial-combined invocation");
	    started = true;
	    if (waiters != 0) {
		notifyAll();
	    }

	    while (mask < size) {
		if ((mask & relrank) == 0) {
		    peer = relrank | mask;
		    if (peer < size) {
			peer = (peer + lroot) % size;
			ParameterVector n = params.getVector();

			while (info.in[peer] == null) {
			    try {
				wait();
			    } catch(Exception e) {
			    }
			}
			inv.binCombiner.combine(params, info.in[peer], n);
			info.in[peer] = null;

// System.out.println("Got parameters from " + peer);

			params = n;
		    }
		} else {
		    peer = ((relrank & (~mask)) + lroot) % size;

// System.out.println("Sending parameters to " + peer);
		    WriteMessage w = Group.unicast[info.participating_cpus[peer]].newMessage();

		    w.writeByte(INVOCATION_BINCOMBINE);
		    w.writeInt(realStubID);
		    w.writeInt(m.index);
		    w.writeInt(rank);

		    w.writeObject(info.stubids_tickets);
		    params.writeParameters(w);
		    w.send();
		    w.finish();
		    break;
		}
		mask <<= 1;
	    }

	    started = false;
	}

	if (rank == lroot) {
// System.out.println("do_invoke");
	    info.out = params;
	    do_invoke(m, inv, personalizer);
	}

	switch(result_mode) {
	case ReplyScheme.R_DISCARD:
	    return null;
	case ReplyScheme.R_FORWARD:
	    synchronized(this) {
		((ForwardReply) rep).f.startReceiving(this, targetGroupSize, ticket);
	    }
	    return null;
	default:
	    return (GroupMessage) replyStack.collect(ticket);
	}
    }

    /**
     * Deals with a message requesting a flat-invocation combine. Blocks until
     * receiver itself also gets involved in the flat-invocation, and wakes
     * up the combiner when all invokers have delivered their parameters.
     *
     * @param msg the message containing the invocation parameters
     */
    protected final void handleFlatInvocationCombineMessage(ReadMessage msg) throws IOException {
	GroupMethod m = methods[msg.readInt()];
	int rank = msg.readInt();
	int ticket = 0;
	
	if (m.result_mode != ReplyScheme.R_DISCARD) {
	    ticket = msg.readInt();
	}

	ParameterVector p = m.parameters.readParameters(msg);
	msg.finish();

	CombinedInvocationInfo info = m.info;

	synchronized(this) {
	    while (! started || invocation_count == info.numInvokers) {
		waiters++;
		try {
		    wait();
		} catch(Exception e) {
		}
		waiters--;
	    }
	    info.in[rank] = p;

	    info.stubids_tickets[rank] = shiftedStubID | ticket;

	    invocation_count++;

	    if (invocation_count == info.numInvokers) {
		notifyAll();
	    }
	}
    }

    /**
     * Deals with a message requesting a binomial-invocation combine. Blocks until
     * receiver itself also gets involved in the binomial-invocation, and wakes
     * him up.
     *
     * @param msg the message containing the invocation parameters
     */
    protected final void handleBinInvocationCombineMessage(ReadMessage msg) throws IOException {
	GroupMethod m = methods[msg.readInt()];
	int rank = msg.readInt();
	int ticket = 0;
	
// System.out.println("Got bin invocation message from " + rank);
	if (m.result_mode != ReplyScheme.R_DISCARD) {
	    ticket = msg.readInt();
	}

	int[] stbs;
	try {
	    stbs = (int[]) msg.readObject();
	} catch (ClassNotFoundException e) {
	    throw new ClassCastException("Expect an int[] but " + e);
	}
	ParameterVector p = m.parameters.readParameters(msg);
	msg.finish();

// System.out.println("Got parameters ...");

	CombinedInvocationInfo info = m.info;

	synchronized(this) {
	    while (! started || info.in[rank] != null) {
		waiters++;
		try {
		    wait();
		} catch(Exception e) {
		}
		waiters--;
	    }
	    for (int i = 0; i < stbs.length; i++) {
		if (stbs[i] != -1) {
		    info.stubids_tickets[i] = stbs[i];
		}
	    }

	    info.in[rank] = p;

	    info.stubids_tickets[rank] = shiftedStubID | ticket;
	    notifyAll();
	}
    }
}
