package ibis.satin;

import ibis.ipl.*;

final class IRVector implements Config {
	InvocationRecord[] l = new InvocationRecord[500];
	int count=0;
	Satin satin;

	IRVector(Satin s) {
		this.satin = s;
	}

	void add(InvocationRecord r) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		if(count >= l.length) {
			InvocationRecord[] nl = new InvocationRecord[l.length*2];
			System.arraycopy(l, 0, nl, 0, l.length);
			l = nl;
		}

		l[count] = r;
		count++;
	}

	int size() {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		return count;
	}

	InvocationRecord remove(int stamp, ibis.ipl.IbisIdentifier owner) {
		InvocationRecord res = null;

		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		for(int i=0; i<count; i++) {
			if(l[i].stamp == stamp && l[i].owner.equals(owner)) {
				res = l[i];
				count--;
				l[i] = l[count];
				return res;
			}
		}

		return null;
	}

	void remove(InvocationRecord r) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		for(int i=count-1; i>=0; i--) {
			if(l[i].equals(r)) {
				count--;
				l[i] = l[count];
				return;
			}
		}

		System.err.println("EEK, IRVector: removeing non-existant elt: " + r);
	}

	void killChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		InvocationRecord curr;
		for(int i=0; i<count; i++) {
			curr = l[i];
			if((curr.parent != null && curr.parent.aborted) || 
			   satin.isDescendentOf(curr, targetStamp, targetOwner)) {
				curr.aborted = true;
				if(ABORT_DEBUG) {
					System.out.println("found stolen child: " + curr.stamp + ", it depends on " + targetStamp);
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
				if(STEAL_STATS) {
					satin.abortMessages++;
				}
				removeIndex(i);
				i--;
				satin.sendAbortMessage(curr);
			}
		}
	}


	InvocationRecord removeIndex(int i) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		if(i >= count) return null;

		InvocationRecord res = l[i];
		count--;
		l[i] = l[count];
		return res;
	}

	void print(java.io.PrintStream out) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		out.println("==============IRVector:=============");
		for(int i=0; i<count; i++) {
			out.println("elt [" + i + "] = " + l[i]);
		}
		out.println("=========end of IRVector:===========");
	}
}
