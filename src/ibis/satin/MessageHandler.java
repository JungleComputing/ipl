package ibis.satin;
import ibis.ipl.*;

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
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e);
			System.exit(1);
		}
		synchronized(satin) {
			satin.addToAbortList(stamp, owner);
		}
	}

	void handleJobResult(ReadMessage m) {
		ReturnRecord rr = null;
		SendPortIdentifier sender = m.origin();
		IbisIdentifier i = null;
		try {
			i = (IbisIdentifier) m.readObject();
			rr = (ReturnRecord) m.readObject();
//			m.finish();
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + satin.ident.name() + 
					   "': got exception while reading job result: " + e);
			System.exit(1);
		}

		synchronized(satin) {
			InvocationRecord r = satin.getStolenInvocationRecord(rr.stamp, sender, i);
			if(r != null) {
				rr.assignTo(r);
				if(r.eek != null) { // we have an exception, add it to the list. the list will be read during the sync
					satin.addToExceptionList(r);
				} else {
					r.spawnCounter.value--;
				}
			} else {
				if(ABORT_DEBUG) {
					satin.out.println("SATIN '" + satin.ident.name() + 
					   "': got result for aborted job, ignoring.");
				}
			}
			satin.notifyAll();
		}
	}

	// Just make this method synchronized, than all the methods we call don't have to be.
	// Furthermore, this makes sure that nobody changes tables while I am busy.
	void handleStealRequest(SendPortIdentifier ident) {
//		long tstart = System.currentTimeMillis();
		if(STEAL_TIMING) {
			satin.handleStealTimer.start();
		}
		synchronized(satin) {
			if(STEAL_STATS) {
				satin.stealRequests++;
			}
			if(STEAL_DEBUG) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						   "': got steal request from " +
						   ident.ibis().name());
			}
			
			SendPort s = satin.getReplyPort(ident.ibis());

			InvocationRecord result = satin.q.getFromTail();
			if(result == null) {
				if(STEAL_DEBUG) {
					satin.out.println("SATIN '" + satin.ident.name() + 
							   "': sending FAILED back to " +
							   ident.ibis().name());
				}

				try {
					WriteMessage m = s.newMessage();
					m.writeByte(STEAL_REPLY_FAILED);
					m.send();
					m.finish();
					if(STEAL_STATS) {
						if(satin.inDifferentCluster(ident.ibis())) {
							satin.interClusterMessages++;
							satin.interClusterBytes += m.getCount();
						} else {
							satin.intraClusterMessages++;
							satin.intraClusterBytes += m.getCount();
						}
					} 

//					long tend = System.currentTimeMillis();
//					System.err.println("failed steal handler took " + (tend - tstart));
					if(STEAL_TIMING) {
						satin.handleStealTimer.stop();
					}

					if(STEAL_DEBUG) {
						satin.out.println("SATIN '" + satin.ident.name() + 
								  "': sending FAILED back to " +
								  ident.ibis().name() + " DONE");
					}

					return;
				} catch (IbisIOException e) {
					System.err.println("SATIN '" + satin.ident.name() + 
							   "': trying to send FAILURE back, but got exception: " + e);
				}
			}

			if(STEAL_STATS) {
				satin.stolenJobs++;
			}

			if(STEAL_DEBUG) {
				satin.out.println("SATIN '" + satin.ident.name() + 
						   "': sending SUCCESS back to " +
						   ident.ibis().name());
			}

			result.stealer = ident.ibis();

			/* store the job int he outstanding list */
			satin.addToOutstandingJobList(result);

			try {
				WriteMessage m = s.newMessage();
				m.writeByte(STEAL_REPLY_SUCCESS);
				m.writeObject(result);
				m.send();
				m.finish();
				if(STEAL_STATS) {
					if(satin.inDifferentCluster(ident.ibis())) {
						satin.interClusterMessages++;
						satin.interClusterBytes += m.getCount();
					} else {
						satin.intraClusterMessages++;
						satin.intraClusterBytes += m.getCount();
					}
				} 

//				long tend = System.currentTimeMillis();
//				System.err.println("succ steal handler took " + (tend - tstart));

				if(STEAL_TIMING) {
					satin.handleStealTimer.stop();
				}
				return;
			} catch (IbisIOException e) {
				System.err.println("SATIN '" + satin.ident.name() + 
						   "': trying to send a job back, but got exception: " + e);
			}
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
				ident = m.origin();
//              m.finish();
				handleStealRequest(ident);
				break;
			case STEAL_REPLY_FAILED:
			case STEAL_REPLY_SUCCESS:
			case BARRIER_REPLY:
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
				}
				if(COMM_DEBUG) {
					ident = m.origin();
					if(opcode == BARRIER_REPLY) {
						satin.out.println("SATIN '" + satin.ident.name() + 
							"': got barrier reply message from " + ident.ibis().name());
					}
				}
				synchronized(satin) {
					if(opcode == BARRIER_REPLY) {
						satin.gotBarrierReply = true;
						satin.notifyAll();
						return;
					} 

					satin.gotStealReply = true;
					
					if(opcode == STEAL_REPLY_SUCCESS) {
						try {
							satin.stolenJob = (InvocationRecord) m.readObject();
//							m.finish();
						} catch (IbisIOException e) {
							ident = m.origin();
							System.err.println("SATIN '" + ident.name() + 
									   "': Got Exception while reading steal reply: " + e);
							System.exit(1);
						}
					} else {
						satin.stolenJob = null;
//						m.finish();
					}
					satin.notifyAll();
				}
				break;
			case JOB_RESULT:
				if (STEAL_DEBUG) {
					ident = m.origin();
					satin.out.println("SATIN '" + satin.ident.name() + 
							   "': got job result message from " + ident.ibis().name());
				}

				handleJobResult(m);
				break;
			case ABORT:
				handleAbort(m);
				break;
			default:
				System.err.println("SATIN '" + satin.ident.name() + 
					   "': Illegal opcode " + opcode + " in MessageHandler");
				System.exit(1);
			}
		} catch (IbisIOException e) {
				// Ignore.
		}
	}
}
