package ibis.satin;
import ibis.ipl.IbisIdentifier;

/** A stack of invocation records. */

final class IRStack implements Config {
	private InvocationRecord[] l = new InvocationRecord[500];
	private int count=0;
	Satin s;

	IRStack(Satin s) {
		this.s = s;
	}

	int size() {
		return count;
	}

	boolean contains(InvocationRecord r) {
		InvocationRecord curr;

		for(int i=0; i<count; i++) {
			curr = l[i];
			if(curr.equals(r)) return true;
		}

		return false;
	}

	void push(InvocationRecord r) {
		if(count >= l.length) {
			InvocationRecord[] nl = new InvocationRecord[l.length*2];
			System.arraycopy(l, 0, nl, 0, l.length);
			l = nl;
		}

		l[count] = r;
		count++;
	}

	void pop() {
		if(ASSERTS && count <= 0) {
			System.err.println("popping from empty IR stack!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		count--;
	}

        /* Observation: mostly, the jobs on the stack are children of eachother.
	   In the normal case, there is no need to run isDescendentOf all the time. */
	void oldkillChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for(int i=0; i<count; i++) {
			curr = l[i];
			if(Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
				curr.aborted = true;
				s.abortedJobs++;

				if(ABORT_DEBUG) {
					System.out.println("found child on the stack: " + curr.stamp + 
							   ", it depends on " + targetStamp);
				}
			}
		}
	}

	void killChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for(int i=0; i<count; i++) {
			curr = l[i];

			if((curr.parent != null && curr.parent.aborted) || 
			   Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
//				if(curr.parent != null && curr.parent.aborted) System.err.print("#");

				curr.aborted = true;
				s.abortedJobs++;

				if(ABORT_DEBUG) {
					System.out.println("found child on the stack: " + curr.stamp + 
							   ", it depends on " + targetStamp);
				}
			}
		}
	}

	void print(java.io.PrintStream out) {
		out.println("==============IRStack:=============");
		for(int i=0; i<count; i++) {
			out.println("elt [" + i + "] = " + l[i]);
		}
		out.println("=========end of IRStack:===========");
	}
}
