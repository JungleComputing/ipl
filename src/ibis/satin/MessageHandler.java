package ibis.satin;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.io.Serializable;

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
			
		synchronized(satin) {
			s = satin.getReplyPort(ident.ibis());
		}

		InvocationRecord result = satin.q.getFromTail();
		if(result == null) {
			try {
				WriteMessage m = s.newMessage();
				if(opcode == STEAL_REQUEST) {
					m.writeByte(STEAL_REPLY_FAILED);
				} else {
					m.writeByte(ASYNC_STEAL_REPLY_FAILED);
				}
				m.send();
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

				return;
			} catch (IOException e) {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': trying to send FAILURE back, but got exception: " + e);
			}
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

		result.stealer = ident.ibis();

		/* store the job in the outstanding list */
		synchronized(satin) {
			satin.addToOutstandingJobList(result);
		}

		try {
			WriteMessage m = s.newMessage();
			if(opcode == STEAL_REQUEST) {
				m.writeByte(STEAL_REPLY_SUCCESS);
			} else {
				m.writeByte(ASYNC_STEAL_REPLY_SUCCESS);
			}

			m.writeObject(result);
			m.send();
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
				tmp = (InvocationRecord) m.readObject();

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

	private void handleTupleAdd(ReadMessage m) {
		try {
			String key = (String) m.readObject();
			Serializable data = (Serializable) m.readObject();
			SatinTupleSpace.remoteAdd(key, data);
		} catch (Exception e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': Got Exception while reading tuple update: " + e);
			System.exit(1);
		}
	}

	private void handleTupleDel(ReadMessage m) {
		try {
			String key = (String) m.readObject();
			SatinTupleSpace.remoteDel(key);
		} catch (Exception e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': Got Exception while reading tuple update: " + e);
			System.exit(1);
		}
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
				if(COMM_DEBUG) {
					ident = m.origin();
					satin.out.println("SATIN '" + satin.ident.name() + 
							  "': got exit ACK message from " + ident.ibis().name());
				}
				satin.exitReplies++;
				break;
			case STEAL_REQUEST:
			case ASYNC_STEAL_REQUEST:
				ident = m.origin();
				//              m.finish();
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
