package ibis.satin;

// No need to delete aborted invocation records, the spawner keeps an
// outstandingJobs list.

public final class DEQueueNormal extends DEQueue implements Config {
	private InvocationRecord head = null;
	private InvocationRecord tail = null;
	private int length = 0;
	private Satin satin;

	public DEQueueNormal(Satin satin) {
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
		if(length > 10000) {
			System.err.println("LARGE Q");
		}
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
		if(length > 10000) {
			System.err.println("LARGE Q");
		}
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

	private void removeElement(InvocationRecord curr) { // curr MUST be in q.
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

	private int size() {
		synchronized(satin) {
			return length;
		}
	}

	private void print(java.io.PrintStream out) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		out.println("work queue: " + length + " elements");
		InvocationRecord curr = head;
		while(curr != null) {
			System.out.println("    " + curr);
			curr = curr.qnext;
		}
	}

	public void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = tail;
		while(curr != null) {
			if((curr.parent != null && curr.parent.aborted) ||
			   Satin.isDescendentOf(curr, targetStamp, targetOwner)) {

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
				removeElement(curr);
			}

			curr = curr.qprev;
		}
	}
}



