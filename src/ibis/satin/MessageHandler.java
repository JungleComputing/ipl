package ibis.satin;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.io.Serializable;

import java.util.HashMap;

final class MessageHandler implements Upcall, Protocol, Config {
	Satin satin;

	MessageHandler(Satin s) {
		satin = s;
	}

	void handleAbort(ReadMessage m) {
		int stamp = -1;
		IbisIdentifier owner = null;
		try {
			stamp = m.readInt();
			owner = (IbisIdentifier) m.readObject();
			//			m.finish();
		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e);
			System.exit(1);
		} catch (ClassNotFoundException e1) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e1);
			System.exit(1);
		}
		synchronized(satin) {
			satin.addToAbortList(stamp, owner);
		}
	}

	void handleJobResult(ReadMessage m, int opcode) {
		ReturnRecord rr = null;
		SendPortIdentifier sender = m.origin();
		IbisIdentifier i = null;
		int stamp = -666;
		Throwable eek = null;
		try {
			i = (IbisIdentifier) m.readObject();
			if(opcode == JOB_RESULT_NORMAL) {
				rr = (ReturnRecord) m.readObject();
				stamp = rr.stamp;
				eek = rr.eek;
			} else {
				eek = (Throwable) m.readObject();
				stamp = m.readInt();
			}
			//			m.finish();
		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e);
			System.exit(1);
		} catch (ClassNotFoundException e1) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e1);
			System.exit(1);
		}

		if(STEAL_DEBUG) {
			if(eek != null) {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': handleJobResult: exception result: " + eek);
			} else {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': handleJobResult: normal result");
			}
		}
		
		satin.addJobResult(rr, sender, i, eek, stamp);
	}

	/* Just make this method synchronized, than all the methods we call
	   don't have to be. */
	// Furthermore, this makes sure that nobody changes tables while I am busy.
	// But, it is scary, then we are sending a reply while holding a lock... --Rob
	void handleStealRequest(SendPortIdentifier ident, int opcode) {
		SendPort s = null;

		if(STEAL_TIMING) {
			satin.handleStealTimer.start();
		}

		if(STEAL_STATS) {
			satin.stealRequests++;
		}
		if(STEAL_DEBUG && opcode == STEAL_REQUEST) {
			satin.out.println("SATIN '" + satin.ident.name() + 
					  "': got steal request from " +
					  ident.ibis().name());
		}
		if(STEAL_DEBUG && opcode == ASYNC_STEAL_REQUEST) {
			satin.out.println("SATIN '" + satin.ident.name() + 
					  "': got ASYNC steal request from " +
					  ident.ibis().name());
		}
			
		InvocationRecord result = null;

		synchronized(satin) {
			s = satin.getReplyPort(ident.ibis());

			while(true) {
				result = satin.q.getFromTail();
				if (result != null) {
					result.stealer = ident.ibis();
					
				/* store the job in the outstanding list */
					satin.addToOutstandingJobList(result);
				}

				if(opcode != BLOCKING_STEAL_REQUEST || satin.exiting || result != null) {
					break;
				} else {
					try {
						satin.wait();
					} catch (Exception e) {
						// Ignore.
					}
				}
			}
		}

		if(result == null) {
			try {
				WriteMessage m = s.newMessage();
				if(opcode == STEAL_REQUEST) {
					m.writeByte(STEAL_REPLY_FAILED);
				} else {
					m.writeByte(ASYNC_STEAL_REPLY_FAILED);
				}
				long cnt = m.finish();
				if(STEAL_STATS) {
					if(satin.inDifferentCluster(ident.ibis())) {
						satin.interClusterMessages++;
						satin.interClusterBytes += cnt;
					} else {
						satin.intraClusterMessages++;
						satin.intraClusterBytes += cnt;
					}
				} 

				if(STEAL_TIMING) {
					satin.handleStealTimer.stop();
				}

				if(STEAL_DEBUG) {
					satin.out.println("SATIN '" + satin.ident.name() + 
							  "': sending FAILED back to " +
							  ident.ibis().name() + " DONE");
				}

			} catch (IOException e) {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': trying to send FAILURE back, but got exception: " + e);
			}
			return;
		}

		/* else */

		if(ASSERTS && result.aborted) {
			System.out.println("SATIN '" + satin.ident.name() + 
					   ": trying to send aborted job!");
		}
		
		if(STEAL_STATS) {
			satin.stolenJobs++;
		}

		if(STEAL_DEBUG) {
			satin.out.println("SATIN '" + satin.ident.name() + 
					  "': sending SUCCESS and #" + result.stamp +
					  " back to " + ident.ibis().name());
		}

		try {
			if(STEAL_TIMING) {
			    satin.invocationRecordWriteTimer.start();
			}
			WriteMessage m = s.newMessage();
			if(opcode == STEAL_REQUEST) {
				m.writeByte(STEAL_REPLY_SUCCESS);
			} else {
				m.writeByte(ASYNC_STEAL_REPLY_SUCCESS);
			}

			if(satin.sequencer != null) { // ordered communication
   			        m.writeInt(satin.expected_seqno);
			}

			m.writeObject(result);
			long cnt = m.finish();
			if(STEAL_TIMING) {
			    satin.invocationRecordWriteTimer.stop();
			}
			if(STEAL_STATS) {
				if(satin.inDifferentCluster(ident.ibis())) {
					satin.interClusterMessages++;
					satin.interClusterBytes += cnt;
				} else {
					satin.intraClusterMessages++;
					satin.intraClusterBytes += cnt;
				}
			} 

			if(STEAL_TIMING) {
				satin.handleStealTimer.stop();
			}
			return;
		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': trying to send a job back, but got exception: " + e);
		}
	}
	
	void handleReply(ReadMessage m, int opcode) {
		SendPortIdentifier ident;
		InvocationRecord tmp = null;

		if(STEAL_DEBUG) {
			ident = m.origin();
			if(opcode == STEAL_REPLY_SUCCESS) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						  "': got steal reply message from " +
						  ident.ibis().name() + ": SUCCESS");
			} else if(opcode == STEAL_REPLY_FAILED) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						  "': got steal reply message from " +
						  ident.ibis().name() + ": FAILED");
			} 
			if(opcode == ASYNC_STEAL_REPLY_SUCCESS) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						  "': got ASYNC steal reply message from " +
						  ident.ibis().name() + ": SUCCESS");
			} else if(opcode == ASYNC_STEAL_REPLY_FAILED) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						  "': got ASYNC steal reply message from " +
						  ident.ibis().name() + ": FAILED");
			} 
		}

		if(COMM_DEBUG) {
			ident = m.origin();
			if(opcode == BARRIER_REPLY) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						  "': got barrier reply message from " +
						  ident.ibis().name());
			}
		}

		switch(opcode) {
		case BARRIER_REPLY:
			synchronized(satin) {
				satin.gotBarrierReply = true;
				satin.notifyAll();
			} 
			break;

		case STEAL_REPLY_SUCCESS:
		case ASYNC_STEAL_REPLY_SUCCESS:

			try {
				if(STEAL_TIMING) {
				    satin.invocationRecordReadTimer.start();
				}
				if(satin.sequencer != null) { // ordered communication
   			                satin.stealReplySeqNr = m.readInt();
				}
				tmp = (InvocationRecord) m.readObject();
				if(STEAL_TIMING) {
				    satin.invocationRecordReadTimer.stop();
				}

				if(ASSERTS && tmp.aborted) {
					System.out.println("SATIN '" + satin.ident.name() + 
							   ": stole aborted job!");
				}
			} catch (IOException e) {
				ident = m.origin();
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': Got Exception while reading steal " +
						   "reply from " + ident.name() + ", opcode:" +
						   + opcode + ", exception: " + e);
				System.exit(1);
			} catch (ClassNotFoundException e1) {
				ident = m.origin();
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': Got Exception while reading steal " +
						   "reply from " + ident.name() + ", opcode:" +
						   + opcode + ", exception: " + e1);
				System.exit(1);
			}
			satin.algorithm.stealReplyHandler(tmp, opcode);
			break;

		case STEAL_REPLY_FAILED:
		case ASYNC_STEAL_REPLY_FAILED:
			satin.algorithm.stealReplyHandler(null, opcode);
			break;

		default:
			System.err.println("INTERNAL ERROR, opcode = " + opcode);
			System.exit(1);
			break;
		}

	}

	private static class tuple_command {
		byte command;
		String key;
		Serializable data;

		tuple_command(byte c, String k, Serializable s) {
			command = c;
			key = k;
			data = s;
		}
	}

	private HashMap saved_tuple_commands = null;

	private void add_to_queue(int seqno,
				  String key,
				  Serializable data, 
				  byte command) {
		if (saved_tuple_commands == null) {
			saved_tuple_commands = new HashMap();
		}
		saved_tuple_commands.put(new Integer(seqno), new tuple_command(command, key, data));
	}

	private void scan_queue() {

		if (saved_tuple_commands == null) {
			return;
		}

		Integer i = new Integer(satin.expected_seqno);
		tuple_command t = (tuple_command) saved_tuple_commands.remove(i);
		while (t != null) {
			switch(t.command) {
			case TUPLE_ADD:
				if(t.data instanceof ActiveTuple) {
					synchronized(satin) {
						satin.addToActiveTupleList(t.key, t.data);
						satin.gotActiveTuples = true;
					}
				} else {
					SatinTupleSpace.remoteAdd(t.key, t.data);
				}
				break;
			case TUPLE_DEL:
				SatinTupleSpace.remoteDel(t.key);
				break;
			}
			satin.expected_seqno++;

			i = new Integer(satin.expected_seqno);
			t = (tuple_command) saved_tuple_commands.remove(i);
		}
	}

	private void handleTupleAdd(ReadMessage m) {
		int seqno = 0;
		boolean done = false;
		try {
			if (SatinTupleSpace.use_seq) {
			    seqno = m.readInt();
			}
			String key = (String) m.readObject();
			Serializable data = (Serializable) m.readObject();

			if (SatinTupleSpace.use_seq && seqno > satin.expected_seqno) {
				add_to_queue(seqno, key, data, TUPLE_ADD);
			}
			else {
				if(data instanceof ActiveTuple) {
					synchronized(satin) {
						satin.addToActiveTupleList(key, data);
						satin.gotActiveTuples = true;
					}
				} else {
					SatinTupleSpace.remoteAdd(key, data);
				}
				if (SatinTupleSpace.use_seq) {
					satin.expected_seqno++;
					scan_queue();
				}
				done = true;

			}
			m.finish();

		} catch (Exception e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': Got Exception while reading tuple update: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		if (SatinTupleSpace.use_seq) {
			if (done) {
				synchronized(satin.tuplePort) {
					satin.tuplePort.notifyAll();
				}
			}
		}
	}

	private void handleTupleDel(ReadMessage m) {
		int seqno = 0;
		boolean done = false;
		try {
			if (SatinTupleSpace.use_seq) {
			    seqno = m.readInt();
			}
			String key = (String) m.readObject();
			if (SatinTupleSpace.use_seq && seqno > satin.expected_seqno) {
				add_to_queue(seqno, key, null, TUPLE_DEL);
			}
			else {
				SatinTupleSpace.remoteDel(key);
				satin.expected_seqno++;
				if (SatinTupleSpace.use_seq) {
					scan_queue();
				}
				done = true;
			}
			m.finish();
		} catch (Exception e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': Got Exception while reading tuple remove: " + e);
			System.exit(1);
		}
		if (SatinTupleSpace.use_seq) {
			if (done) {
				synchronized(satin.tuplePort) {
					satin.tuplePort.notifyAll();
				}
			}
		}
	}

	private void handleExitReply(ReadMessage m) {

		if(COMM_DEBUG) {
			SendPortIdentifier ident = m.origin();
			satin.out.println("SATIN '" + satin.ident.name() + 
					  "': got exit ACK message from " + ident.ibis().name());
		}

		if(satin.stats) {
			try {
				SatinStats s = (SatinStats) m.readObject();
				satin.totalStats.add(s);
			} catch (Exception e) {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': Got Exception while reading stats: " + e);
				System.exit(1);
			}
		}

		satin.exitReplies++;
	}

	public void upcall(ReadMessage m) {
		SendPortIdentifier ident;

		try {
			byte opcode = m.readByte();
			
			switch(opcode) {
			case EXIT:
				if(COMM_DEBUG) {
					ident = m.origin();
					satin.out.println("SATIN '" + satin.ident.name() + 
							  "': got exit message from " + ident.ibis().name());
				}
				satin.exiting = true;
				synchronized(satin) {
					satin.notifyAll();
				}

				//				m.finish();
				break;
			case EXIT_REPLY:
				handleExitReply(m);
				break;
			case STEAL_REQUEST:
			case ASYNC_STEAL_REQUEST:
				ident = m.origin();
				//              m.finish();
				handleStealRequest(ident, opcode);
				break;
			case BLOCKING_STEAL_REQUEST:
                                // If we are doing a blocking steal, we must do a finish first --Rob
				ident = m.origin();
				m.finish();
				handleStealRequest(ident, opcode);
				break;
			case STEAL_REPLY_FAILED:
			case STEAL_REPLY_SUCCESS:
			case ASYNC_STEAL_REPLY_FAILED:
			case ASYNC_STEAL_REPLY_SUCCESS:
			case BARRIER_REPLY:
				handleReply(m, opcode);
				break;
			case JOB_RESULT_NORMAL:
			case JOB_RESULT_EXCEPTION:
				if (STEAL_DEBUG) {
					ident = m.origin();
					satin.out.println("SATIN '" + satin.ident.name() + 
							  "': got job result message from " + ident.ibis().name());
				}
				
				handleJobResult(m, opcode);
				break;
			case ABORT:
				handleAbort(m);
				break;
			case TUPLE_ADD:
				handleTupleAdd(m);
				break;
			case TUPLE_DEL:
				handleTupleDel(m);
				break;
			default:
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': Illegal opcode " + opcode + " in MessageHandler");
				System.exit(1);
			}
		} catch (IOException e) {
			System.err.println("satin msgHandler upcall: " + e);
			e.printStackTrace();
				// Ignore.
		}
	}
}
