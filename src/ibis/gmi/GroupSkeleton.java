package ibis.group;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.ipl.IbisException;
import ibis.ipl.SendPort;

// This is a base class for generated group stubs

public abstract class GroupSkeleton { 
       	
	protected GroupMember destination;
	protected GroupMethod [] methods;
	protected GroupMessageQueue messageQ;
	protected SendPort reply_to_all;

	protected int rank, size;

	// combine opcodes
	public static final byte
	        COMBINE_VOID   = 0,
	        COMBINE_BOOL   = 1,
		COMBINE_BYTE   = 2,
		COMBINE_SHORT  = 3,		
		COMBINE_CHAR   = 4,
		COMBINE_INT    = 5,
		COMBINE_LONG   = 6,
		COMBINE_FLOAT  = 7,
		COMBINE_DOUBLE = 8,
		COMBINE_OBJECT = 9;
	      
	public GroupSkeleton(int numMethods) { 
		methods = new GroupMethod[numMethods];	      
	} 

	public void init(GroupMember dest) { 
		destination = dest;
		messageQ = new GroupMessageQueue(dest.size);

		rank = dest.rank;
		size = dest.size;
	}

	public synchronized final void handleCombineMessage(ReadMessage m) throws IbisException { 
		int rank = m.readInt();
		byte result_type = m.readByte();
		
		GroupMessage message = messageQ.enqueue(rank);

		switch (result_type) { 
		case Group.RESULT_VOID:
			break;			
	        case Group.RESULT_BOOLEAN:
			message.booleanResult = m.readBoolean();
			break;
		case Group.RESULT_BYTE:
			message.byteResult = m.readByte();
			break;
		case Group.RESULT_SHORT:
			message.shortResult = m.readShort();
			break;
		case Group.RESULT_CHAR:
			message.charResult = m.readChar();
			break;
		case Group.RESULT_INT:
			message.intResult = m.readInt();
			break;
		case Group.RESULT_LONG:
			message.longResult = m.readLong();
			break;
		case Group.RESULT_FLOAT:
			message.floatResult = m.readFloat();
			break;
		case Group.RESULT_DOUBLE:
			message.doubleResult = m.readDouble();
			break;
		case Group.RESULT_OBJECT:
			message.objectResult = m.readObject();
			break;
 	        case Group.RESULT_EXCEPTION:
			message.exceptionResult = (Exception) m.readObject();
			break;
		}
		notifyAll();
	}

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

	private final void freeMessage(GroupMessage temp) { 
		messageQ.free(temp);
	} 

	public final synchronized float combine_float(BinaryCombiner combiner, boolean to_all, int lroot, float local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.floatResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_FLOAT);
					w.writeFloat(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_FLOAT);
					w.writeFloat(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.floatResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized double combine_double(BinaryCombiner combiner, boolean to_all, int lroot, double local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.doubleResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_DOUBLE);
					w.writeDouble(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_DOUBLE);
					w.writeDouble(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.doubleResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized long combine_long(BinaryCombiner combiner, boolean to_all, int lroot, long local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.longResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_LONG);
					w.writeLong(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_LONG);
					w.writeLong(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.longResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized int combine_int(BinaryCombiner combiner, boolean to_all, int lroot, int local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.intResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_INT);
					w.writeInt(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_INT);
					w.writeInt(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.intResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized short combine_short(BinaryCombiner combiner, boolean to_all, int lroot, short local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.shortResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_SHORT);
					w.writeShort(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_SHORT);
					w.writeShort(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.shortResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized char combine_char(BinaryCombiner combiner, boolean to_all, int lroot, char local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.charResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_CHAR);
					w.writeChar(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_CHAR);
					w.writeChar(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.charResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized byte combine_byte(BinaryCombiner combiner, boolean to_all, int lroot, byte local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.byteResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_BYTE);
					w.writeByte(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_BYTE);
					w.writeByte(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.byteResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized boolean combine_boolean(BinaryCombiner combiner, boolean to_all, int lroot, boolean local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.booleanResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_BOOLEAN);
					w.writeBoolean(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_BOOLEAN);
					w.writeBoolean(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.booleanResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized Object combine_Object(BinaryCombiner combiner, boolean to_all, int lroot, Object local_result, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							local_result = combiner.combine(rank, local_result, peer, message.objectResult, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_OBJECT);
					w.writeObject(local_result);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_OBJECT);
					w.writeObject(local_result);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			} else { 
				local_result = message.objectResult;
			}				
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}

		return local_result;
	}

	public final synchronized void combine_void(BinaryCombiner combiner, boolean to_all, int lroot, Exception ex) throws Exception {

		int peer;
		int mask = 1;
		int size = this.size;
		int rank = this.rank;
		int relrank = (rank - lroot + size) % size;		
		boolean exception = (ex != null);
		GroupMessage message;

		while (mask < size) {
			if ((mask & relrank) == 0) {
				peer = relrank | mask;
				if (peer < size) {
					peer = (peer + lroot) % size;
					/* receive result */
					message = getMessage(peer);
					exception = exception || (message.exceptionResult != null);
				
					if (!exception) {
						/* call the combiner */
						try {
							combiner.combine(rank, peer, size);
						} catch (Exception e) {
							ex = e;
							exception = true;
						}
					}
					freeMessage(message);
				}
			} else {
				peer = ((relrank & (~mask)) + lroot) % size;
				/* send result */
				long memberID = destination.memberIDs[peer];
				int peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);
				int peer_skeleton = (int) (memberID & 0xFFFFFFFFL);
				if (Group.DEBUG) System.out.println("Sending message to peer " + peer + " on cpu " + peer_rank);
				WriteMessage w = Group.combine_unicast[peer_rank].newMessage();
				w.writeByte(GroupProtocol.COMBINE);
				w.writeInt(peer_skeleton);
				w.writeInt(rank);
				
				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_VOID);
				}
				w.send();
				w.finish();
				break;
			}
			mask <<= 1;
		}

		if (to_all) {
			if (rank == lroot) {
				if (reply_to_all == null) {
					reply_to_all = null; //Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);
				}
				/* forward result to all */
				WriteMessage w = reply_to_all.newMessage();
				w.writeByte(GroupProtocol.COMBINE_RESULT);
				w.writeInt(destination.groupID);
				w.writeInt(lroot);

				if (exception) { 
					w.writeByte(Group.RESULT_EXCEPTION);
					w.writeObject(ex);
				} else { 
					w.writeByte(Group.RESULT_VOID);
				}
				w.send();
				w.finish();
			}
			/* receive result from root */
			message = getMessage(lroot);					
			
			if (message.exceptionResult != null) { 
				exception = true;
				ex = message.exceptionResult;
			}
			freeMessage(message);
		}

		if (exception) {
			/* throw exception here */
		}
	}

	public abstract void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException;	
}






