// @@@ aarg, delete aborted invocation records!!!!
package ibis.satin;

import ibis.ipl.*;

public final class DEQueue implements Config {
	public InvocationRecord head = null;
	public InvocationRecord tail = null;
	public int length = 0;
	public Satin satin;

	public DEQueue(Satin satin) {
		this.satin = satin;
	}

	public InvocationRecord getFromHead() {
		synchronized(satin) {
			if(length == 0) return null;

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

	public InvocationRecord getFromTail() {
		synchronized(satin) {
			if(length == 0) return null;

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

	public void addToHead(InvocationRecord o) {
		synchronized(satin) {
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

	public void addtoTail(InvocationRecord o) {
		synchronized(satin) {
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

	public boolean remove(InvocationRecord o) {
		synchronized(satin) {
			InvocationRecord curr = head;
			while(curr != null) {
				if(curr.equals(o)) {
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
					return true;
				}
				curr = curr.qnext;
			}

			return false;
		}
	}

	public void removeElement(InvocationRecord curr) { // curr MUST be in q, Must hold satin lock
		if(ASSERTS) {
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

	public int size() {
		synchronized(satin) {
			return length;
		}
	}

	// hold satin lock
	void print(java.io.PrintStream out) {
		out.println("work queue: " + length + " elements");
		InvocationRecord curr = head;
		while(curr != null) {
			System.out.println("    " + curr);
			curr = curr.qnext;
		}
	}

	// hold the satin lock
	void oldkillChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = head;
		int oldStamp = -666;
		IbisIdentifier oldOwner = null;
		boolean oldAbort = false;
//		System.err.println("--------------");

		while(curr != null) {
//			System.err.println("stamp = " + curr.stamp + " parent = " + curr.parentStamp);
			if(curr.parentStamp == oldStamp && curr.parentOwner.equals(oldOwner)) {
				System.out.print("+");
				// great, my parent is above me on the stack.
				// if it was to be aborted, so am I.
				if(oldAbort) {
					if(ABORT_DEBUG) {
						System.out.println("found local child: " + curr.stamp + ", it depends on " + targetStamp);
					}
					
					curr.spawnCounter.value--;
					if(ASSERTS && curr.spawnCounter.value < 0) {
						System.out.println("Just made spawncounter < 0");
						new Exception().printStackTrace();
						System.exit(1);
					}
					if(ABORT_STATS) {
						satin.abortedJobs++;
					}
					removeElement(curr);
				}
			} else {
				if(Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
					if(ABORT_DEBUG) {
						System.out.println("found local child: " + curr.stamp + ", it depends on " + targetStamp);
					}
					
					curr.spawnCounter.value--;
					if(ASSERTS && curr.spawnCounter.value < 0) {
						System.out.println("Just made spawncounter < 0");
						new Exception().printStackTrace();
						System.exit(1);
					}
					if(ABORT_STATS) {
						satin.abortedJobs++;
					}
					removeElement(curr);
					oldAbort = true;
				} else {
					oldAbort = false;
				}
			}
			oldStamp = curr.stamp;
			oldOwner = curr.owner;
			curr = curr.qnext;
		}
	}

	// hold the satin lock
	void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = tail;
		while(curr != null) {
			if((curr.parent != null && curr.parent.aborted) ||
			   Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
//				if(curr.parent != null && curr.parent.aborted) System.err.print("!");

				if(ABORT_DEBUG) {
					System.out.println("found local child: " + curr.stamp + ", it depends on " + targetStamp);
				}
				
				curr.spawnCounter.value--;
				if(ASSERTS && curr.spawnCounter.value < 0) {
					System.out.println("Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
				if(ABORT_STATS) {
					satin.abortedJobs++;
				}
				curr.aborted = true;
				removeElement(curr); // put it in the cache
			}
			
			curr = curr.qprev;
		}
/*
		curr = head;
		while(curr != null) {
			if(curr.aborted) removeElement(curr);
			// @@@ put it in the cache!!!
			curr = curr.qnext;
		}
*/
	}
}


