package ibis.satin;

// warning! this does not work correctly in combination with aborts!
// This is because we have to remove elements from random places in the queue
// is this true???

public final class DEQueueDijkstra implements Config {

	private static final int START_SIZE=5000;

	private InvocationRecord[] l = new InvocationRecord[START_SIZE];
	private volatile int size = START_SIZE;
	private volatile int head;
	private volatile int tail;
	private Satin satin;

	DEQueueDijkstra(Satin satin) {
		this.satin = satin;
	}

	void addToHead(InvocationRecord r) {
		if(head == size) {
			System.out.println("doubling DEq, new size = " + (size*2));
			
			synchronized(satin) {
				size *= 2;
				InvocationRecord[] nl = new InvocationRecord[size];
				System.arraycopy(l, 0, nl, 0, l.length);
				l = nl;
			}
		}
		
		l[head] = r;
		head++;
	}

	InvocationRecord getFromHead() {
		head--;
		if(head < tail) {
			head++;
			synchronized(satin) {
				head--;
				if(head < tail) {
					head++;
					return null;
				}
			}
		}

		// success
		return l[head];
	}

	synchronized InvocationRecord getFromTail() {
		tail++;
		if(head < tail) {
			tail--;
			return null;
		}

		return l[tail-1];
	}

	void removeElement(int i) {
		l[i] = l[head-1];
		head--;
	}

	boolean contains(InvocationRecord r) {
		for(int i=tail; i<head; i++) {
			InvocationRecord curr = l[i];
			if(curr.equals(r)) return true;
		}

		return false;
	}

	void removeJob(int index) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		for (int i=index+1; i<head; i++) {
			l[i-1] = l[i];
		}

		head--;
	}

	/* hold the satin lock here! */
	void killChildrenOf(int targetStamp, ibis.ipl.IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		for(int i=tail; i<head; i++) {
			InvocationRecord curr = l[i];
			if(Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
				if(ABORT_DEBUG) {
					System.out.println("found local child: " + curr.stamp + ", it depends on " + targetStamp);
				}

				curr.aborted = true;
				if(ABORT_STATS) {
					satin.abortedJobs++;
				}
				curr.spawnCounter.value--;
				if(ASSERTS && curr.spawnCounter.value < 0) {
					System.out.println("Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
				removeJob(i);
				i--;
//				head--;
//				l[i] = l[head];
//				i--;
			}
		}
	}
}

