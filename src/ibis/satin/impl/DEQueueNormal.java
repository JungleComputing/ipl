package ibis.satin.impl;

/** The `normal' implementation of a double-ended queue. */

// No need to delete aborted invocation records, the spawner keeps an
// outstandingJobs list.
final class DEQueueNormal extends DEQueue implements Config {
	private InvocationRecord head = null;

	private InvocationRecord tail = null;

	private int length = 0;

	private Satin satin;

	DEQueueNormal(Satin satin) {
		this.satin = satin;
	}

	InvocationRecord getFromHead() {
		synchronized (satin) {
			if (length == 0)
				return null;

			InvocationRecord rtn = head;
			head = head.qnext;
			if (head == null) {
				tail = null;
			} else {
				head.qprev = null;
			}
			length--;

			rtn.qprev = rtn.qnext = null;
			return rtn;
		}
	}

	InvocationRecord getFromTail() {
		synchronized (satin) {
			if (length == 0)
				return null;

			InvocationRecord rtn = tail;
			tail = tail.qprev;
			if (tail == null) {
				head = null;
			} else {
				tail.qnext = null;
			}
			length--;

			rtn.qprev = rtn.qnext = null;
			return rtn;
		}
	}

	void addToHead(InvocationRecord o) {
		if (ASSERTS && length > 10000) {
			System.err.println("LARGE Q");
		}
		synchronized (satin) {
			if (length == 0) {
				head = tail = o;
			} else {
				o.qnext = head;
				head.qprev = o;
				head = o;
			}
			length++;
		}
	}

	void addToTail(InvocationRecord o) {
		if (ASSERTS && length > 10000) {
			System.err.println("LARGE Q");
		}
		synchronized (satin) {
			if (length == 0) {
				head = tail = o;
			} else {
				o.qprev = tail;
				tail.qnext = o;
				tail = o;
			}
			length++;
		}
	}

	private void removeElement(InvocationRecord curr) { // curr MUST be in q.
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}
		if (curr.qprev != null) {
			curr.qprev.qnext = curr.qnext;
		} else {
			head = curr.qnext;
			if (head != null) {
				head.qprev = null;
			}
		}

		if (curr.qnext != null) {
			curr.qnext.qprev = curr.qprev;
		} else {
			tail = curr.qprev;
			if (tail != null) {
				tail.qnext = null;
			}
		}
		length--;
	}

	int size() {
		synchronized (satin) {
			return length;
		}
	}


	void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = tail;
		while (curr != null) {
			/*
			 * if(curr.aborted) { curr = curr.qprev; // This is correct, even if
			 * curr was just removed. continue; // already handled. }
			 */
			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf(curr, targetStamp, targetOwner)) {

				if (ABORT_DEBUG) {
					System.err.println("found local child: " + curr.stamp
							+ ", it depends on " + targetStamp);
				}

				if (SPAWN_DEBUG) {
					curr.spawnCounter.decr(curr);
				}
				else	curr.spawnCounter.value--;
				if (ASSERTS && curr.spawnCounter.value < 0) {
					System.err.println("Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
				if (ABORT_STATS) {
					satin.abortedJobs++;
				}
				if (FT_STATS) {
					satin.killedOrphans++;
				}
				curr.aborted = true;

				// Curr is removed, but not put back in cache.
				// this is OK. Moreover, it might have children,
				// so we should keep it alive.
				// cleanup is done inside the spawner itself.
				removeElement(curr);
			}

			curr = curr.qprev; // This is correct, even if curr was just
			// removed.
		}
	}

	/**
	 * Used for fault-tolerance Aborts all the descendents of any job stolen for
	 * the given (crashed) processor
	 * 
	 * @param owner
	 *            IbisIdentifier of the processor whose jobs (and their
	 *            descendents) will be aborted
	 */

	void killSubtreeOf(ibis.ipl.IbisIdentifier owner) {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = tail;
		while (curr != null) {
			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf1(curr, owner)
					|| curr.owner.equals(owner)) //shouldn't happen
			{
				if (SPAWN_DEBUG) {
					curr.spawnCounter.decr(curr);
				}
				else	curr.spawnCounter.value--;
				if (ASSERTS && curr.spawnCounter.value < 0) {
					System.out.println("Just made spawncouter < )");
					new Exception().printStackTrace();
					System.exit(1);
				}
				
				curr.aborted = true;

				if (FT_STATS) {
					satin.killedOrphans++;
				}
				removeElement(curr);
			}

			curr = curr.qprev;
		}
	}
	
	void print(java.io.PrintStream out) {
/*		if (ASSERTS) {
			Satin.assertLocked(satin);
		}*/

		out.println("=Q " + satin.ident.name() + ":=======================");
		InvocationRecord curr = head;
		while (curr != null) {
			out.println("    " + curr);
			curr = curr.qnext;
		}
		out.println("=end of Q=======================");
	}
	
}

