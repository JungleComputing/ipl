package ibis.gmi;

import ibis.ipl.IbisException;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

/**
 * The {@link GroupSkeleton} class serves as a base class for generated
 * skeletons. It also has methods for reply combining.
 */

public abstract class GroupSkeleton implements GroupProtocol {
    /**
     * Indicates the group member for which this is the skeleton.
     */
    protected GroupMember destination;

    /**
     * A multicast send port to all members in the group.
     */
    protected SendPort reply_to_all;

    /**
     * Rank of this member in the group.
     */
    protected int myGroupRank;

    /**
     * The number of group members.
     */
    protected int groupSize;

    /**
     * Group message cache and collector of combine-reply messages.
     */
    private GroupMessageQueue messageQ;

    /**
     * Caching and queueing of group messages. 
     */
    static private final class GroupMessageQueue { 

	/**
	 * Queues of received group messages for each node.
	 */
	private GroupMessage [] qs; 

	/**
	 * Group message cache.
	 */
	private GroupMessage cache;

	/**
	 * Constructor.
	 *
	 * @param groupSize the number of group members
	 */
	protected GroupMessageQueue(int groupSize) { 
	    qs = new GroupMessage[groupSize];		
	}

	/**
	 * Gets a group message from the cache, or allocates a new one.
	 *
	 * @return A group message.
	 */
	private GroupMessage getGroupMessage() { 

	    GroupMessage temp = cache;

	    if (temp == null) { 
		temp = new GroupMessage();
	    } else { 
		cache = temp.next;
	    }

	    return temp;
	} 

	/**
	 * Allocates and enqueues a group message in the queue for node "from".
	 *
	 * @param from the node from which a message was received
	 *
	 * @return the group message that was enqueued, and that can now be
	 * initialized further.
	 */
	public GroupMessage enqueue(int from) { 
	    if (Group.DEBUG) System.out.println("Got message from cpu " + from);
	    GroupMessage temp = getGroupMessage();
	    temp.next = qs[from];
	    qs[from] = temp;
	    return temp;
	} 

	/**
	 * Dequeues and returns a group message that resulted from a message from node
	 * "from".
	 *
	 * @param from the node from which the message was received
	 *
	 * @return the message, or null if not present.
	 */
	public GroupMessage dequeue(int from) { 

	    if (Group.DEBUG) System.out.println("Waiting for message from cpu " + from);

	    if (qs[from] == null) { 
		return null;
	    } else { 
		GroupMessage temp = qs[from];
		qs[from] = temp.next;		
		return temp;
	    }
	}

	/**
	 * Returns a group message to the message cache.
	 *
	 * @param m the group message to be placed in the cache.
	 */
	public void free(GroupMessage m) { 
	    m.objectResult = m.exceptionResult = null;
	    m.next = cache;
	    cache = m;
	} 
    } 

    /**
     * Constructor.
     */
    public GroupSkeleton() { 
    } 

    /**
     * Initializes the skeleton further once the group is complete.
     *
     * @param dest the group member to which this skeleton belongs
     */
    public synchronized void init(GroupMember dest) { 
	destination = dest;
	messageQ = new GroupMessageQueue(dest.groupSize);

	myGroupRank = dest.myGroupRank;
	groupSize = dest.groupSize;
    }

    /**
     * Receives a {@link GroupProtocol#COMBINE COMBINE} or a 
     * {@link GroupProtocol#COMBINE_RESULT COMBINE_RESULT} message.
     * It is placed in a group message and enqueued in the proper queue,
     * and any waiters are notified.
     *
     * @param m the message received
     * @exception IOException is thrown on IO error.
     * @exception ClassNotFoundException is thrown when an object is read
     * whose class could not be found.
     */
    public synchronized final void handleCombineMessage(ReadMessage m) throws IOException, ClassNotFoundException { 
	int rank = m.readInt();
	byte result_type = m.readByte();

	GroupMessage message = messageQ.enqueue(rank);

	switch (result_type) { 
	case RESULT_VOID:
	    break;			
	case RESULT_BOOLEAN:
	    message.booleanResult = m.readBoolean();
	    break;
	case RESULT_BYTE:
	    message.byteResult = m.readByte();
	    break;
	case RESULT_SHORT:
	    message.shortResult = m.readShort();
	    break;
	case RESULT_CHAR:
	    message.charResult = m.readChar();
	    break;
	case RESULT_INT:
	    message.intResult = m.readInt();
	    break;
	case RESULT_LONG:
	    message.longResult = m.readLong();
	    break;
	case RESULT_FLOAT:
	    message.floatResult = m.readFloat();
	    break;
	case RESULT_DOUBLE:
	    message.doubleResult = m.readDouble();
	    break;
	case RESULT_OBJECT:
	    message.objectResult = m.readObject();
	    break;
	case RESULT_EXCEPTION:
	    message.exceptionResult = (Exception) m.readObject();
	    break;
	}
	m.finish();
	notifyAll();
    }

    /**
     * Waits for a message from group member "peer" and returns it.
     * Note: caller must have locked!
     *
     * @param peer the group member from which a message is expected
     * @return the group message.
     */
    private final GroupMessage getMessage(int peer) { 
	GroupMessage temp = messageQ.dequeue(peer);					
	while (temp == null) { 
	    try { 
		wait();
	    } catch (Exception e) { 
	    } 
	    temp = messageQ.dequeue(peer);
	} 
	return temp;
    } 

    /**
     * Releases a group message. Note: caller must have locked.
     *
     * @param temp the group message to be released
     */
    private final void freeMessage(GroupMessage temp) { 
	messageQ.free(temp);
    } 

    /**
     * Combines the result of a group method invocation. The combine method
     * is binomial. Note that it also combines exceptions, and throws an exception
     * when it should be propagated.
     * This version is for group methods with a float result.
     *
     * @param combiner the binomial combiner object
     * @param to_all indicates whether the result of the combine should be sent
     * to all group members
     * @param lroot root of the binomial combine tree
     * @param local_result my own result
     * @param ex my own exception
     *
     * @return the result of the combine.
     * @exception when combiner throws an exception, or on IO error.
     */
    protected final synchronized float combine_float(BinomialCombiner combiner, boolean to_all, int lroot, float local_result, Exception ex) throws Exception {
	// TODO: Have a special exception class that can contain nested exceptions?
	// Or, rethrow exception?

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.floatResult, message.exceptionResult, groupSize);
			/* Any exception now ignored by combiner, otherwise it should
			 * have thrown an exception.
			 */
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_FLOAT);
		    w.writeFloat(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_FLOAT);
		    w.writeFloat(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.floatResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a double result.
     */
    protected final synchronized double combine_double(BinomialCombiner combiner, boolean to_all, int lroot, double local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.doubleResult, message.exceptionResult, groupSize);
			/* Any exception now ignored by combiner ... */
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_FLOAT);
		    w.writeDouble(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_FLOAT);
		    w.writeDouble(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.doubleResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }


    /**
     * See {@link #combine_float}, but for a group method with a long result.
     */
    protected final synchronized long combine_long(BinomialCombiner combiner, boolean to_all, int lroot, long local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.longResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_LONG);
		    w.writeLong(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_LONG);
		    w.writeLong(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.longResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with an int result.
     */
    protected final synchronized int combine_int(BinomialCombiner combiner, boolean to_all, int lroot, int local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);
		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.intResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_INT);
		    w.writeInt(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_INT);
		    w.writeInt(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.intResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a short result.
     */
    protected final synchronized short combine_short(BinomialCombiner combiner, boolean to_all, int lroot, short local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.shortResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_SHORT);
		    w.writeShort(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_SHORT);
		    w.writeShort(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.shortResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a char result.
     */
    protected final synchronized char combine_char(BinomialCombiner combiner, boolean to_all, int lroot, char local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.charResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_CHAR);
		    w.writeChar(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_CHAR);
		    w.writeChar(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.charResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a byte result.
     */
    protected final synchronized byte combine_byte(BinomialCombiner combiner, boolean to_all, int lroot, byte local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    /* call the combiner */
		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.byteResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_BYTE);
		    w.writeByte(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_BYTE);
		    w.writeByte(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.byteResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a boolean result.
     */
    protected final synchronized boolean combine_boolean(BinomialCombiner combiner, boolean to_all, int lroot, boolean local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    /* call the combiner */
		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.booleanResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_BOOLEAN);
		    w.writeBoolean(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_BOOLEAN);
		    w.writeBoolean(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.booleanResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with an Object result.
     */
    protected final synchronized Object combine_Object(BinomialCombiner combiner, boolean to_all, int lroot, Object local_result, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    /* call the combiner */
		    try {
			local_result = combiner.combine(myGroupRank, local_result, ex, peer, message.objectResult, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_OBJECT);
		    w.writeObject(local_result);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_OBJECT);
		    w.writeObject(local_result);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    } else { 
		local_result = message.objectResult;
	    }				
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}

	return local_result;
    }

    /**
     * See {@link #combine_float}, but for a group method with a void result.
     * These need to be combined as well, both for synchronization purposes, and for the
     * exceptions.
     */
    protected final synchronized void combine_void(BinomialCombiner combiner, boolean to_all, int lroot, Exception ex) throws Exception {

	int peer;
	int mask = 1;
	int relrank = (myGroupRank - lroot + groupSize) % groupSize;		
	GroupMessage message;

	while (mask < groupSize) {
	    if ((mask & relrank) == 0) {
		peer = relrank | mask;
		if (peer < groupSize) {
		    peer = (peer + lroot) % groupSize;
		    /* receive result */
		    message = getMessage(peer);

		    /* call the combiner */
		    try {
			combiner.combine(myGroupRank, ex, peer, message.exceptionResult, groupSize);
			ex = null;
		    } catch (Exception e) {
			ex = e;
		    }
		    freeMessage(message);
		}
	    } else {
		peer = ((relrank & (~mask)) + lroot) % groupSize;
		/* send result */
		int peer_rank =  destination.memberRanks[peer];
		int peer_skeleton = destination.memberSkels[peer];
		if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
		WriteMessage w = Group.unicast[peer_rank].newMessage();
		w.writeByte(COMBINE);
		w.writeInt(peer_skeleton);
		w.writeInt(myGroupRank);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_VOID);
		}
		w.send();
		w.finish();
		break;
	    }
	    mask <<= 1;
	}

	if (to_all) {
	    if (myGroupRank == lroot) {
		if (reply_to_all == null) {
		    reply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
		}
		/* forward result to all */
		WriteMessage w = reply_to_all.newMessage();
		w.writeByte(COMBINE_RESULT);
		w.writeInt(destination.groupID);
		w.writeInt(lroot);

		if (ex != null) { 
		    w.writeByte(RESULT_EXCEPTION);
		    w.writeObject(ex);
		} else { 
		    w.writeByte(RESULT_VOID);
		}
		w.send();
		w.finish();
	    }
	    /* receive result from root */
	    message = getMessage(lroot);					

	    if (message.exceptionResult != null) { 
		ex = message.exceptionResult;
	    }
	    freeMessage(message);
	}

	if (ex != null) {
	    throw new IbisException(ex);
	}
    }

    /**
     * To be redefined by the skeletons.
     * Deals with an {@link GroupProtocol#INVOCATION INVOCATION} message.
     * @param invocationMode summary of the invocation scheme of this invocation
     * @param resultMode     summary of the result scheme of this invocation
     * @param r              the message
     * @exception IbisException is thrown when the result is an exception; the
     * nested exception is this result exception.
     * @exception IOException is thrown on IO error.
     */
    public abstract void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException, IOException;	
	// TODO: Exception behavior
}
