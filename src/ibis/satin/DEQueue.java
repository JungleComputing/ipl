package ibis.satin;

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
	void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		InvocationRecord curr = head;
		while(curr != null) {
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
			}

			curr = curr.qnext;
		}
	}
}
