package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class FaultTolerance extends Inlets {

	// The core of the fault tolerance mechanism, the crash recovery procedure
	synchronized void handleCrashes() {
		if (CRASH_TIMING) {
			crashTimer.start();
		}
		if (COMM_DEBUG) {
			out.print("SATIN '" + ident.name() + ": handling crashes");
		}

		gotCrashes = false;
		IbisIdentifier id = null;
		while (crashedIbises.size() > 0) {
			id = (IbisIdentifier) crashedIbises.remove(0);
			if (COMM_DEBUG) {
				out.println("SATIN '" + ident.name() + ": handling crash of "
						+ id.name());
			}

			if (algorithm instanceof ClusterAwareRandomWorkStealing) {
				((ClusterAwareRandomWorkStealing) algorithm)
						.checkAsyncVictimCrash(ident);
			}

			globalResultTable.removeReplica(id);

			if (id.equals(masterIdent)) {
				//master has crashed, let's elect a new one
				try {
					Registry r = ibis.registry();
					masterIdent = (IbisIdentifier) r.reelect("satin master",
							ident, id); //implement it!
					if (masterIdent.equals(ident)) {
						master = true;
					}
					//barrier ports
					if (master) {
						barrierReceivePort = portType
								.createReceivePort("satin barrier receive port on "
										+ ident);
						barrierReceivePort.enableConnections();
					} else {
						barrierSendPort.close();
						barrierSendPort = portType
								.createSendPort("satin barrier send port on "
										+ ident);
						ReceivePortIdentifier barrierIdent = lookup("satin barrier receive port on "
								+ masterIdent);
						connect(barrierSendPort, barrierIdent);
					}

					//statistics
					if (stats && master) {
						totalStats = new StatsMessage();
					}

				} catch (IOException e) {
					System.err.println("SATIN '" + ident.name()
							+ "' :exception while electing a new master "
							+ e.getMessage());
				} catch (ClassNotFoundException e) {
					System.err.println("SATIN '" + ident.name()
							+ "' :exception while electing a new master "
							+ e.getMessage());
				}
				restarted = true;
			}

			/*
			 * if (NUM_CRASHES > 0) { for (int i=1; i <NUM_CRASHES+1 && i
			 * <allIbises.size(); i++) { IbisIdentifier id1 = (IbisIdentifier)
			 * allIbises.get(i); if (id1.equals(ident)) { return; } } }
			 */

			//abort all jobs stolen from id or descendants of jobs stolen from
			// id
			killAndStoreSubtreeOf(id);

			//if using CRS, remove the asynchronously stolen job if it is owned
			// by a
			//crashed machine
			if (algorithm instanceof ClusterAwareRandomWorkStealing) {
				((ClusterAwareRandomWorkStealing) algorithm).killOwnedBy(id);
			}

			//redo all jobs stolen by id (put them back in the task queue)
			redoStolenBy(id);

			//for debugging
			crashedIbis = id;
			del = true;
		}
		if (CRASH_TIMING) {
			crashTimer.stop();
		}

		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name()
					+ ": handling crashes finished");
		}

		notifyAll();
	}

	// Used for fault tolerance
	void sendAbortAndStoreMessage(InvocationRecord r) {
		if (ABORT_DEBUG) {
			out.println("SATIN '" + ident.name()
					+ ": sending abort and store message to: " + r.stealer
					+ " for job " + r.stamp);
		}

		if (deadIbises.contains(r.stealer)) {
			/* don't send abort and store messages to crashed ibises */
			return;
		}

		try {
			SendPort s = getReplyPortNoWait(r.stealer);
			if (s == null)
				return;

			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(Protocol.ABORT_AND_STORE);
			writeMessage.writeInt(r.parentStamp);
			writeMessage.writeObject(r.parentOwner);
			writeMessage.send();
			long cnt = writeMessage.finish();
			if (STEAL_STATS) {
				if (inDifferentCluster(r.stealer)) {
					interClusterMessages++;
					interClusterBytes += cnt;
				} else {
					intraClusterMessages++;
					intraClusterBytes += cnt;
				}
			}
		} catch (IOException e) {
			System.err.println("SATIN '" + ident.name()
					+ "': Got Exception while sending abort message: " + e);
			// This should not be a real problem, it is just inefficient.
			// Let's continue...
			// System.exit(1);
		}
	}

	//used for fault tolerance
	void redoStolenBy(IbisIdentifier targetOwner) {
		outstandingJobs.redoStolenBy(targetOwner);
	}

	//connect upcall functions
	public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
		//	    System.err.println("SATIN '" + ident.name() + "': got gotConnection
		// upcall");
		return true;
	}

	public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe,
			Exception reason) {
		if (COMM_DEBUG) {
			System.err.println("SATIN '" + ident.name()
					+ "': got lostConnection upcall: " + johnDoe.ibis());
		}
		if (connectionUpcallsDisabled)
			return;
		IbisIdentifier ident = johnDoe.ibis();
		if (FAULT_TOLERANCE) {
			synchronized (this) {
				crashedIbises.add(ident);
				deadIbises.add(ident);
				if (ident.equals(currentVictim)) {
					currentVictimCrashed = true;
				}
				gotCrashes = true;
				Victim v = victims.remove(ident);
				notifyAll();
				if (v != null && v.s != null) {
					try {
						v.s.close();
					} catch (IOException e) {
						System.err.println("port.free() throws exception "
								+ e.getMessage());
					}
				}

			}

		}
	}

	public void lostConnection(SendPort me, ReceivePortIdentifier johnDoe,
			Exception reason) {
		if (COMM_DEBUG) {
			System.err.println("SATIN '" + ident.name()
					+ "': got SENDPORT lostConnection upcall: "
					+ johnDoe.ibis());
		}
		if (connectionUpcallsDisabled)
			return;
		IbisIdentifier ident = johnDoe.ibis();
		if (FAULT_TOLERANCE) {
			synchronized (this) {
				crashedIbises.add(ident);
				deadIbises.add(ident);
				if (ident.equals(currentVictim)) {
					currentVictimCrashed = true;
				}

				gotCrashes = true;
				Victim v = victims.remove(ident);
				notifyAll();
				if (v != null && v.s != null) {
					try {
						v.s.close();
					} catch (IOException e) {
						System.err.println("port.free() throws exception "
								+ e.getMessage());
					}
				}

			}
		}
	}

	/**
	 * Used in fault tolerant Satin If the job is being redone (redone flag is
	 * set to true) perform a lookup in the global result table
	 * 
	 * @param r
	 *            invocation record of the job
	 * @return true if an entry was found, false otherwise
	 */
	protected boolean globalResultTableCheck(InvocationRecord r) {
		if (TABLE_CHECK_TIMING) {
			redoTimer.start();
		}

		synchronized (this) {

			if (GRT_TIMING) {
				lookupTimer.start();
			}

			Object key = null;
			if (GLOBALLY_UNIQUE_STAMPS && branchingFactor > 0) {
				key = new Integer(r.stamp);
			} else {
				key = r.getParameterRecord();
			}
			Object value = globalResultTable.lookup(key);
			if (GRT_TIMING) {
				lookupTimer.stop();
			}

			if (value == null) {
				if (TABLE_CHECK_TIMING) {
					redoTimer.stop();
				}
				return false;
			}

			if (GLOBAL_RESULT_TABLE_REPLICATED) {

				ReturnRecord rr = (ReturnRecord) value;
				rr.assignTo(r);
				r.spawnCounter.value--;
				if (TABLE_CHECK_TIMING) {
					redoTimer.stop();
				}
				return true;

			} else {
				//distributed table
				if (value instanceof IbisIdentifier) {
					//remote result

					SendPort s = getReplyPortNoWait((IbisIdentifier) value);
					if (s == null) {
						if (TABLE_CHECK_TIMING) {
							redoTimer.stop();
						}
						return false;
					}
					//put the job in the stolen jobs list
					r.stealer = (IbisIdentifier) value;
					addToOutstandingJobList(r);
					//send a request to the remote node
					try {
						WriteMessage m = s.newMessage();
						m.writeByte(Protocol.RESULT_REQUEST);
						m.writeObject(key);
						m.writeInt(r.stamp); //stamp and owner are not
											 // neccessary when using
						m.writeObject(r.owner);//globally unique stamps, but
											   // let's not make things too
											   // complicated..
						m.send();
						m.finish();
					} catch (IOException e) {
						System.err
								.println("SATIN '"
										+ ident.name()
										+ "': trying to send RESULT_REQUEST but got exception: "
										+ e.getMessage());
						outstandingJobs.remove(r);
						return false;
					}
					if (TABLE_CHECK_TIMING) {
						redoTimer.stop();
					}
					return true;

				} else {
					//local result, handle normally
					ReturnRecord rr = (ReturnRecord) value;
					rr.assignTo(r);
					r.spawnCounter.value--;
					if (TABLE_CHECK_TIMING) {
						redoTimer.stop();
					}
					return true;
				}
			}
		}

	}

	// Used for fault tolerance
	void addToAbortAndStoreList(int stamp, IbisIdentifier owner) {
		if (ASSERTS) {
			assertLocked(this);
		}
		if (ABORT_DEBUG) {
			out.println("SATIN '" + ident.name() + ": got abort message");
		}
		abortAndStoreList.add(stamp, owner);
		gotAbortsAndStores = true;
	}

	// Used for fault tolerance
	synchronized void handleAbortsAndStores() {
		int stamp;
		IbisIdentifier owner;

		while (true) {
			if (abortAndStoreList.count > 0) {
				stamp = abortAndStoreList.stamps[0];
				owner = abortAndStoreList.owners[0];
				abortAndStoreList.removeIndex(0);
			} else {
				gotAbortsAndStores = false;
				return;
			}

			/*
			 * if (NUM_CRASHES > 0) { for (int i=1; i <NUM_CRASHES+1 && i
			 * <allIbises.size(); i++) { IbisIdentifier id = (IbisIdentifier)
			 * allIbises.get(i); if (id.equals(ident)) { return; } } }
			 */

			killAndStoreChildrenOf(stamp, owner);

		}
	}

	public void delete(IbisIdentifier id) {
		//	    System.out.println("SATIN '" + ident.name() + "': got delete " +
		// id.address());

		if (ident.equals(id)) {
			gotDelete = true;
		}

	}

	public void reconfigure() {
	}

	synchronized void handleDelete() {
		onStack.storeAll();
		killedOrphans += onStack.size();
		killedOrphans += q.size();
		printDetailedStats();

		//globalResultTable.exit();
		/*
		 * try { ibis.end(); } catch (IOException e) { System.err.println("SATIN '" +
		 * ident.name() + "': unable to end ibis"); }
		 */
		System.exit(0);
	}
}