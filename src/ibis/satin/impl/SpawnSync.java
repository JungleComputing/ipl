package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;

public abstract class SpawnSync extends Termination {
	/**
	 * Obtains a new spawn counter.
	 * This does not need to be synchronized, only one thread spawns.
	 * @return a new spawn counter.
	 */
	static public SpawnCounter newSpawnCounter() {
		if(spawnCounterCache == null) {
			return new SpawnCounter();
		}

		SpawnCounter res = spawnCounterCache;
		spawnCounterCache = res.next;
		res.value = 0;

		return res;
	}

	/**
	 * Makes a spawn counter available for recycling.
	 * This does not need to be synchronized, only one thread spawns.
	 * @param s the spawn counter made available.
	 */
	static public void deleteSpawnCounter(SpawnCounter s) {
		if(ASSERTS && s.value < 0) {
			System.err.println("deleteSpawnCounter: spawncouner < 0, val =" + s.value);
			new Exception().printStackTrace();
			System.exit(1);
		}

		s.next = spawnCounterCache;
		spawnCounterCache = s;
	}

	protected void callSatinFunction(InvocationRecord r) {
		InvocationRecord oldParent;
		int oldParentStamp;
		IbisIdentifier oldParentOwner;

		handleDelayedMessages();

		if(ABORTS || FAULT_TOLERANCE) {
			oldParent = parent;
			oldParentStamp = parentStamp;
			oldParentOwner = parentOwner;
		}

		if(ASSERTS) {
			if(r == null) {
				out.println("SATIN '" + ident.name() +
					    ": EEK, r = null in callSatinFunc");
				System.exit(1);
			}
			if(r.aborted) {
				out.println("SATIN '" + ident.name() +
					    ": spawning aborted job!");
				System.exit(1);
			}

			if(r.owner == null) {
				out.println("SATIN '" + ident.name() +
					    ": EEK, r.owner = null in callSatinFunc, r = " + r);
				new Throwable().printStackTrace();
				System.exit(1);
			}

			if(r.owner.equals(ident)) {
				if(r.spawnCounter.value < 0) {
					out.println("SATIN '" + ident.name() + 
						    ": spawncounter < 0 in callSatinFunc");
					System.exit(1);
				}
		
				if(ABORTS && r.parent == null && parentOwner.equals(ident) &&
				   r.parentStamp != -1) { 
					out.println("SATIN '" + ident.name() +
						    ": parent is null for non-root, should not happen here! job = " + r);
					System.exit(1);
				}
			}
		}

		if((ABORTS || FAULT_TOLERANCE) && r.parent != null && r.parent.aborted) {
			if(ABORT_DEBUG) { 
				out.print("SATIN '" + ident.name());
				out.print(": spawning job, parent was aborted! job = " + r);
				out.println(", parent = " + r.parent + "\n");
			}
			r.spawnCounter.value--;
			if(ASSERTS) {
				if(r.spawnCounter.value < 0) {
					out.println("SATIN '" + ident.name() + 
						    ": Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
			}
			return;
		}
	
		if(ABORTS || FAULT_TOLERANCE) {
			onStack.push(r);
			parent = r;
			parentStamp = r.stamp;
			parentOwner = r.owner;
		}

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() +
				    "': callSatinFunc: stamp = " + r.stamp +
				    ", owner = " +
				    (r.owner.equals(ident) ? "me" : r.owner.toString()) +
				    ", parentStamp = " + r.parentStamp +
				    ", parentOwner = " + r.parentOwner);
		}

		if(r.owner.equals(ident)) {
			if (SPAWN_DEBUG) {
				out.println("SATIN '" + ident.name() +
					    "': callSatinFunc: spawn counter = " +
					    r.spawnCounter.value);
			}
			if(ABORTS) {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				try {
					r.runLocal();
				} catch (Throwable t) { 
                                        // This can only happen if an inlet has thrown an exception.
					// The semantics of this: all work is aborted,
					// and the exception is passed on to the spawner.
					// The parent is aborted, it must handle the exception.

				    r.eek = t;
				    handleInlet(r);
				}
			} else { // NO aborts
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				try {
				    r.runLocal();
				} catch (Throwable t) {
				    throw new IbisError("Unexpected exception in runLocal", t);
				}
			}

			r.spawnCounter.value--;
			if(ASSERTS && r.spawnCounter.value < 0) {
			    out.println("SATIN '" + ident.name() + ": Just made spawncounter < 0");
			    new Exception().printStackTrace();
			    System.exit(1);
			}

			if(ASSERTS && !ABORTS && r.eek != null) {
				out.println("Got exception: " + r.eek);
				System.exit(1);
			}

			if(SPAWN_DEBUG) {
				out.print("SATIN '" + ident.name() + ": callSatinFunc: stamp = " + r.stamp + 
					  ", parentStamp = " + r.parentStamp + 
					  ", parentOwner = " + r.parentOwner + " spawn counter = " + r.spawnCounter.value);

				if(r.eek == null) {
					out.println(" DONE");
				} else {
					out.println(" DONE with exception: " + r.eek);
				}
			}
			if (FAULT_TOLERANCE) {
			    //add the finished job to children list
			    if (r.parent != null && !r.aborted) {
	    			r.sibling = r.parent.child;
				r.parent.child = r;
			    }
			}
			
		} else {
			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': RUNNING REMOTE CODE!");
			}
			ReturnRecord rr = null;
			if(ABORTS) {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				try {
					rr = r.runRemote();
					// May be needed if the method did not throw an exception,
					// but its child did, and there is an empty inlet.
					rr.eek = r.eek; 
				} catch (Throwable t) {
					out.println("SATIN '" + ident.name() + ": OOOhh dear, got exception in runremote: " + t);
					t.printStackTrace();
					System.exit(1);
				}
			} else {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				rr = r.runRemote();
			}
			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': RUNNING REMOTE CODE DONE!");
			}

			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': REMOTE CODE SEND RESULT!, exception = " + (r.eek == null ? "null" : ("" + r.eek)));
			}
			// send wrapper back to the owner
			if (!r.aborted) {
			    sendResult(r, rr);
			}

			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': REMOTE CODE SEND RESULT DONE!");
			}
		}
		
		if (ABORTS || FAULT_TOLERANCE) {
			// restore these, there may be more spawns afterwards...
			parentStamp = oldParentStamp;
			parentOwner = oldParentOwner;
			parent = oldParent;
			onStack.pop();
		}
		
		if (FAULT_TOLERANCE) {
		    //remove its children list, will anyone ever look at it?
		    r.child = null;
		}

		if(ABORT_DEBUG && r.aborted) {
			System.err.println("Job on the stack was aborted: " + r.stamp + " EEK = " + (r.eek == null ? "null" : ("" + r.eek)));
		}

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': call satin func done!");
		}
	}

	/**
	 * Spawns the method invocation as described by the specified invocation
	 * record. The invocation record is added to the job queue maintained
	 * by this Satin.
	 * @param r the invocation record specifying the spawned invocation.
	 */
	public void spawn(InvocationRecord r) {
		if(ASSERTS) {
			if(algorithm instanceof MasterWorker) {
				synchronized(this) {
					if(!ident.equals(masterIdent)) {
						System.err.println("with the master/worker algorithm, work can only be spawned on the master!");
						System.exit(1);
					}
				}
			}
		}

		if(SPAWN_STATS) {
			spawns++;
		}

		r.spawnCounter.value++; 
				
		if (GLOBALLY_UNIQUE_STAMPS) {
		    //globally unique stamps start from 1 (root job)
		    if (branchingFactor > 0) {
			
			if (parentStamp > 0) {
			    r.stamp = branchingFactor * parentStamp + parent.numSpawned++;
			} else {
			    //parent is the root
			    r.stamp = branchingFactor + rootNumSpawned++;
			}
		    } else {
			r.stamp = stampCounter++;
		    }
		} else {    
		    r.stamp = stampCounter++;
		}
		
		
		r.owner = ident;

		if(ABORTS || FAULT_TOLERANCE) {
			r.parentStamp = parentStamp;
			r.parentOwner = parentOwner;
			r.parent = parent;

/*
			if(parent != null) {
				for(int i=0; i<parent.parentStamps.size(); i++) {
					r.parentStamps.add(parent.parentStamps.get(i));
					r.parentOwners.add(parent.parentOwners.get(i));
				}
			}

			r.parentStamps.add(new Integer(parentStamp));
			r.parentOwners.add(parentOwner);
*/
		}
		
		if (FAULT_TOLERANCE) {
		    if (parent != null && parent.reDone || parent == null && restarted) {
			r.reDone = true;
		    } 
		}
		
		q.addToHead(r);

		algorithm.jobAdded();

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': Spawn, counter = " + r.spawnCounter.value +
				    ", stamp = " + r.stamp + ", parentStamp = " + r.parentStamp +
				    ", owner = " + r.owner + ", parentOwner = " + r.parentOwner);
		}
	}

	/**
	 * Waits for the jobs as specified by the spawncounter given, but
	 * meanwhile execute jobs from the end of the jobqueue (or rather,
	 * the head of the job queue, where new jobs are added).
	 * @param s the spawncounter.
	 */
	public void sync(SpawnCounter s) {
		InvocationRecord r;
		
//		System.err.println("SATIN " + ident.name() + ": sync() started");
		
		if(SPAWN_STATS) {
			syncs++;
		}

		if(s.value == 0) { // sync is poll
			satinPoll();
			handleDelayedMessages();
			return;
		}
		int numStealAttempts = 0;
		while(s.value > 0) {
//		    if(exiting) {
//			System.err.println("EXIT FROM SYNC");
//			exit();
//		    }

			if(SPAWN_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': Sync, counter = " + s.value);
			}

			satinPoll();
			handleDelayedMessages();

			r = q.getFromHead(); // Try the local queue
			if(r != null) {
				if (FAULT_TOLERANCE) {
				    if (r.reDone) {
					if(globalResultTableCheck(r)) {
				    	    continue;
					}
				    }
				}
				callSatinFunction(r);
			} else {
				algorithm.clientIteration();
			}
		}
		
//		System.err.println("SATIN " + ident.name() + ": sync() done");
	}
}
